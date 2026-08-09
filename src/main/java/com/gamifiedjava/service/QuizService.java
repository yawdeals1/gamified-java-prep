package com.gamifiedjava.service;

import com.gamifiedjava.model.*;
import com.gamifiedjava.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuizService {

    private final QuizQuestionRepository questionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final ModuleProgressRepository progressRepository;
    private final GamificationService gamificationService;

    public QuizService(QuizQuestionRepository questionRepository,
                       QuizAttemptRepository attemptRepository,
                       ModuleProgressRepository progressRepository,
                       GamificationService gamificationService) {
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.progressRepository = progressRepository;
        this.gamificationService = gamificationService;
    }

    public List<QuizQuestion> getQuestionsForModule(Integer moduleId) {
        return questionRepository.findByModuleId(moduleId);
    }
    public QuizResult submitAnswers(Integer moduleId, Map<Integer, Integer> answers) {
        List<QuizQuestion> questions = questionRepository.findByModuleId(moduleId);
        int correct = 0;
        int total = questions.size();

        List<QuizAttempt> attempts = new ArrayList<>();
        for (QuizQuestion q : questions) {
            Integer selected = answers.get(q.getId());
            boolean isCorrect = selected != null && selected.equals(q.getCorrectIndex());

            QuizAttempt attempt = new QuizAttempt();
            attempt.setModule(q.getModule());
            attempt.setQuestion(q);
            attempt.setSelectedIndex(selected);
            attempt.setCorrect(isCorrect);
            attemptRepository.save(attempt);

            attempts.add(attempt);
            if (isCorrect) correct++;
        }

        int percentage = total > 0 ? (correct * 100) / total : 0;

        ModuleProgress prog = progressRepository.findByModuleId(moduleId).orElse(null);
        if (prog != null) {
            boolean firstAttempt = prog.getQuizAttempts() == 0; // XP once per module, not per re-submit
            prog.setQuizScore(Math.max(prog.getQuizScore(), percentage));
            prog.setQuizAttempts(prog.getQuizAttempts() + 1);
            if (percentage >= 80 && "quiz_ready".equals(prog.getStatus())) {
                prog.setStatus("challenge_ready");
            }
            prog.setUpdatedAt(java.time.LocalDateTime.now());
            progressRepository.save(prog);

            if (firstAttempt) {
                gamificationService.addXp("quiz_attempt", percentage >= 80 ? 100 : 10,
                        "Quiz score: " + percentage + "%");
            }

            if (percentage == 100) {
                gamificationService.unlockIf("Perfect Score", true);
            }
        }

        return new QuizResult(percentage, correct, total, attempts);
    }

    public record QuizResult(int percentage, int correct, int total, List<QuizAttempt> attempts) {}
}
