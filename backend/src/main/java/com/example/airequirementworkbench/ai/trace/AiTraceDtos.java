package com.example.airequirementworkbench.ai.trace;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class AiTraceDtos {
  private AiTraceDtos() {
  }

  public record AiTraceDto(
      Long id,
      String traceNo,
      Long sessionId,
      String businessObjectType,
      Long businessObjectId,
      String abilityType,
      Long modelConfigId,
      String modelName,
      Long promptTemplateId,
      String promptTemplateCode,
      String promptTemplateName,
      String promptVersion,
      String inputSummary,
      Map<String, Object> inputJson,
      Map<String, Object> outputJson,
      String outputText,
      String intent,
      List<String> nextActions,
      Integer tokenInput,
      Integer tokenOutput,
      Integer durationMs,
      String status,
      String errorCode,
      String errorMessage,
      LocalDateTime createdAt
  ) {
  }
}
