package com.example.airequirementworkbench.feature.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FeatureLibraryDtos {
  private FeatureLibraryDtos() {}

  public record FeatureNodeDto(
      Long id,
      Long productLineId,
      Long parentId,
      String name,
      String description,
      String nodeType,
      String status,
      Integer sortOrder,
      List<FeatureNodeDto> children,
      List<FeatureContentBlockDto> contentBlocks) {
    public FeatureNodeDto withChildrenAndBlocks(List<FeatureNodeDto> children, List<FeatureContentBlockDto> contentBlocks) {
      return new FeatureNodeDto(id, productLineId, parentId, name, description, nodeType, status, sortOrder, children, contentBlocks);
    }
  }

  public record FeatureContentBlockDto(
      Long id,
      Long featureId,
      String blockType,
      String title,
      String content,
      Map<String, Object> metadata,
      String sourceRef,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}

  public record FeatureHistoryDto(
      Long id,
      Long featureId,
      String operationType,
      String description,
      Long operatorId,
      LocalDateTime createdAt) {}

  public record CreateNodeRequest(
      @NotNull(message = "产品线不能为空") Long productLineId,
      Long parentId,
      @NotBlank(message = "名称不能为空") @Size(max = 200, message = "名称不能超过200字符") String name,
      @Size(max = 2000, message = "描述不能超过2000字符") String description,
      @NotBlank(message = "节点类型不能为空") String nodeType) {}

  public static class UpdateNodeRequest {
    @Size(max = 200, message = "名称不能超过200字符")
    private String name;
    @Size(max = 2000, message = "描述不能超过2000字符")
    private String description;
    private String nodeType;
    private Long parentId;
    private boolean parentIdProvided;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    public Long getParentId() { return parentId; }
    public boolean isParentIdProvided() { return parentIdProvided; }

    @JsonSetter("parentId")
    public void setParentId(Long parentId) {
      this.parentId = parentId;
      this.parentIdProvided = true;
    }
  }

  public record MoveNodeRequest(
      Long parentId,
      @NotNull(message = "排序位置不能为空") @Min(value = 0, message = "排序位置不能小于0") Integer index) {}

  public record SaveContentBlockRequest(
      @NotBlank(message = "内容块类型不能为空") String blockType,
      @Size(max = 200, message = "标题不能超过200字符") String title,
      @NotBlank(message = "内容不能为空") String content,
      Map<String, Object> metadata,
      @Size(max = 200, message = "来源引用不能超过200字符") String sourceRef,
      @Min(value = 0, message = "排序不能小于0") @Max(value = 9999, message = "排序不能超过9999") Integer sortOrder) {}

  public static List<FeatureNodeDto> emptyChildren() {
    return new ArrayList<>();
  }
}
