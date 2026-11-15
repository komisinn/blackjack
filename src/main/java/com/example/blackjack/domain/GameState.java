package com.example.blackjack.domain;

import java.util.List;

// 1ゲーム分の「今の状態」をまとめたクラス
// プレイ中はHTTPセッションに保存しておき、ボタンが押されるたびに取り出して更新する
public class GameState {

    // ゲームの進行段階
    public enum Phase {
        PLAYER_TURN, // あなたの番
        FINISHED     // 決着済み
    }

    private final Deck deck;
    private final Dealer dealer;
    private final List<Player> seats; // 座席順：AI 1 → あなた → AI 2 → AI 3
    private final Player you;
    private Phase phase = Phase.PLAYER_TURN;
    private String message = "";

    // --- 決着後に入る値 ---
    private GameResult result;
    private int net;         // チップの増減
    private int bonus;       // 連勝ボーナス
    private int chipsAfter;  // 精算後のチップ
    private int streakAfter; // 精算後の連勝数
    private Long gameId;     // DBに保存したゲームID

    public GameState(Deck deck, Dealer dealer, List<Player> seats, Player you) {
        this.deck = deck;
        this.dealer = dealer;
        this.seats = seats;
        this.you = you;
    }

    // 決着させて結果をセットする
    public void finish(GameResult result, int net, int bonus, int chipsAfter, int streakAfter, Long gameId) {
        this.phase = Phase.FINISHED;
        this.result = result;
        this.net = net;
        this.bonus = bonus;
        this.chipsAfter = chipsAfter;
        this.streakAfter = streakAfter;
        this.gameId = gameId;
    }

    public boolean isFinished() {
        return phase == Phase.FINISHED;
    }

    // --- ゲッター・セッター ---
    public Deck getDeck() { return deck; }
    public Dealer getDealer() { return dealer; }
    public List<Player> getSeats() { return seats; }
    public Player getYou() { return you; }
    public Phase getPhase() { return phase; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public GameResult getResult() { return result; }
    public int getNet() { return net; }
    public int getBonus() { return bonus; }
    public int getChipsAfter() { return chipsAfter; }
    public int getStreakAfter() { return streakAfter; }
    public Long getGameId() { return gameId; }
}
