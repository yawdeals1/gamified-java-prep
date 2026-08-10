package com.gamifiedjava.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamifiedjava.model.AppState;
import com.gamifiedjava.model.CourseModule;
import com.gamifiedjava.model.LessonStep;
import com.gamifiedjava.service.GamificationService;
import com.gamifiedjava.service.LessonStepService;
import com.gamifiedjava.service.ModuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Renders the interactive Lesson Player (the Step Engine). */
@Controller
public class LessonController {

    private final ModuleService moduleService;
    private final LessonStepService lessonStepService;
    private final GamificationService gamificationService;
    private final ObjectMapper objectMapper;

    public LessonController(ModuleService moduleService,
                            LessonStepService lessonStepService,
                            GamificationService gamificationService,
                            ObjectMapper objectMapper) {
        this.moduleService = moduleService;
        this.lessonStepService = lessonStepService;
        this.gamificationService = gamificationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/learn/{slug}")
    public String lesson(@PathVariable String slug, Model model, RedirectAttributes ra) {
        CourseModule module = moduleService.getBySlug(slug);
        if (module == null) {
            ra.addFlashAttribute("error", "Module not found: " + slug);
            return "redirect:/";
        }
        var progress = moduleService.getProgress(module.getId());
        if (progress == null || !progress.isUnlocked()) {
            ra.addFlashAttribute("error", "Complete the previous module first.");
            return "redirect:/";
        }

        List<LessonStep> steps = lessonStepService.getSteps(module.getId());
        Set<Integer> done = lessonStepService.completedStepIds(module.getId());
        List<Map<String, Object>> clientSteps = lessonStepService.toClientSteps(steps, done);
        AppState state = gamificationService.getState();

        // Find the first incomplete step so the player can resume mid-module.
        int resumeIndex = 0;
        for (int idx = 0; idx < steps.size(); idx++) {
            if (!done.contains(steps.get(idx).getId())) {
                resumeIndex = idx;
                break;
            }
        }

        model.addAttribute("module", module);
        model.addAttribute("state", state);
        model.addAttribute("stepCount", steps.size());
        model.addAttribute("stepsJson", writeJson(clientSteps));
        model.addAttribute("resumeStepIndex", resumeIndex);
        return "learn";
    }

    private String writeJson(Object value) {
        try {
            return scriptSafeJson(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String scriptSafeJson(String json) {
        return json.replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
    }
}
