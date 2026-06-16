package com.example.airequirementworkbench.feature.controller;

import com.example.airequirementworkbench.common.response.ApiResponse;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.CreateNodeRequest;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.FeatureContentBlockDto;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.FeatureHistoryDto;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.FeatureNodeDto;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.MoveNodeRequest;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.SaveContentBlockRequest;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.UpdateNodeRequest;
import com.example.airequirementworkbench.feature.service.FeatureLibraryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/feature-library")
public class FeatureLibraryController {
  private final FeatureLibraryService featureLibraryService;

  public FeatureLibraryController(FeatureLibraryService featureLibraryService) {
    this.featureLibraryService = featureLibraryService;
  }

  @GetMapping("/tree")
  public ApiResponse<List<FeatureNodeDto>> tree(
      @RequestParam @NotNull(message = "产品线不能为空") Long productLineId,
      @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(featureLibraryService.tree(productLineId, keyword));
  }

  @PostMapping("/nodes")
  public ApiResponse<FeatureNodeDto> createNode(@Valid @RequestBody CreateNodeRequest request) {
    return ApiResponse.ok(featureLibraryService.createNode(request));
  }

  @PutMapping("/nodes/{id}")
  public ApiResponse<FeatureNodeDto> updateNode(@PathVariable Long id, @Valid @RequestBody UpdateNodeRequest request) {
    return ApiResponse.ok(featureLibraryService.updateNode(id, request));
  }

  @PutMapping("/nodes/{id}/move")
  public ApiResponse<FeatureNodeDto> moveNode(@PathVariable Long id, @Valid @RequestBody MoveNodeRequest request) {
    return ApiResponse.ok(featureLibraryService.moveNode(id, request));
  }

  @DeleteMapping("/nodes/{id}")
  public ApiResponse<Void> deleteNode(@PathVariable Long id) {
    featureLibraryService.deleteNode(id);
    return ApiResponse.ok(null);
  }

  @GetMapping("/nodes/{id}/history")
  public ApiResponse<List<FeatureHistoryDto>> history(@PathVariable Long id) {
    return ApiResponse.ok(featureLibraryService.history(id));
  }

  @PostMapping("/nodes/{id}/content-blocks")
  public ApiResponse<FeatureContentBlockDto> createContentBlock(
      @PathVariable Long id,
      @Valid @RequestBody SaveContentBlockRequest request) {
    return ApiResponse.ok(featureLibraryService.createContentBlock(id, request));
  }

  @PutMapping("/content-blocks/{blockId}")
  public ApiResponse<FeatureContentBlockDto> updateContentBlock(
      @PathVariable Long blockId,
      @Valid @RequestBody SaveContentBlockRequest request) {
    return ApiResponse.ok(featureLibraryService.updateContentBlock(blockId, request));
  }

  @DeleteMapping("/content-blocks/{blockId}")
  public ApiResponse<Void> deleteContentBlock(@PathVariable Long blockId) {
    featureLibraryService.deleteContentBlock(blockId);
    return ApiResponse.ok(null);
  }
}
