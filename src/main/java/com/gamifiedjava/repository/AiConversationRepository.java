package com.gamifiedjava.repository;

import com.gamifiedjava.model.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Integer> {
    List<AiConversation> findAllByOrderByCreatedAtAsc();
    List<AiConversation> findByModuleIdOrderByCreatedAtAsc(Integer moduleId);
}
