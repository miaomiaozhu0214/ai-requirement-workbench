package com.example.airequirementworkbench.ai.orchestrator;

import com.example.airequirementworkbench.ai.client.AiClient;
import com.example.airequirementworkbench.ai.client.AiClientException;
import com.example.airequirementworkbench.ai.dto.AiDtos.CandidatePatch;
import com.example.airequirementworkbench.ai.dto.AiDtos.CardGenerateResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.CompletenessResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.ExtractResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.OrchestrationResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.ReplyResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.RouteContext;
import com.example.airequirementworkbench.ai.dto.AiDtos.RouteResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.SimilarRequirementSearchResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.SplitResult;
import com.example.airequirementworkbench.ai.trace.AiTrace;
import com.example.airequirementworkbench.ai.trace.AiTraceService;
import com.example.airequirementworkbench.common.IdGenerator;
import com.example.airequirementworkbench.common.exception.BusinessException;
import com.example.airequirementworkbench.conversation.entity.ConversationMessage;
import com.example.airequirementworkbench.conversation.entity.ConversationSession;
import com.example.airequirementworkbench.conversation.repository.ConversationMessageRepository;
import com.example.airequirementworkbench.requirement.entity.RequirementCandidate;
import com.example.airequirementworkbench.requirement.entity.RequirementCandidatePatch;
import com.example.airequirementworkbench.requirement.repository.RequirementCandidatePatchRepository;
import com.example.airequirementworkbench.requirement.repository.RequirementCandidateRepository;
import com.example.airequirementworkbench.requirement.repository.RequirementRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class AiOrchestrator {
  private final AiClient aiClient;
  private final AiTraceService aiTraceService;
  private final ConversationMessageRepository messageRepository;
  private final RequirementCandidateRepository candidateRepository;
  private final RequirementCandidatePatchRepository patchRepository;
  private final RequirementRepository requirementRepository;
  private final IdGenerator idGenerator;
  private final Map<String, AiActionHandler> actionRegistry;

  public AiOrchestrator(
      AiClient aiClient,
      AiTraceService aiTraceService,
      ConversationMessageRepository messageRepository,
      RequirementCandidateRepository candidateRepository,
      RequirementCandidatePatchRepository patchRepository,
      RequirementRepository requirementRepository,
      IdGenerator idGenerator
  ) {
    this.aiClient = aiClient;
    this.aiTraceService = aiTraceService;
    this.messageRepository = messageRepository;
    this.candidateRepository = candidateRepository;
    this.patchRepository = patchRepository;
    this.requirementRepository = requirementRepository;
    this.idGenerator = idGenerator;
    this.actionRegistry = buildActionRegistry();
  }

  public OrchestrationResult processUserMessage(ConversationSession session, ConversationMessage userMessage, Long userId) {
    List<RequirementCandidate> candidates = candidateRepository.findBySessionIdAndDeletedFalseOrderByUpdatedAtDesc(session.getId());
    RouteContext routeContext = new RouteContext(
        userMessage.getContent(),
        session.getStatus(),
        session.getCurrentStage(),
        candidateSnapshots(candidates),
        recentMessageSnapshots(session.getId())
    );

    AiCall<RouteResult> routeCall = callAi(session.getId(), "conversation", session.getId(), "intent_router",
        routeContext,
        userId,
        () -> aiClient.route(routeContext));
    RouteResult route = routeCall.value();
    OrchestrationContext context = new OrchestrationContext(session, userMessage, userId, route, candidates);

    if (isLowConfidence(route)) {
      context.addSystemNote("Router 置信度较低，已跳过写入类动作，改为向用户确认。");
      handleReply(context);
      return new OrchestrationResult(context.assistantReply(), routeCall.trace().getId());
    }

    List<String> actions = normalizeActions(route.nextActions());
    if (actions.isEmpty()) {
      actions = List.of("generate_reply");
    }
    if (isStructurallyUnclear(userMessage.getContent())) {
      context.addSystemNote("用户输入过短或缺少业务语义，已跳过写入类动作。");
      actions = actions.stream()
          .filter(action -> !isWriteAction(action))
          .toList();
      if (actions.isEmpty()) {
        actions = List.of("generate_reply");
      }
    }
    for (String action : actions) {
      AiActionHandler handler = actionRegistry.get(action);
      if (handler == null) {
        context.addSystemNote("Router 返回了暂不支持的动作：" + action);
        continue;
      }
      if (!isActionAllowed(action, context)) {
        continue;
      }
      handler.handle(context);
    }

    if (context.assistantReply() == null || context.assistantReply().isBlank()) {
      handleReply(context);
    }
    return new OrchestrationResult(context.assistantReply(), routeCall.trace().getId());
  }

  private Map<String, AiActionHandler> buildActionRegistry() {
    Map<String, AiActionHandler> registry = new HashMap<>();
    registry.put("extract_requirement", this::handleExtract);
    registry.put("extract_requirement_info", this::handleExtract);
    registry.put("split_requirement", this::handleSplit);
    registry.put("requirement_split", this::handleSplit);
    registry.put("check_completeness", this::handleCompleteness);
    registry.put("generate_requirement_card", this::handleCardGenerate);
    registry.put("generate_card", this::handleCardGenerate);
    registry.put("card_generate", this::handleCardGenerate);
    registry.put("search_similar_requirements", this::handleSimilarSearch);
    registry.put("similar_requirement_search", this::handleSimilarSearch);
    registry.put("query_requirement", this::handleSimilarSearch);
    registry.put("generate_reply", this::handleReply);
    registry.put("reply_generate", this::handleReply);
    registry.put("clarify_short_reply", this::handleReply);
    return Map.copyOf(registry);
  }

  private void handleExtract(OrchestrationContext context) {
    AiCall<ExtractResult> extractCall = callAi(context.session().getId(), "conversation", context.session().getId(), "requirement_extract",
        Map.of("latestMessage", context.userMessage().getContent(), "route", context.route()),
        context.userId(),
        () -> aiClient.extractRequirement(context.userMessage().getContent(), context.candidates()));
    applyPatches(context, extractCall.value().patches(), extractCall.trace().getId());
  }

  private void handleSplit(OrchestrationContext context) {
    AiCall<SplitResult> splitCall = callAi(context.session().getId(), "conversation", context.session().getId(), "requirement_split",
        Map.of("latestMessage", context.userMessage().getContent(), "route", context.route(), "currentCandidates", candidateSnapshots(context.candidates())),
        context.userId(),
        () -> aiClient.splitRequirement(context.userMessage().getContent(), context.candidates()));
    applyPatches(context, splitCall.value().patches(), splitCall.trace().getId());
  }

  private void handleCompleteness(OrchestrationContext context) {
    List<RequirementCandidate> targets = new ArrayList<>(context.touchedCandidates().isEmpty()
        ? editableCandidates(context.session().getId())
        : context.touchedCandidates());
    for (RequirementCandidate candidate : targets) {
      updateCompleteness(context, candidate);
    }
  }

  private void handleCardGenerate(OrchestrationContext context) {
    RequirementCandidate candidate = selectCardCandidate(context);
    if (candidate == null) {
      context.addSystemNote("当前没有可生成需求卡片的候选需求。");
      return;
    }

    AiCall<CardGenerateResult> cardCall = callAi(context.session().getId(), "candidate", candidate.getId(), "card_generate",
        Map.of("candidateId", candidate.getId(), "candidate", candidate.getContentJson(), "route", context.route()),
        context.userId(),
        () -> aiClient.generateRequirementCard(candidate));
    CardGenerateResult card = cardCall.value();

    Map<String, Object> content = new LinkedHashMap<>(candidate.getContentJson() == null ? Map.of() : candidate.getContentJson());
    Map<String, Object> draft = new LinkedHashMap<>();
    draft.put("title", card.title());
    draft.put("content", card.content());
    draft.put("missingRequiredFields", card.missingRequiredFields() == null ? List.of() : card.missingRequiredFields());
    draft.put("readyToCreateRequirement", card.readyToCreateRequirement());
    draft.put("reason", card.reason());
    content.put("generatedCardDraft", draft);
    candidate.setContentJson(content);
    candidate.setStatus(card.readyToCreateRequirement() ? "ready_to_card" : "refining");
    candidate.setUpdatedBy(context.userId());
    candidateRepository.save(candidate);
    context.addTouchedCandidate(candidate);
    if (!card.readyToCreateRequirement()) {
      context.addSystemNote("候选需求还缺少生成正式需求卡片所需信息：" + String.join("、", card.missingRequiredFields() == null ? List.of() : card.missingRequiredFields()));
    }
  }

  private void handleSimilarSearch(OrchestrationContext context) {
    AiCall<SimilarRequirementSearchResult> searchCall = callAi(context.session().getId(), "conversation", context.session().getId(), "similar_requirement_search",
        Map.of("latestMessage", context.userMessage().getContent(), "route", context.route()),
        context.userId(),
        () -> aiClient.searchSimilarRequirements(context.userMessage().getContent(), requirementRepository.findByDeletedFalseOrderByUpdatedAtDesc()));
    context.addSystemNote(searchCall.value().summary());
  }

  private void handleReply(OrchestrationContext context) {
    List<RequirementCandidate> latestCandidates = candidateRepository.findBySessionIdAndDeletedFalseOrderByUpdatedAtDesc(context.session().getId());
    ReplyResult reply = callAi(context.session().getId(), "conversation", context.session().getId(), "reply_generate",
        Map.of(
            "latestMessage", context.userMessage().getContent(),
            "route", context.route(),
            "candidateCount", latestCandidates.size(),
            "systemNotes", context.systemNotes()
        ),
        context.userId(),
        () -> aiClient.generateReply(context.userMessage().getContent(), latestCandidates)).value();
    context.setAssistantReply(reply.content());
  }

  private boolean isLowConfidence(RouteResult route) {
    return route.confidence() == null || route.confidence().compareTo(new BigDecimal("0.70")) < 0;
  }

  private List<String> normalizeActions(List<String> actions) {
    if (actions == null) {
      return List.of();
    }
    return actions.stream()
        .filter(action -> action != null && !action.isBlank())
        .map(action -> action.trim().toLowerCase())
        .distinct()
        .toList();
  }

  private boolean isActionAllowed(String action, OrchestrationContext context) {
    if (List.of("generate_reply", "reply_generate", "clarify_short_reply", "search_similar_requirements", "similar_requirement_search", "query_requirement").contains(action)) {
      return true;
    }
    if (isStructurallyUnclear(context.userMessage().getContent()) && isWriteAction(action)) {
      context.addSystemNote("Router 建议了写入类动作，但当前输入不足以更新候选需求。");
      return false;
    }
    if (List.of("generate_requirement_card", "generate_card", "card_generate").contains(action)) {
      if (editableCandidates(context.session().getId()).isEmpty()) {
        context.addSystemNote("当前没有候选需求，不能直接生成需求卡片。");
        return false;
      }
      if (context.route().confidence() == null || context.route().confidence().compareTo(new BigDecimal("0.85")) < 0) {
        context.addSystemNote("生成卡片意图置信度不足，已改为请用户确认。");
        return false;
      }
      return true;
    }
    if ("check_completeness".equals(action)) {
      if (context.touchedCandidates().isEmpty() && editableCandidates(context.session().getId()).isEmpty()) {
        context.addSystemNote("当前没有可检查完整度的候选需求。");
        return false;
      }
      return true;
    }
    if (List.of("extract_requirement", "extract_requirement_info", "split_requirement", "requirement_split").contains(action)) {
      return true;
    }
    return false;
  }

  private boolean isWriteAction(String action) {
    return List.of(
        "extract_requirement",
        "extract_requirement_info",
        "split_requirement",
        "requirement_split",
        "check_completeness",
        "generate_requirement_card",
        "generate_card",
        "card_generate"
    ).contains(action);
  }

  private boolean isStructurallyUnclear(String content) {
    String trimmed = content == null ? "" : content.trim();
    return trimmed.length() < 2 || trimmed.matches("[0-9]+") || trimmed.matches("[\\p{Punct}\\s]+");
  }

  private List<Map<String, Object>> recentMessageSnapshots(Long sessionId) {
    List<ConversationMessage> messages = messageRepository.findBySessionIdAndDeletedFalseOrderByCreatedAtAsc(sessionId);
    return messages.stream()
        .skip(Math.max(0, messages.size() - 10))
        .map(message -> {
          Map<String, Object> snapshot = new LinkedHashMap<>();
          snapshot.put("id", message.getId());
          snapshot.put("role", message.getRole());
          snapshot.put("messageType", message.getMessageType());
          snapshot.put("content", message.getContent());
          snapshot.put("createdAt", message.getCreatedAt());
          return snapshot;
        })
        .toList();
  }

  private List<Map<String, Object>> candidateSnapshots(List<RequirementCandidate> candidates) {
    return candidates.stream()
        .map(candidate -> {
          Map<String, Object> snapshot = new LinkedHashMap<>();
          snapshot.put("id", candidate.getId());
          snapshot.put("title", candidate.getTitle());
          snapshot.put("status", candidate.getStatus());
          snapshot.put("contentJson", candidate.getContentJson());
          snapshot.put("completenessScore", candidate.getCompletenessScore());
          snapshot.put("missingItemsJson", candidate.getMissingItemsJson());
          snapshot.put("suggestedQuestionsJson", candidate.getSuggestedQuestionsJson());
          return snapshot;
        })
        .toList();
  }

  private RequirementCandidate findOrCreateCandidate(
      ConversationSession session,
      ConversationMessage userMessage,
      Long userId,
      List<RequirementCandidate> candidates,
      CandidatePatch patch
  ) {
    return candidates.stream()
        .filter(candidate -> candidate.getTitle().equals(patch.title()))
        .filter(this::isEditableCandidate)
        .findFirst()
        .orElseGet(() -> {
          RequirementCandidate candidate = new RequirementCandidate();
          candidate.setId(idGenerator.nextId());
          candidate.setSessionId(session.getId());
          candidate.setTitle(patch.title());
          candidate.setStatus("draft");
          candidate.setCompletenessScore(BigDecimal.ZERO);
          candidate.setCreatedFromMessageId(userMessage.getId());
          candidate.setCreatedBy(userId);
          candidate.setUpdatedBy(userId);
          return candidateRepository.save(candidate);
        });
  }

  private void applyPatches(OrchestrationContext context, List<CandidatePatch> patches, Long traceId) {
    if (patches == null || patches.isEmpty()) {
      context.addSystemNote("AI 未抽取到可写入的候选需求。");
      return;
    }
    for (CandidatePatch patch : patches) {
      RequirementCandidate candidate = findOrCreateCandidate(context.session(), context.userMessage(), context.userId(), context.candidates(), patch);
      Map<String, Object> before = new LinkedHashMap<>(candidate.getContentJson() == null ? Map.of() : candidate.getContentJson());
      Map<String, Object> after = new LinkedHashMap<>(before);
      if (patch.fields() != null) {
        after.putAll(patch.fields());
      }
      after.put("title", patch.title());

      candidate.setTitle(patch.title());
      candidate.setContentJson(after);
      candidate.setConfidence(patch.confidence());
      candidate.setStatus("refining");
      candidate.setUpdatedBy(context.userId());
      candidate = candidateRepository.save(candidate);

      saveCandidatePatch(context.session(), context.userMessage(), context.userId(), traceId, patch, candidate, before, after);
      context.addTouchedCandidate(candidate);
      context.refreshCandidates(candidateRepository.findBySessionIdAndDeletedFalseOrderByUpdatedAtDesc(context.session().getId()));
    }
  }

  private boolean isEditableCandidate(RequirementCandidate candidate) {
    return !"closed".equals(candidate.getStatus())
        && !"converted".equals(candidate.getStatus());
  }

  private void saveCandidatePatch(
      ConversationSession session,
      ConversationMessage userMessage,
      Long userId,
      Long traceId,
      CandidatePatch patch,
      RequirementCandidate candidate,
      Map<String, Object> before,
      Map<String, Object> after
  ) {
    RequirementCandidatePatch candidatePatch = new RequirementCandidatePatch();
    candidatePatch.setId(idGenerator.nextId());
    candidatePatch.setCandidateId(candidate.getId());
    candidatePatch.setSessionId(session.getId());
    candidatePatch.setSourceMessageId(userMessage.getId());
    candidatePatch.setPatchType(patch.operation());
    candidatePatch.setPatchJson(Map.of("operation", patch.operation(), "fields", patch.fields()));
    candidatePatch.setBeforeJson(before);
    candidatePatch.setAfterJson(after);
    candidatePatch.setAiTraceId(traceId);
    candidatePatch.setCreatedBy(userId);
    patchRepository.save(candidatePatch);
  }

  private List<RequirementCandidate> editableCandidates(Long sessionId) {
    return candidateRepository.findBySessionIdAndDeletedFalseOrderByUpdatedAtDesc(sessionId).stream()
        .filter(this::isEditableCandidate)
        .toList();
  }

  private RequirementCandidate selectCardCandidate(OrchestrationContext context) {
    List<RequirementCandidate> touchedReady = context.touchedCandidates().stream()
        .filter(this::isEditableCandidate)
        .toList();
    if (!touchedReady.isEmpty()) {
      return touchedReady.get(0);
    }
    List<RequirementCandidate> editable = editableCandidates(context.session().getId());
    return editable.stream()
        .filter(candidate -> "ready_to_card".equals(candidate.getStatus()))
        .findFirst()
        .orElse(editable.isEmpty() ? null : editable.get(0));
  }

  private void updateCompleteness(OrchestrationContext context, RequirementCandidate candidate) {
    CompletenessResult completeness = callAi(context.session().getId(), "candidate", candidate.getId(), "completeness_check",
        Map.of("candidateId", candidate.getId(), "content", candidate.getContentJson()),
        context.userId(),
        () -> aiClient.checkCompleteness(candidate)).value();

    candidate.setCompletenessScore(completeness.completenessScore());
    candidate.setMissingItemsJson(completeness.missingItems());
    candidate.setRiskyItemsJson(completeness.riskyItems());
    candidate.setSuggestedQuestionsJson(completeness.suggestedQuestions());
    candidate.setStatus(completeness.readyToGenerateCard() ? "ready_to_card" : "refining");
    candidate.setUpdatedBy(context.userId());
    candidateRepository.save(candidate);
    context.addTouchedCandidate(candidate);
    context.refreshCandidates(candidateRepository.findBySessionIdAndDeletedFalseOrderByUpdatedAtDesc(context.session().getId()));
  }

  private <T> AiCall<T> callAi(
      Long sessionId,
      String objectType,
      Long objectId,
      String abilityType,
      Object input,
      Long userId,
      Supplier<T> supplier
  ) {
    long started = System.currentTimeMillis();
    try {
      T output = supplier.get();
      AiTrace trace = aiTraceService.recordSuccess(
          sessionId, objectType, objectId, abilityType, input, output, userId, started, aiClient.lastCallMetadata()
      );
      return new AiCall<>(output, trace);
    } catch (AiClientException exception) {
      aiTraceService.recordFailure(
          sessionId, objectType, objectId, abilityType,
          exception.getInput() == null ? input : exception.getInput(),
          exception, userId, started, exception.getMetadata()
      );
      throw new BusinessException(exception.getCode(), exception.getMessage());
    } catch (RuntimeException exception) {
      aiTraceService.recordFailure(sessionId, objectType, objectId, abilityType, input, exception, userId, started, aiClient.lastCallMetadata());
      throw exception;
    }
  }

  private record AiCall<T>(T value, AiTrace trace) {
  }

  @FunctionalInterface
  private interface AiActionHandler {
    void handle(OrchestrationContext context);
  }

  private static final class OrchestrationContext {
    private final ConversationSession session;
    private final ConversationMessage userMessage;
    private final Long userId;
    private final RouteResult route;
    private List<RequirementCandidate> candidates;
    private final List<RequirementCandidate> touchedCandidates = new ArrayList<>();
    private final List<String> systemNotes = new ArrayList<>();
    private String assistantReply;

    private OrchestrationContext(
        ConversationSession session,
        ConversationMessage userMessage,
        Long userId,
        RouteResult route,
        List<RequirementCandidate> candidates
    ) {
      this.session = session;
      this.userMessage = userMessage;
      this.userId = userId;
      this.route = route;
      this.candidates = new ArrayList<>(candidates);
    }

    private ConversationSession session() { return session; }
    private ConversationMessage userMessage() { return userMessage; }
    private Long userId() { return userId; }
    private RouteResult route() { return route; }
    private List<RequirementCandidate> candidates() { return candidates; }
    private List<RequirementCandidate> touchedCandidates() { return touchedCandidates; }
    private List<String> systemNotes() { return systemNotes; }
    private String assistantReply() { return assistantReply; }
    private void setAssistantReply(String assistantReply) { this.assistantReply = assistantReply; }

    private void refreshCandidates(List<RequirementCandidate> candidates) {
      this.candidates = new ArrayList<>(candidates);
    }

    private void addTouchedCandidate(RequirementCandidate candidate) {
      touchedCandidates.removeIf(item -> item.getId().equals(candidate.getId()));
      touchedCandidates.add(candidate);
    }

    private void addSystemNote(String note) {
      if (note != null && !note.isBlank()) {
        systemNotes.add(note);
      }
    }
  }
}
