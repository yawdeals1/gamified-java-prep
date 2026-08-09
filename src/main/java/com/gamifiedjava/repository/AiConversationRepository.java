package com.gamifiedjava.repository;

import com.gamifiedjava.model.AiConversation;
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
public class AiConversationRepository extends StudioRepository<AiConversation> {

    private final ModuleRepository moduleRepository;

    public AiConversationRepository(StudioClient client, ModuleRepository moduleRepository) {
        super(client, "ai_conversation");
        this.moduleRepository = moduleRepository;
    }

    @Override
    protected Map<String, Object> toRow(AiConversation c) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("role", c.getRole());
        row.put("message", c.getMessage());
        row.put("module_id", c.getModule() != null ? c.getModule().getId() : null);
        row.put("context_type", c.getContextType());
        row.put("created_at", Ts.iso(c.getCreatedAt()));
        return row;
    }

    @Override
    protected AiConversation fromRow(Map<String, Object> r) {
        AiConversation c = new AiConversation();
        c.setId(asInt(r.get("id")));
        c.setRole(str(r.get("role")));
        c.setMessage(str(r.get("message")));
        Integer moduleId = asInt(r.get("module_id"));
        c.setModule(moduleId != null ? moduleRepository.findById(moduleId).orElse(null) : null);
        c.setContextType(str(r.get("context_type")));
        c.setCreatedAt(dt(r.get("created_at")));
        return c;
    }

    @Override
    protected Integer idOf(AiConversation c) {
        return c.getId();
    }

    @Override
    protected void setId(AiConversation c, Integer id) {
        c.setId(id);
    }

    public List<AiConversation> findAllByOrderByCreatedAtAsc() {
        List<AiConversation> all = findAll();
        all.sort(Comparator.comparing(AiConversation::getCreatedAt,
                Comparator.nullsFirst(Comparator.naturalOrder())));
        return all;
    }

    public List<AiConversation> findByModuleIdOrderByCreatedAtAsc(Integer moduleId) {
        List<AiConversation> all = findBy("module_id", moduleId);
        all.sort(Comparator.comparing(AiConversation::getCreatedAt,
                Comparator.nullsFirst(Comparator.naturalOrder())));
        return all;
    }
}