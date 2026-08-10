package com.gamifiedjava.controller;

import com.gamifiedjava.service.GamificationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ActivityController {
    private final GamificationService gamification;
    public ActivityController(GamificationService gamification) { this.gamification = gamification; }

    @PostMapping("/api/activity")
    public Map<String, Object> recordActivity() {
        var state = gamification.checkStreak();
        return Map.of("streak", state.getStreakCount(), "totalXp", state.getTotalXp());
    }
}
