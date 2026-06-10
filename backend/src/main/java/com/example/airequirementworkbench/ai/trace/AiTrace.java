package com.example.airequirementworkbench.ai.trace;

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
@Table(name = "ai_trace")
public class AiTrace {
  @Id
  private Long id;
  private String traceNo;
  private Long sessionId;
  private String businessObjectType;
  private Long businessObjectId;
  private String abilityType;
  private Long modelConfigId;
  private String modelName;
  private Long promptTemplateId;
  private String promptVersion;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> inputJson;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> outputJson;
  private String outputText;
  private Integer tokenInput = 0;
  private Integer tokenOutput = 0;
  private Integer durationMs = 0;
  private String status = "created";
  private String errorCode;
  private String errorMessage;
  private Long createdBy;
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getTraceNo() { return traceNo; }
  public void setTraceNo(String traceNo) { this.traceNo = traceNo; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
  public String getBusinessObjectType() { return businessObjectType; }
  public void setBusinessObjectType(String businessObjectType) { this.businessObjectType = businessObjectType; }
  public Long getBusinessObjectId() { return businessObjectId; }
  public void setBusinessObjectId(Long businessObjectId) { this.businessObjectId = businessObjectId; }
  public String getAbilityType() { return abilityType; }
  public void setAbilityType(String abilityType) { this.abilityType = abilityType; }
  public Long getModelConfigId() { return modelConfigId; }
  public void setModelConfigId(Long modelConfigId) { this.modelConfigId = modelConfigId; }
  public String getModelName() { return modelName; }
  public void setModelName(String modelName) { this.modelName = modelName; }
  public Long getPromptTemplateId() { return promptTemplateId; }
  public void setPromptTemplateId(Long promptTemplateId) { this.promptTemplateId = promptTemplateId; }
  public String getPromptVersion() { return promptVersion; }
  public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
  public Map<String, Object> getInputJson() { return inputJson; }
  public void setInputJson(Map<String, Object> inputJson) { this.inputJson = inputJson; }
  public Map<String, Object> getOutputJson() { return outputJson; }
  public void setOutputJson(Map<String, Object> outputJson) { this.outputJson = outputJson; }
  public String getOutputText() { return outputText; }
  public void setOutputText(String outputText) { this.outputText = outputText; }
  public Integer getTokenInput() { return tokenInput; }
  public void setTokenInput(Integer tokenInput) { this.tokenInput = tokenInput; }
  public Integer getTokenOutput() { return tokenOutput; }
  public void setTokenOutput(Integer tokenOutput) { this.tokenOutput = tokenOutput; }
  public Integer getDurationMs() { return durationMs; }
  public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getErrorCode() { return errorCode; }
  public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
