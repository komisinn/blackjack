package com.example.blackjack.config;

import com.example.blackjack.entity.User;
import com.example.blackjack.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// アプリ起動時に1回だけ動くクラス
// 管理者アカウントが1つもなければ、初期管理者を自動で作成する
// （管理者は新規登録画面からは作れないため）
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // コンストラクタインジェクション：Springが必要な部品を自動で渡してくれる
    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole("ADMIN")) {
            User admin = new User();
            admin.setUserName("管理者");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin1234")); // ★本番では必ず変更すること
            admin.setRole("ADMIN");
            admin.setChips(0); // 管理者はプレイしないのでチップは不要
            userRepository.save(admin);
            System.out.println("初期管理者を作成しました: admin@example.com / admin1234");
        }
    }
}
