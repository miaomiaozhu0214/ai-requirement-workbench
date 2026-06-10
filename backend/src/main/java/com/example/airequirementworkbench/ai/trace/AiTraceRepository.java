package com.example.airequirementworkbench.ai.trace;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiTraceRepository extends JpaRepository<AiTrace, Long> {
  List<AiTrace> findBySessionIdOrderByCreatedAtDesc(Long sessionId);
  List<AiTrace> findTop50ByOrderByCreatedAtDesc();
}
