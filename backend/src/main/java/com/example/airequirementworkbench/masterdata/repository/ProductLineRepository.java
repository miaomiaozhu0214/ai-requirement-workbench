package com.example.airequirementworkbench.masterdata.repository;

import com.example.airequirementworkbench.masterdata.entity.ProductLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductLineRepository extends JpaRepository<ProductLine, Long> {
  List<ProductLine> findByDeletedFalseAndStatusOrderByLineNameAsc(String status);

  List<ProductLine> findByDeletedFalseOrderByUpdatedAtDesc();

  @Query("""
      select p from ProductLine p
      where p.deleted = false
        and (
          :keyword is null
          or lower(p.lineName) like lower(concat('%', :keyword, '%'))
          or lower(coalesce(p.lineCode, '')) like lower(concat('%', :keyword, '%'))
        )
      order by p.updatedAt desc
      """)
  List<ProductLine> search(@Param("keyword") String keyword);

  @Query("""
      select count(p) > 0 from ProductLine p
      where p.deleted = false
        and lower(p.lineName) = lower(:lineName)
        and (:excludeId is null or p.id <> :excludeId)
      """)
  boolean existsActiveName(@Param("lineName") String lineName, @Param("excludeId") Long excludeId);

  @Query("""
      select count(p) > 0 from ProductLine p
      where p.deleted = false
        and p.lineCode is not null
        and lower(p.lineCode) = lower(:lineCode)
        and (:excludeId is null or p.id <> :excludeId)
      """)
  boolean existsActiveCode(@Param("lineCode") String lineCode, @Param("excludeId") Long excludeId);
}
