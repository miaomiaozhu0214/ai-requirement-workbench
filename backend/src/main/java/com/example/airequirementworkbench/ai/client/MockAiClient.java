package com.example.airequirementworkbench.ai.client;

import com.example.airequirementworkbench.ai.config.PromptTemplate;
import com.example.airequirementworkbench.ai.config.PromptTemplateRepository;
import com.example.airequirementworkbench.ai.dto.AiDtos.CandidatePatch;
import com.example.airequirementworkbench.ai.dto.AiDtos.CardGenerateResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.CompletenessResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.ExtractResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.ReplyResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.RouteContext;
import com.example.airequirementworkbench.ai.dto.AiDtos.RouteResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.SimilarRequirement;
import com.example.airequirementworkbench.ai.dto.AiDtos.SimilarRequirementSearchResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.SplitResult;
import com.example.airequirementworkbench.requirement.entity.Requirement;
import com.example.airequirementworkbench.requirement.entity.RequirementCandidate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock")
public class MockAiClient implements AiClient {
  private final PromptTemplateRepository promptTemplateRepository;
  private final ThreadLocal<AiCallMetadata> lastMetadata = ThreadLocal.withInitial(AiCallMetadata::mock);

  public MockAiClient(PromptTemplateRepository promptTemplateRepository) {
    this.promptTemplateRepository = promptTemplateRepository;
  }

  @Override
  public RouteResult route(RouteContext context) {
    // Mock 只在显式 AI_PROVIDER=mock 时启用，用来稳定跑通本地流程；真实验收仍以 OpenAiClient 为准。
    markCall("intent_router");
    String latestMessage = context.latestMessage();
    if (isUnclear(latestMessage)) {
      return new RouteResult(
          "unclear",
          new BigDecimal("0.96"),
          "conversation",
          null,
          false,
          List.of("generate_reply"),
          "用户输入过短或缺少可识别的需求信息"
      );
    }
    if (contains(latestMessage, "生成卡片", "/生成卡片")) {
      return new RouteResult(
          "generate_card",
          new BigDecimal(currentEditableCandidateCount(context) > 0 ? "0.92" : "0.72"),
          "candidate",
          null,
          false,
          List.of("generate_requirement_card", "generate_reply"),
          "用户要求生成正式需求卡片"
      );
    }
    if (contains(latestMessage, "查询", "历史需求", "相似需求", "有没有类似")) {
      return new RouteResult(
          "query_requirement",
          new BigDecimal("0.90"),
          "requirement",
          null,
          false,
          List.of("search_similar_requirements", "generate_reply"),
          "用户正在查询历史或相似需求"
      );
    }
    if (contains(latestMessage, "拆分", "分成")) {
      return new RouteResult(
          "split_requirement",
          new BigDecimal("0.91"),
          "conversation",
          null,
          true,
          List.of("split_requirement", "check_completeness", "generate_reply"),
          "用户希望将需求拆分为多个候选需求"
      );
    }
    boolean multiple = contains(latestMessage, "而且", "另外", "同时") && contains(latestMessage, "导出", "审批");
    return new RouteResult(
        "new_requirement",
        new BigDecimal("0.93"),
        "conversation",
        null,
        multiple,
        List.of("extract_requirement", "check_completeness", "generate_reply"),
        multiple ? "用户表达了多个功能诉求" : "用户正在描述或补充需求"
    );
  }

  @Override
  public ExtractResult extractRequirement(String latestMessage, List<RequirementCandidate> currentCandidates) {
    markCall("requirement_extract");
    List<CandidatePatch> patches = new ArrayList<>();
    boolean hasExportCandidate = currentCandidates.stream().anyMatch(candidate -> candidate.getTitle().contains("导出"));

    if (contains(latestMessage, "当前筛选", "筛选结果", "运营", "字段", "5000", "日志")) {
      Map<String, Object> fields = new LinkedHashMap<>();
      if (contains(latestMessage, "当前筛选", "筛选结果")) {
        fields.put("exportRange", "当前筛选结果");
        fields.put("businessRules", List.of("导出范围为当前筛选结果"));
      }
      if (contains(latestMessage, "运营")) {
        fields.put("permissions", List.of("仅运营人员允许导出"));
        fields.put("userRoles", List.of("运营人员"));
      }
      if (contains(latestMessage, "字段")) {
        fields.put("fields", List.of("导出字段与列表字段一致"));
      }
      if (contains(latestMessage, "5000")) {
        fields.put("exportLimit", "单次最多导出5000条");
        fields.put("exceptionCases", List.of("超过5000条时提示用户缩小筛选范围"));
      }
      if (contains(latestMessage, "日志")) {
        fields.put("auditLog", "记录导出操作日志");
      }
      patches.add(new CandidatePatch("合同列表导出", hasExportCandidate ? "update" : "create", fields, new BigDecimal("0.90")));
      return new ExtractResult(patches);
    }

    if (contains(latestMessage, "导出")) {
      patches.add(new CandidatePatch("合同列表导出", "create", Map.of(
          "title", "合同列表导出",
          "background", "客户希望合同列表支持数据导出",
          "businessGoal", "支持用户根据筛选条件导出合同数据",
          "scenarios", List.of("合同列表查询"),
          "scope", List.of("合同列表增加导出按钮")
      ), new BigDecimal("0.88")));
    }

    if (contains(latestMessage, "审批记录", "审批")) {
      patches.add(new CandidatePatch("合同详情展示审批记录", "create", Map.of(
          "title", "合同详情展示审批记录",
          "background", "用户需要在合同详情中查看审批过程",
          "businessGoal", "帮助用户追溯合同审批记录",
          "scenarios", List.of("合同详情查看"),
          "scope", List.of("合同详情页增加审批记录区域")
      ), new BigDecimal("0.84")));
    }

    if (patches.isEmpty()) {
      patches.add(new CandidatePatch("待完善需求", currentCandidates.isEmpty() ? "create" : "update", Map.of(
          "title", "待完善需求",
          "background", latestMessage,
          "businessGoal", "根据用户描述继续澄清需求"
      ), new BigDecimal("0.70")));
    }

    return new ExtractResult(patches);
  }

  @Override
  public SplitResult splitRequirement(String latestMessage, List<RequirementCandidate> currentCandidates) {
    markCall("requirement_split");
    ExtractResult extractResult = extractRequirement(latestMessage, currentCandidates);
    markCall("requirement_split");
    return new SplitResult(extractResult.patches(), "已按独立业务诉求拆分候选需求");
  }

  @Override
  public CompletenessResult checkCompleteness(RequirementCandidate candidate) {
    markCall("completeness_check");
    Map<String, Object> content = candidate.getContentJson();
    if (candidate.getTitle().contains("审批记录")) {
      return new CompletenessResult(
          new BigDecimal("48"),
          List.of("展示字段", "记录来源", "可见角色"),
          List.of("审批记录来源系统未明确"),
          List.of("审批记录需要展示哪些字段？", "哪些角色可以查看审批记录？", "审批记录来自哪个系统或表？"),
          false
      );
    }

    List<String> missing = new ArrayList<>();
    if (!content.containsKey("exportRange")) missing.add("导出范围");
    if (!content.containsKey("permissions")) missing.add("权限要求");
    if (!content.containsKey("fields")) missing.add("导出字段");
    if (!content.containsKey("exportLimit")) missing.add("单次导出数量限制");
    if (!content.containsKey("auditLog")) missing.add("是否记录导出日志");

    BigDecimal score = BigDecimal.valueOf(Math.max(45, 95 - missing.size() * 10L));
    return new CompletenessResult(
        score,
        missing,
        missing.contains("权限要求") ? List.of("未说明管理员是否可导出") : List.of(),
        missing.stream().limit(5).map(item -> switch (item) {
          case "导出范围" -> "导出范围是当前筛选结果还是全部数据？";
          case "权限要求" -> "哪些角色可以看到并使用导出按钮？";
          case "导出字段" -> "导出字段是否与列表字段一致？";
          case "单次导出数量限制" -> "单次导出是否需要数量上限？";
          default -> "是否需要记录导出操作日志？";
        }).toList(),
        score.compareTo(new BigDecimal("80")) >= 0
    );
  }

  @Override
  public CardGenerateResult generateRequirementCard(RequirementCandidate candidate) {
    markCall("card_generate");
    List<String> missing = candidate.getMissingItemsJson() == null ? List.of() : candidate.getMissingItemsJson();
    return new CardGenerateResult(
        candidate.getTitle(),
        candidate.getContentJson(),
        missing,
        missing.isEmpty() || candidate.getCompletenessScore().compareTo(new BigDecimal("80")) >= 0,
        missing.isEmpty() ? "候选需求已具备生成需求卡片条件" : "候选需求仍缺少关键信息，需要用户确认后再生成正式需求"
    );
  }

  @Override
  public SimilarRequirementSearchResult searchSimilarRequirements(String latestMessage, List<Requirement> existingRequirements) {
    markCall("similar_requirement_search");
    List<SimilarRequirement> items = existingRequirements.stream()
        .filter(requirement -> contains(latestMessage, requirement.getTitle()) || contains(requirement.getTitle(), "导出", "审批"))
        .limit(5)
        .map(requirement -> new SimilarRequirement(
            requirement.getId(),
            requirement.getRequirementNo(),
            requirement.getTitle(),
            new BigDecimal("0.76"),
            "标题或业务关键词相近"
        ))
        .toList();
    return new SimilarRequirementSearchResult(items, items.isEmpty() ? "未发现明显相似需求" : "发现可能相关的历史需求");
  }

  @Override
  public ReplyResult generateReply(String latestMessage, List<RequirementCandidate> currentCandidates) {
    markCall("reply_generate");
    if (isUnclear(latestMessage)) {
      return new ReplyResult("我还没有识别到明确的需求。请补充你想解决的问题、使用场景、期望功能或业务规则。");
    }
    if (contains(latestMessage, "生成卡片", "/生成卡片")) {
      return new ReplyResult("我会先检查当前候选需求是否具备生成卡片条件。若仍缺少产品线、模块或关键规则，需要先补充后再生成正式需求。");
    }
    if (contains(latestMessage, "查询", "历史需求", "相似需求", "有没有类似")) {
      return new ReplyResult("已帮你检查相似历史需求。你可以根据相似结果决定复用、补充或继续创建新需求。");
    }
    if (contains(latestMessage, "当前筛选", "运营", "字段", "5000", "日志")) {
      return new ReplyResult("已把补充信息合并到候选需求中。建议继续确认单次导出数量限制、异常提示和是否记录导出日志。");
    }
    if (contains(latestMessage, "导出") && contains(latestMessage, "审批")) {
      return new ReplyResult("我识别到两个候选需求：合同列表导出、合同详情展示审批记录。建议先完善导出范围、导出字段和权限要求。");
    }
    if (contains(latestMessage, "导出")) {
      return new ReplyResult("已识别“合同列表导出”候选需求。请确认导出范围、可导出角色、导出字段和异常提示。");
    }
    return new ReplyResult("已收到你的需求描述，并生成候选需求草稿。请继续补充使用场景、业务规则、权限和验收标准。");
  }

  @Override
  public AiCallMetadata lastCallMetadata() {
    return lastMetadata.get();
  }

  private void markCall(String abilityType) {
    // Mock 也尽量关联默认 Prompt 版本，保证 Trace 页面能按同一字段展示模型、模板和版本。
    Optional<PromptTemplate> prompt = promptTemplateRepository.findFirstByAbilityTypeAndStatusAndDeletedFalseOrderByIsDefaultDescUpdatedAtDesc(abilityType, "enabled");
    lastMetadata.set(prompt
        .map(template -> new AiCallMetadata(null, "mock-requirement-model", template.getId(), template.getVersion(), null, null, null, null, null))
        .orElseGet(AiCallMetadata::mock));
  }

  private boolean contains(String text, String... keywords) {
    if (text == null) {
      return false;
    }
    String lower = text.toLowerCase(Locale.ROOT);
    for (String keyword : keywords) {
      if (keyword == null) {
        continue;
      }
      if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    return false;
  }

  private boolean isUnclear(String text) {
    String trimmed = text == null ? "" : text.trim();
    return trimmed.length() < 2 || trimmed.matches("[0-9]+") || trimmed.matches("[\\p{Punct}\\s]+");
  }

  private long currentEditableCandidateCount(RouteContext context) {
    return context.currentCandidates().stream()
        .filter(candidate -> !"closed".equals(candidate.get("status")) && !"converted".equals(candidate.get("status")))
        .count();
  }
}
