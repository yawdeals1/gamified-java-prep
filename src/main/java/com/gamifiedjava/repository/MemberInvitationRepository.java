package com.gamifiedjava.repository;

import com.gamifiedjava.model.MemberInvitation;
import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.studio.StudioRepository;
import com.gamifiedjava.studio.Ts;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class MemberInvitationRepository extends StudioRepository<MemberInvitation> {
    public MemberInvitationRepository(StudioClient client) { super(client, "member_invitation"); }
    @Override protected Map<String, Object> toRow(MemberInvitation i) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("email", i.getEmail());
        row.put("token_hash", i.getTokenHash());
        row.put("status", i.getStatus().name());
        row.put("invited_by", i.getInvitedBy());
        row.put("expires_at", Ts.iso(i.getExpiresAt()));
        row.put("accepted_at", Ts.iso(i.getAcceptedAt()));
        return row;
    }
    @Override protected MemberInvitation fromRow(Map<String, Object> r) {
        MemberInvitation i = new MemberInvitation();
        i.setId(asInt(r.get("id")));
        i.setEmail(str(r.get("email")));
        i.setTokenHash(str(r.get("token_hash")));
        i.setStatus(MemberInvitation.Status.valueOf(str(r.get("status"))));
        i.setInvitedBy(str(r.get("invited_by")));
        i.setExpiresAt(dt(r.get("expires_at")));
        i.setAcceptedAt(dt(r.get("accepted_at")));
        i.setCreatedAt(dt(r.get("created_at")));
        return i;
    }
    @Override protected Integer idOf(MemberInvitation i) { return i.getId(); }
    @Override protected void setId(MemberInvitation i, Integer id) { i.setId(id); }
    public Optional<MemberInvitation> findByTokenHash(String hash) {
        List<MemberInvitation> rows = findBy("token_hash", hash);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
    public List<MemberInvitation> findPending() { return findBy("status", MemberInvitation.Status.PENDING.name()); }
}
