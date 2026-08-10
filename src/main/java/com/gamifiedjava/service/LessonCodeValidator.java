package com.gamifiedjava.service;

import com.gamifiedjava.model.LessonStep;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/** Structural checks that accompany stdout checks for interactive coding steps. */
@Service
public class LessonCodeValidator {
    public boolean isValid(LessonStep step, String source) {
        String code = ChallengeValidator.executableText(source);
        List<String> rules = switch (step.getTitle()) {
            case "Fix the reassignment of a constant" -> List.of("\\bfinal\\s+int\\s+MAX\\b", "!\\bMAX\\s*=\\s*200\\b");
            case "Declare variables for a student" -> List.of("\\bString\\s+name\\b", "\\bint\\s+age\\b", "\\bdouble\\s+gpa\\b");
            case "Boss: temperature converter" -> List.of("\\bdouble\\s+celsius\\b", "\\b(?:double|var)\\s+fahrenheit\\b", "celsius\\s*\\*\\s*9");
            case "Missing this." -> List.of("\\bthis\\.name\\s*=\\s*name\\b");
            case "Write a Book class" -> List.of("\\bclass\\s+Book\\b", "\\bprivate\\s+String\\s+title\\b",
                    "\\bprivate\\s+int\\s+pages\\b", "\\bBook\\s*\\(", "\\bgetTitle\\s*\\(", "\\bgetPages\\s*\\(");
            case "Boss: Person class" -> List.of("\\bclass\\s+Person\\b", "\\bprivate\\s+String\\s+name\\b",
                    "\\bint\\s+age\\b", "\\bPerson\\s*\\(", "\\btoString\\s*\\(");
            case "Missing return on one branch" -> List.of("\\breturn\\s+b\\s*;");
            case "Write multiply()" -> List.of("\\bstatic\\s+int\\s+multiply\\s*\\(", "\\breturn\\s+a\\s*\\*\\s*b\\s*;");
            case "Boss: Calculator" -> List.of("\\badd\\s*\\(", "\\bsubtract\\s*\\(", "\\bmultiply\\s*\\(");
            case "== on a String object" -> List.of("\\.equals\\s*\\(");
            case "Find the maximum in an array" -> List.of("\\bmax\\s*\\(\\s*int\\s*\\[\\]", "\\bfor\\s*\\(");
            case "Boss: FizzBuzz" -> List.of("\\bfor\\s*\\(", "%\\s*3", "%\\s*5", "\\bif\\s*\\(");
            case "Missing interface method" -> List.of("\\bpublic\\s+int\\s+area\\s*\\(", "\\breturn\\b");
            case "Implement Flyable" -> List.of("\\binterface\\s+Flyable\\b", "\\bclass\\s+Bird\\b[^{}]*\\bimplements\\s+Flyable\\b", "\\bvoid\\s+fly\\s*\\(");
            case "Boss: Speaker interface" -> List.of("\\binterface\\s+Speaker\\b", "\\bclass\\s+Robot\\b[^{}]*\\bimplements\\s+Speaker\\b",
                    "\\bclass\\s+Human\\b[^{}]*\\bimplements\\s+Speaker\\b");
            case "Add the type parameter" -> List.of("\\bList\\s*<\\s*String\\s*>", "\\bArrayList\\s*<");
            case "find() returning Optional" -> List.of("\\bOptional\\s*<\\s*String\\s*>\\s+find\\s*\\(", "\\bfor\\s*\\(", "\\bOptional\\.of\\s*\\(");
            case "Boss: Optional safe lookup" -> List.of("\\bOptional\\.ofNullable\\s*\\(", "\\.orElse");
            case "@Override on a non-existent method" -> List.of("!@Override\\b", "\\bstatic\\s+void\\s+greet\\s*\\(");
            case "Override toString()", "Boss: Annotated Book class" -> List.of("@Override\\b", "\\bString\\s+toString\\s*\\(");
            case "Missing imports" -> List.of("\\bimport\\s+java\\.util\\.(?:List|\\*)", "\\bimport\\s+java\\.util\\.(?:ArrayList|\\*)");
            case "Use List<Integer> with imports" -> List.of("\\bimport\\s+java\\.util", "\\bList\\s*<\\s*Integer\\s*>");
            case "Boss: List of languages" -> List.of("\\bimport\\s+java\\.util", "\\bList\\s*<\\s*String\\s*>", "\\.get\\s*\\(");
            case "Multi-parameter lambda missing parentheses" -> List.of("\\(\\s*a\\s*,\\s*b\\s*\\)\\s*->");
            case "Lambda for string validation" -> List.of("\\bValidator\\s+\\w+\\s*=\\s*\\w+\\s*->", "\\.isEmpty\\s*\\(");
            case "Boss: Execute a Runnable lambda" -> List.of("\\bexecute\\s*\\(\\s*Runnable", "\\.run\\s*\\(", "->");
            default -> List.of("\\b(?:class|interface|record|enum)\\s+\\w+");
        };
        return rules.stream().allMatch(rule -> matches(code, rule));
    }

    private boolean matches(String code, String rule) {
        boolean negate = rule.startsWith("!");
        String regex = negate ? rule.substring(1) : rule;
        boolean found = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(code).find();
        return negate ? !found : found;
    }
}
