package com.gamifiedjava.repository;

import com.gamifiedjava.auth.CurrentUserContext;
import com.gamifiedjava.model.ChallengeSubmission;
import com.gamifiedjava.model.CourseModule;
import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.studio.StudioRepository;
import com.gamifiedjava.studio.Ts;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ChallengeSubmissionRepository extends StudioRepository<ChallengeSubmission> {

    private final ModuleRepository moduleRepository;

    public ChallengeSubmissionRepository(StudioClient client, ModuleRepository moduleRepository,
                                         CurrentUserContext users) {
        super(client, "challenge_submission", users);
        this.moduleRepository = moduleRepository;
    }

    @Override protected String ownerColumn() { return "auth_user_id"; }

    @Override
    protected Map<String, Object> toRow(ChallengeSubmission s) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("module_id", s.getModule() != null ? s.getModule().getId() : null);
        row.put("source_code", s.getSourceCode());
        row.put("compile_output", s.getCompileOutput());
        row.put("compile_success", s.getCompileSuccess());
        row.put("ai_feedback", s.getAiFeedback());
        row.put("ai_score", s.getAiScore());
        row.put("passed", s.getPassed());
        row.put("submitted_at", Ts.iso(s.getSubmittedAt()));
        return row;
    }

    @Override
    protected ChallengeSubmission fromRow(Map<String, Object> r) {
        ChallengeSubmission s = new ChallengeSubmission();
        s.setId(asInt(r.get("id")));
        Integer moduleId = asInt(r.get("module_id"));
        s.setModule(moduleRepository.reference(moduleId));
        s.setSourceCode(str(r.get("source_code")));
        s.setCompileOutput(str(r.get("compile_output")));
        s.setCompileSuccess(asBool(r.get("compile_success")));
        s.setAiFeedback(str(r.get("ai_feedback")));
        s.setAiScore(asInt(r.get("ai_score")));
        s.setPassed(asBool(r.get("passed")));
        s.setSubmittedAt(dt(r.get("submitted_at")));
        return s;
    }

    @Override
    protected Integer idOf(ChallengeSubmission s) {
        return s.getId();
    }

    @Override
    protected void setId(ChallengeSubmission s, Integer id) {
        s.setId(id);
    }

    public List<ChallengeSubmission> findByModuleIdOrderBySubmittedAtDesc(Integer moduleId) {
        List<ChallengeSubmission> all = findBy("module_id", moduleId);
        all.sort(Comparator.comparing(ChallengeSubmission::getSubmittedAt,
                Comparator.nullsFirst(Comparator.reverseOrder())));
        return all;
    }
}
