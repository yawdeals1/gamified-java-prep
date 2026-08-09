package com.gamifiedjava.model;

import java.time.LocalDateTime;

public class ModuleProgress {

    private Integer id;

    private CourseModule module;

    private String status = "locked";

    private Integer quizScore = 0;

    private Integer quizAttempts = 0;

    private Boolean challengePassed = false;

    private Integer challengeAttempts = 0;

    private LocalDateTime completedAt;

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
