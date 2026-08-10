package com.gamifiedjava.controller;

import com.gamifiedjava.config.RateLimiter;
import com.gamifiedjava.auth.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import com.gamifiedjava.service.*;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
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
    public Map<String, Object> ask(@RequestParam("message") @NotBlank @Size(max = MAX_MESSAGE_CHARS) String message,
                                   @RequestParam(value = "moduleId", required = false) @Positive Integer moduleId,
                                   @RequestParam(value = "chatId", required = false) @Positive Integer chatId,
                                   @RequestParam(value = "context", defaultValue = "ask")
                                   @Pattern(regexp = "ask|explain|hint|review|error|quiz") String context,
                                   HttpServletRequest request) {
        if (message == null || message.isBlank()) {
            return Map.of("response", "Please type a message first.");
        }
        AuthUser user = (AuthUser) request.getAttribute("authUser");
        RateLimiter.Lease lease = rateLimiter.tryAcquire("ai", user.id());
        if (lease == null) {
            return Map.of("response", "You're asking a lot right now — wait a moment and try again.");
        }
        try (lease) {
            String response = ollamaService.ask(message, moduleId, context, user.id(), chatId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("response", response);
            if (chatId != null) {
                ollamaService.getChat(chatId, user.id()).ifPresent(chat -> {
                    result.put("chatId", chat.getId());
                    result.put("chatTitle", chat.getTitle());
                });
            }
            return result;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found");
        } catch (IllegalStateException e) {
            String messageForUser = "Add your Ollama API key in the AI API tab first.".equals(e.getMessage())
                    ? e.getMessage() : "The AI tutor is temporarily unavailable.";
            return Map.of("response", messageForUser);
        }
    }

    @PostMapping("/chats")
    @ResponseBody
    public Map<String, Object> createChat(HttpServletRequest request) {
        AuthUser user = (AuthUser) request.getAttribute("authUser");
        RateLimiter.Lease lease = rateLimiter.tryAcquire("general", user.id());
        if (lease == null) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        var chat = useLeaseToCreateChat(lease, user.id());
        return Map.of(
                "id", chat.getId(),
                "title", chat.getTitle(),
                "updatedAt", chat.getUpdatedAt()
        );
    }

    private com.gamifiedjava.model.AiChat useLeaseToCreateChat(RateLimiter.Lease lease, String userId) {
        try (lease) { return ollamaService.createChat(userId); }
    }

    @GetMapping("/chats/{chatId}/messages")
    @ResponseBody
    public List<Map<String, Object>> chatMessages(@PathVariable @Positive Integer chatId,
                                                   HttpServletRequest request) {
        AuthUser user = (AuthUser) request.getAttribute("authUser");
        try {
            return ollamaService.getConversationHistory(chatId, user.id()).stream()
                    .map(message -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", message.getId());
                        row.put("role", message.getRole());
                        row.put("message", message.getMessage());
                        row.put("createdAt", message.getCreatedAt());
                        return row;
                    })
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found");
        }
    }

    @GetMapping("/chat")
    public String chatPage(Model model, HttpServletRequest request) {
        AuthUser user = (AuthUser) request.getAttribute("authUser");
        var chats = ollamaService.getChats(user.id());
        var activeChat = chats.isEmpty() ? null : chats.getFirst();
        var history = activeChat == null
                ? List.of()
                : ollamaService.getConversationHistory(activeChat.getId(), user.id());
        model.addAttribute("chats", chats);
        model.addAttribute("activeChat", activeChat);
        model.addAttribute("history", history);
        model.addAttribute("state", gamificationService.getState());
        model.addAttribute("content", "ai-chat :: content");
        return "ai-chat";
    }
}
