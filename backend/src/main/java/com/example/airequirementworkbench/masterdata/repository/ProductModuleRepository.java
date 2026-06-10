package com.example.airequirementworkbench.masterdata.repository;

import com.example.airequirementworkbench.masterdata.entity.ProductModule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductModuleRepository extends JpaRepository<ProductModule, Long> {
  List<ProductModule> findByProductLineIdAndDeletedFalseAndStatusOrderByModuleNameAsc(Long productLineId, String status);
}
