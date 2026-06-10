package com.example.airequirementworkbench.conversation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_session")
public class ConversationSession {
  @Id
  private Long id;
  private String title;
  private String status = "active";
  private String currentStage = "empty";
  private String summary;
  private LocalDateTime lastMessageAt;
  private Integer candidateCount = 0;
  private Integer requirementCount = 0;
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
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getCurrentStage() { return currentStage; }
  public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
  public String getSummary() { return summary; }
  public void setSummary(String summary) { this.summary = summary; }
  public LocalDateTime getLastMessageAt() { return lastMessageAt; }
  public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
  public Integer getCandidateCount() { return candidateCount; }
  public void setCandidateCount(Integer candidateCount) { this.candidateCount = candidateCount; }
  public Integer getRequirementCount() { return requirementCount; }
  public void setRequirementCount(Integer requirementCount) { this.requirementCount = requirementCount; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public Boolean getDeleted() { return deleted; }
  public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
