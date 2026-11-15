package com.example.blackjack.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 手札を表すクラス
// ★ポイント：A（エース）を 1 と 11 のどちらで数えるかを自動で計算する
public class Hand {

    private final List<Card> cards = new ArrayList<>();

    // カードを1枚加える
    public void add(Card card) {
        cards.add(card);
    }

    // 手札の合計点を計算する
    // 考え方：
    //   1. まず A を全部 11 として足す
    //   2. 21 を超えていて、まだ 11 扱いの A があれば、1 枚ずつ 1 扱い（-10）に直す
    public int getTotal() {
        int total = 0;
        int aces = 0; // 11として数えているAの枚数
        for (Card card : cards) {
            total += card.getValue();
            if (card.isAce()) {
                aces++;
            }
        }
        while (total > 21 && aces > 0) {
            total -= 10; // A を 11 → 1 に変更
            aces--;
        }
        return total;
    }

    // ソフトハンドかどうか
    // ＝ A を 11 として数えている状態（次に何を引いてもバーストしない）
    public boolean isSoft() {
        int hardTotal = 0;      // A をすべて 1 として数えた合計
        boolean hasAce = false;
        for (Card card : cards) {
            if (card.isAce()) {
                hardTotal += 1;
                hasAce = true;
            } else {
                hardTotal += card.getValue();
            }
        }
        // A があり、そのうち1枚を11にしても21以下ならソフト
        return hasAce && hardTotal + 10 <= 21;
    }

    // ブラックジャック（最初の2枚で21）かどうか
    public boolean isBlackjack() {
        return cards.size() == 2 && getTotal() == 21;
    }

    // バースト（21超え）かどうか
    public boolean isBust() {
        return getTotal() > 21;
    }

    // 手札の枚数
    public int size() {
        return cards.size();
    }

    // 手札のカード一覧（外から書き換えられないように読み取り専用で返す）
    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }
}
