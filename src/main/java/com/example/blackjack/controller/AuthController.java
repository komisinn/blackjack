package com.example.blackjack.controller;

import com.example.blackjack.entity.User;
import com.example.blackjack.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// 新規登録（G1）・登録完了（G2）・ログイン（G3）の画面を担当
// ※ログインの認証そのもの（POST /login）は Spring Security が自動で処理する
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // G3 ログイン画面を表示
    @GetMapping("/login")
    public String login() {
        return "login"; // templates/login.html
    }

    // G1 新規登録画面を表示
    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    // G1 「登録する」ボタン
    @PostMapping("/register")
    public String register(@RequestParam String userName,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String passwordConfirm,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            User user = userService.register(userName, email, password, passwordConfirm);
            // 次の画面（G2）に1回だけ渡したい値は FlashAttribute に入れる
            redirectAttributes.addFlashAttribute("registeredName", user.getUserName());
            redirectAttributes.addFlashAttribute("registeredChips", user.getChips());
            return "redirect:/register/complete";
        } catch (IllegalArgumentException e) {
            // 入力に誤りがある時は同じ画面に留まり、エラーメッセージ（⑤）を表示する
            model.addAttribute("error", e.getMessage());
            model.addAttribute("userName", userName); // 入力し直さなくて済むように戻す
            model.addAttribute("email", email);
            return "register";
        }
    }

    // G2 登録完了画面
    @GetMapping("/register/complete")
    public String registerComplete(Model model) {
        // 登録直後以外（URLを直接開いた時など）はログイン画面へ
        if (!model.containsAttribute("registeredName")) {
            return "redirect:/login";
        }
        return "register-complete";
    }
}
