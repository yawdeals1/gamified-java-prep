package com.gamifiedjava.model;

import jakarta.persistence.*;

/**
 * A single interactive step inside a module's lesson (the Step Engine).
 * The learner sees ONE step at a time and acts on it. See CLAUDE.md § Step Engine.
 */
@Entity
@Table(name = "lesson_step")
public class LessonStep {

    /** Step kinds — kept as String in the column, validated against this enum. */
    public enum Type {
        CONCEPT, CODE_DEMO, PREDICT_OUTPUT, FILL_BLANK, FIX_THE_BUG, LIVE_CODE, MCQ, CHECKPOINT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private String title;

    /** Prompt / concept body (markdown-ish, rendered as text). */
    @Column(name = "body_markdown", columnDefinition = "TEXT")
    private String bodyMarkdown;

    /** Starter/demo code shown in the editor. */
    @Column(columnDefinition = "TEXT")
    private String code;

    /** Canonical answer for FILL_BLANK / reference solution for LIVE_CODE. */
    @Column(columnDefinition = "TEXT")
    private String solution;

    /** JSON array of options for MCQ (TEXT, like QuizQuestion.options). */
    @Column(columnDefinition = "TEXT")
    private String options;

    /** Correct option index for MCQ. */
    @Column(name = "correct_index")
    private Integer correctIndex;

    /** Expected stdout for PREDICT_OUTPUT and for LIVE_CODE output checks. */
    @Column(name = "expected_output", columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(name = "xp_reward", nullable = false)
    private Integer xpReward = 5;

    public LessonStep() {}

    public LessonStep(CourseModule module, int orderIndex, Type type, String title, int xpReward) {
        this.module = module;
        this.orderIndex = orderIndex;
        this.type = type.name();
        this.title = title;
        this.xpReward = xpReward;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public CourseModule getModule() { return module; }
    public void setModule(CourseModule module) { this.module = module; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBodyMarkdown() { return bodyMarkdown; }
    public void setBodyMarkdown(String bodyMarkdown) { this.bodyMarkdown = bodyMarkdown; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public Integer getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(Integer correctIndex) { this.correctIndex = correctIndex; }
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    public Integer getXpReward() { return xpReward; }
    public void setXpReward(Integer xpReward) { this.xpReward = xpReward; }
}
