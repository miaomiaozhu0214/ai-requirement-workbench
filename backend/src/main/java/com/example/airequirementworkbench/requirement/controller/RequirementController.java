package com.example.airequirementworkbench.requirement.controller;

import com.example.airequirementworkbench.common.response.ApiResponse;
import com.example.airequirementworkbench.requirement.dto.RequirementDtos.RequirementDto;
import com.example.airequirementworkbench.requirement.service.RequirementService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {
  private final RequirementService requirementService;

  public RequirementController(RequirementService requirementService) {
    this.requirementService = requirementService;
  }

  @GetMapping
  public ApiResponse<List<RequirementDto>> list() {
    return ApiResponse.ok(requirementService.listRequirements());
  }

  @GetMapping("/{id}")
  public ApiResponse<RequirementDto> detail(@PathVariable Long id) {
    return ApiResponse.ok(requirementService.getRequirement(id));
  }
}
