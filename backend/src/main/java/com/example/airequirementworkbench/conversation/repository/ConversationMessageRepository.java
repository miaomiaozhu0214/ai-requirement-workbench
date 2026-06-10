package com.example.airequirementworkbench.conversation.repository;

import com.example.airequirementworkbench.conversation.entity.ConversationMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
  List<ConversationMessage> findBySessionIdAndDeletedFalseOrderByCreatedAtAsc(Long sessionId);
}
