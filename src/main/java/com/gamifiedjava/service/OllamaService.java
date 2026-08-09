package com.gamifiedjava.service;

import com.gamifiedjava.model.AiConversation;
import com.gamifiedjava.model.CourseModule;
import com.gamifiedjava.repository.AiConversationRepository;
import com.gamifiedjava.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class OllamaService {

    private final RestClient restClient;
    private final AiConversationRepository conversationRepository;
    private final ModuleRepository moduleRepository;
    private final GamificationService gamificationService;

    @Value("${ollama.model}")
    private String model;

    @Value("${ollama.protocol:ollama}")
    private String protocol;

    @Value("${ollama.base-url}")
    private String baseUrl;

    public OllamaService(RestClient ollamaRestClient,
                         AiConversationRepository conversationRepository,
                         ModuleRepository moduleRepository,
                         GamificationService gamificationService) {
        this.restClient = ollamaRestClient;
        this.conversationRepository = conversationRepository;
        this.moduleRepository = moduleRepository;
        this.gamificationService = gamificationService;
    }

    public String ask(String message, Integer moduleId, String contextType) {
        CourseModule mod = moduleId != null ? moduleRepository.findById(moduleId).orElse(null) : null;

        String systemPrompt = buildSystemPrompt(mod, contextType);

        Map<String, Object> request;
        if ("openai".equalsIgnoreCase(protocol)) {
            request = new LinkedHashMap<>();
            request.put("model", model);
            request.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", message)
            ));
            request.put("stream", false);
        } else {
            request = new LinkedHashMap<>();
            request.put("model", model);
            request.put("system", systemPrompt);
            request.put("prompt", message);
            request.put("stream", false);
        }

        String finalResponse = callModel(request);

        ConversationLogger.log(conversationRepository, "user", message, mod, contextType);
        ConversationLogger.log(conversationRepository, "assistant", finalResponse, mod, contextType);

        long chatCount = conversationRepository.count();
        if (chatCount >= 20) {
            gamificationService.unlockIf("Curious Mind", true);
        }

        gamificationService.addXp("ai_query", 5, "Asked AI: " + contextType);

        return finalResponse;
    }

    public String gradeCode(String code, Integer moduleId) {
        CourseModule mod = moduleId != null ? moduleRepository.findById(moduleId).orElse(null) : null;
        String moduleContext = mod != null ?
                "Module: " + mod.getTitle() + "\nChallenge: " + mod.getChallengeInstructions() :
                "General Java code review";

        String prompt = "You are a Java instructor reviewing a student's code submission. " +
                "Review the following code and provide:\n" +
                "1. A score from 0-100\n" +
                "2. Brief feedback on correctness, style, and areas for improvement\n" +
                "3. Specific suggestions if anything is wrong\n\n" +
                "Module context: " + moduleContext + "\n\n" +
                "Student code:\n```java\n" + code + "\n```\n\n" +
                "Format your response as:\nSCORE: [number]\nFEEDBACK: [your feedback]";

        Map<String, Object> request;
        if ("openai".equalsIgnoreCase(protocol)) {
            request = new LinkedHashMap<>();
            request.put("model", model);
            request.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            request.put("stream", false);
        } else {
            request = new LinkedHashMap<>();
            request.put("model", model);
            request.put("prompt", prompt);
            request.put("stream", false);
        }

        return callModel(request);
    }

    private String callModel(Map<String, Object> request) {
        try {
            String uri = "openai".equalsIgnoreCase(protocol) ? "/v1/chat/completions" : "/api/generate";
            Map result = restClient.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (result == null) return "No response from model.";
            if ("openai".equalsIgnoreCase(protocol)) {
                var choices = (List<Map<String, Object>>) result.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    var message = (Map<String, Object>) choices.get(0).get("message");
                    Object content = message != null ? message.get("content") : null;
                    return content != null ? content.toString() : "No response from model.";
                }
                return "No response from model.";
            }
            return (String) result.getOrDefault("response", "No response from model.");
        } catch (Exception e) {
            return "Could not reach the AI backend at " + baseUrl + ". Is the service running?";
        }
    }

    public List<AiConversation> getConversationHistory() {
        return conversationRepository.findAllByOrderByCreatedAtAsc();
    }

    private String buildSystemPrompt(CourseModule mod, String contextType) {
        String base = "You are a friendly Java tutor helping a student learn Java backend development. " +
                "The student is working through a gamified prerequisite course before starting Spring Boot. " +
                "Keep explanations clear, concise, and practical. Use code examples when helpful. " +
                "Do not solve the student's code challenges for them — guide them to the answer.";

        if (mod != null && contextType != null) {
            String contextDesc = switch (contextType) {
                case "hint" -> "The student is asking for a HINT on a quiz question or code challenge.";
                case "explain" -> "The student wants you to EXPLAIN a concept from this module in more detail.";
                case "ask" -> "The student has a general question related to this module's content.";
                default -> "";
            };
            return base + "\n\nCurrent module: " + mod.getTitle() +
                    "\nModule topics: " + mod.getDescription() +
                    "\nContext: " + contextDesc;
        }

        return base;
    }

    private static class ConversationLogger {
        static void log(AiConversationRepository repo, String role, String msg, CourseModule mod, String ctxType) {
            AiConversation c = new AiConversation();
            c.setRole(role);
            c.setMessage(msg);
            c.setModule(mod);
            c.setContextType(ctxType);
            repo.save(c);
        }
    }
}
