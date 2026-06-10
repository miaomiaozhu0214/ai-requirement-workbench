package com.example.airequirementworkbench.ai.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public final class AiConfigDtos {
  private AiConfigDtos() {
  }

  public record ModelConfigDto(
      Long id,
      String provider,
      String modelName,
      String displayName,
      String apiBaseUrl,
      String apiKeyEnv,
      String apiKeySecret,
      BigDecimal temperature,
      Integer maxOutputTokens,
      Integer timeoutSeconds,
      String status,
      Boolean isDefault,
      LocalDateTime updatedAt
  ) {
  }

  public record SaveModelConfigRequest(
      Long id,
      @NotBlank String provider,
      @NotBlank String modelName,
      @NotBlank String displayName,
      @NotBlank String apiBaseUrl,
      String apiKeyEnv,
      String apiKeySecret,
      @NotNull @DecimalMin("0.00") @DecimalMax("2.00") BigDecimal temperature,
      @NotNull @Min(128) @Max(16000) Integer maxOutputTokens,
      @NotNull @Min(5) @Max(300) Integer timeoutSeconds,
      @NotBlank String status,
      Boolean isDefault
  ) {
  }

  public record PromptTemplateDto(
      Long id,
      String abilityType,
      String templateCode,
      String templateName,
      String version,
      String systemPrompt,
      String userPrompt,
      Map<String, Object> jsonSchema,
      String status,
      Boolean isDefault,
      LocalDateTime updatedAt
  ) {
  }

  public record SavePromptTemplateRequest(
      Long id,
      @NotBlank String abilityType,
      @NotBlank String templateCode,
      @NotBlank String templateName,
      @NotBlank String version,
      @NotBlank String systemPrompt,
      @NotBlank String userPrompt,
      @NotNull Map<String, Object> jsonSchema,
      @NotBlank String status,
      Boolean isDefault
  ) {
  }

  public record AbilityConfigDto(
      Long id,
      String abilityType,
      String abilityName,
      Boolean enabled,
      Long modelConfigId,
      Long promptTemplateId,
      Boolean fallbackToMock,
      String status,
      LocalDateTime updatedAt
  ) {
  }

  public record AiConfigStatusDto(
      String provider,
      String modeLabel,
      Boolean llmConfigured,
      Boolean defaultModelConfigured,
      Boolean apiKeyConfigured,
      Integer enabledModelCount,
      String modelName,
      java.util.List<String> missingItems
  ) {
  }

  public record SaveAbilityConfigRequest(
      Long id,
      @NotBlank String abilityType,
      @NotBlank String abilityName,
      @NotNull Boolean enabled,
      @NotNull Long modelConfigId,
      @NotNull Long promptTemplateId,
      Boolean fallbackToMock,
      @NotBlank String status
  ) {
  }
}
