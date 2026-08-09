package com.gamifiedjava.controller;

import com.gamifiedjava.service.GamificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SandboxController {

    private final GamificationService gamificationService;

    public SandboxController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/sandbox")
    public String sandbox(Model model) {
        model.addAttribute("state", gamificationService.getState());
        return "sandbox";
    }
}
