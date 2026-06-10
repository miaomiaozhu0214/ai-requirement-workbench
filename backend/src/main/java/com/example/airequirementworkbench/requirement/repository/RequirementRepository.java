package com.example.airequirementworkbench.requirement.repository;

import com.example.airequirementworkbench.requirement.entity.Requirement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementRepository extends JpaRepository<Requirement, Long> {
  List<Requirement> findByDeletedFalseOrderByUpdatedAtDesc();
  List<Requirement> findBySourceSessionIdAndDeletedFalseOrderByUpdatedAtDesc(Long sourceSessionId);
  long countBySourceSessionIdAndDeletedFalse(Long sourceSessionId);
}
