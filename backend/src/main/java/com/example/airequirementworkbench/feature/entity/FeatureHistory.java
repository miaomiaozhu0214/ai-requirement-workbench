package com.example.airequirementworkbench.feature.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "feature_history")
public class FeatureHistory {
  @Id
  private Long id;
  private Long featureId;
  private String operationType;
  private String description;
  private Long operatorId;
  private LocalDateTime createdAt;

  @PrePersist
  void prePersist() {
    createdAt = LocalDateTime.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getFeatureId() { return featureId; }
  public void setFeatureId(Long featureId) { this.featureId = featureId; }
  public String getOperationType() { return operationType; }
  public void setOperationType(String operationType) { this.operationType = operationType; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Long getOperatorId() { return operatorId; }
  public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
