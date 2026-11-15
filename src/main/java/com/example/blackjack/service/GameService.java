package com.example.blackjack.service;

import com.example.blackjack.domain.*;
import com.example.blackjack.entity.Game;
import com.example.blackjack.entity.User;
import com.example.blackjack.repository.GameRepository;
import com.example.blackjack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// ゲームの進行・勝敗判定・チップ精算を担当するクラス（アプリの心臓部）
//
// 1ゲームの流れ：
//   start()  … ベットを受け取り、カードを配る。AI1 がプレイする
//   act()    … あなたの行動（ヒット等）を処理。番が終わったら finishRound() へ
//   finishRound() … AI2・AI3・ディーラーがプレイ → 勝敗判定 → チップ精算 → DB保存
//
// カードを配るのも勝敗を決めるのも、すべてサーバー側（このクラス）で行う
@Service
public class GameService {

    public static final int MIN_BET = 1;         // 最低ベット額（設計書：1以上、所持チップ以下）
    public static final int NUMBER_OF_DECKS = 6; // 使うトランプの組数
    public static final int BONUS_3_WINS = 1000; // 3連勝ボーナス
    public static final int BONUS_5_WINS = 2000; // 5連勝ボーナス
    public static final int STREAK_MIN_BET = 100; // この額以上を賭けて勝った時だけ連勝数が増える
    public static final int AI_INITIAL_CHIPS = 1000; // AIの初期チップ

    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    public GameService(UserRepository userRepository, GameRepository gameRepository) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
    }

    // =========================================================
    // ゲーム開始（ベット確定 → 配札）
    //   aiChips … AI1〜AI3 の所持チップ（セッションで覚えておく。精算後にこの配列を書き換える）
    // =========================================================
    @Transactional
    public GameState start(Long userId, int bet, int[] aiChips) {
        User user = loadUser(userId);

        // --- ベット額のチェック ---
        if (bet < MIN_BET) {
            throw new IllegalArgumentException("賭け金を決めてください");
        }
        if (bet > user.getChips()) {
            throw new IllegalArgumentException("チップが足りません（所持: " + user.getChips() + "）");
        }

        // --- テーブルの準備 ---
        // AIのチップが賭け金に足りなくなっていたら初期額に戻す
        for (int i = 0; i < aiChips.length; i++) {
            if (aiChips[i] < AiPlayer.AI_BET) {
                aiChips[i] = AI_INITIAL_CHIPS;
            }
        }
        Deck deck = new Deck(NUMBER_OF_DECKS);
        Dealer dealer = new Dealer();
        AiPlayer ai1 = new AiPlayer("AI1", aiChips[0]);
        Player you = new Player("You");
        AiPlayer ai2 = new AiPlayer("AI2", aiChips[1]);
        AiPlayer ai3 = new AiPlayer("AI3", aiChips[2]);
        you.setBet(bet);

        // ★賭け金はこの時点で所持チップから引いておく
        //   （決着の前にログアウトしても、賭けた分は戻らないようにするため）
        user.setChips(user.getChips() - bet);
        userRepository.save(user);
        List<Player> seats = List.of(ai1, you, ai2, ai3); // 座席順

        // --- 配札：全員に1枚ずつ、それを2周（実際のカジノと同じ順番） ---
        for (int round = 0; round < 2; round++) {
            for (Player p : seats) {
                p.receive(deck.draw());
            }
            dealer.receive(deck.draw());
        }

        GameState state = new GameState(deck, dealer, seats, you);

        // ディーラーがブラックジャックなら、誰も行動せずに即決着
        if (dealer.getHand().isBlackjack()) {
            finishRound(user, state, aiChips);
            return state;
        }

        // あなたより前の席の AI1 がプレイ
        ai1.playTurn(deck);

        // あなたがブラックジャックなら行動不要 → そのまま決着
        if (you.getHand().isBlackjack()) {
            you.setDone(true);
            finishRound(user, state, aiChips);
        }
        return state;
    }

    // =========================================================
    // あなたの行動を処理する
    // =========================================================
    @Transactional
    public GameState act(Long userId, GameState state, Action action, int[] aiChips) {
        if (state.isFinished()) {
            throw new IllegalStateException("このゲームはすでに終了しています");
        }
        User user = loadUser(userId);
        Player you = state.getYou();

        // --- その行動ができるかチェック ---
        if (action == Action.DOUBLE && !canDouble(state, user)) {
            throw new IllegalArgumentException("今はダブルできません");
        }
        if (action == Action.SURRENDER && !canSurrender(state)) {
            throw new IllegalArgumentException("サレンダーは最初の2枚の時だけできます");
        }

        // --- 行動を実行 ---
        Deck deck = state.getDeck();
        switch (action) {
            case HIT -> you.hit(deck.draw());
            case STAND -> you.stand();
            case DOUBLE -> {
                // ダブルは賭け金をもう1回分追加するので、その分も先に引く
                user.setChips(user.getChips() - you.getBet());
                userRepository.save(user);
                you.doubleDown(deck.draw());
            }
            case SURRENDER -> you.surrender();
        }

        // あなたの番が終わったら、残りの人とディーラーを進めて決着
        if (you.isDone()) {
            finishRound(user, state, aiChips);
        }
        return state;
    }

    // ダブルできる条件：自分の番 & 最初の2枚 & 追加の賭け金（最初と同じ額）を払える
    public boolean canDouble(GameState state, User user) {
        Player you = state.getYou();
        return !state.isFinished() && you.getHand().size() == 2
                && user.getChips() >= you.getBet();
    }

    // サレンダーできる条件：自分の番 & 最初の2枚
    public boolean canSurrender(GameState state) {
        return !state.isFinished() && state.getYou().getHand().size() == 2;
    }

    // =========================================================
    // 決着処理
    // =========================================================
    private void finishRound(User user, GameState state, int[] aiChips) {
        Dealer dealer = state.getDealer();
        Deck deck = state.getDeck();

        // ディーラーがブラックジャックの時は、誰もこれ以上引かない
        if (!dealer.getHand().isBlackjack()) {
            // まだ番が終わっていないAI（あなたより後ろの席）がプレイ
            for (Player p : state.getSeats()) {
                if (p instanceof AiPlayer ai && !ai.isDone()) {
                    ai.playTurn(deck);
                }
            }
            // ディーラーがプレイ
            dealer.playTurn(deck);
        }

        // 全員の勝敗を判定。AIはチップも増減させる
        int aiIndex = 0;
        for (Player p : state.getSeats()) {
            p.setResult(judge(p, dealer));
            if (p instanceof AiPlayer ai) {
                ai.setChips(Math.max(0, ai.getChips() + calcNet(ai.getResult(), ai)));
                aiChips[aiIndex++] = ai.getChips(); // 次のゲームに持ち越す
            }
        }

        // --- あなたのチップ精算 ---
        Player you = state.getYou();
        GameResult result = you.getResult();
        int net = calcNet(result, you);

        // 連勝ボーナス
        //   ・賭け金が100以上のゲームで勝つと連勝数+1（少額の賭けでボーナスだけ稼げないようにするため）
        //   ・賭け金が100未満の勝ちは、連勝数を変えない
        //   ・負けたら賭け金に関係なく0に戻る。引き分けはそのまま
        int streak = user.getStreak();
        int bonus = 0;
        if (result.isWin() && you.getBet() >= STREAK_MIN_BET) {
            streak++;
            if (streak == 3) {
                bonus = BONUS_3_WINS;
            } else if (streak == 5) {
                bonus = BONUS_5_WINS;
            }
        } else if (result.isLoss()) {
            streak = 0;
        }

        // 賭け金は開始時に引いてあるので、まず賭けた分を戻してから増減（net）とボーナスを足す
        //   例：100賭けて勝ち  → 100戻す + 100 = 200 増える（開始時から見ると +100）
        //       100賭けて負け  → 100戻す - 100 =   0 増える（開始時から見ると -100）
        int chipsAfter = Math.max(0, user.getChips() + you.getTotalBet() + net + bonus);
        user.setChips(chipsAfter);
        user.setStreak(streak);
        userRepository.save(user);

        // --- DBへ保存（games） ---
        Game game = new Game();
        game.setUser(user);
        game.setBet(you.getBet());
        game.setDoubled(you.isDoubled());
        game.setResult(result.name());
        game.setPlayerTotal(you.getHand().getTotal());
        game.setDealerTotal(dealer.getHand().getTotal());
        game.setNet(net);
        game.setBonus(bonus);
        game.setChipsAfter(chipsAfter);
        gameRepository.save(game);

        state.finish(result, net, bonus, chipsAfter, streak, game.getGameId());
    }

    // 1人分の勝敗を判定する
    private GameResult judge(Player p, Dealer dealer) {
        Hand ph = p.getHand();
        Hand dh = dealer.getHand();

        if (p.isSurrendered()) return GameResult.SURRENDER;
        if (ph.isBust()) return GameResult.BUST;           // 自分がバストしたらディーラーに関係なく負け
        if (ph.isBlackjack() && dh.isBlackjack()) return GameResult.PUSH;
        if (ph.isBlackjack()) return GameResult.BLACKJACK;
        if (dh.isBlackjack()) return GameResult.LOSE;
        if (dh.isBust()) return GameResult.WIN;
        if (ph.getTotal() > dh.getTotal()) return GameResult.WIN;
        if (ph.getTotal() < dh.getTotal()) return GameResult.LOSE;
        return GameResult.PUSH;
    }

    // 結果からチップの増減を計算する
    private int calcNet(GameResult result, Player p) {
        int bet = p.getBet();
        int totalBet = p.getTotalBet(); // ダブルなら2倍
        return switch (result) {
            case BLACKJACK -> bet * 3 / 2; // 1.5倍（端数切り捨て）
            case WIN -> totalBet;
            case PUSH -> 0;
            case LOSE, BUST -> -totalBet;
            case SURRENDER -> -(bet / 2);  // 半分だけ失う
        };
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません"));
    }

    // =========================================================
    // 成績（G4・G8）と結果ログ（G6 ⑨）
    // =========================================================
    public StatsSummary getStats(User user) {
        long wins = gameRepository.countByUserAndResult(user, "WIN")
                + gameRepository.countByUserAndResult(user, "BLACKJACK");
        return new StatsSummary(gameRepository.countByUser(user), wins, gameRepository.sumNetByUser(user));
    }

    // チップ推移グラフ用：直近30ゲームの「精算後チップ」を古い順に並べる
    public List<Integer> getChipHistory(User user) {
        List<Game> games = gameRepository.findTop30ByUserOrderByPlayedAtDesc(user); // 新しい順で届く
        List<Integer> history = new ArrayList<>();
        for (int i = games.size() - 1; i >= 0; i--) { // 後ろから読んで古い順にする
            history.add(games.get(i).getChipsAfter());
        }
        return history;
    }

    // 直近3ゲームの結果を "you win 100" の形にする
    public List<String> getResultLog(User user) {
        List<String> lines = new ArrayList<>();
        for (Game g : gameRepository.findTop3ByUserOrderByPlayedAtDesc(user)) {
            int amount = g.getNet() + g.getBonus();
            if (amount > 0) {
                lines.add("you win " + amount);
            } else if (amount < 0) {
                lines.add("you lose " + (-amount));
            } else {
                lines.add("you push");
            }
        }
        return lines;
    }
}
