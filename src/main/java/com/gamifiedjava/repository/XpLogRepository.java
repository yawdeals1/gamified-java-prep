package com.gamifiedjava.repository;

import com.gamifiedjava.auth.CurrentUserContext;
import com.gamifiedjava.model.XpLog;
import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.studio.StudioRepository;
import com.gamifiedjava.studio.Ts;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class XpLogRepository extends StudioRepository<XpLog> {

    public XpLogRepository(StudioClient client, CurrentUserContext users) {
        super(client, "xp_log", users);
    }

    @Override protected String ownerColumn() { return "auth_user_id"; }

    @Override
    protected Map<String, Object> toRow(XpLog l) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("action", l.getAction());
        row.put("xp_gained", l.getXpGained());
        row.put("note", l.getNote());
        row.put("created_at", Ts.iso(l.getCreatedAt()));
        return row;
    }

    @Override
    protected XpLog fromRow(Map<String, Object> r) {
        XpLog l = new XpLog();
        l.setId(asInt(r.get("id")));
        l.setAction(str(r.get("action")));
        l.setXpGained(asInt(r.get("xp_gained")));
        l.setNote(str(r.get("note")));
        l.setCreatedAt(dt(r.get("created_at")));
        return l;
    }

    @Override
    protected Integer idOf(XpLog l) {
        return l.getId();
    }

    @Override
    protected void setId(XpLog l, Integer id) {
        l.setId(id);
    }

    public List<XpLog> findAllByOrderByCreatedAtDesc() {
        List<XpLog> all = findAll();
        all.sort(Comparator.comparing(XpLog::getCreatedAt,
                Comparator.nullsFirst(Comparator.reverseOrder())));
        return all;
    }
}
