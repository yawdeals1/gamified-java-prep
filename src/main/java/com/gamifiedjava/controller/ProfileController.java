package com.gamifiedjava.controller;

import com.gamifiedjava.model.*;
import com.gamifiedjava.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Controller
public class ProfileController {

    private static final int XP_RING_CIRC   = 377;
    private static final int DAILY_GOAL_XP  = 100;

    private final GamificationService gamificationService;
    private final ModuleService moduleService;
    private final LessonStepService lessonStepService;

    public ProfileController(GamificationService gamificationService,
                             ModuleService moduleService,
                             LessonStepService lessonStepService) {
        this.gamificationService = gamificationService;
        this.moduleService       = moduleService;
        this.lessonStepService   = lessonStepService;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        AppState state;
        List<XpLog> logs;
        List<Achievement> achievements;
        List<CourseModule> modules;
        List<ModuleProgress> progressList;
        Map<Integer, Integer> masteryByModule;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var stateFuture = executor.submit(gamificationService::getState);
            var logsFuture = executor.submit(gamificationService::getRecentXpLogs);
            var achievementsFuture = executor.submit(gamificationService::getAchievements);
            var modulesFuture = executor.submit(moduleService::getAllModules);
            var progressFuture = executor.submit(moduleService::getAllProgress);
            var masteryFuture = executor.submit(lessonStepService::masteryPercentByModule);
            try {
                state = stateFuture.get();
                logs = logsFuture.get();
                achievements = achievementsFuture.get();
                modules = modulesFuture.get();
                progressList = progressFuture.get();
                masteryByModule = masteryFuture.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while loading profile data", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Could not load profile data", e.getCause());
            }
        }

        // Level math
        int lvl       = state.getCurrentLevel();
        int levelBase = 100 * (lvl - 1) * (lvl - 1);
        int levelNext = 100 * lvl * lvl;
        int intoLevel = state.getTotalXp() - levelBase;
        int levelSpan = Math.max(1, levelNext - levelBase);
        int levelPct  = Math.max(0, Math.min(100, (int) Math.round(intoLevel * 100.0 / levelSpan)));
        int xpToNext  = Math.max(0, levelNext - state.getTotalXp());

        // Daily XP
        LocalDate today = LocalDate.now();
        int earnedToday = logs.stream()
                .filter(l -> l.getCreatedAt() != null && l.getCreatedAt().toLocalDate().equals(today))
                .mapToInt(XpLog::getXpGained).sum();
        int dailyPct = Math.min(100, (int) Math.round(earnedToday * 100.0 / DAILY_GOAL_XP));

        // Days since account created
        long daysActive = 1;
        if (state.getCreatedAt() != null) {
            daysActive = Math.max(1, ChronoUnit.DAYS.between(state.getCreatedAt().toLocalDate(), today) + 1);
        }

        // Per-module mastery + same progressive-unlock logic as the dashboard
        Map<Integer, ModuleProgress> progressByModule = new HashMap<>();
        for (ModuleProgress p : progressList) progressByModule.put(p.getModule().getId(), p);

        List<Map<String, Object>> moduleStats = new ArrayList<>();
        int completedCount = 0;
        boolean prevHadProgress = true; // module 1 always reachable
        for (int i = 0; i < modules.size(); i++) {
            CourseModule m = modules.get(i);
            int mastery = masteryByModule.getOrDefault(m.getId(), 0);
            if (mastery >= 100) completedCount++;

            ModuleProgress prog = progressByModule.get(m.getId());
            boolean dbUnlocked = prog != null && !"locked".equals(prog.getStatus());
            boolean unlocked = i == 0 || prevHadProgress || dbUnlocked;

            Map<String, Object> ms = new LinkedHashMap<>();
            ms.put("title",    m.getTitle());
            ms.put("slug",     m.getSlug());
            ms.put("mastery",  mastery);
            ms.put("unlocked", unlocked);
            moduleStats.add(ms);

            prevHadProgress = mastery >= 100;
        }

        // Achievement stats
        long unlockedAch = achievements.stream().filter(Achievement::isUnlocked).count();

        // Total XP from logs (sum)
        int totalXpLogged = logs.stream().mapToInt(XpLog::getXpGained).sum();

        model.addAttribute("state",        state);
        model.addAttribute("rank",         rankTitle(lvl));
        model.addAttribute("levelPct",     levelPct);
        model.addAttribute("levelRingOffset", Math.round(XP_RING_CIRC * (1 - levelPct / 100.0)));
        model.addAttribute("xpToNext",     xpToNext);
        model.addAttribute("dailyEarned",  earnedToday);
        model.addAttribute("dailyGoal",    DAILY_GOAL_XP);
        model.addAttribute("dailyPct",     dailyPct);
        model.addAttribute("daysActive",   daysActive);
        model.addAttribute("moduleStats",  moduleStats);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("totalModules", modules.size());
        model.addAttribute("unlockedAch",  unlockedAch);
        model.addAttribute("totalAch",     achievements.size());
        model.addAttribute("logs",         logs.stream().limit(20).collect(Collectors.toList()));
        model.addAttribute("totalXpLogged", totalXpLogged);
        return "profile";
    }

    private String rankTitle(int level) {
        return switch (Math.min(level, 10)) {
            case 1  -> "Bootstrapper";
            case 2  -> "Syntax Scout";
            case 3  -> "Loop Wrangler";
            case 4  -> "Class Architect";
            case 5  -> "Interface Artisan";
            case 6  -> "Generics Adept";
            case 7  -> "Concurrency Knight";
            case 8  -> "Bytecode Sage";
            case 9  -> "JVM Whisperer";
            default -> "Senior Compiler";
        };
    }
}
