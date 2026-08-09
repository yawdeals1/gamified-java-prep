package com.gamifiedjava.repository;

import com.gamifiedjava.model.CourseModule;
import com.gamifiedjava.model.LessonStep;
import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.studio.StudioRepository;
import com.gamifiedjava.studio.Ts;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class LessonStepRepository extends StudioRepository<LessonStep> {

    private final ModuleRepository moduleRepository;

    public LessonStepRepository(StudioClient client, ModuleRepository moduleRepository) {
        super(client, "lesson_step");
        this.moduleRepository = moduleRepository;
    }

    @Override
    protected Map<String, Object> toRow(LessonStep s) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("module_id", s.getModule() != null ? s.getModule().getId() : null);
        row.put("order_index", s.getOrderIndex());
        row.put("type", s.getType());
        row.put("title", s.getTitle());
        row.put("body_markdown", s.getBodyMarkdown());
        row.put("code", s.getCode());
        row.put("solution", s.getSolution());
        row.put("options", s.getOptions());
        row.put("correct_index", s.getCorrectIndex());
        row.put("expected_output", s.getExpectedOutput());
        row.put("xp_reward", s.getXpReward());
        return row;
    }

    @Override
    protected LessonStep fromRow(Map<String, Object> r) {
        LessonStep s = new LessonStep();
        s.setId(asInt(r.get("id")));
        Integer moduleId = asInt(r.get("module_id"));
        s.setModule(moduleRepository.reference(moduleId));
        s.setOrderIndex(asInt(r.get("order_index")));
        s.setType(str(r.get("type")));
        s.setTitle(str(r.get("title")));
        s.setBodyMarkdown(str(r.get("body_markdown")));
        s.setCode(str(r.get("code")));
        s.setSolution(str(r.get("solution")));
        s.setOptions(str(r.get("options")));
        s.setCorrectIndex(asInt(r.get("correct_index")));
        s.setExpectedOutput(str(r.get("expected_output")));
        s.setXpReward(asInt(r.get("xp_reward")));
        return s;
    }

    @Override
    protected Integer idOf(LessonStep s) {
        return s.getId();
    }

    @Override
    protected void setId(LessonStep s, Integer id) {
        s.setId(id);
    }

    public List<LessonStep> findByModuleIdOrderByOrderIndexAsc(Integer moduleId) {
        List<LessonStep> all = findBy("module_id", moduleId);
        all.sort(Comparator.comparingInt(s -> s.getOrderIndex() == null ? Integer.MAX_VALUE : s.getOrderIndex()));
        return all;
    }

    public long countByModuleId(Integer moduleId) {
        return findBy("module_id", moduleId).size();
    }

    /** One Studio API request for every module's step count. */
    public Map<Integer, Long> countByModule() {
        Map<Integer, Long> counts = new HashMap<>();
        for (Map<String, Object> row : client.list("lesson_step", null, 10000)) {
            Integer moduleId = asInt(row.get("module_id"));
            if (moduleId != null) counts.merge(moduleId, 1L, Long::sum);
        }
        return counts;
    }
}
