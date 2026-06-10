package com.example.airequirementworkbench.ai.client;

import com.example.airequirementworkbench.ai.config.AiAbilityConfig;
import com.example.airequirementworkbench.ai.config.AiAbilityConfigRepository;
import com.example.airequirementworkbench.ai.config.AiModelConfig;
import com.example.airequirementworkbench.ai.config.AiModelConfigRepository;
import com.example.airequirementworkbench.ai.config.PromptTemplate;
import com.example.airequirementworkbench.ai.config.PromptTemplateRepository;
import com.example.airequirementworkbench.ai.dto.AiDtos.CompletenessResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.ExtractResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.CardGenerateResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.ReplyResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.RouteContext;
import com.example.airequirementworkbench.ai.dto.AiDtos.RouteResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.SimilarRequirementSearchResult;
import com.example.airequirementworkbench.ai.dto.AiDtos.SplitResult;
import com.example.airequirementworkbench.common.exception.BusinessException;
import com.example.airequirementworkbench.requirement.entity.Requirement;
import com.example.airequirementworkbench.requirement.entity.RequirementCandidate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiClient implements AiClient {
  private final AiAbilityConfigRepository abilityRepository;
  private final AiModelConfigRepository modelRepository;
  private final PromptTemplateRepository promptRepository;
  private final JsonSchemaValidator jsonSchemaValidator;
  private final ObjectMapper objectMapper;
  private final ThreadLocal<AiCallMetadata> lastMetadata = ThreadLocal.withInitial(AiCallMetadata::mock);

  public OpenAiClient(
      AiAbilityConfigRepository abilityRepository,
      AiModelConfigRepository modelRepository,
      PromptTemplateRepository promptRepository,
      JsonSchemaValidator jsonSchemaValidator,
      ObjectMapper objectMapper
  ) {
    this.abilityRepository = abilityRepository;
    this.modelRepository = modelRepository;
    this.promptRepository = promptRepository;
    this.jsonSchemaValidator = jsonSchemaValidator;
    this.objectMapper = objectMapper;
  }

  @Override
  public RouteResult route(RouteContext context) {
    return invoke("intent_router", Map.of(
        "latestMessage", context.latestMessage(),
        "sessionStatus", context.sessionStatus(),
        "currentStage", context.currentStage(),
        "currentCandidatesJson", context.currentCandidates(),
        "recentMessagesJson", context.recentMessages()
    ), RouteResult.class);
  }

  @Override
  public ExtractResult extractRequirement(String latestMessage, List<RequirementCandidate> currentCandidates) {
    return invoke("requirement_extract", Map.of(
        "latestMessage", latestMessage,
        "currentCandidatesJson", candidateSnapshots(currentCandidates)
    ), ExtractResult.class);
  }

  @Override
  public SplitResult splitRequirement(String latestMessage, List<RequirementCandidate> currentCandidates) {
    return invoke("requirement_split", Map.of(
        "latestMessage", latestMessage,
        "currentCandidatesJson", candidateSnapshots(currentCandidates)
    ), SplitResult.class);
  }

  @Override
  public CompletenessResult checkCompleteness(RequirementCandidate candidate) {
    return invoke("completeness_check", Map.of(
        "candidateJson", candidateSnapshot(candidate)
    ), CompletenessResult.class);
  }

  @Override
  public CardGenerateResult generateRequirementCard(RequirementCandidate candidate) {
    return invoke("card_generate", Map.of(
        "candidateJson", candidateSnapshot(candidate)
    ), CardGenerateResult.class);
  }

  @Override
  public SimilarRequirementSearchResult searchSimilarRequirements(String latestMessage, List<Requirement> existingRequirements) {
    return invoke("similar_requirement_search", Map.of(
        "latestMessage", latestMessage,
        "existingRequirementsJson", requirementSnapshots(existingRequirements)
    ), SimilarRequirementSearchResult.class);
  }

  @Override
  public ReplyResult generateReply(String latestMessage, List<RequirementCandidate> currentCandidates) {
    return invoke("reply_generate", Map.of(
        "latestMessage", latestMessage,
        "currentCandidatesJson", candidateSnapshots(currentCandidates)
    ), ReplyResult.class);
  }

  @Override
  public AiCallMetadata lastCallMetadata() {
    return lastMetadata.get();
  }

  private <T> T invoke(String abilityType, Map<String, Object> input, Class<T> resultType) {
    long startedAt = System.currentTimeMillis();
    AiAbilityConfig ability = abilityRepository.findByAbilityTypeAndDeletedFalse(abilityType)
        .filter(item -> Boolean.TRUE.equals(item.getEnabled()) && "enabled".equals(item.getStatus()))
        .orElseThrow(() -> new BusinessException("AI_ABILITY_DISABLED", abilityType + " AI能力未启用"));
    AiModelConfig model = resolveModel(ability);
    PromptTemplate prompt = resolvePrompt(ability, abilityType);

    String systemPrompt = render(prompt.getSystemPrompt(), input);
    String userPrompt = render(prompt.getUserPrompt(), input);
    String renderedPrompt = "SYSTEM:\n" + systemPrompt + "\n\nUSER:\n" + userPrompt;
    AiCallMetadata baseMetadata = new AiCallMetadata(
        model.getId(), model.getModelName(), prompt.getId(), prompt.getVersion(),
        renderedPrompt, null, null, null, null
    );

    String responseBody = null;
    try {
      String apiKey = resolveApiKey(model);
      if (apiKey == null || apiKey.isBlank()) {
        throw new AiClientException("OPENAI_API_KEY_MISSING", "未找到OpenAI API Key，请在模型配置中填写真实Key，或填写可用的环境变量名", input, baseMetadata);
      }
      String requestBody = buildRequest(model, prompt, systemPrompt, userPrompt, abilityType);
      HttpResponse<String> response = send(model, requestBody, apiKey);
      responseBody = response.body();
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        AiCallMetadata metadata = withOutput(baseMetadata, responseBody, startedAt, 0, 0);
        lastMetadata.set(metadata);
        throw new AiClientException("OPENAI_REQUEST_FAILED", "OpenAI请求失败：" + response.statusCode(), input, metadata);
      }

      JsonNode responseJson = objectMapper.readTree(responseBody);
      String outputText = extractOutputText(responseJson);
      if (outputText == null || outputText.isBlank()) {
        AiCallMetadata metadata = withOutput(baseMetadata, responseBody, startedAt, readUsage(responseJson, "input_tokens"), readUsage(responseJson, "output_tokens"));
        lastMetadata.set(metadata);
        throw new AiClientException("OPENAI_EMPTY_OUTPUT", "OpenAI未返回可解析内容", input, metadata);
      }

      JsonNode outputJson = objectMapper.readTree(outputText);
      try {
        jsonSchemaValidator.validate(prompt.getJsonSchema(), outputJson);
      } catch (IllegalArgumentException exception) {
        AiCallMetadata metadata = withOutput(baseMetadata, outputText, startedAt, readUsage(responseJson, "input_tokens"), readUsage(responseJson, "output_tokens"));
        lastMetadata.set(metadata);
        throw new AiClientException("AI_OUTPUT_SCHEMA_INVALID", "AI输出不符合JSON Schema：" + exception.getMessage(), input, metadata, exception);
      }

      AiCallMetadata metadata = withOutput(baseMetadata, outputText, startedAt, readUsage(responseJson, "input_tokens"), readUsage(responseJson, "output_tokens"));
      lastMetadata.set(metadata);
      return objectMapper.treeToValue(outputJson, resultType);
    } catch (AiClientException exception) {
      throw exception;
    } catch (JsonProcessingException exception) {
      AiCallMetadata metadata = withOutput(baseMetadata, responseBody, startedAt, 0, 0);
      lastMetadata.set(metadata);
      throw new AiClientException("AI_OUTPUT_JSON_INVALID", "AI输出不是合法JSON，请检查Prompt模板", input, metadata, exception);
    } catch (IOException exception) {
      AiCallMetadata metadata = withOutput(baseMetadata, responseBody, startedAt, 0, 0);
      lastMetadata.set(metadata);
      throw new AiClientException("OPENAI_NETWORK_ERROR", "OpenAI调用失败，请检查网络、Key或模型配置", input, metadata, exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      AiCallMetadata metadata = withOutput(baseMetadata, responseBody, startedAt, 0, 0);
      lastMetadata.set(metadata);
      throw new AiClientException("OPENAI_INTERRUPTED", "OpenAI调用被中断，请稍后重试", input, metadata, exception);
    }
  }

  private AiModelConfig resolveModel(AiAbilityConfig ability) {
    if (ability.getModelConfigId() != null) {
      return modelRepository.findById(ability.getModelConfigId())
          .filter(item -> !Boolean.TRUE.equals(item.getDeleted()) && "enabled".equals(item.getStatus()))
          .orElseThrow(() -> new BusinessException("MODEL_NOT_FOUND", "AI模型配置不可用"));
    }
    return modelRepository.findFirstByProviderAndStatusAndDeletedFalseOrderByIsDefaultDescUpdatedAtDesc("openai", "enabled")
        .orElseThrow(() -> new BusinessException("MODEL_NOT_FOUND", "未配置可用OpenAI模型"));
  }

  private PromptTemplate resolvePrompt(AiAbilityConfig ability, String abilityType) {
    if (ability.getPromptTemplateId() != null) {
      return promptRepository.findById(ability.getPromptTemplateId())
          .filter(item -> !Boolean.TRUE.equals(item.getDeleted()) && "enabled".equals(item.getStatus()))
          .orElseThrow(() -> new BusinessException("PROMPT_NOT_FOUND", "Prompt模板不可用"));
    }
    return promptRepository.findFirstByAbilityTypeAndStatusAndDeletedFalseOrderByIsDefaultDescUpdatedAtDesc(abilityType, "enabled")
        .orElseThrow(() -> new BusinessException("PROMPT_NOT_FOUND", abilityType + " 未配置可用Prompt模板"));
  }

  private String buildRequest(AiModelConfig model, PromptTemplate prompt, String systemPrompt, String userPrompt, String abilityType) throws JsonProcessingException {
    Map<String, Object> format = new LinkedHashMap<>();
    format.put("type", "json_schema");
    format.put("name", abilityType.replaceAll("[^A-Za-z0-9_]", "_"));
    format.put("strict", false);
    format.put("schema", prompt.getJsonSchema());

    List<Map<String, Object>> input = new ArrayList<>();
    input.add(message("system", systemPrompt));
    input.add(message("user", userPrompt));

    Map<String, Object> text = new LinkedHashMap<>();
    text.put("format", format);

    Map<String, Object> request = new LinkedHashMap<>();
    request.put("model", model.getModelName());
    request.put("input", input);
    request.put("temperature", model.getTemperature());
    request.put("max_output_tokens", model.getMaxOutputTokens());
    request.put("text", text);
    return objectMapper.writeValueAsString(request);
  }

  private Map<String, Object> message(String role, String text) {
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("type", "input_text");
    content.put("text", text);
    Map<String, Object> message = new LinkedHashMap<>();
    message.put("role", role);
    message.put("content", List.of(content));
    return message;
  }

  private HttpResponse<String> send(AiModelConfig model, String requestBody, String apiKey) throws IOException, InterruptedException {
    String baseUrl = model.getApiBaseUrl().replaceAll("/+$", "");
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/responses"))
        .timeout(Duration.ofSeconds(model.getTimeoutSeconds()))
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build();
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(model.getTimeoutSeconds()))
        .build()
        .send(request, HttpResponse.BodyHandlers.ofString());
  }

  private String resolveApiKey(AiModelConfig model) {
    if (model.getApiKeySecret() != null && !model.getApiKeySecret().isBlank()) {
      return model.getApiKeySecret().trim();
    }
    if (model.getApiKeyEnv() == null || model.getApiKeyEnv().isBlank()) {
      return null;
    }
    String trimmed = model.getApiKeyEnv().trim();
    if (trimmed.startsWith("sk-")) {
      return trimmed;
    }
    return System.getenv(trimmed);
  }

  private String render(String template, Map<String, Object> input) {
    String rendered = template;
    for (Map.Entry<String, Object> entry : input.entrySet()) {
      rendered = rendered.replace("{{" + entry.getKey() + "}}", toJson(entry.getValue()));
    }
    return rendered;
  }

  private String toJson(Object value) {
    if (value instanceof String text) {
      return text;
    }
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      return String.valueOf(value);
    }
  }

  private List<Map<String, Object>> candidateSnapshots(List<RequirementCandidate> candidates) {
    return candidates.stream().map(this::candidateSnapshot).toList();
  }

  private Map<String, Object> candidateSnapshot(RequirementCandidate candidate) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("id", candidate.getId());
    snapshot.put("title", candidate.getTitle());
    snapshot.put("status", candidate.getStatus());
    snapshot.put("contentJson", candidate.getContentJson());
    snapshot.put("completenessScore", candidate.getCompletenessScore());
    snapshot.put("missingItemsJson", candidate.getMissingItemsJson());
    snapshot.put("suggestedQuestionsJson", candidate.getSuggestedQuestionsJson());
    return snapshot;
  }

  private List<Map<String, Object>> requirementSnapshots(List<Requirement> requirements) {
    return requirements.stream().map(requirement -> {
      Map<String, Object> snapshot = new LinkedHashMap<>();
      snapshot.put("id", requirement.getId());
      snapshot.put("requirementNo", requirement.getRequirementNo());
      snapshot.put("title", requirement.getTitle());
      snapshot.put("status", requirement.getStatus());
      snapshot.put("contentJson", requirement.getContentJson());
      snapshot.put("completenessScore", requirement.getCompletenessScore());
      return snapshot;
    }).toList();
  }

  private String extractOutputText(JsonNode responseJson) {
    if (responseJson.hasNonNull("output_text")) {
      return responseJson.get("output_text").asText();
    }
    JsonNode output = responseJson.get("output");
    if (output != null && output.isArray()) {
      for (JsonNode item : output) {
        JsonNode content = item.get("content");
        if (content != null && content.isArray()) {
          for (JsonNode contentItem : content) {
            if (contentItem.hasNonNull("text")) {
              return contentItem.get("text").asText();
            }
          }
        }
      }
    }
    return null;
  }

  private int readUsage(JsonNode responseJson, String field) {
    JsonNode usage = responseJson.get("usage");
    if (usage != null && usage.has(field)) {
      return usage.get(field).asInt();
    }
    return 0;
  }

  private AiCallMetadata withOutput(AiCallMetadata metadata, String outputText, long startedAt, Integer inputTokens, Integer outputTokens) {
    return new AiCallMetadata(
        metadata.modelConfigId(),
        metadata.modelName(),
        metadata.promptTemplateId(),
        metadata.promptVersion(),
        metadata.renderedPrompt(),
        outputText,
        inputTokens,
        outputTokens,
        (int) Math.max(1, System.currentTimeMillis() - startedAt)
    );
  }
}
