package com.gamifiedjava.repository;

import com.gamifiedjava.model.StepProgress;
import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.studio.StudioRepository;
import com.gamifiedjava.studio.Ts;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class StepProgressRepository extends StudioRepository<StepProgress> {

    public StepProgressRepository(StudioClient client) {
        super(client, "step_progress");
    }

    @Override
    protected Map<String, Object> toRow(StepProgress p) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("step_id", p.getStepId());
        row.put("module_id", p.getModuleId());
        row.put("attempts", p.getAttempts());
        row.put("completed_at", Ts.iso(p.getCompletedAt()));
        return row;
    }

    @Override
    protected StepProgress fromRow(Map<String, Object> r) {
        StepProgress p = new StepProgress();
        p.setId(asInt(r.get("id")));
        p.setStepId(asInt(r.get("step_id")));
        p.setModuleId(asInt(r.get("module_id")));
        p.setAttempts(asInt(r.get("attempts")));
        p.setCompletedAt(dt(r.get("completed_at")));
        return p;
    }

    @Override
    protected Integer idOf(StepProgress p) {
        return p.getId();
    }

    @Override
    protected void setId(StepProgress p, Integer id) {
        p.setId(id);
    }

    public Optional<StepProgress> findByStepId(Integer stepId) {
        List<StepProgress> matches = findBy("step_id", stepId);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    public List<StepProgress> findByModuleId(Integer moduleId) {
        return findBy("module_id", moduleId);
    }
}