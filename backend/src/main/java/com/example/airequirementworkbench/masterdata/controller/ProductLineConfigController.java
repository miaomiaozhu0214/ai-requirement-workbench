package com.example.airequirementworkbench.masterdata.controller;

import com.example.airequirementworkbench.common.response.ApiResponse;
import com.example.airequirementworkbench.masterdata.dto.ProductLineConfigDtos.ProductLineDetailDto;
import com.example.airequirementworkbench.masterdata.dto.ProductLineConfigDtos.SaveProductLineRequest;
import com.example.airequirementworkbench.masterdata.service.ProductLineConfigService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-lines")
public class ProductLineConfigController {
  private final ProductLineConfigService productLineConfigService;

  public ProductLineConfigController(ProductLineConfigService productLineConfigService) {
    this.productLineConfigService = productLineConfigService;
  }

  @GetMapping
  public ApiResponse<List<ProductLineDetailDto>> list(@RequestParam(required = false) String keyword) {
    return ApiResponse.ok(productLineConfigService.list(keyword));
  }

  @GetMapping("/{id}")
  public ApiResponse<ProductLineDetailDto> detail(@PathVariable Long id) {
    return ApiResponse.ok(productLineConfigService.detail(id));
  }

  @PostMapping
  public ApiResponse<ProductLineDetailDto> create(@Valid @RequestBody SaveProductLineRequest request) {
    return ApiResponse.ok(productLineConfigService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<ProductLineDetailDto> update(@PathVariable Long id, @Valid @RequestBody SaveProductLineRequest request) {
    return ApiResponse.ok(productLineConfigService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    productLineConfigService.delete(id);
    return ApiResponse.ok(null);
  }
}
