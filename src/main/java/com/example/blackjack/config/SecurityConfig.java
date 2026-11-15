package com.example.blackjack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// Spring Securityの設定クラス
// 「どのURLを誰が見られるか」「ログイン画面はどこか」などを決める
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // --- URLごとのアクセス権限 ---
            .authorizeHttpRequests(auth -> auth
                // ログインしていなくても見られるページ（登録・ログイン・CSS/JSなど）
                .requestMatchers("/register", "/register/**", "/login",
                                 "/css/**", "/js/**", "/images/**", "/error",
                                 "/api/dev/health").permitAll()
                // 管理者だけが見られるページ
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // 一般ユーザーだけが使えるページ（管理者はゲームをプレイできない仕様）
                .requestMatchers("/home", "/game/**", "/stats", "/rules", "/profile", "/chips/**").hasRole("USER")
                // それ以外はログインしていれば誰でもOK
                // ※権限のない画面を開いた場合は、Spring Securityがエラー画面（403）を表示する
                .anyRequest().authenticated()
            )
            // --- ログインの設定 ---
            .formLogin(form -> form
                .loginPage("/login")              // 自作のログイン画面(G3)を使う
                .usernameParameter("email")       // ユーザー名の代わりにメールアドレスでログイン
                .passwordParameter("password")
                // ログイン成功時：管理者なら管理者ホーム、一般ユーザーならホームへ
                .successHandler((request, response, authentication) -> {
                    boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    response.sendRedirect(request.getContextPath() + (isAdmin ? "/admin" : "/home"));
                })
                .failureUrl("/login?error")       // 失敗したら ?error を付けてログイン画面へ
                .permitAll()
            )
            // --- ログアウトの設定 ---
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        return http.build();
    }

    // パスワードをハッシュ化する仕組み（BCrypt）
    // DBには生のパスワードではなく、ハッシュ化した文字列を保存する
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
