package com.gamifiedjava.repository;

import com.gamifiedjava.auth.CurrentUserContext;
import com.gamifiedjava.model.AiChat;
import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.studio.StudioRepository;
import com.gamifiedjava.studio.Ts;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AiChatRepository extends StudioRepository<AiChat> {

    public AiChatRepository(StudioClient client, CurrentUserContext users) {
        super(client, "ai_chat", users);
    }

    @Override protected String ownerColumn() { return "auth_user_id"; }

    @Override
    protected Map<String, Object> toRow(AiChat chat) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("auth_user_id", chat.getAuthUserId());
        row.put("title", chat.getTitle());
        row.put("created_at", Ts.iso(chat.getCreatedAt()));
        row.put("updated_at", Ts.iso(chat.getUpdatedAt()));
        return row;
    }

    @Override
    protected AiChat fromRow(Map<String, Object> row) {
        AiChat chat = new AiChat();
        chat.setId(asInt(row.get("id")));
        chat.setAuthUserId(str(row.get("auth_user_id")));
        chat.setTitle(str(row.get("title")));
        chat.setCreatedAt(dt(row.get("created_at")));
        chat.setUpdatedAt(dt(row.get("updated_at")));
        return chat;
    }

    @Override
    protected Integer idOf(AiChat chat) { return chat.getId(); }

    @Override
    protected void setId(AiChat chat, Integer id) { chat.setId(id); }

    public List<AiChat> findByAuthUserIdOrderByUpdatedAtDesc(String authUserId) {
        List<AiChat> chats = findBy("auth_user_id", authUserId);
        chats.sort(Comparator.comparing(AiChat::getUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return chats;
    }
}
