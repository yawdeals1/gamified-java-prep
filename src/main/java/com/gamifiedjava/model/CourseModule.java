package com.gamifiedjava.model;

import java.time.LocalDateTime;

public class CourseModule {

    private Integer id;

    private String title;

    private String slug;

    private String description;

    private String contentMarkdown;

    private Integer orderIndex;

    private Integer xpReward = 100;

    private String challengeInstructions;

    private String challengeTemplateCode;

    private LocalDateTime createdAt = LocalDateTime.now();

    public CourseModule() {}

    public CourseModule(String title, String slug, String description, Integer orderIndex, Integer xpReward) {
        this.title = title;
        this.slug = slug;
        this.description = description;
        this.orderIndex = orderIndex;
        this.xpReward = xpReward;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContentMarkdown() { return contentMarkdown; }
    public void setContentMarkdown(String contentMarkdown) { this.contentMarkdown = contentMarkdown; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
    public Integer getXpReward() { return xpReward; }
    public void setXpReward(Integer xpReward) { this.xpReward = xpReward; }
    public String getChallengeInstructions() { return challengeInstructions; }
    public void setChallengeInstructions(String challengeInstructions) { this.challengeInstructions = challengeInstructions; }
    public String getChallengeTemplateCode() { return challengeTemplateCode; }
    public void setChallengeTemplateCode(String challengeTemplateCode) { this.challengeTemplateCode = challengeTemplateCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
