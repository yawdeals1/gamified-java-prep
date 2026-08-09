package com.gamifiedjava.model;

import java.time.LocalDateTime;

public class ChallengeSubmission {

    private Integer id;

    private CourseModule module;

    private String sourceCode;

    private String compileOutput;

    private Boolean compileSuccess = false;

    private String aiFeedback;

    private Integer aiScore;

    private Boolean passed = false;

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
