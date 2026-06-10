package com.example.airequirementworkbench.requirement.controller;

import com.example.airequirementworkbench.common.response.ApiResponse;
import com.example.airequirementworkbench.requirement.dto.CandidateDto;
import com.example.airequirementworkbench.requirement.dto.RequirementDtos.GenerateCardRequest;
import com.example.airequirementworkbench.requirement.dto.RequirementDtos.RequirementDto;
import com.example.airequirementworkbench.requirement.service.RequirementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class RequirementCandidateController {
  private final RequirementService requirementService;

  public RequirementCandidateController(RequirementService requirementService) {
    this.requirementService = requirementService;
  }

  @GetMapping("/api/conversations/{id}/candidates")
  public ApiResponse<List<CandidateDto>> candidates(@PathVariable Long id) {
    return ApiResponse.ok(requirementService.listCandidates(id));
  }

  @PostMapping("/api/candidates/{id}/close")
  public ApiResponse<CandidateDto> close(@PathVariable Long id) {
    return ApiResponse.ok(requirementService.closeCandidate(id));
  }

  @PostMapping("/api/candidates/{id}/generate-card")
  public ApiResponse<RequirementDto> generateCard(@PathVariable Long id, @Valid @RequestBody GenerateCardRequest request) {
    return ApiResponse.ok(requirementService.generateCard(id, request));
  }
}
