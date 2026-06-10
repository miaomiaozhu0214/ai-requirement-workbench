package com.example.airequirementworkbench.ai.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class AiDtos {
  private AiDtos() {
  }

  public record RouteResult(
      String intent,
      BigDecimal confidence,
      String targetType,
      String targetId,
      boolean mayContainMultipleRequirements,
      List<String> nextActions,
      String reason
  ) {
  }

  public record RouteContext(
      String latestMessage,
      String sessionStatus,
      String currentStage,
      List<Map<String, Object>> currentCandidates,
      List<Map<String, Object>> recentMessages
  ) {
  }

  public record ExtractResult(
      List<CandidatePatch> patches
  ) {
  }

  public record CandidatePatch(
      String title,
      String operation,
      Map<String, Object> fields,
      BigDecimal confidence
  ) {
  }

  public record CompletenessResult(
      BigDecimal completenessScore,
      List<String> missingItems,
      List<String> riskyItems,
      List<String> suggestedQuestions,
      boolean readyToGenerateCard
  ) {
  }

  public record SplitResult(
      List<CandidatePatch> patches,
      String reason
  ) {
  }

  public record CardGenerateResult(
      String title,
      Map<String, Object> content,
      List<String> missingRequiredFields,
      boolean readyToCreateRequirement,
      String reason
  ) {
  }

  public record SimilarRequirementSearchResult(
      List<SimilarRequirement> items,
      String summary
  ) {
  }

  public record SimilarRequirement(
      Long requirementId,
      String requirementNo,
      String title,
      BigDecimal similarity,
      String reason
  ) {
  }

  public record ReplyResult(String content) {
  }

  public record OrchestrationResult(String assistantReply, Long extractTraceId) {
  }
}
