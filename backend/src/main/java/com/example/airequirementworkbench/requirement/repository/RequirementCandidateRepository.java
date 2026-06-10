package com.example.airequirementworkbench.requirement.repository;

import com.example.airequirementworkbench.requirement.entity.RequirementCandidate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementCandidateRepository extends JpaRepository<RequirementCandidate, Long> {
  List<RequirementCandidate> findBySessionIdAndDeletedFalseOrderByUpdatedAtDesc(Long sessionId);
  long countBySessionIdAndDeletedFalse(Long sessionId);
}
