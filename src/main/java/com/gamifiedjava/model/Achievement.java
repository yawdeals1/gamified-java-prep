package com.gamifiedjava.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "achievement")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    private String icon;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    public Achievement() {}

    public Achievement(String name, String description, String icon) {
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
    public boolean isUnlocked() { return unlockedAt != null; }

    /** Map the stored shorthand icon to a Material Symbols Outlined name. */
    public String getMaterialIcon() {
        if (icon == null) return "emoji_events";
        return switch (icon.toLowerCase()) {
            case "footprints", "foot", "steps" -> "footprint";
            case "book"                          -> "menu_book";
            case "sword", "swords"               -> "swords";
            case "trophy"                        -> "emoji_events";
            case "fire", "flame"                 -> "local_fire_department";
            case "brain", "mind"                 -> "psychology";
            case "bug"                           -> "bug_report";
            case "rocket"                        -> "rocket_launch";
            case "star"                          -> "grade";
            case "shield"                        -> "shield";
            case "sun"                           -> "wb_sunny";
            case "seedling", "eco", "leaf"       -> "eco";
            case "hat", "cap"                    -> "school";
            default                              -> "emoji_events";
        };
    }
}
