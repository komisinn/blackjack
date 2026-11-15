package com.example.blackjack.controller;

import com.example.blackjack.domain.*;
import com.example.blackjack.entity.User;
import com.example.blackjack.service.GameService;
import com.example.blackjack.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ベット（G5）・ゲームプレイ（G6）・結果（G7）を担当
//
// プレイ中のゲームの状態（GameState）はHTTPセッションに入れておく。
// G6 のボタン（ヒット等）は Ajax で /game/api/action を呼び、
// 返ってきたJSONを使って JavaScript（game.js）が画面を書き換える。
@Controller
@RequestMapping("/game")
public class GameController {

    // セッションに保存する時の名前
    private static final String STATE = "gameState";       // プレイ中のゲーム
    private static final String AI_CHIPS = "aiChips";       // AI1〜AI3の所持チップ

    private final GameService gameService;
    private final UserService userService;

    public GameController(GameService gameService, UserService userService) {
        this.gameService = gameService;
        this.userService = userService;
    }

    // =========================================================
    // G5 ベット画面
    // =========================================================
    @GetMapping("/bet")
    public String bet(Principal principal, HttpSession session, Model model) {
        GameState state = (GameState) session.getAttribute(STATE);
        if (state != null && !state.isFinished()) {
            return "redirect:/game/play"; // プレイ途中なら続きへ
        }
        session.removeAttribute(STATE); // 終わったゲームは片付ける

        User user = userService.findByEmail(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("aiChips", getAiChips(session));
        model.addAttribute("streakMessage", streakMessage(user.getStreak())); // ② 連勝表示
        return "bet";
    }

    // ② 連勝表示（2連勝以上の時だけ）
    private String streakMessage(int streak) {
        if (streak < 2) {
            return null;
        }
        if (streak < 3) {
            return streak + "連勝中　あと" + (3 - streak) + "勝でボーナス";
        }
        if (streak < 5) {
            return streak + "連勝中　あと" + (5 - streak) + "勝でボーナス";
        }
        return streak + "連勝中";
    }

    // G5 「ベットする」ボタン → カードを配って G6 へ（配った直後に決着したら G7 へ）
    @PostMapping("/start")
    public String start(@RequestParam int bet, Principal principal, HttpSession session,
                        RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(principal.getName());
        try {
            GameState state = gameService.start(user.getUserId(), bet, getAiChips(session));
            session.setAttribute(STATE, state);
            return state.isFinished() ? "redirect:/game/result" : "redirect:/game/play";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/game/bet";
        }
    }

    // =========================================================
    // G6 ゲームプレイ画面
    // =========================================================
    @GetMapping("/play")
    public String play(HttpSession session) {
        GameState state = (GameState) session.getAttribute(STATE);
        if (state == null) {
            return "redirect:/game/bet";
        }
        if (state.isFinished()) {
            return "redirect:/game/result";
        }
        return "play"; // 中身は game.js が /game/api/state を読んで描く
    }

    // 【Ajax】今のゲームの状態をJSONで返す
    @GetMapping("/api/state")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiState(Principal principal, HttpSession session) {
        GameState state = (GameState) session.getAttribute(STATE);
        if (state == null) {
            return ResponseEntity.status(404).body(Map.of("error", "ゲームが開始されていません"));
        }
        User user = userService.findByEmail(principal.getName());
        return ResponseEntity.ok(buildView(state, user));
    }

    // 【Ajax】行動（HIT / STAND / DOUBLE / SURRENDER）を受け取り、結果をJSONで返す
    @PostMapping("/api/action")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiAction(@RequestParam String action,
                                                         Principal principal, HttpSession session) {
        GameState state = (GameState) session.getAttribute(STATE);
        if (state == null) {
            return ResponseEntity.status(404).body(Map.of("error", "ゲームが開始されていません"));
        }
        User user = userService.findByEmail(principal.getName());
        try {
            Action chosen = Action.valueOf(action); // "HIT" → Action.HIT
            gameService.act(user.getUserId(), state, chosen, getAiChips(session));
            session.setAttribute(STATE, state);
            // 精算でチップが変わっているかもしれないので、ユーザーを読み直す
            user = userService.findById(user.getUserId());
            return ResponseEntity.ok(buildView(state, user));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================
    // G7 結果画面
    // =========================================================
    @GetMapping("/result")
    public String result(Principal principal, HttpSession session, Model model) {
        GameState state = (GameState) session.getAttribute(STATE);
        if (state == null) {
            return "redirect:/game/bet";
        }
        if (!state.isFinished()) {
            return "redirect:/game/play";
        }
        User user = userService.findByEmail(principal.getName());
        model.addAttribute("view", buildView(state, user));
        model.addAttribute("state", state);
        return "result";
    }

    // =========================================================
    // セッションの小道具
    // =========================================================
    private int[] getAiChips(HttpSession session) {
        int[] chips = (int[]) session.getAttribute(AI_CHIPS);
        if (chips == null) {
            int c = GameService.AI_INITIAL_CHIPS;
            chips = new int[]{c, c, c};
            session.setAttribute(AI_CHIPS, chips);
        }
        return chips;
    }

    // =========================================================
    // 画面に渡すデータを組み立てる（G6のJSONとG7の表示で共通）
    // ★プレイ中はディーラーの2枚目を「裏向き」とだけ伝え、中身は送らない（のぞき見防止）
    // =========================================================
    private Map<String, Object> buildView(GameState state, User user) {
        boolean finished = state.isFinished();
        Player you = state.getYou();
        Dealer dealer = state.getDealer();

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("finished", finished);
        view.put("bet", you.getTotalBet());
        // プレイ中は「賭けた分を引いた残り」（賭け金は開始時に引いてある）、決着後は精算後のチップ
        view.put("chips", finished ? state.getChipsAfter() : user.getChips());

        // --- ディーラー ---
        Map<String, Object> d = new LinkedHashMap<>();
        List<Map<String, Object>> dealerCards = new ArrayList<>();
        List<Card> dc = dealer.getHand().getCards();
        for (int i = 0; i < dc.size(); i++) {
            boolean hidden = !finished && i == 1; // 2枚目は裏向き
            dealerCards.add(cardView(dc.get(i), hidden));
        }
        d.put("cards", dealerCards);
        int up = dealer.getUpCard().getValue();
        d.put("label", finished ? "合計 " + dealer.getHand().getTotal()
                                : "アップカード " + (up == 11 ? "A" : String.valueOf(up)));
        view.put("dealer", d);

        // --- 各席 ---
        List<Map<String, Object>> seats = new ArrayList<>();
        for (Player p : state.getSeats()) {
            boolean isYou = (p == you);
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("name", p.getName());
            s.put("you", isYou);
            s.put("bet", p.getTotalBet());
            s.put("chips", isYou ? view.get("chips") : ((AiPlayer) p).getChips());
            List<Map<String, Object>> cards = new ArrayList<>();
            for (Card c : p.getHand().getCards()) {
                cards.add(cardView(c, false));
            }
            s.put("cards", cards);
            s.put("total", p.getHand().getTotal());
            s.put("soft", p.getHand().isSoft() && p.getHand().getTotal() < 21);

            // 席の横に出すタグ
            String tag;      // 表示する文字
            String tagClass; // 色を決めるCSSクラス
            if (finished) {
                tag = p.getResult().getTag();
                tagClass = tag;
            } else if (isYou) {
                tag = "あなたの番";
                tagClass = "turn";
            } else if (p.isDone()) {
                tag = p.getHand().isBust() ? "BUST" : "STAND";
                tagClass = tag;
            } else {
                tag = "待機中";
                tagClass = "wait";
            }
            s.put("tag", tag);
            s.put("tagClass", tagClass);
            seats.add(s);
        }
        view.put("seats", seats);

        // --- ボタンを押せるか ---
        view.put("canDouble", gameService.canDouble(state, user));
        view.put("canSurrender", gameService.canSurrender(state));

        // --- ⑨ 結果ログ（直近3ゲーム） ---
        view.put("log", gameService.getResultLog(user));

        // --- 決着後だけ入る値 ---
        if (finished) {
            view.put("result", state.getResult().name());
            view.put("resultLabel", state.getResult().getLabel());
            view.put("net", state.getNet());
            view.put("bonus", state.getBonus());
        }
        return view;
    }

    // カード1枚を画面用のデータにする
    private Map<String, Object> cardView(Card card, boolean hidden) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("hidden", hidden);
        if (!hidden) {
            c.put("rank", card.getRank());
            c.put("suit", card.getSuit());
            c.put("red", card.isRed());
        }
        return c;
    }
}
