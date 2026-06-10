package com.example.airequirementworkbench.requirement.service;

import com.example.airequirementworkbench.requirement.dto.CandidateDto;
import com.example.airequirementworkbench.requirement.dto.RequirementDtos.RequirementDto;
import com.example.airequirementworkbench.requirement.entity.Requirement;
import com.example.airequirementworkbench.requirement.entity.RequirementCandidate;

public final class RequirementMapper {
  private RequirementMapper() {
  }

  public static CandidateDto toCandidateDto(RequirementCandidate candidate) {
    return new CandidateDto(
        candidate.getId(),
        candidate.getSessionId(),
        candidate.getTitle(),
        candidate.getStatus(),
        candidate.getContentJson(),
        candidate.getCompletenessScore(),
        candidate.getMissingItemsJson(),
        candidate.getRiskyItemsJson(),
        candidate.getSuggestedQuestionsJson(),
        candidate.getConfidence(),
        candidate.getConvertedRequirementId(),
        candidate.getUpdatedAt()
    );
  }

  public static RequirementDto toRequirementDto(Requirement requirement) {
    return new RequirementDto(
        requirement.getId(),
        requirement.getRequirementNo(),
        requirement.getSourceSessionId(),
        requirement.getSourceCandidateId(),
        requirement.getTitle(),
        requirement.getProductLineId(),
        requirement.getModuleId(),
        requirement.getRequirementType(),
        requirement.getPriority(),
        requirement.getStatus(),
        requirement.getContentJson(),
        requirement.getCompletenessScore(),
        requirement.getCurrentVersion(),
        requirement.getCreatedAt(),
        requirement.getUpdatedAt()
    );
  }
}
