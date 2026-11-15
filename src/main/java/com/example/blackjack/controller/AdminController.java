package com.example.blackjack.controller;

import com.example.blackjack.entity.Game;
import com.example.blackjack.entity.User;
import com.example.blackjack.repository.GameRepository;
import com.example.blackjack.repository.UserRepository;
import com.example.blackjack.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 管理者ホーム（G11）・ユーザー管理（G12）・プレイ履歴（G13）を担当
// /admin/** は SecurityConfig で「管理者だけ」に制限している
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int PAGE_SIZE = 20; // G13：1ページ20件

    private final UserService userService;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    public AdminController(UserService userService, UserRepository userRepository,
                           GameRepository gameRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
    }

    // G11 管理者ホーム
    @GetMapping
    public String home(Model model) {
        // ④ 概況
        model.addAttribute("userCount", userRepository.countByRole("USER"));
        model.addAttribute("gameCount", gameRepository.count());
        model.addAttribute("disabledCount", userRepository.countByRoleAndEnabled("USER", false));
        return "admin/home";
    }

    // G12 ユーザー管理
    //   q  … 検索ワード（①）
    //   id … 選んだ行のユーザーID（④ 詳細を開く）
    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "") String q,
                        @RequestParam(required = false) Long id,
                        Model model) {
        model.addAttribute("q", q);
        model.addAttribute("users", userService.searchUsers(q));
        if (id != null) {
            User selected = userService.findById(id);
            model.addAttribute("selected", selected);
            model.addAttribute("selectedPlays", gameRepository.countByUser(selected)); // プレイ数
        }
        return "admin/users";
    }

    // G12 ⑤ 利用停止／解除
    @PostMapping("/users/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam(defaultValue = "") String q,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.toggleEnabled(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        // 同じユーザーを選んだ状態で一覧に戻る
        redirectAttributes.addAttribute("q", q);
        redirectAttributes.addAttribute("id", id);
        return "redirect:/admin/users";
    }

    // G13 プレイ履歴
    //   name   … ユーザー名（一部でも可）
    //   period … 期間。「2026/11/01〜11/30」や「2026/11/01〜2026/12/15」の形で入力する（空欄なら全期間）
    //   page   … 何ページ目か（1から）
    @GetMapping("/history")
    public String history(@RequestParam(defaultValue = "") String name,
                          @RequestParam(defaultValue = "") String period,
                          @RequestParam(defaultValue = "1") int page,
                          Model model) {
        // 期間が空欄の時は、十分に広い範囲で検索する
        LocalDateTime fromTime = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime toTime = LocalDateTime.of(2999, 1, 1, 0, 0);

        if (!period.isBlank()) {
            try {
                LocalDate[] range = parsePeriod(period);
                fromTime = range[0].atStartOfDay();
                // 「終了日の終わりまで」を含めたいので、翌日の0時より前、という条件にする
                toTime = range[1].plusDays(1).atStartOfDay();
            } catch (DateTimeException | IllegalArgumentException e) {
                model.addAttribute("error", "期間は「2026/11/01〜11/30」の形で入力してください");
            }
        }

        int pageIndex = Math.max(0, page - 1); // Springのページ番号は0から始まる
        Page<Game> games = gameRepository.searchHistory(name.trim(), fromTime, toTime,
                PageRequest.of(pageIndex, PAGE_SIZE));

        model.addAttribute("games", games.getContent());
        model.addAttribute("page", pageIndex + 1);
        model.addAttribute("totalPages", Math.max(1, games.getTotalPages()));
        model.addAttribute("name", name);
        model.addAttribute("period", period);
        return "admin/history";
    }

    // 「2026/11/01〜11/30」のような文字を [開始日, 終了日] に変換する
    //   ・終了日の年を省略したら、開始日と同じ年とみなす
    //   ・「2026/11/01」のように1日だけなら、その1日だけを検索する
    private LocalDate[] parsePeriod(String period) {
        // 区切りは「〜」「～」「~」のどれでもOK。前後の空白は取り除く
        String[] parts = period.trim().split("\\s*[〜～~]\\s*");
        LocalDate from = parseDate(parts[0], null);
        LocalDate to = (parts.length >= 2) ? parseDate(parts[1], from.getYear()) : from;
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("終了日が開始日より前です");
        }
        return new LocalDate[]{from, to};
    }

    // 「2026/11/01」または「11/30」（年なし）を日付にする
    private LocalDate parseDate(String text, Integer defaultYear) {
        String[] nums = text.trim().split("/");
        if (nums.length == 3) {
            return LocalDate.of(Integer.parseInt(nums[0]), Integer.parseInt(nums[1]), Integer.parseInt(nums[2]));
        }
        if (nums.length == 2 && defaultYear != null) {
            return LocalDate.of(defaultYear, Integer.parseInt(nums[0]), Integer.parseInt(nums[1]));
        }
        throw new IllegalArgumentException("日付の形が正しくありません");
    }
}
