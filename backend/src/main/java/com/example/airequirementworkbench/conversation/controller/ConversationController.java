package com.example.airequirementworkbench.conversation.controller;

import com.example.airequirementworkbench.common.response.ApiResponse;
import com.example.airequirementworkbench.conversation.dto.ConversationDtos.ConversationDetailDto;
import com.example.airequirementworkbench.conversation.dto.ConversationDtos.ConversationSummaryDto;
import com.example.airequirementworkbench.conversation.dto.ConversationDtos.CreateConversationRequest;
import com.example.airequirementworkbench.conversation.dto.ConversationDtos.MessageDto;
import com.example.airequirementworkbench.conversation.dto.ConversationDtos.SendMessageRequest;
import com.example.airequirementworkbench.conversation.service.ConversationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
  private final ConversationService conversationService;

  public ConversationController(ConversationService conversationService) {
    this.conversationService = conversationService;
  }

  @GetMapping
  public ApiResponse<List<ConversationSummaryDto>> list() {
    return ApiResponse.ok(conversationService.listSessions());
  }

  @PostMapping
  public ApiResponse<ConversationDetailDto> create(@RequestBody(required = false) CreateConversationRequest request) {
    return ApiResponse.ok(conversationService.create(request == null ? new CreateConversationRequest(null) : request));
  }

  @GetMapping("/{id}")
  public ApiResponse<ConversationDetailDto> detail(@PathVariable Long id) {
    return ApiResponse.ok(conversationService.getDetail(id));
  }

  @GetMapping("/{id}/messages")
  public ApiResponse<List<MessageDto>> messages(@PathVariable Long id) {
    return ApiResponse.ok(conversationService.getMessages(id));
  }

  @PostMapping("/{id}/messages")
  public ApiResponse<ConversationDetailDto> sendMessage(@PathVariable Long id, @Valid @RequestBody SendMessageRequest request) {
    return ApiResponse.ok(conversationService.sendMessage(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    conversationService.deleteSession(id);
    return ApiResponse.ok(null);
  }
}
