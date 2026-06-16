package com.example.airequirementworkbench.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "product_line")
public class ProductLine {
  @Id
  private Long id;
  private String lineCode;
  private String lineName;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> owners = new ArrayList<>();
  private String productType = "face_to_customer";
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> platforms = new ArrayList<>();
  private String description;
  private Integer version = 0;
  private Boolean isProcessing = false;
  private String status = "enabled";
  private Long createdBy;
  private LocalDateTime createdAt;
  private Long updatedBy;
  private LocalDateTime updatedAt;
  private Boolean deleted = false;

  @PrePersist
  void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = createdAt == null ? now : createdAt;
    updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getLineCode() { return lineCode; }
  public void setLineCode(String lineCode) { this.lineCode = lineCode; }
  public String getLineName() { return lineName; }
  public void setLineName(String lineName) { this.lineName = lineName; }
  public List<String> getOwners() { return owners; }
  public void setOwners(List<String> owners) { this.owners = owners; }
  public String getProductType() { return productType; }
  public void setProductType(String productType) { this.productType = productType; }
  public List<String> getPlatforms() { return platforms; }
  public void setPlatforms(List<String> platforms) { this.platforms = platforms; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
  public Boolean getIsProcessing() { return isProcessing; }
  public void setIsProcessing(Boolean processing) { isProcessing = processing; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  public Boolean getDeleted() { return deleted; }
  public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
