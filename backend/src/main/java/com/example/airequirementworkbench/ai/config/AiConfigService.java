package com.example.airequirementworkbench.ai.config;

import com.example.airequirementworkbench.ai.config.AiConfigDtos.AbilityConfigDto;
import com.example.airequirementworkbench.ai.config.AiConfigDtos.AiConfigStatusDto;
import com.example.airequirementworkbench.ai.config.AiConfigDtos.ModelConfigDto;
import com.example.airequirementworkbench.ai.config.AiConfigDtos.PromptTemplateDto;
import com.example.airequirementworkbench.ai.config.AiConfigDtos.SaveAbilityConfigRequest;
import com.example.airequirementworkbench.ai.config.AiConfigDtos.SaveModelConfigRequest;
import com.example.airequirementworkbench.ai.config.AiConfigDtos.SavePromptTemplateRequest;
import com.example.airequirementworkbench.common.IdGenerator;
import com.example.airequirementworkbench.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiConfigService {
  private final AiModelConfigRepository modelRepository;
  private final PromptTemplateRepository promptRepository;
  private final AiAbilityConfigRepository abilityRepository;
  private final IdGenerator idGenerator;
  private final ObjectMapper objectMapper;
  private final Long mockUserId;
  private final String aiProvider;

  public AiConfigService(
      AiModelConfigRepository modelRepository,
      PromptTemplateRepository promptRepository,
      AiAbilityConfigRepository abilityRepository,
      IdGenerator idGenerator,
      ObjectMapper objectMapper,
      @Value("${app.mock-user-id}") Long mockUserId,
      @Value("${app.ai.provider:openai}") String aiProvider
  ) {
    this.modelRepository = modelRepository;
    this.promptRepository = promptRepository;
    this.abilityRepository = abilityRepository;
    this.idGenerator = idGenerator;
    this.objectMapper = objectMapper;
    this.mockUserId = mockUserId;
    this.aiProvider = aiProvider;
  }

  public List<ModelConfigDto> listModels() {
    return modelRepository.findByDeletedFalseOrderByUpdatedAtDesc().stream().map(this::toModelDto).toList();
  }

  public List<PromptTemplateDto> listPrompts() {
    return promptRepository.findByDeletedFalseOrderByAbilityTypeAscUpdatedAtDesc().stream().map(this::toPromptDto).toList();
  }

  public List<AbilityConfigDto> listAbilities() {
    return abilityRepository.findByDeletedFalseOrderByAbilityTypeAsc().stream().map(this::toAbilityDto).toList();
  }

  public AiConfigStatusDto getStatus() {
    String provider = aiProvider == null || aiProvider.isBlank() ? "openai" : aiProvider.trim().toLowerCase();
    if ("mock".equals(provider)) {
      return new AiConfigStatusDto("mock", "Mock", false, false, false, 0, null, List.of());
    }

    List<AiModelConfig> enabledModels = modelRepository.findByDeletedFalseOrderByUpdatedAtDesc().stream()
        .filter(model -> "openai".equalsIgnoreCase(model.getProvider()))
        .filter(model -> "enabled".equals(model.getStatus()))
        .toList();
    AiModelConfig defaultModel = enabledModels.stream()
        .filter(model -> Boolean.TRUE.equals(model.getIsDefault()))
        .findFirst()
        .orElse(enabledModels.isEmpty() ? null : enabledModels.get(0));

    List<String> missing = new java.util.ArrayList<>();
    if (enabledModels.isEmpty()) {
      missing.add("未配置启用状态的 OpenAI 模型");
    }
    if (defaultModel == null || !Boolean.TRUE.equals(defaultModel.getIsDefault())) {
      missing.add("未设置默认 OpenAI 模型");
    }
    boolean apiKeyConfigured = defaultModel != null && hasUsableApiKey(defaultModel);
    if (!apiKeyConfigured) {
      missing.add("默认模型未配置可用 API Key");
    }

    List<String> requiredAbilities = List.of("intent_router", "requirement_extract", "completeness_check", "reply_generate");
    for (String abilityType : requiredAbilities) {
      AiAbilityConfig ability = abilityRepository.findByAbilityTypeAndDeletedFalse(abilityType).orElse(null);
      if (ability == null || !Boolean.TRUE.equals(ability.getEnabled()) || !"enabled".equals(ability.getStatus())) {
        missing.add(abilityType + " 能力未启用");
      } else if (ability.getModelConfigId() == null || ability.getPromptTemplateId() == null) {
        missing.add(abilityType + " 未完整绑定模型或 Prompt");
      }
    }

    return new AiConfigStatusDto(
        provider,
        "真实LLM",
        missing.isEmpty(),
        defaultModel != null && Boolean.TRUE.equals(defaultModel.getIsDefault()),
        apiKeyConfigured,
        enabledModels.size(),
        defaultModel == null ? null : defaultModel.getModelName(),
        missing
    );
  }

  @Transactional
  public ModelConfigDto saveModel(SaveModelConfigRequest request) {
    AiModelConfig model = request.id() == null ? new AiModelConfig() : modelRepository.findById(request.id())
        .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
        .orElseThrow(() -> new BusinessException("MODEL_NOT_FOUND", "模型配置不存在"));
    if (model.getId() == null) {
      model.setId(idGenerator.nextId());
      model.setCreatedBy(mockUserId);
    }
    model.setProvider(request.provider().trim());
    model.setModelName(request.modelName().trim());
    model.setDisplayName(request.displayName().trim());
    model.setApiBaseUrl(request.apiBaseUrl().trim());
    if (request.apiKeyEnv() == null || request.apiKeyEnv().isBlank()) {
      model.setApiKeyEnv("OPENAI_API_KEY");
    } else if (!request.apiKeyEnv().contains("********")) {
      model.setApiKeyEnv(request.apiKeyEnv().trim());
    }
    if (request.apiKeySecret() != null && !request.apiKeySecret().isBlank() && !request.apiKeySecret().contains("********")) {
      model.setApiKeySecret(request.apiKeySecret().trim());
    }
    if ((model.getApiKeySecret() == null || model.getApiKeySecret().isBlank()) && (model.getApiKeyEnv() == null || model.getApiKeyEnv().isBlank())) {
      throw new BusinessException("MODEL_API_KEY_REQUIRED", "请填写真实 API Key，或填写可用的环境变量名");
    }
    model.setTemperature(request.temperature());
    model.setMaxOutputTokens(request.maxOutputTokens());
    model.setTimeoutSeconds(request.timeoutSeconds());
    model.setStatus(request.status());
    model.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
    model.setUpdatedBy(mockUserId);
    return toModelDto(modelRepository.save(model));
  }

  @Transactional
  public PromptTemplateDto savePrompt(SavePromptTemplateRequest request) {
    validateSchema(request.jsonSchema());
    PromptTemplate prompt = request.id() == null ? new PromptTemplate() : promptRepository.findById(request.id())
        .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
        .orElseThrow(() -> new BusinessException("PROMPT_NOT_FOUND", "Prompt模板不存在"));
    if (prompt.getId() == null) {
      prompt.setId(idGenerator.nextId());
      prompt.setCreatedBy(mockUserId);
    }
    prompt.setAbilityType(request.abilityType().trim());
    prompt.setTemplateCode(request.templateCode().trim());
    prompt.setTemplateName(request.templateName().trim());
    prompt.setVersion(request.version().trim());
    prompt.setSystemPrompt(request.systemPrompt().trim());
    prompt.setUserPrompt(request.userPrompt().trim());
    prompt.setJsonSchema(request.jsonSchema());
    prompt.setStatus(request.status());
    prompt.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
    prompt.setUpdatedBy(mockUserId);
    PromptTemplate saved = promptRepository.save(prompt);
    if (Boolean.TRUE.equals(saved.getIsDefault())) {
      promptRepository.findByAbilityTypeAndDeletedFalse(saved.getAbilityType()).stream()
          .filter(item -> !item.getId().equals(saved.getId()))
          .filter(item -> Boolean.TRUE.equals(item.getIsDefault()))
          .forEach(item -> {
            item.setIsDefault(false);
            item.setUpdatedBy(mockUserId);
            promptRepository.save(item);
          });
    }
    return toPromptDto(saved);
  }

  @Transactional
  public AbilityConfigDto saveAbility(SaveAbilityConfigRequest request) {
    modelRepository.findById(request.modelConfigId())
        .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
        .orElseThrow(() -> new BusinessException("MODEL_NOT_FOUND", "模型配置不存在"));
    promptRepository.findById(request.promptTemplateId())
        .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
        .orElseThrow(() -> new BusinessException("PROMPT_NOT_FOUND", "Prompt模板不存在"));

    AiAbilityConfig ability = request.id() == null ? new AiAbilityConfig() : abilityRepository.findById(request.id())
        .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
        .orElseThrow(() -> new BusinessException("ABILITY_NOT_FOUND", "AI能力配置不存在"));
    if (ability.getId() == null) {
      ability.setId(idGenerator.nextId());
      ability.setCreatedBy(mockUserId);
    }
    ability.setAbilityType(request.abilityType().trim());
    ability.setAbilityName(request.abilityName().trim());
    ability.setEnabled(request.enabled());
    ability.setModelConfigId(request.modelConfigId());
    ability.setPromptTemplateId(request.promptTemplateId());
    ability.setFallbackToMock(Boolean.TRUE.equals(request.fallbackToMock()));
    ability.setStatus(request.status());
    ability.setUpdatedBy(mockUserId);
    return toAbilityDto(abilityRepository.save(ability));
  }

  private void validateSchema(Object schema) {
    try {
      objectMapper.writeValueAsString(schema);
    } catch (Exception exception) {
      throw new BusinessException("INVALID_JSON_SCHEMA", "JSON Schema格式不正确");
    }
  }

  private ModelConfigDto toModelDto(AiModelConfig model) {
    return new ModelConfigDto(
        model.getId(), model.getProvider(), model.getModelName(), model.getDisplayName(),
        model.getApiBaseUrl(), model.getApiKeyEnv(), maskSecret(model.getApiKeySecret()), model.getTemperature(), model.getMaxOutputTokens(),
        model.getTimeoutSeconds(), model.getStatus(), model.getIsDefault(), model.getUpdatedAt()
    );
  }

  private String maskSecret(String value) {
    if (value == null) {
      return null;
    }
    if (value.startsWith("sk-")) {
      return "sk-********";
    }
    return value;
  }

  private boolean hasUsableApiKey(AiModelConfig model) {
    if (model.getApiKeySecret() != null && !model.getApiKeySecret().isBlank() && !model.getApiKeySecret().contains("********")) {
      return true;
    }
    if (model.getApiKeyEnv() == null || model.getApiKeyEnv().isBlank()) {
      return false;
    }
    String env = model.getApiKeyEnv().trim();
    return env.startsWith("sk-") || System.getenv(env) != null;
  }

  private PromptTemplateDto toPromptDto(PromptTemplate prompt) {
    return new PromptTemplateDto(
        prompt.getId(), prompt.getAbilityType(), prompt.getTemplateCode(), prompt.getTemplateName(), prompt.getVersion(),
        prompt.getSystemPrompt(), prompt.getUserPrompt(), prompt.getJsonSchema(), prompt.getStatus(),
        prompt.getIsDefault(), prompt.getUpdatedAt()
    );
  }

  private AbilityConfigDto toAbilityDto(AiAbilityConfig ability) {
    return new AbilityConfigDto(
        ability.getId(), ability.getAbilityType(), ability.getAbilityName(), ability.getEnabled(),
        ability.getModelConfigId(), ability.getPromptTemplateId(), ability.getFallbackToMock(),
        ability.getStatus(), ability.getUpdatedAt()
    );
  }
}
