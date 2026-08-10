package com.gamifiedjava.controller;

import com.gamifiedjava.auth.AuthUser;
import com.gamifiedjava.service.OllamaService;
import com.gamifiedjava.service.UserAiSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AiApiController {
    private final UserAiSettingsService settingsService;
    private final OllamaService ollamaService;

    public AiApiController(UserAiSettingsService settingsService, OllamaService ollamaService) {
        this.settingsService = settingsService;
        this.ollamaService = ollamaService;
    }

    @GetMapping("/ai-api")
    public String page(HttpServletRequest request, Model model) {
        AuthUser user = user(request);
        var settings = settingsService.settings(user.id());
        model.addAttribute("configured", settings.isPresent());
        model.addAttribute("lastFour", settings.map(s -> s.getKeyLastFour()).orElse(""));
        model.addAttribute("encryptionReady", settingsService.encryptionReady());
        return "ai-api";
    }

    @PostMapping("/ai-api")
    public String save(@RequestParam String apiKey, HttpServletRequest request, RedirectAttributes flash) {
        String clean = apiKey == null ? "" : apiKey.trim();
        if (!ollamaService.validateApiKey(clean)) {
            flash.addFlashAttribute("error", "Ollama rejected that key. Check it and try again.");
            return "redirect:/ai-api";
        }
        try {
            settingsService.save(user(request).id(), clean);
            flash.addFlashAttribute("success", "Ollama API key verified and saved securely.");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ai-api";
    }

    @PostMapping("/ai-api/remove")
    public String remove(HttpServletRequest request, RedirectAttributes flash) {
        settingsService.remove(user(request).id());
        flash.addFlashAttribute("success", "Ollama API key removed.");
        return "redirect:/ai-api";
    }

    private AuthUser user(HttpServletRequest request) { return (AuthUser) request.getAttribute("authUser"); }
}
