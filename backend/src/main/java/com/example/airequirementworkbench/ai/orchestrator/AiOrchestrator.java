package com.example.airequirementworkbench.ai.orchestrator;

import com.example.airequirementworkbench.ai.client.AiClient;
import com.example.airequirementworkbench.ai.client.AiClientException;
import com.example.airequirementworkbench.ai.config.AiAbilityConfig;
import com.example.airequirementworkbench.ai.config.AiAbilityConfigRepository;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(AiOrchestrator.class);
  private static final int DEFAULT_EXECUTION_ORDER = 100;
  private static final Map<String, String> ACTION_ABILITY_TYPES = Map.ofEntries(
      Map.entry("extract_requirement", "requirement_extract"),
      Map.entry("extract_requirement_info", "requirement_extract"),
      Map.entry("split_requirement", "requirement_split"),
      Map.entry("requirement_split", "requirement_split"),
      Map.entry("check_completeness", "completeness_check"),
      Map.entry("generate_requirement_card", "card_generate"),
      Map.entry("generate_card", "card_generate"),
      Map.entry("card_generate", "card_generate"),
      Map.entry("search_similar_requirements", "similar_requirement_search"),
      Map.entry("similar_requirement_search", "similar_requirement_search"),
      Map.entry("query_requirement", "similar_requirement_search"),
      Map.entry("generate_reply", "reply_generate"),
      Map.entry("reply_generate", "reply_generate"),
      Map.entry("clarify_short_reply", "reply_generate")
  );
  private final AiClient aiClient;
  private final AiTraceService aiTraceService;
  private final AiAbilityConfigRepository abilityRepository;
  private final ConversationMessageRepository messageRepository;
  private final RequirementCandidateRepository candidateRepository;
  private final RequirementCandidatePatchRepository patchRepository;
  private final RequirementRepository requirementRepository;
  private final IdGenerator idGenerator;
  private final Map<String, AiActionHandler> actionRegistry;

  public AiOrchestrator(
      AiClient aiClient,
      AiTraceService aiTraceService,
      AiAbilityConfigRepository abilityRepository,
      ConversationMessageRepository messageRepository,
      RequirementCandidateRepository candidateRepository,
      RequirementCandidatePatchRepository patchRepository,
      RequirementRepository requirementRepository,
      IdGenerator idGenerator
  ) {
    this.aiClient = aiClient;
    this.aiTraceService = aiTraceService;
    this.abilityRepository = abilityRepository;
    this.messageRepository = messageRepository;
    this.candidateRepository = candidateRepository;
    this.patchRepository = patchRepository;
    this.requirementRepository = requirementRepository;
    this.idGenerator = idGenerator;
    this.actionRegistry = buildActionRegistry();
  }

  public OrchestrationResult processUserMessage(ConversationSession session, ConversationMessage userMessage, Long userId) {
    List<RequirementCandidate> candidates = candidateRepository.findBySessionIdAndDeletedFalseOrderByUpdatedAtDesc(session.getId());
    /*
     * 所有用户消息都必须先进入 intent_router。
     * Router 只判断意图和建议动作，不能直接创建候选需求或正式需求；这样可以把“模型判断”和“后端写库”
     * 明确分开，避免大模型输出绕过状态校验、Trace记录和会话绑定。
     */
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
      // 低置信度时不执行写入类动作，先让用户确认，降低误建候选需求或误生成卡片的风险。
      context.addSystemNote("Router 置信度较低，已跳过写入类动作，改为向用户确认。");
      handleReply(context);
      return new OrchestrationResult(context.assistantReply(), routeCall.trace().getId());
    }

    List<String> actions = normalizeActions(route.nextActions());
    if (actions.isEmpty()) {
      actions = List.of("generate_reply");
    }
    if (isStructurallyUnclear(userMessage.getContent())) {
      // “1”、纯数字、纯符号这类输入必须保留 Router Trace，但不能直接驱动需求抽取写库。
      context.addSystemNote("用户输入过短或缺少业务语义，已跳过写入类动作。");
      actions = filterWriteActionsForStructurallyUnclearInput(actions);
    }
    List<String> executionPlan = resolveExecutionPlan(actions);
    log.debug("AI action execution plan resolved. routerActions={}, resolvedActions={}", actions, executionPlan);
    for (String action : executionPlan) {
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
    // Registry 把 Router 建议动作映射到后端可控能力，便于集中校验动作是否允许执行。
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

  List<String> resolveExecutionPlan(List<String> actions) {
    List<ActionPlanItem> actionPlan = new ArrayList<>();
    for (int index = 0; index < actions.size(); index++) {
      String action = actions.get(index);
      actionPlan.add(new ActionPlanItem(action, canonicalAbilityType(action), index));
    }

    Set<String> abilityTypes = actionPlan.stream()
        .map(ActionPlanItem::abilityType)
        .filter(abilityType -> abilityType != null)
        .collect(Collectors.toSet());
    Map<String, Integer> executionOrders = abilityTypes.isEmpty()
        ? Map.of()
        : abilityRepository.findByAbilityTypeInAndDeletedFalse(abilityTypes).stream()
            .collect(Collectors.toMap(
                AiAbilityConfig::getAbilityType,
                ability -> ability.getExecutionOrder() == null ? DEFAULT_EXECUTION_ORDER : ability.getExecutionOrder(),
                (left, right) -> left
            ));

    return resolveExecutionPlan(actions, executionOrders);
  }

  static List<String> resolveExecutionPlan(List<String> actions, Map<String, Integer> executionOrders) {
    List<ActionPlanItem> actionPlan = new ArrayList<>();
    for (int index = 0; index < actions.size(); index++) {
      String action = actions.get(index);
      actionPlan.add(new ActionPlanItem(action, canonicalAbilityType(action), index));
    }
    return actionPlan.stream()
        .map(item -> item.withExecutionOrder(item.abilityType() == null
            ? DEFAULT_EXECUTION_ORDER
            : executionOrders.getOrDefault(item.abilityType(), DEFAULT_EXECUTION_ORDER)))
        .sorted(Comparator.comparingInt(ActionPlanItem::executionOrder).thenComparingInt(ActionPlanItem::originalIndex))
        .map(ActionPlanItem::action)
        .toList();
  }

  private static String canonicalAbilityType(String action) {
    return ACTION_ABILITY_TYPES.get(action);
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
      // 生成正式卡片是强写入前置动作：必须已有候选需求，且 Router 置信度足够高。
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

  static List<String> filterWriteActionsForStructurallyUnclearInput(List<String> actions) {
    List<String> safeActions = actions.stream()
        .filter(action -> !isWriteAction(action))
        .toList();
    return safeActions.isEmpty() ? List.of("generate_reply") : safeActions;
  }

  private static boolean isWriteAction(String action) {
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
    // Router 需要理解多轮上下文，但只取最近 10 条，避免 Prompt 过长并降低无关历史干扰。
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
    // 候选需求与会话强绑定：同一会话内标题相同且未关闭/未转正时做增量合并，否则新建候选。
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

      // 大模型只产出 patch，真正的 JSON 合并、状态流转和持久化由后端完成，保证可审计、可回滚。
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
    // patch 表保存本轮 AI 修改前后快照，并关联 Trace，方便测试和产品复盘多轮补充是如何合并的。
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
    // 优先选择本轮刚更新的候选，避免用户补充后生成了其他历史候选的卡片。
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
    /*
     * AI 调用统一从这里记录 Trace。成功和失败都落库，且失败使用独立事务写入，
     * 避免外层业务回滚时丢失排查真实 LLM、Prompt 或 JSON Schema 问题所需的证据。
     */
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

  private record ActionPlanItem(String action, String abilityType, int originalIndex, int executionOrder) {
    private ActionPlanItem(String action, String abilityType, int originalIndex) {
      this(action, abilityType, originalIndex, DEFAULT_EXECUTION_ORDER);
    }

    private ActionPlanItem withExecutionOrder(int executionOrder) {
      return new ActionPlanItem(action, abilityType, originalIndex, executionOrder);
    }
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
