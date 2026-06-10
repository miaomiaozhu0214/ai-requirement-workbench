package com.example.airequirementworkbench.masterdata.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_module")
public class ProductModule {
  @Id
  private Long id;
  private Long productLineId;
  private String moduleCode;
  private String moduleName;
  private Long parentId;
  private String description;
  private String status = "enabled";
  private Long createdBy;
  private LocalDateTime createdAt;
  private Long updatedBy;
  private LocalDateTime updatedAt;
  private Boolean deleted = false;

  public Long getId() { return id; }
  public Long getProductLineId() { return productLineId; }
  public String getModuleCode() { return moduleCode; }
  public String getModuleName() { return moduleName; }
  public Long getParentId() { return parentId; }
  public String getDescription() { return description; }
  public String getStatus() { return status; }
  public Long getCreatedBy() { return createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public Long getUpdatedBy() { return updatedBy; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public Boolean getDeleted() { return deleted; }
}
