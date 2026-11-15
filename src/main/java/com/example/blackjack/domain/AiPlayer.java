package com.example.blackjack.domain;

// 同じテーブルに座るAIプレイヤー
// ルールは単純：合計が17以上になるまでヒットし、17以上になったらスタンドする
// （ダブル・サレンダーはしない）
public class AiPlayer extends Player {

    public static final int AI_BET = 25;     // AIは毎回25チップ賭ける
    public static final int STAND_LIMIT = 17; // この点数以上になったら引くのをやめる

    private int chips; // AIの所持チップ（画面に表示するだけで、DBには保存しない）

    public AiPlayer(String name, int chips) {
        super(name);
        this.chips = chips;
        setBet(AI_BET);
    }

    // 自分の番を最後まで自動でプレイする
    public void playTurn(Deck deck) {
        while (!isDone()) {
            if (hand.getTotal() < STAND_LIMIT) {
                hit(deck.draw()); // バストか21になると hit() の中で自動的に番が終わる
            } else {
                stand();
            }
        }
    }

    @Override
    public boolean isAi() {
        return true;
    }

    public int getChips() { return chips; }
    public void setChips(int chips) { this.chips = chips; }
}
