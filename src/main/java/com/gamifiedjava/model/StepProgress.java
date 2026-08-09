package com.gamifiedjava.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Records that a learner has completed a given lesson step, so XP is awarded once.
 */
@Entity
@Table(name = "step_progress", uniqueConstraints = @UniqueConstraint(columnNames = "step_id"))
public class StepProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "step_id", nullable = false)
    private Integer stepId;

    @Column(name = "module_id", nullable = false)
    private Integer moduleId;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public StepProgress() {}

    public StepProgress(Integer stepId, Integer moduleId) {
        this.stepId = stepId;
        this.moduleId = moduleId;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getStepId() { return stepId; }
    public void setStepId(Integer stepId) { this.stepId = stepId; }
    public Integer getModuleId() { return moduleId; }
    public void setModuleId(Integer moduleId) { this.moduleId = moduleId; }
    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public boolean isDone() { return completedAt != null; }
}
