package com.example.airequirementworkbench.ai.config;

import com.example.airequirementworkbench.ai.config.AiConfigDtos.AbilityConfigDto;
import com.example.airequirementworkbench.ai.config.AiConfigDtos.AiConfigStatusDto;
import com.example.airequirementworkbench.ai.config.AiConfigDtos.ModelConfigDto;
import com.example.airequirementworkbench.ai.config.AiConfigDtos.PromptTemplateDto;
import com.example.airequirementworkbench.ai.config.AiConfigDtos.SaveAbilityConfigRequest;
import com.example.airequirementworkbench.ai.config.AiConfigDtos.SaveModelConfigRequest;
import com.example.airequirementworkbench.ai.config.AiConfigDtos.SavePromptTemplateRequest;
import com.example.airequirementworkbench.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-config")
public class AiConfigController {
  private final AiConfigService aiConfigService;

  public AiConfigController(AiConfigService aiConfigService) {
    this.aiConfigService = aiConfigService;
  }

  @GetMapping("/models")
  public ApiResponse<List<ModelConfigDto>> models() {
    return ApiResponse.ok(aiConfigService.listModels());
  }

  @GetMapping("/status")
  public ApiResponse<AiConfigStatusDto> status() {
    return ApiResponse.ok(aiConfigService.getStatus());
  }

  @PostMapping("/models")
  public ApiResponse<ModelConfigDto> saveModel(@Valid @RequestBody SaveModelConfigRequest request) {
    return ApiResponse.ok(aiConfigService.saveModel(request));
  }

  @GetMapping("/prompts")
  public ApiResponse<List<PromptTemplateDto>> prompts() {
    return ApiResponse.ok(aiConfigService.listPrompts());
  }

  @PostMapping("/prompts")
  public ApiResponse<PromptTemplateDto> savePrompt(@Valid @RequestBody SavePromptTemplateRequest request) {
    return ApiResponse.ok(aiConfigService.savePrompt(request));
  }

  @GetMapping("/abilities")
  public ApiResponse<List<AbilityConfigDto>> abilities() {
    return ApiResponse.ok(aiConfigService.listAbilities());
  }

  @PostMapping("/abilities")
  public ApiResponse<AbilityConfigDto> saveAbility(@Valid @RequestBody SaveAbilityConfigRequest request) {
    return ApiResponse.ok(aiConfigService.saveAbility(request));
  }
}
