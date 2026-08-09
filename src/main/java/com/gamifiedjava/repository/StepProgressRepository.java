package com.gamifiedjava.repository;

import com.gamifiedjava.model.StepProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StepProgressRepository extends JpaRepository<StepProgress, Integer> {
    Optional<StepProgress> findByStepId(Integer stepId);
    List<StepProgress> findByModuleId(Integer moduleId);
}
