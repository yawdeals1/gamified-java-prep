package com.gamifiedjava.controller;

import com.gamifiedjava.model.*;
import com.gamifiedjava.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/module/{slug}/challenge")
public class ChallengeController {

    private final ModuleService moduleService;
    private final ChallengeService challengeService;
    private final GamificationService gamificationService;

    public ChallengeController(ModuleService moduleService,
                               ChallengeService challengeService,
                               GamificationService gamificationService) {
        this.moduleService = moduleService;
        this.challengeService = challengeService;
        this.gamificationService = gamificationService;
    }

    @GetMapping
    public String showChallenge(@PathVariable String slug, Model model) {
        CourseModule mod = moduleService.getBySlug(slug);
        if (mod == null) return "redirect:/";

        ModuleProgress progress = moduleService.getProgress(mod.getId());
        if (progress == null || !progress.isUnlocked()) {
            return "redirect:/module/" + slug;
        }

        List<ChallengeSubmission> submissions = challengeService.getSubmissions(mod.getId());

        model.addAttribute("module", mod);
        model.addAttribute("progress", progress);
        model.addAttribute("submissions", submissions);
        model.addAttribute("state", gamificationService.getState());
        model.addAttribute("content", "challenge :: content");

        return "challenge";
    }

    @PostMapping
    public String submitChallenge(@PathVariable String slug,
                                  @RequestParam("sourceCode") String sourceCode,
                                  Model model) {
        CourseModule mod = moduleService.getBySlug(slug);
        if (mod == null) return "redirect:/";

        ChallengeService.ChallengeResult result = challengeService.submit(mod.getId(), sourceCode);

        ModuleProgress progress = moduleService.getProgress(mod.getId());
        List<ChallengeSubmission> submissions = challengeService.getSubmissions(mod.getId());

        if (result.passed() && progress != null && !progress.isComplete()) {
            moduleService.markModuleComplete(mod.getId());
            progress = moduleService.getProgress(mod.getId());
        }

        model.addAttribute("module", mod);
        model.addAttribute("result", result);
        model.addAttribute("progress", progress);
        model.addAttribute("submissions", submissions);
        model.addAttribute("state", gamificationService.getState());
        model.addAttribute("content", "challenge-result :: content");

        return "challenge-result";
    }
}
