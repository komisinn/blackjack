package com.example.blackjack.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 山札（デッキ）を表すクラス
// カジノと同じく、複数組のトランプを混ぜて使う
public class Deck {

    private static final String[] SUITS = {"♠", "♥", "♦", "♣"};
    private static final String[] RANKS = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};

    private final int numberOfDecks;           // 何組のトランプを使うか
    private final List<Card> cards = new ArrayList<>();

    public Deck(int numberOfDecks) {
        this.numberOfDecks = numberOfDecks;
        refill();
    }

    // 山札を作り直してシャッフルする
    private void refill() {
        cards.clear();
        for (int d = 0; d < numberOfDecks; d++) {   // 組数分くり返す
            for (String suit : SUITS) {             // 4マーク
                for (String rank : RANKS) {         // 13種類
                    cards.add(new Card(suit, rank));
                }
            }
        }
        Collections.shuffle(cards); // ランダムに並び替える
    }

    // 一番上のカードを1枚引く
    // 山札が空になったら作り直す（1ゲーム中に尽きることはまず無いが念のため）
    public Card draw() {
        if (cards.isEmpty()) {
            refill();
        }
        return cards.remove(cards.size() - 1); // 末尾を取り出すほうが高速
    }

    // 残り枚数
    public int remaining() {
        return cards.size();
    }
}
