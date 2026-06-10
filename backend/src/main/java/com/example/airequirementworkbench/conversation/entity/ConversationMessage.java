package com.example.airequirementworkbench.conversation.entity;

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
@Table(name = "conversation_message")
public class ConversationMessage {
  @Id
  private Long id;
  private Long sessionId;
  private String role;
  private String messageType = "text";
  private String content;
  private String commandName;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> metadataJson;
  private Long createdBy;
  private LocalDateTime createdAt;
  private Boolean deleted = false;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public String getMessageType() { return messageType; }
  public void setMessageType(String messageType) { this.messageType = messageType; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getCommandName() { return commandName; }
  public void setCommandName(String commandName) { this.commandName = commandName; }
  public Map<String, Object> getMetadataJson() { return metadataJson; }
  public void setMetadataJson(Map<String, Object> metadataJson) { this.metadataJson = metadataJson; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public Boolean getDeleted() { return deleted; }
  public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
