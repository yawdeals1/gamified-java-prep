package com.gamifiedjava.repository;

import com.gamifiedjava.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Integer> {
    Optional<Achievement> findByName(String name);
    List<Achievement> findAllByOrderByUnlockedAtAsc();
    long countByUnlockedAtIsNotNull();
}
