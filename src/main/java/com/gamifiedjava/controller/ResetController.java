package com.gamifiedjava.controller;

import com.gamifiedjava.service.*;
import com.gamifiedjava.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequestMapping("/reset")
public class ResetController {

    private final ModuleRepository moduleRepository;
    private final ModuleProgressRepository progressRepository;
    private final QuizAttemptRepository attemptRepository;
    private final ChallengeSubmissionRepository challengeRepository;
    private final AchievementRepository achievementRepository;
    private final XpLogRepository xpLogRepository;
    private final AiConversationRepository aiRepository;
    private final AiChatRepository aiChatRepository;
    private final AppStateRepository appStateRepository;
    private final StepProgressRepository stepProgressRepository;
    private final ModuleService moduleService;
    private final GamificationService gamificationService;

    public ResetController(ModuleRepository moduleRepository,
                           ModuleProgressRepository progressRepository,
                           QuizAttemptRepository attemptRepository,
                           ChallengeSubmissionRepository challengeRepository,
                           AchievementRepository achievementRepository,
                           XpLogRepository xpLogRepository,
                           AiConversationRepository aiRepository,
                           AiChatRepository aiChatRepository,
                           AppStateRepository appStateRepository,
                           StepProgressRepository stepProgressRepository,
                           ModuleService moduleService,
                           GamificationService gamificationService) {
        this.moduleRepository = moduleRepository;
        this.progressRepository = progressRepository;
        this.attemptRepository = attemptRepository;
        this.challengeRepository = challengeRepository;
        this.achievementRepository = achievementRepository;
        this.xpLogRepository = xpLogRepository;
        this.aiRepository = aiRepository;
        this.aiChatRepository = aiChatRepository;
        this.appStateRepository = appStateRepository;
        this.stepProgressRepository = stepProgressRepository;
        this.moduleService = moduleService;
        this.gamificationService = gamificationService;
    }

    @PostMapping("/progress")
    public String resetProgress() {
        stepProgressRepository.deleteAll();
        xpLogRepository.deleteAll();
        attemptRepository.deleteAll();
        challengeRepository.deleteAll();
        progressRepository.deleteAll();
        achievementRepository.deleteAll();
        aiRepository.deleteAll();
        aiChatRepository.deleteAll();
        appStateRepository.deleteAll();

        moduleService.seedModules();
        gamificationService.seedAchievements();

        return "redirect:/";
    }
}
