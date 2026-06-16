package com.example.airequirementworkbench.feature.repository;

import com.example.airequirementworkbench.feature.entity.FeatureHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureHistoryRepository extends JpaRepository<FeatureHistory, Long> {
  List<FeatureHistory> findByFeatureIdOrderByCreatedAtDesc(Long featureId);
}
