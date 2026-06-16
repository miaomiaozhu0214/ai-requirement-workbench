package com.example.airequirementworkbench.ai.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AiOrchestratorExecutionPlanTests {

  @Test
  void shouldUseConfiguredOrderBeforeRouterOrder() {
    List<String> plan = AiOrchestrator.resolveExecutionPlan(
        List.of("generate_reply", "extract_requirement", "check_completeness"),
        Map.of(
            "reply_generate", 100,
            "requirement_extract", 20,
            "completeness_check", 30
        )
    );

    assertThat(plan).containsExactly("extract_requirement", "check_completeness", "generate_reply");
  }

  @Test
  void shouldKeepRouterOrderWhenAllOrdersAreSame() {
    List<String> plan = AiOrchestrator.resolveExecutionPlan(
        List.of("generate_reply", "extract_requirement", "check_completeness"),
        Map.of(
            "reply_generate", 100,
            "requirement_extract", 100,
            "completeness_check", 100
        )
    );

    assertThat(plan).containsExactly("generate_reply", "extract_requirement", "check_completeness");
  }

  @Test
  void shouldKeepRouterOrderForActionsWithSameConfiguredOrder() {
    List<String> plan = AiOrchestrator.resolveExecutionPlan(
        List.of("extract_requirement", "split_requirement", "generate_reply"),
        Map.of(
            "requirement_extract", 20,
            "requirement_split", 20,
            "reply_generate", 100
        )
    );

    assertThat(plan).containsExactly("extract_requirement", "split_requirement", "generate_reply");
  }

  @Test
  void shouldReadAliasActionsFromCanonicalAbilityOrder() {
    List<String> plan = AiOrchestrator.resolveExecutionPlan(
        List.of("clarify_short_reply", "extract_requirement_info"),
        Map.of(
            "requirement_extract", 20,
            "reply_generate", 100
        )
    );

    assertThat(plan).containsExactly("extract_requirement_info", "clarify_short_reply");
  }

  @Test
  void shouldKeepUnknownActionsWithDefaultOrderWithoutBreakingKnownActions() {
    List<String> plan = AiOrchestrator.resolveExecutionPlan(
        List.of("extract_requirement", "unknown_action", "generate_reply"),
        Map.of(
            "requirement_extract", 20,
            "reply_generate", 100
        )
    );

    assertThat(plan).containsExactly("extract_requirement", "unknown_action", "generate_reply");
  }

  @Test
  void shouldFilterWriteActionsBeforeSortingForShortInput() {
    List<String> filtered = AiOrchestrator.filterWriteActionsForStructurallyUnclearInput(
        List.of("extract_requirement", "check_completeness", "generate_reply")
    );
    List<String> plan = AiOrchestrator.resolveExecutionPlan(
        filtered,
        Map.of(
            "requirement_extract", 20,
            "completeness_check", 30,
            "reply_generate", 100
        )
    );

    assertThat(plan).containsExactly("generate_reply");
  }

  @Test
  void shouldAllowExtractionBeforeCompletenessWhenRouterReturnsDependentActionsInWrongOrder() {
    List<String> plan = AiOrchestrator.resolveExecutionPlan(
        List.of("check_completeness", "extract_requirement", "generate_reply"),
        Map.of(
            "requirement_extract", 20,
            "completeness_check", 30,
            "reply_generate", 100
        )
    );

    assertThat(plan).containsExactly("extract_requirement", "check_completeness", "generate_reply");
  }
}
