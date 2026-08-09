package com.gamifiedjava.controller;

import com.gamifiedjava.config.RateLimiter;
import com.gamifiedjava.model.QuizQuestion;
import com.gamifiedjava.repository.QuizQuestionRepository;
import com.gamifiedjava.service.CodeRunnerService;
import com.gamifiedjava.service.GamificationService;
import com.gamifiedjava.service.LessonStepService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** JSON API backing the Lesson Player: run code + check a step. XP is awarded server-side. */
@Validated
@RestController
@RequestMapping("/api")
public class LessonApiController {

    private static final int MAX_CODE_CHARS = 50_000;
    private static final int MAX_ANSWER_CHARS = 500;

    private final CodeRunnerService codeRunner;
    private final LessonStepService lessonStepService;
    private final GamificationService gamificationService;
    private final QuizQuestionRepository quizQuestionRepository;
    private final RateLimiter rateLimiter;

    public LessonApiController(CodeRunnerService codeRunner,
                               LessonStepService lessonStepService,
                               GamificationService gamificationService,
                               QuizQuestionRepository quizQuestionRepository,
                               RateLimiter rateLimiter) {
        this.codeRunner = codeRunner;
        this.lessonStepService = lessonStepService;
        this.gamificationService = gamificationService;
        this.quizQuestionRepository = quizQuestionRepository;
        this.rateLimiter = rateLimiter;
    }

    /** Compile + run arbitrary Java; returns compile status, stdout, stderr. */
    @PostMapping("/run")
    public Object run(@Valid @RequestBody RunRequest req) {
        if (!rateLimiter.tryAcquire()) {
            return Map.of("compiled", false, "compileSuccess", false,
                    "stdout", "", "stderr", "Too many requests — try again shortly.", "timedOut", false);
        }
        try {
            return codeRunner.run(req.sourceCode());
        } finally {
            rateLimiter.release();
        }
    }

    /** Validate a step answer, award XP once, return feedback + fresh state. */
    @PostMapping("/steps/{id}/check")
    public Map<String, Object> check(@PathVariable @Positive Integer id, @Valid @RequestBody CheckRequest req) {
        LessonStepService.StepResult result =
                lessonStepService.check(id, req.selectedIndex(), req.answer(), req.code());
        var state = gamificationService.getState();
        return Map.of(
                "correct", result.correct(),
                "xpAwarded", result.xpAwarded(),
                "feedback", result.feedback(),
                "expected", result.expected() == null ? "" : result.expected(),
                "run", result.run() == null ? Map.of() : result.run(),
                "totalXp", state.getTotalXp(),
                "level", state.getCurrentLevel()
        );
    }

    /** Reveal the correct answer + explanation for a quiz question after the student has picked. */
    @PostMapping("/quiz/check")
    public Map<String, Object> quizCheck(@Valid @RequestBody QuizCheckRequest req) {
        QuizQuestion q = quizQuestionRepository.findById(req.questionId()).orElse(null);
        if (q == null) return Map.of("correct", false, "correctIndex", -1, "explanation", "");
        boolean correct = req.selectedIndex() != null && req.selectedIndex().equals(q.getCorrectIndex());
        return Map.of(
                "correct", correct,
                "correctIndex", q.getCorrectIndex(),
                "explanation", q.getExplanation() == null ? "" : q.getExplanation()
        );
    }

    public record RunRequest(@NotNull @Size(max = MAX_CODE_CHARS) String sourceCode) {}
    public record CheckRequest(Integer selectedIndex,
                               @Size(max = MAX_ANSWER_CHARS) String answer,
                               @Size(max = MAX_CODE_CHARS) String code) {}
    public record QuizCheckRequest(@NotNull @Positive Integer questionId, Integer selectedIndex) {}
}