package com.gamifiedjava.service;

import com.gamifiedjava.model.LessonStep;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonCodeValidatorTest {
    private final LessonCodeValidator validator = new LessonCodeValidator();

    @Test
    void printingExpectedOutputWithoutImplementationDoesNotPass() {
        LessonStep step = new LessonStep();
        step.setTitle("Write multiply()");
        String shortcut = """
                public class Main {
                    // static int multiply(int a, int b) { return a * b; }
                    public static void main(String[] args) { System.out.println(42); }
                }
                """;
        assertThat(validator.isValid(step, shortcut)).isFalse();
    }
}
