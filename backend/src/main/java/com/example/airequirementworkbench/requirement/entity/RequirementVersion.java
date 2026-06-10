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
@Table(name = "requirement_version")
public class RequirementVersion {
  @Id
  private Long id;
  private Long requirementId;
  private Integer versionNo;
  private String title;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> contentJson;
  private String changeSummary;
  private Long createdBy;
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getRequirementId() { return requirementId; }
  public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
  public Integer getVersionNo() { return versionNo; }
  public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Map<String, Object> getContentJson() { return contentJson; }
  public void setContentJson(Map<String, Object> contentJson) { this.contentJson = contentJson; }
  public String getChangeSummary() { return changeSummary; }
  public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
