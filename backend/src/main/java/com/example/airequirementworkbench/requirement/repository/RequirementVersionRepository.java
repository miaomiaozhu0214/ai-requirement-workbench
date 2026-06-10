package com.example.airequirementworkbench.requirement.repository;

import com.example.airequirementworkbench.requirement.entity.RequirementVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementVersionRepository extends JpaRepository<RequirementVersion, Long> {
}
