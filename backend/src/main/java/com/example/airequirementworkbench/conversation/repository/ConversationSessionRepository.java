package com.example.airequirementworkbench.conversation.repository;

import com.example.airequirementworkbench.conversation.entity.ConversationSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {
  List<ConversationSession> findByDeletedFalseOrderByUpdatedAtDesc();
}
