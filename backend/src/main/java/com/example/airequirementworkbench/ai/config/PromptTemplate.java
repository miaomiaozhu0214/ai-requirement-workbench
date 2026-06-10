package com.example.airequirementworkbench.ai.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "prompt_template")
public class PromptTemplate {
  @Id
  private Long id;
  private String abilityType;
  @Column(length = 100)
  private String templateCode;
  private String templateName;
  private String version;
  @Column(columnDefinition = "text")
  private String systemPrompt;
  @Column(columnDefinition = "text")
  private String userPrompt;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> jsonSchema;
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
  public String getAbilityType() { return abilityType; }
  public void setAbilityType(String abilityType) { this.abilityType = abilityType; }
  public String getTemplateCode() { return templateCode; }
  public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
  public String getTemplateName() { return templateName; }
  public void setTemplateName(String templateName) { this.templateName = templateName; }
  public String getVersion() { return version; }
  public void setVersion(String version) { this.version = version; }
  public String getSystemPrompt() { return systemPrompt; }
  public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
  public String getUserPrompt() { return userPrompt; }
  public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }
  public Map<String, Object> getJsonSchema() { return jsonSchema; }
  public void setJsonSchema(Map<String, Object> jsonSchema) { this.jsonSchema = jsonSchema; }
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
