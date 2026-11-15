package com.example.blackjack.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// ゲームテーブルのエンティティ（DB設計書03のgamesテーブルに対応）
@Entity
@Table(name = "games")
public class Game {

    // ゲームID（主キー、自動採番）
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id")
    private Long gameId;

    // どのユーザーのゲームか（外部キー → users.user_id）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ベット額
    @Column(name = "bet", nullable = false)
    private Integer bet;

    // ダブルしたかどうか
    @Column(name = "doubled", nullable = false)
    private Boolean doubled = false;

    // 結果（"WIN", "LOSE", "PUSH", "BLACKJACK", "SURRENDER", "BUST"）
    @Column(name = "result", nullable = false, length = 10)
    private String result;

    // プレイヤーの最終手札合計
    @Column(name = "player_total", nullable = false)
    private Integer playerTotal;

    // ディーラーの最終手札合計
    @Column(name = "dealer_total", nullable = false)
    private Integer dealerTotal;

    // チップの増減（+なら勝ち、-なら負け）
    @Column(name = "net", nullable = false)
    private Integer net;

    // 連勝ボーナス額
    @Column(name = "bonus", nullable = false)
    private Integer bonus = 0;

    // ゲーム後のチップ残高
    @Column(name = "chips_after", nullable = false)
    private Integer chipsAfter;

    // プレイ日時
    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;

    // INSERT前に現在日時をセット
    @PrePersist
    protected void onCreate() {
        this.playedAt = LocalDateTime.now();
    }

    // --- ゲッター・セッター ---

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getBet() {
        return bet;
    }

    public void setBet(Integer bet) {
        this.bet = bet;
    }

    public Boolean getDoubled() {
        return doubled;
    }

    public void setDoubled(Boolean doubled) {
        this.doubled = doubled;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getPlayerTotal() {
        return playerTotal;
    }

    public void setPlayerTotal(Integer playerTotal) {
        this.playerTotal = playerTotal;
    }

    public Integer getDealerTotal() {
        return dealerTotal;
    }

    public void setDealerTotal(Integer dealerTotal) {
        this.dealerTotal = dealerTotal;
    }

    public Integer getNet() {
        return net;
    }

    public void setNet(Integer net) {
        this.net = net;
    }

    public Integer getBonus() {
        return bonus;
    }

    public void setBonus(Integer bonus) {
        this.bonus = bonus;
    }

    public Integer getChipsAfter() {
        return chipsAfter;
    }

    public void setChipsAfter(Integer chipsAfter) {
        this.chipsAfter = chipsAfter;
    }

    public LocalDateTime getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(LocalDateTime playedAt) {
        this.playedAt = playedAt;
    }
}
