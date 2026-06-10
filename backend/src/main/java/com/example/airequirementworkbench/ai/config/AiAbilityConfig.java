package com.example.airequirementworkbench.ai.config;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_ability_config")
public class AiAbilityConfig {
  @Id
  private Long id;
  private String abilityType;
  private String abilityName;
  private Boolean enabled = true;
  private Long modelConfigId;
  private Long promptTemplateId;
  private Boolean fallbackToMock = false;
  private String status = "enabled";
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
  public String getAbilityType() { return abilityType; }
  public void setAbilityType(String abilityType) { this.abilityType = abilityType; }
  public String getAbilityName() { return abilityName; }
  public void setAbilityName(String abilityName) { this.abilityName = abilityName; }
  public Boolean getEnabled() { return enabled; }
  public void setEnabled(Boolean enabled) { this.enabled = enabled; }
  public Long getModelConfigId() { return modelConfigId; }
  public void setModelConfigId(Long modelConfigId) { this.modelConfigId = modelConfigId; }
  public Long getPromptTemplateId() { return promptTemplateId; }
  public void setPromptTemplateId(Long promptTemplateId) { this.promptTemplateId = promptTemplateId; }
  public Boolean getFallbackToMock() { return fallbackToMock; }
  public void setFallbackToMock(Boolean fallbackToMock) { this.fallbackToMock = fallbackToMock; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public Boolean getDeleted() { return deleted; }
  public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
