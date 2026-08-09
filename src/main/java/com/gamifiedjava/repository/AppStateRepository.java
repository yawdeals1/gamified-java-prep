package com.gamifiedjava.repository;

import com.gamifiedjava.model.AppState;
import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.studio.StudioRepository;
import com.gamifiedjava.studio.Ts;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class AppStateRepository extends StudioRepository<AppState> {

    public AppStateRepository(StudioClient client) {
        super(client, "app_state");
    }

    @Override
    protected Map<String, Object> toRow(AppState s) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("total_xp", s.getTotalXp());
        row.put("current_level", s.getCurrentLevel());
        row.put("streak_count", s.getStreakCount());
        row.put("last_active_date", Ts.iso(s.getLastActiveDate()));
        row.put("created_at", Ts.iso(s.getCreatedAt()));
        row.put("updated_at", Ts.iso(s.getUpdatedAt()));
        return row;
    }

    @Override
    protected AppState fromRow(Map<String, Object> r) {
        AppState s = new AppState();
        s.setId(asInt(r.get("id")));
        s.setTotalXp(asInt(r.get("total_xp")));
        s.setCurrentLevel(asInt(r.get("current_level")));
        s.setStreakCount(asInt(r.get("streak_count")));
        s.setLastActiveDate(date(r.get("last_active_date")));
        s.setCreatedAt(dt(r.get("created_at")));
        s.setUpdatedAt(dt(r.get("updated_at")));
        return s;
    }

    @Override
    protected Integer idOf(AppState s) {
        return s.getId();
    }

    @Override
    protected void setId(AppState s, Integer id) {
        s.setId(id);
    }

    public AppState getOrCreate() {
        return findById(1).orElseGet(() -> {
            AppState s = new AppState();
            s.setId(1);
            return save(s);
        });
    }
}