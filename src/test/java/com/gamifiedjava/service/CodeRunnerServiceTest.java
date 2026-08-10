package com.gamifiedjava.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeRunnerServiceTest {
    @Test
    void publicBindCannotExecuteBytecodeEvenWhenFlagIsEnabled() {
        var runner = new CodeRunnerService(true, "0.0.0.0");
        var result = runner.run("""
                public class Main {
                    public static void main(String[] args) {
                        System.out.print(System.getenv("DEPLORO_API_TOKEN"));
                    }
                }
                """);

        assertThat(result.compileSuccess()).isTrue();
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains("Execution is disabled");
    }
}
