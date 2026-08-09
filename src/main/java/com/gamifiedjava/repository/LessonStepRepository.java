package com.gamifiedjava.repository;

import com.gamifiedjava.model.LessonStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonStepRepository extends JpaRepository<LessonStep, Integer> {
    List<LessonStep> findByModuleIdOrderByOrderIndexAsc(Integer moduleId);
    long countByModuleId(Integer moduleId);
}
