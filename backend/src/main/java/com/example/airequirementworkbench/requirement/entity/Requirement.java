package com.example.airequirementworkbench.requirement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "requirement")
public class Requirement {
  @Id
  private Long id;
  private String requirementNo;
  private Long sourceSessionId;
  private Long sourceCandidateId;
  private String title;
  private Long productLineId;
  private Long moduleId;
  private String requirementType;
  private String priority = "medium";
  private String status = "confirmed";
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> contentJson;
  private BigDecimal completenessScore = BigDecimal.ZERO;
  private Integer currentVersion = 1;
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
  public String getRequirementNo() { return requirementNo; }
  public void setRequirementNo(String requirementNo) { this.requirementNo = requirementNo; }
  public Long getSourceSessionId() { return sourceSessionId; }
  public void setSourceSessionId(Long sourceSessionId) { this.sourceSessionId = sourceSessionId; }
  public Long getSourceCandidateId() { return sourceCandidateId; }
  public void setSourceCandidateId(Long sourceCandidateId) { this.sourceCandidateId = sourceCandidateId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Long getProductLineId() { return productLineId; }
  public void setProductLineId(Long productLineId) { this.productLineId = productLineId; }
  public Long getModuleId() { return moduleId; }
  public void setModuleId(Long moduleId) { this.moduleId = moduleId; }
  public String getRequirementType() { return requirementType; }
  public void setRequirementType(String requirementType) { this.requirementType = requirementType; }
  public String getPriority() { return priority; }
  public void setPriority(String priority) { this.priority = priority; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Map<String, Object> getContentJson() { return contentJson; }
  public void setContentJson(Map<String, Object> contentJson) { this.contentJson = contentJson; }
  public BigDecimal getCompletenessScore() { return completenessScore; }
  public void setCompletenessScore(BigDecimal completenessScore) { this.completenessScore = completenessScore; }
  public Integer getCurrentVersion() { return currentVersion; }
  public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public Boolean getDeleted() { return deleted; }
  public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
