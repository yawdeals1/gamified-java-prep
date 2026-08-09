package com.gamifiedjava.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "xp_log")
public class XpLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "xp_gained", nullable = false)
    private Integer xpGained;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at")
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
