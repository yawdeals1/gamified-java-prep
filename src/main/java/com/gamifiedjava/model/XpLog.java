package com.gamifiedjava.model;

import java.time.LocalDateTime;

public class XpLog {

    private Integer id;

    private String action;

    private Integer xpGained;

    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();

    public XpLog() {}

    public XpLog(String action, Integer xpGained, String note) {
        this.action = action;
        this.xpGained = xpGained;
        this.note = note;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getXpGained() { return xpGained; }
    public void setXpGained(Integer xpGained) { this.xpGained = xpGained; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
