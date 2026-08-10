package com.gamifiedjava.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContentEncodingRepairTest {
    @Test
    void patchesOnlyBrokenConfiguredColumns() {
        Map<String, Object> row = Map.of(
                "id", 7,
                "title", "Clean title",
                "body_markdown", "Wrong \u00E2\u20AC\u201D text",
                "ignored", "Wrong \u00E2\u20AC\u201D text"
        );

        assertThat(ContentEncodingRepair.repairColumns(row, List.of("title", "body_markdown")))
                .containsExactly(Map.entry("body_markdown", "Wrong \u2014 text"));
    }
}
