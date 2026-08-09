package com.gamifiedjava.controller;

import com.gamifiedjava.model.*;
import com.gamifiedjava.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.*;

@Controller
public class DashboardController {

    private final ModuleService moduleService;
    private final GamificationService gamificationService;
    private final LessonStepService lessonStepService;

    /** Diagonal offsets (px) that give the skill-map path its zig-zag. */
    private static final int[] OFFSETS = {-40, 40, -20, 60, -50, 20, -30, 45, -25};
    private static final int NODE_RING_CIRC = 176; // 2*pi*28
    private static final int XP_RING_CIRC = 377;   // 2*pi*60
    private static final int DAILY_GOAL_XP = 100;

    public DashboardController(ModuleService moduleService,
                               GamificationService gamificationService,
                               LessonStepService lessonStepService) {
        this.moduleService = moduleService;
        this.gamificationService = gamificationService;
        this.lessonStepService = lessonStepService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        gamificationService.checkStreak();

        List<CourseModule> modules = moduleService.getAllModules();
        List<ModuleProgress> progressList = moduleService.getAllProgress();
        AppState state = gamificationService.getState();

        Map<Integer, ModuleProgress> progressByModule = new HashMap<>();
        for (ModuleProgress p : progressList) progressByModule.put(p.getModule().getId(), p);

        // ---- module nodes (state + mastery), with progressive unlock ----
        List<Map<String, Object>> nodes = new ArrayList<>();
        boolean prevHadProgress = true; // module 1 is always reachable
        Map<String, Object> continueTarget = null;
        boolean firstAvailableMarked = false;
        long mastered = 0;

        for (int i = 0; i < modules.size(); i++) {
            CourseModule m = modules.get(i);
            ModuleProgress prog = progressByModule.get(m.getId());
            int mastery = lessonStepService.masteryPercent(m.getId());
            boolean dbUnlocked = prog != null && !"locked".equals(prog.getStatus());
            boolean unlocked = i == 0 || prevHadProgress || dbUnlocked;

            String stateName;
            if (mastery >= 100) stateName = "mastered";
            else if (unlocked && mastery > 0) stateName = "in_progress";
            else if (unlocked) stateName = "available";
            else stateName = "locked";

            boolean isNext = false;
            if ("available".equals(stateName) && !firstAvailableMarked) {
                isNext = true; firstAvailableMarked = true;
            }
            if ("mastered".equals(stateName)) mastered++;

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("order", m.getOrderIndex());
            node.put("num", String.format("%02d", m.getOrderIndex()));
            node.put("slug", m.getSlug());
            node.put("title", m.getTitle());
            node.put("description", m.getDescription());
            node.put("state", stateName);
            node.put("mastery", mastery);
            node.put("isNext", isNext);
            node.put("offset", OFFSETS[i % OFFSETS.length]);
            node.put("ringOffset", Math.round(NODE_RING_CIRC * (1 - mastery / 100.0)));
            node.put("label", labelFor(stateName, mastery, isNext));
            nodes.add(node);

            // continue = first in-progress, else first available/next
            if (continueTarget == null && ("in_progress".equals(stateName)
                    || (isNext))) {
                continueTarget = node;
            }

            prevHadProgress = mastery >= 100;
        }
        if (continueTarget == null && !nodes.isEmpty()) continueTarget = nodes.get(0);

        // ---- level / XP progress ----
        int lvl = state.getCurrentLevel();
        int levelBase = 100 * (lvl - 1) * (lvl - 1);
        int levelNext = 100 * lvl * lvl;
        int intoLevel = state.getTotalXp() - levelBase;
        int levelSpan = Math.max(1, levelNext - levelBase);
        int levelPct = Math.max(0, Math.min(100, (int) Math.round(intoLevel * 100.0 / levelSpan)));
        int xpToNext = Math.max(0, levelNext - state.getTotalXp());

        // ---- daily goal (XP earned today) ----
        LocalDate today = LocalDate.now();
        int earnedToday = gamificationService.getRecentXpLogs().stream()
                .filter(l -> l.getCreatedAt() != null && l.getCreatedAt().toLocalDate().equals(today))
                .mapToInt(XpLog::getXpGained).sum();
        int dailyPct = Math.min(100, (int) Math.round(earnedToday * 100.0 / DAILY_GOAL_XP));

        // ---- most recent achievement ----
        Achievement recent = gamificationService.getAchievements().stream()
                .filter(Achievement::isUnlocked)
                .max(Comparator.comparing(Achievement::getUnlockedAt))
                .orElse(null);

        model.addAttribute("state", state);
        model.addAttribute("rank", rankTitle(lvl));
        model.addAttribute("levelPct", levelPct);
        model.addAttribute("levelRingOffset", Math.round(XP_RING_CIRC * (1 - levelPct / 100.0)));
        model.addAttribute("xpToNext", xpToNext);
        model.addAttribute("dailyEarned", earnedToday);
        model.addAttribute("dailyGoal", DAILY_GOAL_XP);
        model.addAttribute("dailyPct", dailyPct);
        model.addAttribute("nodes", nodes);
        model.addAttribute("continueTarget", continueTarget);
        model.addAttribute("masteredCount", mastered);
        model.addAttribute("totalModules", modules.size());
        model.addAttribute("recentAchievement", recent);
        return "dashboard";
    }

    private String labelFor(String state, int mastery, boolean isNext) {
        return switch (state) {
            case "mastered" -> "Mastered";
            case "in_progress" -> mastery + "% Mastery";
            case "available" -> isNext ? "Next Step" : "Available";
            default -> "Locked";
        };
    }

    /** Thematic Java rank titles by level. */
    private String rankTitle(int level) {
        return switch (Math.min(level, 10)) {
            case 1 -> "Bootstrapper";
            case 2 -> "Syntax Scout";
            case 3 -> "Loop Wrangler";
            case 4 -> "Class Architect";
            case 5 -> "Interface Artisan";
            case 6 -> "Generics Adept";
            case 7 -> "Concurrency Knight";
            case 8 -> "Bytecode Sage";
            case 9 -> "JVM Whisperer";
            default -> "Senior Compiler";
        };
    }
}
