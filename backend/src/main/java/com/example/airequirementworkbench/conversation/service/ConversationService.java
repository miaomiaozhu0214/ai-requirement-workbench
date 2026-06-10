package com.example.airequirementworkbench.conversation.service;

import com.example.airequirementworkbench.ai.orchestrator.AiOrchestrator;
import com.example.airequirementworkbench.ai.trace.AiTrace;
import com.example.airequirementworkbench.ai.trace.AiTraceRepository;
import com.example.airequirementworkbench.ai.trace.AiTraceService;
import com.example.airequirementworkbench.ai.trace.AiTraceDtos.AiTraceDto;
import com.example.airequirementworkbench.common.IdGenerator;
import com.example.airequirementworkbench.common.exception.BusinessException;
import com.example.airequirementworkbench.conversation.dto.ConversationDtos.AiActionDto;
import com.example.airequirementworkbench.conversation.dto.ConversationDtos.ConversationDetailDto;
import com.example.airequirementworkbench.conversation.dto.ConversationDtos.ConversationSummaryDto;
import com.example.airequirementworkbench.conversation.dto.ConversationDtos.CreateConversationRequest;
import com.example.airequirementworkbench.conversation.dto.ConversationDtos.MessageDto;
import com.example.airequirementworkbench.conversation.dto.ConversationDtos.SendMessageRequest;
import com.example.airequirementworkbench.conversation.entity.ConversationMessage;
import com.example.airequirementworkbench.conversation.entity.ConversationSession;
import com.example.airequirementworkbench.conversation.repository.ConversationMessageRepository;
import com.example.airequirementworkbench.conversation.repository.ConversationSessionRepository;
import com.example.airequirementworkbench.requirement.repository.RequirementCandidateRepository;
import com.example.airequirementworkbench.requirement.repository.RequirementRepository;
import com.example.airequirementworkbench.requirement.entity.RequirementCandidate;
import com.example.airequirementworkbench.requirement.service.RequirementMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {
  private final ConversationSessionRepository sessionRepository;
  private final ConversationMessageRepository messageRepository;
  private final RequirementCandidateRepository candidateRepository;
  private final RequirementRepository requirementRepository;
  private final AiTraceRepository aiTraceRepository;
  private final AiTraceService aiTraceService;
  private final AiOrchestrator aiOrchestrator;
  private final IdGenerator idGenerator;
  private final Long mockUserId;

  public ConversationService(
      ConversationSessionRepository sessionRepository,
      ConversationMessageRepository messageRepository,
      RequirementCandidateRepository candidateRepository,
      RequirementRepository requirementRepository,
      AiTraceRepository aiTraceRepository,
      AiTraceService aiTraceService,
      AiOrchestrator aiOrchestrator,
      IdGenerator idGenerator,
      @Value("${app.mock-user-id}") Long mockUserId
  ) {
    this.sessionRepository = sessionRepository;
    this.messageRepository = messageRepository;
    this.candidateRepository = candidateRepository;
    this.requirementRepository = requirementRepository;
    this.aiTraceRepository = aiTraceRepository;
    this.aiTraceService = aiTraceService;
    this.aiOrchestrator = aiOrchestrator;
    this.idGenerator = idGenerator;
    this.mockUserId = mockUserId;
  }

  public List<ConversationSummaryDto> listSessions() {
    return sessionRepository.findByDeletedFalseOrderByUpdatedAtDesc().stream()
        .map(this::toSummaryDto)
        .toList();
  }

  @Transactional
  public ConversationDetailDto create(CreateConversationRequest request) {
    ConversationSession session = new ConversationSession();
    session.setId(idGenerator.nextId());
    session.setTitle(request.title() == null || request.title().isBlank() ? "新需求会话" : request.title().trim());
    session.setStatus("active");
    session.setCurrentStage("empty");
    session.setCreatedBy(mockUserId);
    session.setUpdatedBy(mockUserId);
    session = sessionRepository.save(session);
    return getDetail(session.getId());
  }

  public ConversationDetailDto getDetail(Long id) {
    ConversationSession session = findSession(id);
    return new ConversationDetailDto(
        toSummaryDto(session),
        messageRepository.findBySessionIdAndDeletedFalseOrderByCreatedAtAsc(id).stream().map(this::toMessageDto).toList(),
        candidateRepository.findBySessionIdAndDeletedFalseOrderByUpdatedAtDesc(id).stream().map(RequirementMapper::toCandidateDto).toList(),
        requirementRepository.findBySourceSessionIdAndDeletedFalseOrderByUpdatedAtDesc(id).stream().map(RequirementMapper::toRequirementDto).toList(),
        aiTraceService.toDtos(aiTraceRepository.findBySessionIdOrderByCreatedAtDesc(id)).stream().map(this::toAiActionDto).toList()
    );
  }

  public List<MessageDto> getMessages(Long sessionId) {
    findSession(sessionId);
    return messageRepository.findBySessionIdAndDeletedFalseOrderByCreatedAtAsc(sessionId).stream()
        .map(this::toMessageDto)
        .toList();
  }

  @Transactional
  public void deleteSession(Long id) {
    ConversationSession session = findSession(id);
    // 会话删除采用软删除：对话和候选从列表隐藏，但正式需求和 AI Trace 保留用于追溯。
    session.setDeleted(true);
    session.setStatus("deleted");
    session.setCurrentStage("closed");
    session.setUpdatedBy(mockUserId);
    sessionRepository.save(session);

    List<ConversationMessage> messages = messageRepository.findBySessionIdAndDeletedFalseOrderByCreatedAtAsc(id);
    messages.forEach(message -> message.setDeleted(true));
    messageRepository.saveAll(messages);

    List<RequirementCandidate> candidates = candidateRepository.findBySessionIdAndDeletedFalseOrderByUpdatedAtDesc(id);
    candidates.forEach(candidate -> {
      candidate.setDeleted(true);
      candidate.setUpdatedBy(mockUserId);
    });
    candidateRepository.saveAll(candidates);
  }

  @Transactional
  public ConversationDetailDto sendMessage(Long sessionId, SendMessageRequest request) {
    ConversationSession session = findSession(sessionId);
    if (!"active".equals(session.getStatus())) {
      throw new BusinessException("SESSION_NOT_ACTIVE", "当前会话不可发送消息");
    }

    String content = request.content().trim();
    // 用户消息先落库，再交给 Orchestrator；这样 Router 和后续能力都能拿到完整会话上下文。
    ConversationMessage userMessage = new ConversationMessage();
    userMessage.setId(idGenerator.nextId());
    userMessage.setSessionId(sessionId);
    userMessage.setRole("user");
    userMessage.setMessageType(content.startsWith("/") ? "command" : "text");
    userMessage.setContent(content);
    userMessage.setCommandName(content.startsWith("/") ? content.split("\\s+", 2)[0] : null);
    userMessage.setCreatedBy(mockUserId);
    userMessage = messageRepository.save(userMessage);

    if ("empty".equals(session.getCurrentStage())) {
      session.setTitle(summarizeTitle(content));
      session.setCurrentStage("collecting");
    }
    session.setLastMessageAt(LocalDateTime.now());
    session.setUpdatedBy(mockUserId);
    sessionRepository.save(session);

    // Orchestrator 内部固定先调用 intent_router，再根据 Router 结果决定是否抽取、检查完整度或生成回复。
    String assistantReply = aiOrchestrator.processUserMessage(session, userMessage, mockUserId).assistantReply();
    ConversationMessage assistantMessage = new ConversationMessage();
    assistantMessage.setId(idGenerator.nextId());
    assistantMessage.setSessionId(sessionId);
    assistantMessage.setRole("assistant");
    assistantMessage.setMessageType("ai_reply");
    assistantMessage.setContent(assistantReply);
    assistantMessage.setCreatedBy(mockUserId);
    messageRepository.save(assistantMessage);

    refreshSessionCounters(session);
    return getDetail(sessionId);
  }

  private void refreshSessionCounters(ConversationSession session) {
    long candidateCount = candidateRepository.countBySessionIdAndDeletedFalse(session.getId());
    long requirementCount = requirementRepository.countBySourceSessionIdAndDeletedFalse(session.getId());
    boolean hasReadyCandidate = candidateRepository.findBySessionIdAndDeletedFalseOrderByUpdatedAtDesc(session.getId()).stream()
        .anyMatch(candidate -> "ready_to_card".equals(candidate.getStatus()));
    session.setCandidateCount((int) candidateCount);
    session.setRequirementCount((int) requirementCount);
    if (requirementCount > 0) {
      session.setCurrentStage("card_generated");
    } else if (hasReadyCandidate) {
      session.setCurrentStage("candidate_ready");
    } else if (candidateCount > 0) {
      session.setCurrentStage("refining");
    } else {
      session.setCurrentStage("collecting");
    }
    session.setLastMessageAt(LocalDateTime.now());
    session.setUpdatedBy(mockUserId);
    sessionRepository.save(session);
  }

  private ConversationSession findSession(Long id) {
    return sessionRepository.findById(id)
        .filter(session -> !Boolean.TRUE.equals(session.getDeleted()))
        .orElseThrow(() -> new BusinessException("SESSION_NOT_FOUND", "会话不存在"));
  }

  private ConversationSummaryDto toSummaryDto(ConversationSession session) {
    return new ConversationSummaryDto(
        session.getId(),
        session.getTitle(),
        session.getStatus(),
        session.getCurrentStage(),
        session.getCandidateCount(),
        session.getRequirementCount(),
        session.getLastMessageAt(),
        session.getUpdatedAt()
    );
  }

  private MessageDto toMessageDto(ConversationMessage message) {
    return new MessageDto(
        message.getId(),
        message.getSessionId(),
        message.getRole(),
        message.getMessageType(),
        message.getContent(),
        message.getMetadataJson(),
        message.getCreatedAt()
    );
  }

  private AiActionDto toAiActionDto(AiTraceDto trace) {
    return new AiActionDto(
        trace.id(),
        trace.traceNo(),
        trace.abilityType(),
        trace.status(),
        trace.intent(),
        trace.nextActions(),
        trace.businessObjectType(),
        trace.businessObjectId(),
        trace.modelName(),
        trace.promptTemplateId(),
        trace.promptTemplateCode(),
        trace.promptTemplateName(),
        trace.promptVersion(),
        trace.inputSummary(),
        trace.outputJson(),
        trace.durationMs(),
        trace.tokenInput(),
        trace.tokenOutput(),
        trace.errorCode(),
        trace.errorMessage(),
        trace.createdAt()
    );
  }

  private String summarizeTitle(String content) {
    String normalized = content.replaceAll("\\s+", " ").trim();
    return normalized.length() <= 40 ? normalized : normalized.substring(0, 40);
  }
}
