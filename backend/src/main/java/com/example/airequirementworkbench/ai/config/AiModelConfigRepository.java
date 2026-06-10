package com.example.airequirementworkbench.ai.config;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiModelConfigRepository extends JpaRepository<AiModelConfig, Long> {
  List<AiModelConfig> findByDeletedFalseOrderByUpdatedAtDesc();

  Optional<AiModelConfig> findFirstByProviderAndStatusAndDeletedFalseOrderByIsDefaultDescUpdatedAtDesc(String provider, String status);
}
