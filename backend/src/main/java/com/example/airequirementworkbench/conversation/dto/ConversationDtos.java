package com.example.airequirementworkbench.conversation.dto;

import com.example.airequirementworkbench.requirement.dto.CandidateDto;
import com.example.airequirementworkbench.requirement.dto.RequirementDtos.RequirementDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ConversationDtos {
  private ConversationDtos() {
  }

  public record CreateConversationRequest(@Size(max = 120) String title) {
  }

  public record SendMessageRequest(
      @NotBlank(message = "请输入需求内容或命令")
      @Size(max = 10000, message = "消息内容最多10000字符")
      String content
  ) {
  }

  public record ConversationSummaryDto(
      Long id,
      String title,
      String status,
      String currentStage,
      Integer candidateCount,
      Integer requirementCount,
      LocalDateTime lastMessageAt,
      LocalDateTime updatedAt
  ) {
  }

  public record MessageDto(
      Long id,
      Long sessionId,
      String role,
      String messageType,
      String content,
      Map<String, Object> metadataJson,
      LocalDateTime createdAt
  ) {
  }

  public record ConversationDetailDto(
      ConversationSummaryDto session,
      List<MessageDto> messages,
      List<CandidateDto> candidates,
      List<RequirementDto> requirements,
      List<AiActionDto> aiActions
  ) {
  }

  public record AiActionDto(
      Long traceId,
      String traceNo,
      String abilityType,
      String status,
      String intent,
      List<String> nextActions,
      String businessObjectType,
      Long businessObjectId,
      String modelName,
      Long promptTemplateId,
      String promptTemplateCode,
      String promptTemplateName,
      String promptVersion,
      String inputSummary,
      Map<String, Object> outputJson,
      Integer durationMs,
      Integer tokenInput,
      Integer tokenOutput,
      String errorCode,
      String errorMessage,
      LocalDateTime createdAt
  ) {
  }
}
