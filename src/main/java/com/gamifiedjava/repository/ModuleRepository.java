package com.gamifiedjava.repository;

import com.gamifiedjava.model.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<CourseModule, Integer> {
    Optional<CourseModule> findBySlug(String slug);
    List<CourseModule> findAllByOrderByOrderIndexAsc();
}
