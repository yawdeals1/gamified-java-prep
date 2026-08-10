package com.gamifiedjava.repository;

import com.gamifiedjava.auth.CurrentUserContext;
import com.gamifiedjava.model.CourseModule;
import com.gamifiedjava.model.QuizAttempt;
import com.gamifiedjava.model.QuizQuestion;
import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.studio.StudioRepository;
import com.gamifiedjava.studio.Ts;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class QuizAttemptRepository extends StudioRepository<QuizAttempt> {

    private final ModuleRepository moduleRepository;
    private final QuizQuestionRepository questionRepository;

    public QuizAttemptRepository(StudioClient client,
                                 ModuleRepository moduleRepository,
                                 QuizQuestionRepository questionRepository,
                                 CurrentUserContext users) {
        super(client, "quiz_attempt", users);
        this.moduleRepository = moduleRepository;
        this.questionRepository = questionRepository;
    }

    @Override protected String ownerColumn() { return "auth_user_id"; }

    @Override
    protected Map<String, Object> toRow(QuizAttempt a) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("module_id", a.getModule() != null ? a.getModule().getId() : null);
        row.put("question_id", a.getQuestion() != null ? a.getQuestion().getId() : null);
        row.put("selected_index", a.getSelectedIndex());
        row.put("correct", a.getCorrect());
        row.put("attempted_at", Ts.iso(a.getAttemptedAt()));
        return row;
    }

    @Override
    protected QuizAttempt fromRow(Map<String, Object> r) {
        QuizAttempt a = new QuizAttempt();
        a.setId(asInt(r.get("id")));
        Integer moduleId = asInt(r.get("module_id"));
        a.setModule(moduleRepository.reference(moduleId));
        Integer questionId = asInt(r.get("question_id"));
        a.setQuestion(questionRepository.reference(questionId));
        a.setSelectedIndex(asInt(r.get("selected_index")));
        a.setCorrect(asBool(r.get("correct")));
        a.setAttemptedAt(dt(r.get("attempted_at")));
        return a;
    }

    @Override
    protected Integer idOf(QuizAttempt a) {
        return a.getId();
    }

    @Override
    protected void setId(QuizAttempt a, Integer id) {
        a.setId(id);
    }

    public List<QuizAttempt> findByModuleIdOrderByAttemptedAtDesc(Integer moduleId) {
        List<QuizAttempt> all = findBy("module_id", moduleId);
        all.sort(Comparator.comparing(QuizAttempt::getAttemptedAt,
                Comparator.nullsFirst(Comparator.reverseOrder())));
        return all;
    }

    public long countByModuleIdAndCorrectTrue(Integer moduleId) {
        return findBy("module_id", moduleId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getCorrect()))
                .count();
    }

    public long countByModuleId(Integer moduleId) {
        return findBy("module_id", moduleId).size();
    }
}
