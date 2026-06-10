package com.example.airequirementworkbench.ai.trace;

import com.example.airequirementworkbench.common.response.ApiResponse;
import com.example.airequirementworkbench.ai.trace.AiTraceDtos.AiTraceDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/traces")
public class AiTraceController {
  private final AiTraceService aiTraceService;

  public AiTraceController(AiTraceService aiTraceService) {
    this.aiTraceService = aiTraceService;
  }

  @GetMapping
  public ApiResponse<List<AiTraceDto>> list(@RequestParam(required = false) Long sessionId) {
    return ApiResponse.ok(aiTraceService.listLatest(sessionId));
  }

  @GetMapping("/{id}")
  public ApiResponse<AiTraceDto> get(@PathVariable Long id) {
    return ApiResponse.ok(aiTraceService.get(id));
  }
}
