package com.gamifiedjava.repository;

import com.gamifiedjava.auth.CurrentUserContext;
import com.gamifiedjava.model.CourseModule;
import com.gamifiedjava.model.ModuleProgress;
import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.studio.StudioRepository;
import com.gamifiedjava.studio.Ts;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ModuleProgressRepository extends StudioRepository<ModuleProgress> {

    private final ModuleRepository moduleRepository;

    public ModuleProgressRepository(StudioClient client, ModuleRepository moduleRepository,
                                    CurrentUserContext users) {
        super(client, "module_progress", users);
        this.moduleRepository = moduleRepository;
    }

    @Override protected String ownerColumn() { return "auth_user_id"; }

    @Override
    protected Map<String, Object> toRow(ModuleProgress p) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("module_id", p.getModule() != null ? p.getModule().getId() : null);
        row.put("status", p.getStatus());
        row.put("quiz_score", p.getQuizScore());
        row.put("quiz_attempts", p.getQuizAttempts());
        row.put("challenge_passed", p.getChallengePassed());
        row.put("challenge_attempts", p.getChallengeAttempts());
        row.put("completed_at", Ts.iso(p.getCompletedAt()));
        row.put("updated_at", Ts.iso(p.getUpdatedAt()));
        return row;
    }

    @Override
    protected ModuleProgress fromRow(Map<String, Object> r) {
        ModuleProgress p = new ModuleProgress();
        p.setId(asInt(r.get("id")));
        Integer moduleId = asInt(r.get("module_id"));
        p.setModule(moduleRepository.reference(moduleId));
        p.setStatus(str(r.get("status")));
        p.setQuizScore(asInt(r.get("quiz_score")));
        p.setQuizAttempts(asInt(r.get("quiz_attempts")));
        p.setChallengePassed(asBool(r.get("challenge_passed")));
        p.setChallengeAttempts(asInt(r.get("challenge_attempts")));
        p.setCompletedAt(dt(r.get("completed_at")));
        p.setUpdatedAt(dt(r.get("updated_at")));
        return p;
    }

    @Override
    protected Integer idOf(ModuleProgress p) {
        return p.getId();
    }

    @Override
    protected void setId(ModuleProgress p, Integer id) {
        p.setId(id);
    }

    public Optional<ModuleProgress> findByModuleId(Integer moduleId) {
        List<ModuleProgress> matches = findBy("module_id", moduleId);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    public List<ModuleProgress> findAllByOrderByIdAsc() {
        List<ModuleProgress> all = findAll();
        all.sort(Comparator.comparingInt(p -> p.getId() == null ? Integer.MAX_VALUE : p.getId()));
        return all;
    }
}
