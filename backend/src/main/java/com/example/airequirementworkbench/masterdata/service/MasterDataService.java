package com.example.airequirementworkbench.masterdata.service;

import com.example.airequirementworkbench.masterdata.dto.ProductLineDto;
import com.example.airequirementworkbench.masterdata.dto.ProductModuleDto;
import com.example.airequirementworkbench.masterdata.repository.ProductLineRepository;
import com.example.airequirementworkbench.masterdata.repository.ProductModuleRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MasterDataService {
  private final ProductLineRepository productLineRepository;
  private final ProductModuleRepository productModuleRepository;

  public MasterDataService(ProductLineRepository productLineRepository, ProductModuleRepository productModuleRepository) {
    this.productLineRepository = productLineRepository;
    this.productModuleRepository = productModuleRepository;
  }

  public List<ProductLineDto> productLines() {
    return productLineRepository.findByDeletedFalseAndStatusOrderByLineNameAsc("enabled").stream()
        .map(line -> new ProductLineDto(line.getId(), line.getLineCode(), line.getLineName()))
        .toList();
  }

  public List<ProductModuleDto> modules(Long productLineId) {
    return productModuleRepository.findByProductLineIdAndDeletedFalseAndStatusOrderByModuleNameAsc(productLineId, "enabled").stream()
        .map(module -> new ProductModuleDto(module.getId(), module.getProductLineId(), module.getModuleCode(), module.getModuleName()))
        .toList();
  }
}
