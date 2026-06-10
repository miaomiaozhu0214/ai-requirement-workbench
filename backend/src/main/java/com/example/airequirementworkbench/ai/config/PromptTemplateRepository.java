package com.example.airequirementworkbench.ai.config;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
  List<PromptTemplate> findByDeletedFalseOrderByAbilityTypeAscUpdatedAtDesc();

  List<PromptTemplate> findByAbilityTypeAndDeletedFalse(String abilityType);

  List<PromptTemplate> findByAbilityTypeAndDeletedFalseOrderByIsDefaultDescUpdatedAtDesc(String abilityType);

  Optional<PromptTemplate> findFirstByAbilityTypeAndStatusAndDeletedFalseOrderByIsDefaultDescUpdatedAtDesc(String abilityType, String status);
}
