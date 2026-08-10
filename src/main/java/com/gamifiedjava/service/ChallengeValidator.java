package com.gamifiedjava.service;

import com.gamifiedjava.model.CourseModule;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Deterministic challenge grading. AI feedback is never used as authorization. */
@Service
public class ChallengeValidator {
    private record Rule(String label, Pattern pattern) {}
    public record Result(int score, boolean passed, String feedback) {}

    public Result validate(CourseModule module, String source) {
        String code = executableText(source);
        List<Rule> rules = rulesFor(module.getOrderIndex());
        List<String> missing = new ArrayList<>();
        for (Rule rule : rules) {
            if (!rule.pattern.matcher(code).find()) missing.add(rule.label);
        }
        int score = rules.isEmpty() ? 0 : (int) Math.round((rules.size() - missing.size()) * 100.0 / rules.size());
        boolean passed = score >= 70;
        String feedback = passed
                ? "Automated checks passed (" + score + "/100)."
                : "Automated checks scored " + score + "/100. Missing: " + String.join(", ", missing) + ".";
        return new Result(score, passed, feedback);
    }

    private List<Rule> rulesFor(int module) {
        return switch (module) {
            case 1 -> List.of(
                    rule("Person class", "\\bclass\\s+Person\\b"),
                    rule("String name field", "\\bString\\s+name\\b"),
                    rule("int age field", "\\bint\\s+age\\b"),
                    rule("double height field", "\\bdouble\\s+height\\b"),
                    rule("final species field", "\\bfinal\\s+[^;=]*species\\b"),
                    rule("constructor", "\\bPerson\\s*\\("),
                    rule("getters", "\\bget(?:Name|Age|Height)\\s*\\("));
            case 2 -> List.of(
                    rule("Car class", "\\bclass\\s+Car\\b"),
                    rule("private fields", "\\bprivate\\s+"),
                    rule("constructor", "\\bCar\\s*\\("),
                    rule("getter", "\\bget[A-Z]\\w*\\s*\\("),
                    rule("displayInfo", "\\bdisplayInfo\\s*\\("),
                    rule("Garage class", "\\bclass\\s+Garage\\b"));
            case 3 -> List.of(
                    rule("Calculator class", "\\bclass\\s+Calculator\\b"),
                    rule("add method", "\\badd\\s*\\("),
                    rule("double overload", "\\bdouble\\s+add\\s*\\([^)]*double"),
                    rule("static square", "\\bstatic\\s+[^;{]*\\bsquare\\s*\\("),
                    rule("void print method", "\\bvoid\\s+print\\w*\\s*\\("),
                    rule("chaining return", "\\breturn\\s+this\\b"));
            case 4 -> List.of(
                    rule("GradeBook class", "\\bclass\\s+GradeBook\\b"),
                    rule("score array", "\\b(?:int|double)\\s*\\[\\]"),
                    rule("conditional grading", "\\bif\\s*\\("),
                    rule("loop", "\\b(?:for|while)\\s*\\("),
                    rule("invalid-score exception", "\\bthrow\\s+new\\b"),
                    rule("null handling", "\\bnull\\b"));
            case 5 -> List.of(
                    rule("Drawable interface", "\\binterface\\s+Drawable\\b"),
                    rule("draw method", "\\bvoid\\s+draw\\s*\\("),
                    rule("Circle implementation", "\\bclass\\s+Circle\\b[^{}]*\\bimplements\\s+Drawable\\b"),
                    rule("Rectangle implementation", "\\bclass\\s+Rectangle\\b[^{}]*\\bimplements\\s+Drawable\\b"),
                    rule("Drawable collection", "\\b(?:List|Collection)\\s*<\\s*Drawable\\s*>"));
            case 6 -> List.of(
                    rule("generic Storage", "\\bclass\\s+Storage\\s*<"),
                    rule("generic item", "\\bT\\s+\\w+"),
                    rule("generic swap", "<\\s*T\\s*>[^;{]*\\bswap\\s*\\("),
                    rule("List<T>", "\\bList\\s*<\\s*T\\s*>"),
                    rule("Optional", "\\bOptional\\s*<"));
            case 7 -> List.of(
                    rule("@Entity", "@Entity\\b"),
                    rule("@Id", "@Id\\b"),
                    rule("@GeneratedValue", "@GeneratedValue\\b"),
                    rule("@Column", "@Column\\b"),
                    rule("Product class", "\\bclass\\s+Product\\b"));
            case 8 -> List.of(
                    rule("package declaration", "\\bpackage\\s+[a-zA-Z_][\\w.]*\\s*;"),
                    rule("imports", "\\bimport\\s+[a-zA-Z_][\\w.*]*\\s*;"),
                    rule("layer class", "\\bclass\\s+(?:User|UserRepository|UserService|UserController)\\b"));
            case 9 -> List.of(
                    rule("lambda expression", "->"),
                    rule("orElseThrow", "\\borElseThrow\\s*\\("),
                    rule("sort", "\\b(?:sort|sorted)\\s*\\("),
                    rule("collection", "\\bList\\s*<"));
            default -> List.of(rule("Java type", "\\b(?:class|interface|record|enum)\\s+\\w+"));
        };
    }

    private Rule rule(String label, String regex) {
        return new Rule(label, Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
    }

    static String executableText(String source) {
        if (source == null) return "";
        return source
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ")
                .replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"")
                .replaceAll("'(?:\\\\.|[^'\\\\])'", "''");
    }
}
