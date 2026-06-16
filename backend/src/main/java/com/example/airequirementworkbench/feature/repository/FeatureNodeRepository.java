package com.example.airequirementworkbench.feature.repository;

import com.example.airequirementworkbench.feature.entity.FeatureNode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeatureNodeRepository extends JpaRepository<FeatureNode, Long> {
  List<FeatureNode> findByProductLineIdAndDeletedFalseOrderBySortOrderAscCreatedAtAsc(Long productLineId);

  List<FeatureNode> findByProductLineIdAndParentIdAndDeletedFalseOrderBySortOrderAscCreatedAtAsc(Long productLineId, Long parentId);

  List<FeatureNode> findByProductLineIdAndDeletedFalseAndParentIdIsNullOrderBySortOrderAscCreatedAtAsc(Long productLineId);

  @Query("""
      select n from FeatureNode n
      where n.productLineId = :productLineId
        and n.deleted = false
        and ((:parentId is null and n.parentId is null) or n.parentId = :parentId)
      order by n.sortOrder asc, n.createdAt asc
      """)
  List<FeatureNode> findSiblings(@Param("productLineId") Long productLineId, @Param("parentId") Long parentId);

  @Query("""
      select coalesce(max(n.sortOrder), -1) from FeatureNode n
      where n.productLineId = :productLineId
        and n.deleted = false
        and ((:parentId is null and n.parentId is null) or n.parentId = :parentId)
      """)
  int maxSortOrder(@Param("productLineId") Long productLineId, @Param("parentId") Long parentId);

  @Query("""
      select count(n) > 0 from FeatureNode n
      where n.productLineId = :productLineId
        and n.deleted = false
        and ((:parentId is null and n.parentId is null) or n.parentId = :parentId)
        and lower(n.name) = lower(:name)
        and (:excludeId is null or n.id <> :excludeId)
      """)
  boolean existsSiblingName(
      @Param("productLineId") Long productLineId,
      @Param("parentId") Long parentId,
      @Param("name") String name,
      @Param("excludeId") Long excludeId);
}
