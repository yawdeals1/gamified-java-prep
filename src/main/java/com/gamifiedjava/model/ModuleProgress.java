package com.gamifiedjava.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "module_progress")
public class ModuleProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private CourseModule module;

    @Column(length = 20)
    private String status = "locked";

    @Column(name = "quiz_score")
    private Integer quizScore = 0;

    @Column(name = "quiz_attempts")
    private Integer quizAttempts = 0;

    @Column(name = "challenge_passed")
    private Boolean challengePassed = false;

    @Column(name = "challenge_attempts")
    private Integer challengeAttempts = 0;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ModuleProgress() {}

    public ModuleProgress(CourseModule CourseModule) {
        this.module = CourseModule;
        this.status = CourseModule.getOrderIndex() == 1 ? "available" : "locked";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public CourseModule getModule() { return module; }
    public void setModule(CourseModule module) { this.module = module; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getQuizScore() { return quizScore; }
    public void setQuizScore(Integer quizScore) { this.quizScore = quizScore; }
    public Integer getQuizAttempts() { return quizAttempts; }
    public void setQuizAttempts(Integer quizAttempts) { this.quizAttempts = quizAttempts; }
    public Boolean getChallengePassed() { return challengePassed; }
    public void setChallengePassed(Boolean challengePassed) { this.challengePassed = challengePassed; }
    public Integer getChallengeAttempts() { return challengeAttempts; }
    public void setChallengeAttempts(Integer challengeAttempts) { this.challengeAttempts = challengeAttempts; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isComplete() {
        return "completed".equals(status);
    }

    public boolean isUnlocked() {
        return !"locked".equals(status);
    }

    public boolean isQuizPassed() {
        return quizScore != null && quizScore >= 80;
    }
}
