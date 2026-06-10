package com.example.airequirementworkbench.masterdata.repository;

import com.example.airequirementworkbench.masterdata.entity.ProductLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductLineRepository extends JpaRepository<ProductLine, Long> {
  List<ProductLine> findByDeletedFalseAndStatusOrderByLineNameAsc(String status);
}
