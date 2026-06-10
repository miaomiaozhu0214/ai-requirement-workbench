package com.example.airequirementworkbench.masterdata.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "sys_user")
public class SysUser {
  @Id
  private Long id;
  private String username;
  private String displayName;
  private String email;
  private String roleCode;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public String getUsername() { return username; }
  public String getDisplayName() { return displayName; }
  public String getEmail() { return email; }
  public String getRoleCode() { return roleCode; }
  public String getStatus() { return status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
}
