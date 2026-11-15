package com.example.blackjack.domain;

// プレイヤーが選べる行動の種類（列挙型 enum）
// 文字列で "HIT" などと書くとタイプミスが起きるので、enum で決まった値だけを使えるようにする
public enum Action {
    HIT("ヒット"),          // もう1枚引く
    STAND("スタンド"),      // これで勝負する
    DOUBLE("ダブル"),       // 賭け金を2倍にして、1枚だけ引いて終了
    SURRENDER("サレンダー"); // 降参して賭け金の半分を返してもらう

    private final String label; // 画面に表示する日本語名

    Action(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
