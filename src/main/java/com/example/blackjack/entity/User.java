package com.example.blackjack.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// ユーザーテーブルのエンティティ（DB設計書03のusersテーブルに対応）
@Entity
@Table(name = "users")
public class User {

    // ユーザーID（主キー、自動採番）
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    // ユーザー名（最大50文字、必須）
    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    // メールアドレス（一意制約、必須）
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    // パスワード（ハッシュ化して保存、必須）
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    // ロール（"USER" または "ADMIN"）
    @Column(name = "role", nullable = false, length = 10)
    private String role = "USER";

    // チップ残高（初期値1000）
    @Column(name = "chips", nullable = false)
    private Integer chips = 1000;

    // 連勝数（初期値0）
    @Column(name = "streak", nullable = false)
    private Integer streak = 0;

    // 有効フラグ（管理者が利用停止にできる）
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    // 作成日時（INSERT時に自動設定）
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 更新日時（UPDATE時に自動設定）
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // INSERT前に現在日時をセットする
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // UPDATE前に更新日時をセットする
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- ゲッター・セッター ---

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getChips() {
        return chips;
    }

    public void setChips(Integer chips) {
        this.chips = chips;
    }

    public Integer getStreak() {
        return streak;
    }

    public void setStreak(Integer streak) {
        this.streak = streak;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
