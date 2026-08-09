package com.gamifiedjava.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

public class QuizQuestion {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Integer id;

    private CourseModule module;

    private String questionText;

    private String options;

    private Integer correctIndex;

    private String explanation;

    private String difficulty = "easy";

    private LocalDateTime createdAt = LocalDateTime.now();

    public QuizQuestion() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public CourseModule getModule() { return module; }
    public void setModule(CourseModule module) { this.module = module; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public List<String> getOptionList() {
        try {
            return OBJECT_MAPPER.readValue(options, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
    public Integer getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(Integer correctIndex) { this.correctIndex = correctIndex; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
