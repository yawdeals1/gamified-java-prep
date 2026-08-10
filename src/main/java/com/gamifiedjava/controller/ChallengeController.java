package com.gamifiedjava.controller;

import com.gamifiedjava.model.*;
import com.gamifiedjava.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.gamifiedjava.auth.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Controller
@Validated
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
        if (!challengeAllowed(progress)) {
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
                                  @RequestParam("sourceCode") @NotBlank @Size(max = 50_000) String sourceCode,
                                  HttpServletRequest request,
                                  Model model) {
        CourseModule mod = moduleService.getBySlug(slug);
        if (mod == null) return "redirect:/";
        ModuleProgress existingProgress = moduleService.getProgress(mod.getId());
        if (!challengeAllowed(existingProgress)) return "redirect:/module/" + slug;

        AuthUser user = (AuthUser) request.getAttribute("authUser");
        ChallengeService.ChallengeResult result = challengeService.submit(mod.getId(), sourceCode, user.id());

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

    private boolean challengeAllowed(ModuleProgress progress) {
        return progress != null && ("challenge_ready".equals(progress.getStatus())
                || "completed".equals(progress.getStatus()));
    }
}
