package com.gamifiedjava.model;

import java.time.LocalDateTime;

public class AiConversation {

    private Integer id;

    private String role;

    private String message;

    private CourseModule module;

    private String contextType;

    private LocalDateTime createdAt = LocalDateTime.now();

    public AiConversation() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public CourseModule getModule() { return module; }
    public void setModule(CourseModule module) { this.module = module; }
    public String getContextType() { return contextType; }
    public void setContextType(String contextType) { this.contextType = contextType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
