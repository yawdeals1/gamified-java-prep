package com.gamifiedjava.repository;

import com.gamifiedjava.model.ChallengeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeSubmissionRepository extends JpaRepository<ChallengeSubmission, Integer> {
    List<ChallengeSubmission> findByModuleIdOrderBySubmittedAtDesc(Integer moduleId);
}
