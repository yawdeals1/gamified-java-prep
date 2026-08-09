package com.gamifiedjava.repository;

import com.gamifiedjava.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Integer> {
    List<QuizQuestion> findByModuleId(Integer moduleId);
    long countByModuleId(Integer moduleId);
}
