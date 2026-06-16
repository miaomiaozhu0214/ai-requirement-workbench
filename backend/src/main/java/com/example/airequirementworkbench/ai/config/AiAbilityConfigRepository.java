package com.example.airequirementworkbench.ai.config;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAbilityConfigRepository extends JpaRepository<AiAbilityConfig, Long> {
  List<AiAbilityConfig> findByDeletedFalseOrderByAbilityTypeAsc();

  Optional<AiAbilityConfig> findByAbilityTypeAndDeletedFalse(String abilityType);

  List<AiAbilityConfig> findByAbilityTypeInAndDeletedFalse(Collection<String> abilityTypes);
}
