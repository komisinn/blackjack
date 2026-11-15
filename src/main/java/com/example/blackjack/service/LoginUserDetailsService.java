package com.example.blackjack.service;

import com.example.blackjack.entity.User;
import com.example.blackjack.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// ログイン時にSpring Securityから呼ばれるクラス
// 「入力されたメールアドレスのユーザー情報をDBから探して渡す」のが役割
// パスワードの照合はSpring Securityが自動でやってくれる
@Service
public class LoginUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public LoginUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + email));

        // Spring Security用のユーザー情報に変換する
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole())          // "USER" → 権限 "ROLE_USER" になる
                .disabled(!user.getEnabled())   // 管理者に利用停止されていたらログイン不可
                .build();
    }
}
