package com.example.airequirementworkbench.requirement.service;

import com.example.airequirementworkbench.common.IdGenerator;
import com.example.airequirementworkbench.common.exception.BusinessException;
import com.example.airequirementworkbench.conversation.entity.ConversationSession;
import com.example.airequirementworkbench.conversation.repository.ConversationSessionRepository;
import com.example.airequirementworkbench.requirement.dto.CandidateDto;
import com.example.airequirementworkbench.requirement.dto.RequirementDtos.GenerateCardRequest;
import com.example.airequirementworkbench.requirement.dto.RequirementDtos.RequirementDto;
import com.example.airequirementworkbench.requirement.entity.Requirement;
import com.example.airequirementworkbench.requirement.entity.RequirementCandidate;
import com.example.airequirementworkbench.requirement.entity.RequirementVersion;
import com.example.airequirementworkbench.requirement.repository.RequirementCandidateRepository;
import com.example.airequirementworkbench.requirement.repository.RequirementRepository;
import com.example.airequirementworkbench.requirement.repository.RequirementVersionRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequirementService {
  private final RequirementCandidateRepository candidateRepository;
  private final RequirementRepository requirementRepository;
  private final RequirementVersionRepository versionRepository;
  private final ConversationSessionRepository sessionRepository;
  private final IdGenerator idGenerator;
  private final Long mockUserId;

  public RequirementService(
      RequirementCandidateRepository candidateRepository,
      RequirementRepository requirementRepository,
      RequirementVersionRepository versionRepository,
      ConversationSessionRepository sessionRepository,
      IdGenerator idGenerator,
      @Value("${app.mock-user-id}") Long mockUserId
  ) {
    this.candidateRepository = candidateRepository;
    this.requirementRepository = requirementRepository;
    this.versionRepository = versionRepository;
    this.sessionRepository = sessionRepository;
    this.idGenerator = idGenerator;
    this.mockUserId = mockUserId;
  }

  public List<RequirementDto> listRequirements() {
    return requirementRepository.findByDeletedFalseOrderByUpdatedAtDesc().stream()
        .map(RequirementMapper::toRequirementDto)
        .toList();
  }

  public RequirementDto getRequirement(Long id) {
    return requirementRepository.findById(id)
        .filter(requirement -> !Boolean.TRUE.equals(requirement.getDeleted()))
        .map(RequirementMapper::toRequirementDto)
        .orElseThrow(() -> new BusinessException("REQUIREMENT_NOT_FOUND", "需求不存在"));
  }

  public List<CandidateDto> listCandidates(Long sessionId) {
    return candidateRepository.findBySessionIdAndDeletedFalseOrderByUpdatedAtDesc(sessionId).stream()
        .map(RequirementMapper::toCandidateDto)
        .toList();
  }

  @Transactional
  public CandidateDto closeCandidate(Long candidateId) {
    RequirementCandidate candidate = candidateRepository.findById(candidateId)
        .orElseThrow(() -> new BusinessException("CANDIDATE_NOT_FOUND", "候选需求不存在"));
    if ("converted".equals(candidate.getStatus())) {
      throw new BusinessException("CANDIDATE_CONVERTED", "已生成正式需求的候选需求不可关闭");
    }
    candidate.setStatus("closed");
    candidate.setUpdatedBy(mockUserId);
    return RequirementMapper.toCandidateDto(candidateRepository.save(candidate));
  }

  @Transactional
  public RequirementDto generateCard(Long candidateId, GenerateCardRequest request) {
    RequirementCandidate candidate = candidateRepository.findById(candidateId)
        .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
        .orElseThrow(() -> new BusinessException("CANDIDATE_NOT_FOUND", "候选需求不存在"));
    if ("closed".equals(candidate.getStatus())) {
      throw new BusinessException("CANDIDATE_CLOSED", "已关闭的候选需求不能生成需求卡片");
    }
    if (candidate.getConvertedRequirementId() != null) {
      throw new BusinessException("CANDIDATE_CONVERTED", "该候选需求已生成正式需求");
    }

    Requirement requirement = new Requirement();
    requirement.setId(idGenerator.nextId());
    requirement.setRequirementNo(idGenerator.nextRequirementNo());
    requirement.setSourceSessionId(candidate.getSessionId());
    requirement.setSourceCandidateId(candidate.getId());
    requirement.setTitle(request.title().trim());
    requirement.setProductLineId(request.productLineId());
    requirement.setModuleId(request.moduleId());
    requirement.setRequirementType(request.requirementType());
    requirement.setPriority(request.priority() == null || request.priority().isBlank() ? "medium" : request.priority());
    requirement.setStatus("confirmed");
    requirement.setContentJson(candidate.getContentJson());
    requirement.setCompletenessScore(candidate.getCompletenessScore());
    requirement.setCurrentVersion(1);
    requirement.setCreatedBy(mockUserId);
    requirement.setUpdatedBy(mockUserId);
    requirement = requirementRepository.save(requirement);

    RequirementVersion version = new RequirementVersion();
    version.setId(idGenerator.nextId());
    version.setRequirementId(requirement.getId());
    version.setVersionNo(1);
    version.setTitle(requirement.getTitle());
    version.setContentJson(requirement.getContentJson());
    version.setChangeSummary("由候选需求生成正式需求卡片");
    version.setCreatedBy(mockUserId);
    versionRepository.save(version);

    candidate.setStatus("converted");
    candidate.setConvertedRequirementId(requirement.getId());
    candidate.setUpdatedBy(mockUserId);
    candidateRepository.save(candidate);

    ConversationSession session = sessionRepository.findById(candidate.getSessionId())
        .orElseThrow(() -> new BusinessException("SESSION_NOT_FOUND", "会话不存在"));
    session.setCurrentStage("card_generated");
    session.setRequirementCount((int) requirementRepository.countBySourceSessionIdAndDeletedFalse(session.getId()));
    session.setUpdatedBy(mockUserId);
    sessionRepository.save(session);

    return RequirementMapper.toRequirementDto(requirement);
  }
}
