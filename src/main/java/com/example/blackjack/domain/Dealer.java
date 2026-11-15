package com.example.blackjack.domain;

// ディーラーを表すクラス
// ルール：合計が17以上になるまで必ず引く（ソフト17でも止まる）
public class Dealer extends Participant {

    public static final int STAND_LIMIT = 17;

    public Dealer() {
        super("ディーラー");
    }

    // 表向きのカード（アップカード）＝最初に配られた1枚目
    public Card getUpCard() {
        return hand.getCards().get(0);
    }

    // ディーラーの番：17以上になるまで引き続ける
    public void playTurn(Deck deck) {
        while (hand.getTotal() < STAND_LIMIT) {
            receive(deck.draw());
        }
    }

    @Override
    public boolean isDealer() {
        return true;
    }
}
