package com.gamifiedjava.repository;

import com.gamifiedjava.model.AppUser;
import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.studio.StudioRepository;
import com.gamifiedjava.studio.Ts;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AppUserRepository extends StudioRepository<AppUser> {
    public AppUserRepository(StudioClient client) { super(client, "app_user"); }

    @Override protected Map<String, Object> toRow(AppUser u) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("auth_user_id", u.getAuthUserId());
        row.put("email", u.getEmail());
        row.put("display_name", u.getDisplayName());
        row.put("role", u.getRole().name());
        row.put("active", u.isActive());
        row.put("updated_at", Ts.iso(u.getUpdatedAt()));
        return row;
    }
    @Override protected AppUser fromRow(Map<String, Object> r) {
        AppUser u = new AppUser();
        u.setId(asInt(r.get("id")));
        u.setAuthUserId(str(r.get("auth_user_id")));
        u.setEmail(str(r.get("email")));
        u.setDisplayName(str(r.get("display_name")));
        u.setRole(AppUser.Role.valueOf(str(r.get("role"))));
        u.setActive(Boolean.TRUE.equals(asBool(r.get("active"))));
        u.setCreatedAt(dt(r.get("created_at")));
        u.setUpdatedAt(dt(r.get("updated_at")));
        return u;
    }
    @Override protected Integer idOf(AppUser u) { return u.getId(); }
    @Override protected void setId(AppUser u, Integer id) { u.setId(id); }

    public Optional<AppUser> findByAuthUserId(String id) { return first(findBy("auth_user_id", id)); }
    public Optional<AppUser> findByEmail(String email) { return first(findBy("email", email)); }
    public List<AppUser> findActive() { return findBy("active", true); }
    private Optional<AppUser> first(List<AppUser> rows) { return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0)); }
}
