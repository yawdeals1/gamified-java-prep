package com.gamifiedjava.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "challenge_submission")
public class ChallengeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private CourseModule module;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Column(columnDefinition = "TEXT")
    private String compileOutput;

    @Column(name = "compile_success")
    private Boolean compileSuccess = false;

    @Column(columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(name = "ai_score")
    private Integer aiScore;

    private Boolean passed = false;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();

    public ChallengeSubmission() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public CourseModule getModule() { return module; }
    public void setModule(CourseModule module) { this.module = module; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public String getCompileOutput() { return compileOutput; }
    public void setCompileOutput(String compileOutput) { this.compileOutput = compileOutput; }
    public Boolean getCompileSuccess() { return compileSuccess; }
    public void setCompileSuccess(Boolean compileSuccess) { this.compileSuccess = compileSuccess; }
    public String getAiFeedback() { return aiFeedback; }
    public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }
    public Integer getAiScore() { return aiScore; }
    public void setAiScore(Integer aiScore) { this.aiScore = aiScore; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
