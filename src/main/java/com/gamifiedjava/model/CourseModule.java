package com.gamifiedjava.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_module")
public class CourseModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "content_markdown", columnDefinition = "TEXT")
    private String contentMarkdown;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "xp_reward")
    private Integer xpReward = 100;

    @Column(name = "challenge_instructions", columnDefinition = "TEXT")
    private String challengeInstructions;

    @Column(name = "challenge_template_code", columnDefinition = "TEXT")
    private String challengeTemplateCode;

    @Column(name = "created_at")
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
