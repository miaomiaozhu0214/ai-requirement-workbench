package com.example.airequirementworkbench.masterdata.controller;

import com.example.airequirementworkbench.common.response.ApiResponse;
import com.example.airequirementworkbench.masterdata.dto.ProductLineDto;
import com.example.airequirementworkbench.masterdata.dto.ProductModuleDto;
import com.example.airequirementworkbench.masterdata.service.MasterDataService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master-data")
public class MasterDataController {
  private final MasterDataService masterDataService;

  public MasterDataController(MasterDataService masterDataService) {
    this.masterDataService = masterDataService;
  }

  @GetMapping("/product-lines")
  public ApiResponse<List<ProductLineDto>> productLines() {
    return ApiResponse.ok(masterDataService.productLines());
  }

  @GetMapping("/product-lines/{lineId}/modules")
  public ApiResponse<List<ProductModuleDto>> modules(@PathVariable Long lineId) {
    return ApiResponse.ok(masterDataService.modules(lineId));
  }
}
