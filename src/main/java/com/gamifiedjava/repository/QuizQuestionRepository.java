package com.gamifiedjava.repository;

import com.gamifiedjava.model.CourseModule;
import com.gamifiedjava.model.QuizQuestion;
import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.studio.StudioRepository;
import com.gamifiedjava.studio.Ts;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class QuizQuestionRepository extends StudioRepository<QuizQuestion> {

    private final ModuleRepository moduleRepository;

    public QuizQuestionRepository(StudioClient client, ModuleRepository moduleRepository) {
        super(client, "quiz_question");
        this.moduleRepository = moduleRepository;
    }

    @Override
    protected Map<String, Object> toRow(QuizQuestion q) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("module_id", q.getModule() != null ? q.getModule().getId() : null);
        row.put("question_text", q.getQuestionText());
        row.put("options", q.getOptions());
        row.put("correct_index", q.getCorrectIndex());
        row.put("explanation", q.getExplanation());
        row.put("difficulty", q.getDifficulty());
        row.put("created_at", Ts.iso(q.getCreatedAt()));
        return row;
    }

    @Override
    protected QuizQuestion fromRow(Map<String, Object> r) {
        QuizQuestion q = new QuizQuestion();
        q.setId(asInt(r.get("id")));
        Integer moduleId = asInt(r.get("module_id"));
        q.setModule(moduleId != null ? moduleRepository.findById(moduleId).orElse(null) : null);
        q.setQuestionText(str(r.get("question_text")));
        q.setOptions(str(r.get("options")));
        q.setCorrectIndex(asInt(r.get("correct_index")));
        q.setExplanation(str(r.get("explanation")));
        q.setDifficulty(str(r.get("difficulty")));
        q.setCreatedAt(dt(r.get("created_at")));
        return q;
    }

    @Override
    protected Integer idOf(QuizQuestion q) {
        return q.getId();
    }

    @Override
    protected void setId(QuizQuestion q, Integer id) {
        q.setId(id);
    }

    public List<QuizQuestion> findByModuleId(Integer moduleId) {
        return findBy("module_id", moduleId);
    }

    public long countByModuleId(Integer moduleId) {
        List<QuizQuestion> q = findBy("module_id", moduleId);
        return q.size();
    }
}