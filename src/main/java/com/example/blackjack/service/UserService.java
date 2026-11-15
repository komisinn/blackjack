package com.example.blackjack.service;

import com.example.blackjack.entity.User;
import com.example.blackjack.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// ユーザーに関する処理（登録・プロフィール更新・チップリセット・管理者操作）
// うまくいかない時は IllegalArgumentException を投げ、そのメッセージを画面に表示する
@Service
public class UserService {

    public static final int INITIAL_CHIPS = 1000;      // 初期チップ
    public static final int MIN_PASSWORD_LENGTH = 8;   // パスワードの最低文字数
    public static final int MAX_NAME_LENGTH = 50;      // ユーザー名の最大文字数（画面設計書の入力桁数）
    public static final int MAX_EMAIL_LENGTH = 255;    // メールアドレスの最大文字数
    public static final int MAX_PASSWORD_LENGTH = 64;  // パスワードの最大文字数

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================================================
    // 新規登録（G1）
    // =========================================================
    @Transactional
    public User register(String userName, String email, String password, String passwordConfirm) {
        // --- 入力チェック ---
        if (isBlank(userName) || isBlank(email) || isBlank(password)) {
            throw new IllegalArgumentException("未入力の項目があります");
        }
        if (userName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("ユーザー名は" + MAX_NAME_LENGTH + "文字以内にしてください");
        }
        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("メールアドレスが長すぎます");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("メールアドレスの形式が正しくありません");
        }
        checkPassword(password, passwordConfirm);
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("このメールアドレスはすでに登録されています");
        }

        // --- 保存 ---
        User user = new User();
        user.setUserName(userName.trim());
        user.setEmail(email.trim());
        user.setPassword(passwordEncoder.encode(password)); // ハッシュ化して保存
        user.setRole("USER");
        user.setChips(INITIAL_CHIPS);
        return userRepository.save(user);
    }

    // =========================================================
    // プロフィール更新（G10）
    // メールアドレスは変更不可。パスワードは入力された時だけ変更する
    // =========================================================
    @Transactional
    public void updateProfile(Long userId, String userName, String newPassword, String newPasswordConfirm) {
        if (isBlank(userName)) {
            throw new IllegalArgumentException("ユーザー名を入力してください");
        }
        if (userName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("ユーザー名は" + MAX_NAME_LENGTH + "文字以内にしてください");
        }
        User user = findById(userId);
        user.setUserName(userName.trim());

        // 新しいパスワードが空欄なら、パスワードは変えない
        if (!isBlank(newPassword) || !isBlank(newPasswordConfirm)) {
            checkPassword(newPassword, newPasswordConfirm);
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        userRepository.save(user);
    }

    // パスワードの共通チェック（8文字以上・確認用と一致）
    private void checkPassword(String password, String confirm) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("パスワードは" + MIN_PASSWORD_LENGTH + "文字以上にしてください");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("パスワードは" + MAX_PASSWORD_LENGTH + "文字以内にしてください");
        }
        if (!password.equals(confirm)) {
            throw new IllegalArgumentException("パスワードが一致しません");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // =========================================================
    // 検索・取得
    // =========================================================
    // メールアドレスで取得（ログイン中のユーザーを探す時に使う）
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません"));
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません"));
    }

    // チップランキング（G8）：一般ユーザーをチップの多い順に
    public List<User> getChipRanking() {
        return userRepository.findByRoleOrderByChipsDescUserIdAsc("USER");
    }

    // =========================================================
    // チップリセット（G4 ⑨）：残高が0の時だけ1000に戻す（連勝も0に。戦績は残す）
    // =========================================================
    @Transactional
    public void resetChips(Long userId) {
        User user = findById(userId);
        if (user.getChips() > 0) {
            throw new IllegalArgumentException("チップが残っている間はリセットできません");
        }
        user.setChips(INITIAL_CHIPS);
        user.setStreak(0);
        userRepository.save(user);
    }

    // =========================================================
    // 管理者用（G12）
    // =========================================================
    // ユーザー一覧。検索ワードがあれば絞り込む
    public List<User> searchUsers(String q) {
        if (isBlank(q)) {
            return userRepository.findAllByOrderByUserIdAsc();
        }
        return userRepository.search(q.trim());
    }

    // 利用停止 ⇔ 解除 を切り替える（一般ユーザーのみ）
    @Transactional
    public void toggleEnabled(Long userId) {
        User user = findById(userId);
        if ("ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("管理者アカウントは停止できません");
        }
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
    }
}
