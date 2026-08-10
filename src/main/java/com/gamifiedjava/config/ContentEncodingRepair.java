package com.gamifiedjava.config;

import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.util.MojibakeRepair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(200)
public class ContentEncodingRepair implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ContentEncodingRepair.class);
    private static final Map<String, List<String>> TEXT_COLUMNS = Map.of(
            "achievement", List.of("name", "description"),
            "ai_conversation", List.of("message"),
            "challenge_submission", List.of("compile_output", "ai_feedback"),
            "course_module", List.of("title", "description", "content_markdown", "challenge_instructions"),
            "lesson_step", List.of("title", "body_markdown", "code", "solution", "options", "expected_output"),
            "quiz_question", List.of("question_text", "options", "explanation"),
            "xp_log", List.of("action", "note")
    );

    private final StudioClient client;

    public ContentEncodingRepair(StudioClient client) {
        this.client = client;
    }

    @Override
    public void run(String... args) {
        int repairedRows = 0;
        for (Map.Entry<String, List<String>> table : TEXT_COLUMNS.entrySet()) {
            for (Map<String, Object> row : client.list(table.getKey(), null, 10000)) {
                Map<String, Object> patch = repairColumns(row, table.getValue());
                if (!patch.isEmpty() && row.get("id") != null) {
                    client.update(table.getKey(), row.get("id"), patch);
                    repairedRows++;
                }
            }
        }
        if (repairedRows > 0) {
            log.info("Repaired character encoding in {} content rows", repairedRows);
        }
    }

    static Map<String, Object> repairColumns(Map<String, Object> row, List<String> columns) {
        Map<String, Object> patch = new LinkedHashMap<>();
        for (String column : columns) {
            Object value = row.get(column);
            if (value instanceof String text) {
                String repaired = MojibakeRepair.repair(text);
                if (!repaired.equals(text)) patch.put(column, repaired);
            }
        }
        return patch;
    }
}
