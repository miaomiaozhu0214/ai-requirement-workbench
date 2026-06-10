package com.example.airequirementworkbench.requirement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "requirement_candidate_patch")
public class RequirementCandidatePatch {
  @Id
  private Long id;
  private Long candidateId;
  private Long sessionId;
  private Long sourceMessageId;
  private String patchType = "update";
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> patchJson;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> beforeJson;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> afterJson;
  private Long aiTraceId;
  private Long createdBy;
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getCandidateId() { return candidateId; }
  public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
  public Long getSourceMessageId() { return sourceMessageId; }
  public void setSourceMessageId(Long sourceMessageId) { this.sourceMessageId = sourceMessageId; }
  public String getPatchType() { return patchType; }
  public void setPatchType(String patchType) { this.patchType = patchType; }
  public Map<String, Object> getPatchJson() { return patchJson; }
  public void setPatchJson(Map<String, Object> patchJson) { this.patchJson = patchJson; }
  public Map<String, Object> getBeforeJson() { return beforeJson; }
  public void setBeforeJson(Map<String, Object> beforeJson) { this.beforeJson = beforeJson; }
  public Map<String, Object> getAfterJson() { return afterJson; }
  public void setAfterJson(Map<String, Object> afterJson) { this.afterJson = afterJson; }
  public Long getAiTraceId() { return aiTraceId; }
  public void setAiTraceId(Long aiTraceId) { this.aiTraceId = aiTraceId; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
