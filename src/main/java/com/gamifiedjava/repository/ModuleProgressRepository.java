package com.gamifiedjava.repository;

import com.gamifiedjava.model.ModuleProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleProgressRepository extends JpaRepository<ModuleProgress, Integer> {
    Optional<ModuleProgress> findByModuleId(Integer moduleId);
    List<ModuleProgress> findAllByOrderByIdAsc();
}
