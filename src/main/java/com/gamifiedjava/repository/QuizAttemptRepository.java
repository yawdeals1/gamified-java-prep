package com.gamifiedjava.repository;

import com.gamifiedjava.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Integer> {
    List<QuizAttempt> findByModuleIdOrderByAttemptedAtDesc(Integer moduleId);
    long countByModuleIdAndCorrectTrue(Integer moduleId);
    long countByModuleId(Integer moduleId);
}
