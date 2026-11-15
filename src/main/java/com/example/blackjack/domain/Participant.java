package com.example.blackjack.domain;

// テーブルに座る参加者の共通部分（抽象クラス）
// プレイヤーもディーラーも「名前」と「手札」を持つので、ここにまとめる
// abstract なので new Participant() とは書けず、必ず子クラス（Player / Dealer）を使う
public abstract class Participant {

    protected final String name;
    protected final Hand hand = new Hand();

    protected Participant(String name) {
        this.name = name;
    }

    // カードを1枚受け取る
    public void receive(Card card) {
        hand.add(card);
    }

    public String getName() {
        return name;
    }

    public Hand getHand() {
        return hand;
    }

    // ディーラーかどうか（子クラスで答えを決める）
    public abstract boolean isDealer();
}
