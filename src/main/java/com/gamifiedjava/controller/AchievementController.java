package com.gamifiedjava.controller;

import com.gamifiedjava.model.*;
import com.gamifiedjava.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/achievements")
public class AchievementController {

    private final GamificationService gamificationService;

    public AchievementController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping
    public String showAchievements(Model model) {
        AppState state = gamificationService.getState();
        List<Achievement> achievements = gamificationService.getAchievements();
        List<XpLog> logs = gamificationService.getRecentXpLogs();

        long unlockedCount = achievements.stream().filter(Achievement::isUnlocked).count();

        model.addAttribute("state", state);
        model.addAttribute("achievements", achievements);
        model.addAttribute("unlockedCount", unlockedCount);
        model.addAttribute("totalCount", achievements.size());
        model.addAttribute("logs", logs);
        model.addAttribute("content", "achievements :: content");

        return "achievements";
    }
}
