package com.example.airequirementworkbench.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class ProductLineConfigDtos {
  private ProductLineConfigDtos() {}

  public record ProductLineDetailDto(
      Long id,
      String lineCode,
      String lineName,
      List<String> owners,
      String productType,
      List<String> platforms,
      String description,
      Integer version,
      Boolean isProcessing,
      Long featureCount,
      Boolean canEdit,
      Boolean canDelete,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}

  public record SaveProductLineRequest(
      @Size(max = 64, message = "产品线编码不能超过64字符") String lineCode,
      @NotBlank(message = "产品线名称不能为空") @Size(max = 100, message = "产品线名称不能超过100字符") String lineName,
      @NotEmpty(message = "负责人不能为空") List<String> owners,
      @NotBlank(message = "产品线类型不能为空") String productType,
      @NotEmpty(message = "涉及平台不能为空") List<String> platforms,
      @Size(max = 2000, message = "业务介绍不能超过2000字符") String description) {}
}
