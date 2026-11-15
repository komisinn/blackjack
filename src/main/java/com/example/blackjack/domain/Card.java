package com.example.blackjack.domain;

// トランプ1枚を表すクラス
// 一度作ったカードはマーク・数字が変わらないので、フィールドは final にしている
public class Card {

    private final String suit; // マーク："♠" "♥" "♦" "♣"
    private final String rank; // 数字："A" "2"〜"10" "J" "Q" "K"

    public Card(String suit, String rank) {
        this.suit = suit;
        this.rank = rank;
    }

    // ブラックジャックでの点数を返す
    // A は 11 として返す（1として数えるかどうかは Hand クラスが判断する）
    public int getValue() {
        switch (rank) {
            case "A":
                return 11;
            case "J":
            case "Q":
            case "K":
                return 10;
            default:
                return Integer.parseInt(rank); // "2"〜"10" は数字に変換
        }
    }

    // エースかどうか
    public boolean isAce() {
        return rank.equals("A");
    }

    // 赤いマーク（♥ ♦）かどうか（画面の色分け用）
    public boolean isRed() {
        return suit.equals("♥") || suit.equals("♦");
    }

    public String getSuit() {
        return suit;
    }

    public String getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return suit + rank;
    }
}
