package com.gamifiedjava.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeRunnerServiceTest {
    @Test
    void enabledRunnerReturnsProgramOutput() {
        var runner = new CodeRunnerService(true);
        var result = runner.run("""
                public class Main {
                    public static void main(String[] args) {
                        System.out.println(7 / 2);
                    }
                }
                """);

        assertThat(result.compileSuccess()).isTrue();
        assertThat(result.stdout()).isEqualTo("3");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void disabledRunnerStillCompilesWithoutExecuting() {
        var runner = new CodeRunnerService(false);
        var result = runner.run("""
                public class Main {
                    public static void main(String[] args) {
                        System.out.print("not run");
                    }
                }
                """);

        assertThat(result.compileSuccess()).isTrue();
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains("server configuration");
    }

    @Test
    void enabledRunnerRejectsHostAccessApis() {
        var runner = new CodeRunnerService(true);
        var result = runner.run("""
                public class Main {
                    public static void main(String[] args) {
                        System.out.print(System.getenv("DEPLORO_API_TOKEN"));
                    }
                }
                """);

        assertThat(result.compileSuccess()).isFalse();
        assertThat(result.stderr()).contains("APIs are blocked");
    }
}
