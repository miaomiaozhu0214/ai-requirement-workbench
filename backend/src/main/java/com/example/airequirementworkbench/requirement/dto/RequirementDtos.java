package com.example.airequirementworkbench.requirement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public final class RequirementDtos {
  private RequirementDtos() {
  }

  public record GenerateCardRequest(
      @NotBlank(message = "需求标题不能为空")
      @Size(min = 2, max = 120, message = "需求标题需为2-120字符")
      String title,
      @NotNull(message = "请选择产品线")
      Long productLineId,
      @NotNull(message = "请选择模块")
      Long moduleId,
      @NotBlank(message = "请选择需求类型")
      String requirementType,
      String priority
  ) {
  }

  public record RequirementDto(
      Long id,
      String requirementNo,
      Long sourceSessionId,
      Long sourceCandidateId,
      String title,
      Long productLineId,
      Long moduleId,
      String requirementType,
      String priority,
      String status,
      Map<String, Object> contentJson,
      BigDecimal completenessScore,
      Integer currentVersion,
      LocalDateTime createdAt,
      LocalDateTime updatedAt
  ) {
  }
}
