package com.example.airequirementworkbench.ai.client;

public record AiCallMetadata(
    Long modelConfigId,
    String modelName,
    Long promptTemplateId,
    String promptVersion,
    String renderedPrompt,
    String outputText,
    Integer tokenInput,
    Integer tokenOutput,
    Integer durationMs
) {
  public static AiCallMetadata mock() {
    return new AiCallMetadata(null, "mock-requirement-model", null, "mock-v1.0", null, null, null, null, null);
  }
}
