package com.gamifiedjava.service;

import com.gamifiedjava.auth.CurrentUserContext;
import com.gamifiedjava.model.*;
import com.gamifiedjava.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GamificationService {

    private final AppStateRepository appStateRepository;
    private final XpLogRepository xpLogRepository;
    private final AchievementRepository achievementRepository;
    private final ModuleProgressRepository progressRepository;
    private final CurrentUserContext users;
    private final ConcurrentHashMap<String, Object> userLocks = new ConcurrentHashMap<>();

    public GamificationService(AppStateRepository appStateRepository,
                               XpLogRepository xpLogRepository,
                               AchievementRepository achievementRepository,
                               ModuleProgressRepository progressRepository,
                               CurrentUserContext users) {
        this.appStateRepository = appStateRepository;
        this.xpLogRepository = xpLogRepository;
        this.achievementRepository = achievementRepository;
        this.progressRepository = progressRepository;
        this.users = users;
    }

    public AppState getState() {
        return appStateRepository.getOrCreate();
    }
    public AppState addXp(String action, int amount, String note) {
        if (amount < 0 || amount > 1_000) throw new IllegalArgumentException("Invalid XP amount.");
        synchronized (userLock()) {
            AppState state = getState();
            state.setTotalXp(state.getTotalXp() + amount);
            state.setCurrentLevel(calculateLevel(state.getTotalXp()));
            state.setUpdatedAt(LocalDateTime.now());
            appStateRepository.save(state);
            xpLogRepository.save(new XpLog(action, amount, note));
            checkAchievements(state);
            return state;
        }
    }
    public AppState checkStreak() {
        synchronized (userLock()) {
            AppState state = getState();
            LocalDate today = LocalDate.now();

        // A dashboard view is a read for the rest of the day. Avoid a remote
        // PATCH on every navigation once today's streak has already been set.
            if (today.equals(state.getLastActiveDate())) return state;

            if (state.getLastActiveDate() != null) {
                LocalDate yesterday = today.minusDays(1);
                if (state.getLastActiveDate().equals(yesterday)) {
                    state.setStreakCount(state.getStreakCount() + 1);
                    int bonus = 10 * state.getStreakCount();
                    state.setTotalXp(state.getTotalXp() + bonus);
                    state.setCurrentLevel(calculateLevel(state.getTotalXp()));
                    xpLogRepository.save(new XpLog("streak_day", bonus,
                            "Day " + state.getStreakCount() + " streak bonus"));
                } else {
                    state.setStreakCount(1);
                }
            } else {
                state.setStreakCount(1);
            }

            state.setLastActiveDate(today);
            state.setUpdatedAt(LocalDateTime.now());
            appStateRepository.save(state);
            checkAchievements(state);
            return state;
        }
    }

    private int calculateLevel(int xp) {
        return (int) Math.floor(Math.sqrt(xp / 100.0)) + 1;
    }

    public List<XpLog> getRecentXpLogs() {
        return xpLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Achievement> getAchievements() {
        seedAchievements();
        return achievementRepository.findAllByOrderByUnlockedAtAsc();
    }
    public void seedAchievements() {
        if (users.currentUserId().isEmpty()) return;
        if (achievementRepository.count() == 0) {
            achievementRepository.save(new Achievement("First Steps", "Complete your first module", "footprints"));
            achievementRepository.save(new Achievement("Knowledge Seeker", "Pass all quizzes with >=80%", "book"));
            achievementRepository.save(new Achievement("Code Warrior", "Pass all code challenges", "sword"));
            achievementRepository.save(new Achievement("Full Stack", "Complete all 9 modules", "trophy"));
            achievementRepository.save(new Achievement("Streak Master", "Maintain a 7-day streak", "fire"));
            achievementRepository.save(new Achievement("Curious Mind", "Ask the AI 10 questions", "brain"));
            achievementRepository.save(new Achievement("Bug Hunter", "Fix 5 AI-identified issues", "bug"));
            achievementRepository.save(new Achievement("Speed Learner", "Complete 3 modules in one day", "rocket"));
            achievementRepository.save(new Achievement("Perfect Score", "Get 100% on any quiz", "star"));
            achievementRepository.save(new Achievement("Persistent", "Retry a challenge 3+ times until passing", "shield"));
            achievementRepository.save(new Achievement("Early Adopter", "Complete the first CourseModule on day 1", "sun"));
            achievementRepository.save(new Achievement("Java Rookie", "Reach level 3", "seedling"));
            achievementRepository.save(new Achievement("Java Apprentice", "Reach level 5", "hat"));
        }
    }

    private void checkAchievements(AppState state) {
        seedAchievements();
        long unlockedCount = achievementRepository.countByUnlockedAtIsNotNull();

        if (unlockedCount >= achievementRepository.count()) return;

        List<ModuleProgress> allProgress = progressRepository.findAllByOrderByIdAsc();
        long completedModules = allProgress.stream().filter(ModuleProgress::isComplete).count();
        long passedChallenges = allProgress.stream().filter(p -> Boolean.TRUE.equals(p.getChallengePassed())).count();
        long passedQuizzes = allProgress.stream().filter(ModuleProgress::isQuizPassed).count();

        unlockIf("First Steps", completedModules >= 1);
        unlockIf("Full Stack", completedModules >= 9);
        unlockIf("Code Warrior", passedChallenges >= 9);
        unlockIf("Knowledge Seeker", passedQuizzes >= 9);
        unlockIf("Java Rookie", state.getCurrentLevel() >= 3);
        unlockIf("Java Apprentice", state.getCurrentLevel() >= 5);
        unlockIf("Streak Master", state.getStreakCount() >= 7);
        unlockIf("Early Adopter", completedModules >= 1);
    }
    protected void unlockIf(String name, boolean condition) {
        if (!condition) return;
        achievementRepository.findByName(name).ifPresent(a -> {
            if (a.getUnlockedAt() == null) {
                a.setUnlockedAt(LocalDateTime.now());
                achievementRepository.save(a);
            }
        });
    }

    private Object userLock() {
        return userLocks.computeIfAbsent(users.requireUserId(), ignored -> new Object());
    }
}
