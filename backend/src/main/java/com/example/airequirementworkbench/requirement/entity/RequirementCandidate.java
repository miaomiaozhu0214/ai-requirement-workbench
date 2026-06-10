package com.example.airequirementworkbench.requirement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "requirement_candidate")
public class RequirementCandidate {
  @Id
  private Long id;
  // 候选需求必须归属于一个会话，多轮补充和 Trace 排查都依赖这个绑定关系。
  private Long sessionId;
  private String title;
  private String status = "draft";
  // 结构化需求内容使用 jsonb 保存，便于 AI patch 增量合并不同业务字段。
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> contentJson = Map.of();
  private BigDecimal completenessScore = BigDecimal.ZERO;
  // 完整度检查输出的缺失项、风险项、建议问题直接支撑右侧候选卡片展示。
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> missingItemsJson;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> riskyItemsJson;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> suggestedQuestionsJson;
  private BigDecimal confidence;
  private Long createdFromMessageId;
  private Long convertedRequirementId;
  private Long createdBy;
  private LocalDateTime createdAt;
  private Long updatedBy;
  private LocalDateTime updatedAt;
  private Boolean deleted = false;

  @PrePersist
  public void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Map<String, Object> getContentJson() { return contentJson; }
  public void setContentJson(Map<String, Object> contentJson) { this.contentJson = contentJson; }
  public BigDecimal getCompletenessScore() { return completenessScore; }
  public void setCompletenessScore(BigDecimal completenessScore) { this.completenessScore = completenessScore; }
  public List<String> getMissingItemsJson() { return missingItemsJson; }
  public void setMissingItemsJson(List<String> missingItemsJson) { this.missingItemsJson = missingItemsJson; }
  public List<String> getRiskyItemsJson() { return riskyItemsJson; }
  public void setRiskyItemsJson(List<String> riskyItemsJson) { this.riskyItemsJson = riskyItemsJson; }
  public List<String> getSuggestedQuestionsJson() { return suggestedQuestionsJson; }
  public void setSuggestedQuestionsJson(List<String> suggestedQuestionsJson) { this.suggestedQuestionsJson = suggestedQuestionsJson; }
  public BigDecimal getConfidence() { return confidence; }
  public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
  public Long getCreatedFromMessageId() { return createdFromMessageId; }
  public void setCreatedFromMessageId(Long createdFromMessageId) { this.createdFromMessageId = createdFromMessageId; }
  public Long getConvertedRequirementId() { return convertedRequirementId; }
  public void setConvertedRequirementId(Long convertedRequirementId) { this.convertedRequirementId = convertedRequirementId; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public Boolean getDeleted() { return deleted; }
  public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
