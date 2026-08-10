package com.gamifiedjava.controller;

import com.gamifiedjava.model.*;
import com.gamifiedjava.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/module")
public class ModuleController {

    private final ModuleService moduleService;
    private final GamificationService gamificationService;

    public ModuleController(ModuleService moduleService,
                            GamificationService gamificationService) {
        this.moduleService = moduleService;
        this.gamificationService = gamificationService;
    }

    @GetMapping("/{slug}")
    public String viewModule(@PathVariable String slug, Model model) {
        CourseModule mod = moduleService.getBySlug(slug);
        if (mod == null) {
            return "redirect:/";
        }

        ModuleProgress progress = moduleService.getProgress(mod.getId());

        model.addAttribute("module", mod);
        model.addAttribute("progress", progress);
        model.addAttribute("state", gamificationService.getState());
        model.addAttribute("content", "module-view :: content");

        return "module-view";
    }

    @PostMapping("/{slug}/read")
    public String markRead(@PathVariable String slug) {
        CourseModule mod = moduleService.getBySlug(slug);
        if (mod != null) {
            moduleService.completeModuleReading(mod.getId());
        }
        return "redirect:/module/" + slug;
    }
}
