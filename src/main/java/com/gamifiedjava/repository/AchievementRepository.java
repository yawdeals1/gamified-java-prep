package com.gamifiedjava.repository;

import com.gamifiedjava.auth.CurrentUserContext;
import com.gamifiedjava.model.Achievement;
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
public class AchievementRepository extends StudioRepository<Achievement> {

    public AchievementRepository(StudioClient client, CurrentUserContext users) {
        super(client, "achievement", users);
    }

    @Override protected String ownerColumn() { return "auth_user_id"; }

    @Override
    protected Map<String, Object> toRow(Achievement a) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", a.getName());
        row.put("description", a.getDescription());
        row.put("icon", a.getIcon());
        row.put("unlocked_at", Ts.iso(a.getUnlockedAt()));
        return row;
    }

    @Override
    protected Achievement fromRow(Map<String, Object> r) {
        Achievement a = new Achievement();
        a.setId(asInt(r.get("id")));
        a.setName(str(r.get("name")));
        a.setDescription(str(r.get("description")));
        a.setIcon(str(r.get("icon")));
        a.setUnlockedAt(dt(r.get("unlocked_at")));
        return a;
    }

    @Override
    protected Integer idOf(Achievement a) {
        return a.getId();
    }

    @Override
    protected void setId(Achievement a, Integer id) {
        a.setId(id);
    }

    public Optional<Achievement> findByName(String name) {
        List<Achievement> matches = findBy("name", name);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    public List<Achievement> findAllByOrderByUnlockedAtAsc() {
        List<Achievement> all = findAll();
        all.sort(Comparator.comparing(Achievement::getUnlockedAt,
                Comparator.nullsFirst(Comparator.naturalOrder())));
        return all;
    }

    public long countByUnlockedAtIsNotNull() {
        return findAll().stream().filter(Achievement::isUnlocked).count();
    }
}
