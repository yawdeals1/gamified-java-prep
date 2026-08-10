package com.gamifiedjava.repository;

import com.gamifiedjava.model.UserAiSettings;
import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.studio.StudioRepository;
import com.gamifiedjava.studio.Ts;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserAiSettingsRepository extends StudioRepository<UserAiSettings> {
    public UserAiSettingsRepository(StudioClient client) { super(client, "user_ai_settings"); }
    @Override protected Map<String, Object> toRow(UserAiSettings s) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("auth_user_id", s.getAuthUserId());
        row.put("encrypted_api_key", s.getEncryptedApiKey());
        row.put("key_last_four", s.getKeyLastFour());
        row.put("updated_at", Ts.iso(s.getUpdatedAt()));
        return row;
    }
    @Override protected UserAiSettings fromRow(Map<String, Object> r) {
        UserAiSettings s = new UserAiSettings();
        s.setId(asInt(r.get("id")));
        s.setAuthUserId(str(r.get("auth_user_id")));
        s.setEncryptedApiKey(str(r.get("encrypted_api_key")));
        s.setKeyLastFour(str(r.get("key_last_four")));
        s.setUpdatedAt(dt(r.get("updated_at")));
        return s;
    }
    @Override protected Integer idOf(UserAiSettings s) { return s.getId(); }
    @Override protected void setId(UserAiSettings s, Integer id) { s.setId(id); }
    public Optional<UserAiSettings> findByAuthUserId(String id) {
        List<UserAiSettings> rows = findBy("auth_user_id", id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
