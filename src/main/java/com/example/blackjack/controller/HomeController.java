package com.example.blackjack.controller;

import com.example.blackjack.entity.User;
import com.example.blackjack.service.GameService;
import com.example.blackjack.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

// ホーム（G4）・戦績（G8）・ルール説明（G9）・プロフィール編集（G10）を担当
@Controller
public class HomeController {

    private final UserService userService;
    private final GameService gameService;

    public HomeController(UserService userService, GameService gameService) {
        this.userService = userService;
        this.gameService = gameService;
    }

    // "/" に来たら、権限に応じてホームへ振り分ける
    @GetMapping("/")
    public String root(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin ? "redirect:/admin" : "redirect:/home";
    }

    // G4 ホーム
    @GetMapping("/home")
    public String home(Principal principal, Model model) {
        User user = userService.findByEmail(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("stats", gameService.getStats(user)); // ⑤ これまでの成績
        return "home";
    }

    // G4 ⑨ チップリセット（残高0の時だけ）
    @PostMapping("/chips/reset")
    public String resetChips(Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(principal.getName());
        try {
            userService.resetChips(user.getUserId());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/home";
    }

    // G8 戦績
    @GetMapping("/stats")
    public String stats(Principal principal, Model model) {
        User user = userService.findByEmail(principal.getName());
        model.addAttribute("stats", gameService.getStats(user));               // ①〜③
        model.addAttribute("chipHistory", gameService.getChipHistory(user));   // ④ グラフ用
        model.addAttribute("ranking", userService.getChipRanking());           // ⑤ ランキング
        model.addAttribute("myId", user.getUserId());                          // 自分の行を強調するため
        return "stats";
    }

    // G9 ルール説明
    @GetMapping("/rules")
    public String rules() {
        return "rules";
    }

    // G10 プロフィール編集（表示）
    @GetMapping("/profile")
    public String profileForm(Principal principal, Model model) {
        User user = userService.findByEmail(principal.getName());
        if (!model.containsAttribute("userName")) {
            model.addAttribute("userName", user.getUserName());
        }
        model.addAttribute("email", user.getEmail());
        return "profile";
    }

    // G10 「変更を保存」ボタン
    @PostMapping("/profile")
    public String updateProfile(@RequestParam String userName,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String newPasswordConfirm,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(principal.getName());
        try {
            userService.updateProfile(user.getUserId(), userName, newPassword, newPasswordConfirm);
            redirectAttributes.addFlashAttribute("message", "変更を保存しました");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("userName", userName);
        }
        return "redirect:/profile";
    }
}
