package com.example.airequirementworkbench.ai.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_model_config")
public class AiModelConfig {
  @Id
  private Long id;
  private String provider = "openai";
  private String modelName;
  private String displayName;
  private String apiBaseUrl = "https://api.openai.com/v1";
  private String apiKeyEnv = "OPENAI_API_KEY";
  @Column(columnDefinition = "text")
  private String apiKeySecret;
  private BigDecimal temperature = new BigDecimal("0.20");
  private Integer maxOutputTokens = 1600;
  private Integer timeoutSeconds = 60;
  private String status = "enabled";
  private Boolean isDefault = false;
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
  public String getProvider() { return provider; }
  public void setProvider(String provider) { this.provider = provider; }
  public String getModelName() { return modelName; }
  public void setModelName(String modelName) { this.modelName = modelName; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public String getApiBaseUrl() { return apiBaseUrl; }
  public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
  public String getApiKeyEnv() { return apiKeyEnv; }
  public void setApiKeyEnv(String apiKeyEnv) { this.apiKeyEnv = apiKeyEnv; }
  public String getApiKeySecret() { return apiKeySecret; }
  public void setApiKeySecret(String apiKeySecret) { this.apiKeySecret = apiKeySecret; }
  public BigDecimal getTemperature() { return temperature; }
  public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
  public Integer getMaxOutputTokens() { return maxOutputTokens; }
  public void setMaxOutputTokens(Integer maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
  public Integer getTimeoutSeconds() { return timeoutSeconds; }
  public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Boolean getIsDefault() { return isDefault; }
  public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public Boolean getDeleted() { return deleted; }
  public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
