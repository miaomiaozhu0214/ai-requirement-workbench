package com.example.airequirementworkbench.common;

import com.example.airequirementworkbench.common.response.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemController {
  @GetMapping("/")
  public ApiResponse<Map<String, String>> root() {
    return ApiResponse.ok(Map.of(
        "service", "AI需求工作台后端服务",
        "frontend", "请访问 http://127.0.0.1:5174/",
        "health", "/api/health"
    ));
  }

  @GetMapping("/api/health")
  public ApiResponse<Map<String, String>> health() {
    return ApiResponse.ok(Map.of("status", "UP"));
  }
}
