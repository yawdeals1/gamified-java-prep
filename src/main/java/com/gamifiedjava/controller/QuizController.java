package com.gamifiedjava.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamifiedjava.model.*;
import com.gamifiedjava.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/module/{slug}/quiz")
public class QuizController {

    private final ModuleService moduleService;
    private final QuizService quizService;
    private final GamificationService gamificationService;
    private final ObjectMapper mapper;

    public QuizController(ModuleService moduleService,
                          QuizService quizService,
                          GamificationService gamificationService,
                          ObjectMapper mapper) {
        this.moduleService = moduleService;
        this.quizService = quizService;
        this.gamificationService = gamificationService;
        this.mapper = mapper;
    }

    @GetMapping
    public String showQuiz(@PathVariable String slug, Model model) throws Exception {
        CourseModule mod = moduleService.getBySlug(slug);
        if (mod == null) return "redirect:/";
        ModuleProgress progress = moduleService.getProgress(mod.getId());
        if (!quizAllowed(progress)) return "redirect:/module/" + slug;

        var questions = quizService.getQuestionsForModule(mod.getId());
        if (questions.isEmpty()) return "redirect:/";

        // Build client-safe question list (options visible, answers NOT included)
        List<Map<String, Object>> clientQs = new ArrayList<>();
        for (QuizQuestion q : questions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("text", q.getQuestionText());
            m.put("options", q.getOptionList());
            m.put("difficulty", q.getDifficulty());
            clientQs.add(m);
        }

        model.addAttribute("module", mod);
        model.addAttribute("state", gamificationService.getState());
        model.addAttribute("questionsJson", scriptSafeJson(mapper.writeValueAsString(clientQs)));
        model.addAttribute("questionCount", questions.size());
        return "quiz";
    }

    @PostMapping
    public String submitQuiz(@PathVariable String slug,
                             @RequestParam Map<String, String> allParams,
                             Model model) {
        CourseModule mod = moduleService.getBySlug(slug);
        if (mod == null) return "redirect:/";
        ModuleProgress progress = moduleService.getProgress(mod.getId());
        if (!quizAllowed(progress)) return "redirect:/module/" + slug;

        Map<Integer, Integer> answers = new LinkedHashMap<>();
        for (var entry : allParams.entrySet()) {
            if (entry.getKey().startsWith("q_") && entry.getKey().length() <= 32) {
                try {
                    int qId = Integer.parseInt(entry.getKey().replace("q_", ""));
                    String value = entry.getValue();
                    if (value == null || value.length() > 8) continue; // selected index is tiny
                    int selected = Integer.parseInt(value);
                    if (selected < 0 || selected > 100) continue;
                    answers.put(qId, selected);
                } catch (NumberFormatException ignored) {}
            }
        }

        QuizService.QuizResult result = quizService.submitAnswers(mod.getId(), answers);
        AppState state = gamificationService.getState();

        model.addAttribute("module", mod);
        model.addAttribute("result", result);
        model.addAttribute("state", state);
        return "quiz-result";
    }

    private boolean quizAllowed(ModuleProgress progress) {
        if (progress == null) return false;
        return Set.of("quiz_ready", "challenge_ready", "completed").contains(progress.getStatus());
    }

    private String scriptSafeJson(String json) {
        return json.replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
    }
}
