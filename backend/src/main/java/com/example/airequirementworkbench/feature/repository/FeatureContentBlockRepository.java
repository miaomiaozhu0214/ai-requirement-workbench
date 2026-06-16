package com.example.airequirementworkbench.feature.repository;

import com.example.airequirementworkbench.feature.entity.FeatureContentBlock;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeatureContentBlockRepository extends JpaRepository<FeatureContentBlock, Long> {
  List<FeatureContentBlock> findByFeatureIdInAndDeletedFalseOrderBySortOrderAscCreatedAtAsc(Collection<Long> featureIds);

  List<FeatureContentBlock> findByFeatureIdAndDeletedFalseOrderBySortOrderAscCreatedAtAsc(Long featureId);

  @Query("""
      select coalesce(max(b.sortOrder), -1) from FeatureContentBlock b
      where b.featureId = :featureId and b.deleted = false
      """)
  int maxSortOrder(@Param("featureId") Long featureId);
}
