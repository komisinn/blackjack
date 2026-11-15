package com.example.blackjack.service;

// 成績の集計結果をまとめたクラス（G4 ホーム・G8 戦績で表示する）
public class StatsSummary {

    private final long totalGames; // プレイ数
    private final long wins;       // 勝ち数（WIN + BLACKJACK）
    private final long netTotal;   // チップ収支の合計

    public StatsSummary(long totalGames, long wins, long netTotal) {
        this.totalGames = totalGames;
        this.wins = wins;
        this.netTotal = netTotal;
    }

    // 勝率（%）＝ 勝ち数 ÷ プレイ数。0除算を避けるため、プレイ数0なら0を返す
    public int getWinRate() {
        return totalGames == 0 ? 0 : (int) Math.round(wins * 100.0 / totalGames);
    }

    // 収支を「+450」「-200」のように符号つきの文字にする
    public String getNetLabel() {
        return (netTotal > 0 ? "+" : "") + String.format("%,d", netTotal);
    }

    public long getTotalGames() { return totalGames; }
    public long getWins() { return wins; }
    public long getNetTotal() { return netTotal; }
}
