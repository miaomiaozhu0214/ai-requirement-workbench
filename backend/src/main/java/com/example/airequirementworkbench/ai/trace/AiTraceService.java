package com.example.airequirementworkbench.ai.trace;

import com.example.airequirementworkbench.ai.client.AiCallMetadata;
import com.example.airequirementworkbench.ai.client.AiClientException;
import com.example.airequirementworkbench.ai.config.PromptTemplate;
import com.example.airequirementworkbench.ai.config.PromptTemplateRepository;
import com.example.airequirementworkbench.ai.trace.AiTraceDtos.AiTraceDto;
import com.example.airequirementworkbench.common.IdGenerator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiTraceService {
  private final AiTraceRepository aiTraceRepository;
  private final PromptTemplateRepository promptTemplateRepository;
  private final IdGenerator idGenerator;
  private final ObjectMapper objectMapper;

  public AiTraceService(
      AiTraceRepository aiTraceRepository,
      PromptTemplateRepository promptTemplateRepository,
      IdGenerator idGenerator,
      ObjectMapper objectMapper
  ) {
    this.aiTraceRepository = aiTraceRepository;
    this.promptTemplateRepository = promptTemplateRepository;
    this.idGenerator = idGenerator;
    this.objectMapper = objectMapper;
  }

  public AiTrace recordSuccess(Long sessionId, String objectType, Long objectId, String abilityType, Object input, Object output, Long userId, long startedAt) {
    return recordSuccess(sessionId, objectType, objectId, abilityType, input, output, userId, startedAt, AiCallMetadata.mock());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AiTrace recordSuccess(Long sessionId, String objectType, Long objectId, String abilityType, Object input, Object output, Long userId, long startedAt, AiCallMetadata metadata) {
    // Trace 使用独立事务保存，确保外层候选需求写入失败时也能看到本次 AI 调用的输入输出。
    AiTrace trace = baseTrace(sessionId, objectType, objectId, abilityType, input, userId, startedAt);
    applyMetadata(trace, metadata);
    trace.setOutputJson(asMap(output));
    trace.setStatus("success");
    trace.setTokenInput(metadata.tokenInput() == null || metadata.tokenInput() == 0 ? estimateTokens(input) : metadata.tokenInput());
    trace.setTokenOutput(metadata.tokenOutput() == null || metadata.tokenOutput() == 0 ? estimateTokens(output) : metadata.tokenOutput());
    return aiTraceRepository.save(trace);
  }

  public AiTrace recordFailure(Long sessionId, String objectType, Long objectId, String abilityType, Object input, Exception exception, Long userId, long startedAt) {
    return recordFailure(sessionId, objectType, objectId, abilityType, input, exception, userId, startedAt, AiCallMetadata.mock());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AiTrace recordFailure(Long sessionId, String objectType, Long objectId, String abilityType, Object input, Exception exception, Long userId, long startedAt, AiCallMetadata metadata) {
    // 真实 LLM、Prompt、Schema 任一环节失败都要落 Trace，页面才能给出可排查的错误信息。
    AiTrace trace = baseTrace(sessionId, objectType, objectId, abilityType, input, userId, startedAt);
    applyMetadata(trace, metadata);
    trace.setStatus("failed");
    trace.setErrorCode(exception instanceof AiClientException aiException ? aiException.getCode() : exception.getClass().getSimpleName());
    trace.setErrorMessage(exception.getMessage());
    trace.setTokenInput(metadata.tokenInput() == null ? estimateTokens(input) : metadata.tokenInput());
    trace.setTokenOutput(metadata.tokenOutput() == null ? 0 : metadata.tokenOutput());
    return aiTraceRepository.save(trace);
  }

  public List<AiTraceDto> listLatest(Long sessionId) {
    // Trace 列表支持两种入口：全局最近调用，或从某个会话进入时只看该会话的调用链。
    List<AiTrace> traces = sessionId == null
        ? aiTraceRepository.findTop50ByOrderByCreatedAtDesc()
        : aiTraceRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    return toDtos(traces);
  }

  public AiTraceDto get(Long id) {
    return toDto(aiTraceRepository.findById(id).orElseThrow(), Map.of());
  }

  public List<AiTraceDto> toDtos(List<AiTrace> traces) {
    Map<Long, PromptTemplate> promptMap = new HashMap<>();
    List<Long> promptIds = traces.stream()
        .map(AiTrace::getPromptTemplateId)
        .filter(id -> id != null)
        .distinct()
        .toList();
    promptTemplateRepository.findAllById(promptIds).forEach(prompt -> promptMap.put(prompt.getId(), prompt));
    return traces.stream().map(trace -> toDto(trace, promptMap)).toList();
  }

  public AiTraceDto toDto(AiTrace trace) {
    return toDto(trace, Map.of());
  }

  private AiTrace baseTrace(Long sessionId, String objectType, Long objectId, String abilityType, Object input, Long userId, long startedAt) {
    AiTrace trace = new AiTrace();
    trace.setId(idGenerator.nextId());
    trace.setTraceNo(idGenerator.nextTraceNo());
    trace.setSessionId(sessionId);
    trace.setBusinessObjectType(objectType);
    trace.setBusinessObjectId(objectId);
    trace.setAbilityType(abilityType);
    trace.setModelName("mock-requirement-model");
    trace.setPromptVersion("mock-v1.0");
    trace.setInputJson(asMap(input));
    trace.setDurationMs((int) Math.max(1, System.currentTimeMillis() - startedAt));
    trace.setCreatedBy(userId);
    return trace;
  }

  private void applyMetadata(AiTrace trace, AiCallMetadata metadata) {
    if (metadata == null) {
      return;
    }
    trace.setModelConfigId(metadata.modelConfigId());
    trace.setModelName(metadata.modelName() == null ? trace.getModelName() : metadata.modelName());
    trace.setPromptTemplateId(metadata.promptTemplateId());
    trace.setPromptVersion(metadata.promptVersion() == null ? trace.getPromptVersion() : metadata.promptVersion());
    trace.setOutputText(metadata.outputText());
    if (metadata.durationMs() != null) {
      trace.setDurationMs(metadata.durationMs());
    }
    if (metadata.renderedPrompt() != null) {
      // 渲染后的 Prompt 放在 inputJson，方便排查模板变量替换是否正确，但不改变 AI 输出结构。
      Map<String, Object> inputJson = trace.getInputJson() == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(trace.getInputJson());
      inputJson.put("renderedPrompt", metadata.renderedPrompt());
      trace.setInputJson(inputJson);
    }
  }

  private AiTraceDto toDto(AiTrace trace, Map<Long, PromptTemplate> promptMap) {
    PromptTemplate prompt = trace.getPromptTemplateId() == null
        ? null
        : Optional.ofNullable(promptMap.get(trace.getPromptTemplateId()))
            .or(() -> promptTemplateRepository.findById(trace.getPromptTemplateId()))
            .orElse(null);
    return new AiTraceDto(
        trace.getId(),
        trace.getTraceNo(),
        trace.getSessionId(),
        trace.getBusinessObjectType(),
        trace.getBusinessObjectId(),
        trace.getAbilityType(),
        trace.getModelConfigId(),
        trace.getModelName(),
        trace.getPromptTemplateId(),
        prompt == null ? null : prompt.getTemplateCode(),
        prompt == null ? null : prompt.getTemplateName(),
        trace.getPromptVersion(),
        summarizeInput(trace.getInputJson()),
        trace.getInputJson(),
        trace.getOutputJson(),
        trace.getOutputText(),
        readString(trace.getOutputJson(), "intent"),
        readStringList(trace.getOutputJson(), "nextActions"),
        trace.getTokenInput(),
        trace.getTokenOutput(),
        trace.getDurationMs(),
        trace.getStatus(),
        trace.getErrorCode(),
        trace.getErrorMessage(),
        trace.getCreatedAt()
    );
  }

  private String summarizeInput(Map<String, Object> inputJson) {
    // 前端列表只展示摘要；完整输入仍保留在 inputJson 里，供详情弹层排查。
    if (inputJson == null || inputJson.isEmpty()) {
      return "";
    }
    if (inputJson.get("latestMessage") != null) {
      return shorten("用户输入：" + inputJson.get("latestMessage"));
    }
    if (inputJson.get("candidateId") != null) {
      return "候选需求：" + inputJson.get("candidateId");
    }
    if (inputJson.get("renderedPrompt") != null) {
      return shorten(String.valueOf(inputJson.get("renderedPrompt")));
    }
    return shorten(inputJson.toString());
  }

  private String readString(Map<String, Object> json, String field) {
    return json == null || json.get(field) == null ? null : String.valueOf(json.get(field));
  }

  private List<String> readStringList(Map<String, Object> json, String field) {
    if (json == null || !(json.get(field) instanceof List<?> values)) {
      return null;
    }
    return values.stream().map(String::valueOf).toList();
  }

  private String shorten(String value) {
    if (value == null) {
      return "";
    }
    String normalized = value.replaceAll("\\s+", " ").trim();
    return normalized.length() <= 180 ? normalized : normalized.substring(0, 180) + "...";
  }

  private Map<String, Object> asMap(Object value) {
    if (value instanceof Map<?, ?> map) {
      return map.entrySet().stream().collect(java.util.stream.Collectors.toMap(entry -> String.valueOf(entry.getKey()), Map.Entry::getValue));
    }
    if (value == null) {
      return Collections.emptyMap();
    }
    ObjectMapper traceMapper = objectMapper.copy();
    traceMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    return traceMapper.convertValue(value, new TypeReference<>() {
    });
  }

  private int estimateTokens(Object value) {
    if (value == null) {
      return 0;
    }
    // 某些 Mock 或失败响应拿不到真实 token，用粗略估算保留字段可见性。
    return Math.max(1, value.toString().length() / 4);
  }
}
