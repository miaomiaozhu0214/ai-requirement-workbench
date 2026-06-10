package com.example.airequirementworkbench.ai.client;

public class AiClientException extends RuntimeException {
  private final String code;
  private final Object input;
  private final AiCallMetadata metadata;

  public AiClientException(String code, String message, Object input, AiCallMetadata metadata) {
    super(message);
    this.code = code;
    this.input = input;
    this.metadata = metadata;
  }

  public AiClientException(String code, String message, Object input, AiCallMetadata metadata, Throwable cause) {
    super(message, cause);
    this.code = code;
    this.input = input;
    this.metadata = metadata;
  }

  public String getCode() {
    return code;
  }

  public Object getInput() {
    return input;
  }

  public AiCallMetadata getMetadata() {
    return metadata;
  }
}
