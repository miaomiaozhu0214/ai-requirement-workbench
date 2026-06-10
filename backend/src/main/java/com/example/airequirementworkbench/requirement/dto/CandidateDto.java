package com.example.airequirementworkbench.requirement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record CandidateDto(
    Long id,
    Long sessionId,
    String title,
    String status,
    Map<String, Object> contentJson,
    BigDecimal completenessScore,
    List<String> missingItemsJson,
    List<String> riskyItemsJson,
    List<String> suggestedQuestionsJson,
    BigDecimal confidence,
    Long convertedRequirementId,
    LocalDateTime updatedAt
) {
}
