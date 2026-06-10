package com.example.airequirementworkbench.ai.client;

import com.example.airequirementworkbench.ai.dto.AiDtos.CompletenessResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.ExtractResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.CardGenerateResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.ReplyResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.RouteContext;
import com.example.airequirementworkbench.ai.dto.AiDtos.RouteResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.SimilarRequirementSearchResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.SplitResult;
import com.example.airequirementworkbench.requirement.entity.RequirementCandidate;
import com.example.airequirementworkbench.requirement.entity.Requirement;
import java.util.List;

public interface AiClient {
  RouteResult route(RouteContext context);
  ExtractResult extractRequirement(String latestMessage, List<RequirementCandidate> currentCandidates);
  SplitResult splitRequirement(String latestMessage, List<RequirementCandidate> currentCandidates);
  CompletenessResult checkCompleteness(RequirementCandidate candidate);
  CardGenerateResult generateRequirementCard(RequirementCandidate candidate);
  SimilarRequirementSearchResult searchSimilarRequirements(String latestMessage, List<Requirement> existingRequirements);
  ReplyResult generateReply(String latestMessage, List<RequirementCandidate> currentCandidates);

  default AiCallMetadata lastCallMetadata() {
    return AiCallMetadata.mock();
  }
}
