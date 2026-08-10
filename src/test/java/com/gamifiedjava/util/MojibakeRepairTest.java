package com.gamifiedjava.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MojibakeRepairTest {
    @Test
    void repairsCommonBrokenUtf8Sequences() {
        String broken = "Java \u00E2\u20AC\u201D clear \u00C2\u00B7 ready \u00E2\u20AC\u00A6";

        assertThat(MojibakeRepair.repair(broken)).isEqualTo("Java \u2014 clear \u00B7 ready \u2026");
    }

    @Test
    void leavesNormalTextUntouched() {
        assertThat(MojibakeRepair.repair("Java -> Spring Boot")).isEqualTo("Java -> Spring Boot");
    }
}
