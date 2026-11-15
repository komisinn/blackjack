package com.example.blackjack.domain;

// プレイヤー（あなた）を表すクラス
public class Player extends Participant {

    private int bet;                  // 最初に賭けた額
    private boolean doubled = false;  // ダブルしたか
    private boolean surrendered = false; // サレンダーしたか
    private boolean done = false;     // 自分の番が終わったか
    private GameResult result;        // 勝敗（ゲーム終了時に入る）

    public Player(String name) {
        super(name);
    }

    // ヒット：1枚引く。バーストまたは21になったら自動で番を終える
    public void hit(Card card) {
        receive(card);
        if (hand.isBust() || hand.getTotal() == 21) {
            done = true;
        }
    }

    // スタンド：番を終える
    public void stand() {
        done = true;
    }

    // ダブル：賭け金2倍、1枚だけ引いて番を終える
    public void doubleDown(Card card) {
        doubled = true;
        receive(card);
        done = true;
    }

    // サレンダー：降参して番を終える
    public void surrender() {
        surrendered = true;
        done = true;
    }

    // 実際に賭かっている額（ダブルなら2倍）
    public int getTotalBet() {
        return doubled ? bet * 2 : bet;
    }

    // AIプレイヤーかどうか（子クラス AiPlayer で true を返す）
    public boolean isAi() {
        return false;
    }

    @Override
    public boolean isDealer() {
        return false;
    }

    // --- ゲッター・セッター ---
    public int getBet() { return bet; }
    public void setBet(int bet) { this.bet = bet; }
    public boolean isDoubled() { return doubled; }
    public boolean isSurrendered() { return surrendered; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
    public GameResult getResult() { return result; }
    public void setResult(GameResult result) { this.result = result; }
}
