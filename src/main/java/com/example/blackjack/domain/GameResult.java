package com.example.blackjack.domain;

// 1ゲームの勝敗結果（gamesテーブルの result 列には name() の文字列が入る）
public enum GameResult {
    BLACKJACK("ブラックジャック！", "BJ"), // 最初の2枚で21（配当1.5倍）
    WIN("勝ち", "WIN"),
    PUSH("プッシュ（引き分け）", "PUSH"),
    LOSE("負け", "LOSE"),
    BUST("バスト（負け）", "BUST"),        // 21を超えて負け
    SURRENDER("サレンダー", "SUR");        // 降参（賭け金の半分が戻る）

    private final String label; // 結果メッセージ（G7 ③）
    private final String tag;   // 席の横に出す短いタグ（G7 ②、G13）

    GameResult(String label, String tag) {
        this.label = label;
        this.tag = tag;
    }

    public String getLabel() {
        return label;
    }

    public String getTag() {
        return tag;
    }

    // 勝ちに数える結果か（連勝カウント用）
    public boolean isWin() {
        return this == WIN || this == BLACKJACK;
    }

    // 負けに数える結果か（連勝リセット用）
    public boolean isLoss() {
        return this == LOSE || this == BUST || this == SURRENDER;
    }
}
