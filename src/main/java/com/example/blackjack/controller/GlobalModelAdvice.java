package com.example.blackjack.controller;

import com.example.blackjack.entity.User;
import com.example.blackjack.repository.UserRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.blackjack.domain.GameResult;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

// すべての画面で共通して使うデータを用意するクラス
// @ControllerAdvice を付けると、全コントローラの処理の前にここが動く
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserRepository userRepository;

    public GlobalModelAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ログイン中のユーザーを "loginUser" という名前で全画面に渡す（ヘッダーの名前表示などに使う）
    // ログインしていない時（ログイン画面など）は null
    @ModelAttribute("loginUser")
    public User loginUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        // principal.getName() にはログインに使ったメールアドレスが入っている
        return userRepository.findByEmail(principal.getName()).orElse(null);
    }

    // DBに入っている結果の文字（"BLACKJACK"など）→ 画面に出す短いタグ（"BJ"など）の対応表
    // テンプレートでは ${resultTags[g.result]} のように使う
    @ModelAttribute("resultTags")
    public Map<String, String> resultTags() {
        Map<String, String> tags = new HashMap<>();
        for (GameResult r : GameResult.values()) {
            tags.put(r.name(), r.getTag());
        }
        return tags;
    }
}
