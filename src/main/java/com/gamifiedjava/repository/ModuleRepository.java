package com.gamifiedjava.repository;

import com.gamifiedjava.model.CourseModule;
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
public class ModuleRepository extends StudioRepository<CourseModule> {

    public ModuleRepository(StudioClient client) {
        super(client, "course_module");
    }

    @Override
    protected Map<String, Object> toRow(CourseModule m) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("title", m.getTitle());
        row.put("slug", m.getSlug());
        row.put("description", m.getDescription());
        row.put("content_markdown", m.getContentMarkdown());
        row.put("order_index", m.getOrderIndex());
        row.put("xp_reward", m.getXpReward());
        row.put("challenge_instructions", m.getChallengeInstructions());
        row.put("challenge_template_code", m.getChallengeTemplateCode());
        row.put("created_at", Ts.iso(m.getCreatedAt()));
        return row;
    }

    @Override
    protected CourseModule fromRow(Map<String, Object> r) {
        CourseModule m = new CourseModule();
        m.setId(asInt(r.get("id")));
        m.setTitle(str(r.get("title")));
        m.setSlug(str(r.get("slug")));
        m.setDescription(str(r.get("description")));
        m.setContentMarkdown(str(r.get("content_markdown")));
        m.setOrderIndex(asInt(r.get("order_index")));
        Integer reward = asInt(r.get("xp_reward"));
        m.setXpReward(reward != null ? reward : 100);
        m.setChallengeInstructions(str(r.get("challenge_instructions")));
        m.setChallengeTemplateCode(str(r.get("challenge_template_code")));
        m.setCreatedAt(dt(r.get("created_at")));
        return m;
    }

    @Override
    protected Integer idOf(CourseModule m) {
        return m.getId();
    }

    @Override
    protected void setId(CourseModule m, Integer id) {
        m.setId(id);
    }

    public Optional<CourseModule> findBySlug(String slug) {
        List<CourseModule> matches = findBy("slug", slug);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    public List<CourseModule> findAllByOrderByOrderIndexAsc() {
        List<CourseModule> all = findAll();
        all.sort(Comparator.comparingInt(m -> m.getOrderIndex() == null ? Integer.MAX_VALUE : m.getOrderIndex()));
        return all;
    }

    /**
     * Creates an id-only relationship reference without another Studio API call.
     * Repository row mappers only need the foreign key for subsequent writes and
     * service-level comparisons; callers that need module details load them
     * explicitly through this repository.
     */
    public CourseModule reference(Integer id) {
        if (id == null) return null;
        CourseModule module = new CourseModule();
        module.setId(id);
        return module;
    }
}
