package com.gamifiedjava.controller;

import com.gamifiedjava.config.RateLimiter;
import com.gamifiedjava.auth.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import com.gamifiedjava.service.*;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@Controller
@RequestMapping("/ai")
public class AiController {

    private static final int MAX_MESSAGE_CHARS = 4_000;

    private final OllamaService ollamaService;
    private final GamificationService gamificationService;
    private final RateLimiter rateLimiter;

    public AiController(OllamaService ollamaService,
                        GamificationService gamificationService,
                        RateLimiter rateLimiter) {
        this.ollamaService = ollamaService;
        this.gamificationService = gamificationService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/ask")
    @ResponseBody
    public Map<String, String> ask(@RequestParam("message") @Size(max = MAX_MESSAGE_CHARS) String message,
                                   @RequestParam(value = "moduleId", required = false) Integer moduleId,
                                   @RequestParam(value = "context", defaultValue = "ask") @Size(max = 32) String context,
                                   HttpServletRequest request) {
        if (message == null || message.isBlank()) {
            return Map.of("response", "Please type a message first.");
        }
        if (!rateLimiter.tryAcquire()) {
            return Map.of("response", "You're asking a lot right now — wait a moment and try again.");
        }
        try {
            AuthUser user = (AuthUser) request.getAttribute("authUser");
            String response = ollamaService.ask(message, moduleId, context, user.id());
            return Map.of("response", response);
        } catch (IllegalStateException e) {
            return Map.of("response", e.getMessage());
        } finally {
            rateLimiter.release();
        }
    }

    @GetMapping("/chat")
    public String chatPage(Model model) {
        var history = ollamaService.getConversationHistory();
        model.addAttribute("history", history);
        model.addAttribute("state", gamificationService.getState());
        model.addAttribute("content", "ai-chat :: content");
        return "ai-chat";
    }
}
