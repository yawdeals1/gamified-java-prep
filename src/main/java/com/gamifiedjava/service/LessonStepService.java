package com.gamifiedjava.service;

import com.gamifiedjava.model.CourseModule;
import com.gamifiedjava.model.LessonStep;
import com.gamifiedjava.model.LessonStep.Type;
import com.gamifiedjava.model.StepProgress;
import com.gamifiedjava.repository.LessonStepRepository;
import com.gamifiedjava.repository.ModuleRepository;
import com.gamifiedjava.repository.ModuleProgressRepository;
import com.gamifiedjava.auth.CurrentUserContext;
import com.gamifiedjava.repository.StepProgressRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class LessonStepService {

    private static final int XP_SMALL = 5, XP_MEDIUM = 15, XP_LARGE = 40, XP_CHECKPOINT = 100;

    private final LessonStepRepository stepRepository;
    private final StepProgressRepository progressRepository;
    private final ModuleRepository moduleRepository;
    private final CodeRunnerService codeRunner;
    private final GamificationService gamificationService;
    private final ModuleProgressRepository moduleProgressRepository;
    private final ModuleService moduleService;
    private final CurrentUserContext users;
    private final LessonCodeValidator codeValidator;
    private final ConcurrentHashMap<String, Object> completionLocks = new ConcurrentHashMap<>();

    public LessonStepService(LessonStepRepository stepRepository,
                             StepProgressRepository progressRepository,
                             ModuleRepository moduleRepository,
                             CodeRunnerService codeRunner,
                             GamificationService gamificationService,
                             ModuleProgressRepository moduleProgressRepository,
                             ModuleService moduleService,
                             CurrentUserContext users,
                             LessonCodeValidator codeValidator) {
        this.stepRepository = stepRepository;
        this.progressRepository = progressRepository;
        this.moduleRepository = moduleRepository;
        this.codeRunner = codeRunner;
        this.gamificationService = gamificationService;
        this.moduleProgressRepository = moduleProgressRepository;
        this.moduleService = moduleService;
        this.users = users;
        this.codeValidator = codeValidator;
    }

    // ---------------------------------------------------------------- reads

    public List<LessonStep> getSteps(Integer moduleId) {
        return stepRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
    }

    public Set<Integer> completedStepIds(Integer moduleId) {
        return progressRepository.findByModuleId(moduleId).stream()
                .filter(StepProgress::isDone)
                .map(StepProgress::getStepId)
                .collect(Collectors.toSet());
    }

    /** Mastery = completed steps / total steps, 0-100. */
    public int masteryPercent(Integer moduleId) {
        long total = stepRepository.countByModuleId(moduleId);
        if (total == 0) return 0;
        long done = completedStepIds(moduleId).size();
        return (int) Math.round(done * 100.0 / total);
    }

    /**
     * Calculates mastery for every module with two bulk Studio API reads instead
     * of two remote requests per module (plus relationship N+1 lookups).
     */
    public Map<Integer, Integer> masteryPercentByModule() {
        Map<Integer, Long> totals;
        Map<Integer, Long> completed;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var totalsFuture = executor.submit(stepRepository::countByModule);
            var completedFuture = executor.submit(progressRepository::completedCountByModule);
            try {
                totals = totalsFuture.get();
                completed = completedFuture.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while loading mastery data", e);
            } catch (java.util.concurrent.ExecutionException e) {
                throw new IllegalStateException("Could not load mastery data", e.getCause());
            }
        }
        Map<Integer, Integer> mastery = new HashMap<>();
        totals.forEach((moduleId, total) -> {
            long done = completed.getOrDefault(moduleId, 0L);
            mastery.put(moduleId, total == 0 ? 0 : (int) Math.round(done * 100.0 / total));
        });
        return mastery;
    }

    /** Client-facing view of a step - deliberately omits answers (correctIndex, expectedOutput, solution). */
    public List<Map<String, Object>> toClientSteps(List<LessonStep> steps, Set<Integer> done) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (LessonStep s : steps) {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("orderIndex", s.getOrderIndex());
            m.put("type", s.getType());
            m.put("title", s.getTitle());
            m.put("body", s.getBodyMarkdown());
            m.put("code", s.getCode());
            m.put("options", s.getOptions());     // JSON array string (safe to show)
            m.put("xp", s.getXpReward());
            m.put("done", done.contains(s.getId()));
            out.add(m);
        }
        return out;
    }

    // ---------------------------------------------------------------- check
    public StepResult check(Integer stepId, Integer selectedIndex, String answer, String code) {
        LessonStep step = stepRepository.findById(stepId).orElse(null);
        if (step == null) return new StepResult(false, 0, "Step not found.", null, null);
        var moduleProgress = moduleProgressRepository.findByModuleId(step.getModule().getId()).orElse(null);
        if (moduleProgress == null || !moduleProgress.isUnlocked()) {
            return new StepResult(false, 0, "Complete the previous module first.", null, null);
        }

        // Progression gate: only the first not-yet-done step of a module can be
        // graded (plus already-done steps for re-checks). Prevents XP/skip farming
        // by brute-forcing step ids. Resume mid-module is still supported because
        // "first not-done" is exactly where the lesson player resumes.
        StepProgress sp = progressRepository.findByStepId(stepId).orElse(null);
        boolean alreadyDone = sp != null && sp.isDone();
        if (!alreadyDone) {
            List<LessonStep> moduleSteps = stepRepository.findByModuleIdOrderByOrderIndexAsc(step.getModule().getId());
            Integer firstNotDoneOrder = moduleSteps.stream()
                    .filter(s -> progressRepository.findByStepId(s.getId())
                            .map(p -> !p.isDone()).orElse(true))
                    .map(LessonStep::getOrderIndex)
                    .min(Integer::compareTo)
                    .orElse(null);
            if (firstNotDoneOrder != null && !firstNotDoneOrder.equals(step.getOrderIndex())) {
                Integer neededId = moduleSteps.stream()
                        .filter(s -> firstNotDoneOrder.equals(s.getOrderIndex()))
                        .map(LessonStep::getId).findFirst().orElse(null);
                return new StepResult(false, 0,
                        "Complete the previous step first"
                                + (neededId != null ? " (step " + neededId + ")" : "") + ".", null, null);
            }
        }

        Type type = Type.valueOf(step.getType());
        boolean correct;
        String feedback;
        String expected = null;
        CodeRunnerService.RunResult run = null;

        switch (type) {
            case CONCEPT, CODE_DEMO -> {
                correct = true;
                feedback = "Nice - on to the next.";
            }
            case MCQ -> {
                correct = selectedIndex != null && selectedIndex.equals(step.getCorrectIndex());
                feedback = correct ? "Correct!" : "Not quite - give it another look.";
            }
            case FILL_BLANK -> {
                correct = normalize(answer).equals(normalize(step.getSolution()));
                feedback = correct ? "Exactly right." : "Close - check the token you filled in.";
                if (!correct) expected = null; // don't leak on first misses
            }
            case PREDICT_OUTPUT -> {
                correct = looseMatch(answer, step.getExpectedOutput());
                expected = step.getExpectedOutput();
                feedback = correct ? "Spot on - that's the output." : "Not quite. The actual output is shown below - trace through the code and see why.";
            }
            case LIVE_CODE, FIX_THE_BUG, CHECKPOINT -> {
                run = codeRunner.run(code);
                if (!run.compileSuccess()) {
                    correct = false;
                    feedback = run.timedOut() ? "Timed out - check for an infinite loop."
                            : "It doesn't compile yet. Read the console below.";
                } else if (step.getExpectedOutput() != null && !step.getExpectedOutput().isBlank()) {
                    boolean outputMatches = looseMatch(run.stdout(), step.getExpectedOutput());
                    boolean structureMatches = codeValidator.isValid(step, code);
                    correct = outputMatches && structureMatches;
                    feedback = correct ? "Compiled and the output matches. Well done."
                            : outputMatches
                            ? "The output matches, but the required implementation is missing."
                            : "Compiles, but the output isn't what we expected.";
                    if (!correct) expected = step.getExpectedOutput();
                } else {
                    correct = codeValidator.isValid(step, code);
                    feedback = correct ? "Compiled cleanly. Nice work."
                            : "It compiles, but it does not implement the requested structure yet.";
                }
            }
            default -> {
                correct = false;
                feedback = "Unknown step type.";
            }
        }

        int xpAwarded = correct ? awardOnce(step) : 0;
        return new StepResult(correct, xpAwarded, feedback, expected, run);
    }

    /** Award XP the first time a step is cleared; idempotent thereafter. Also bumps attempts. */
    private int awardOnce(LessonStep step) {
        String key = users.requireUserId() + ":" + step.getId();
        synchronized (completionLocks.computeIfAbsent(key, ignored -> new Object())) {
            StepProgress sp = progressRepository.findByStepId(step.getId())
                    .orElseGet(() -> new StepProgress(step.getId(), step.getModule().getId()));
            sp.setAttempts(sp.getAttempts() + 1);
            if (sp.isDone()) {
                progressRepository.save(sp);
                return 0;
            }
            sp.setCompletedAt(LocalDateTime.now());
            progressRepository.save(sp);
            gamificationService.addXp("step_" + step.getType().toLowerCase(),
                    step.getXpReward(), "Cleared step: " + step.getTitle());
            long totalSteps = stepRepository.countByModuleId(step.getModule().getId());
            if (totalSteps > 0 && completedStepIds(step.getModule().getId()).size() >= totalSteps) {
                moduleService.unlockNextModule(step.getModule().getId());
            }
            return step.getXpReward();
        }
    }

    private String normalize(String s) {
        return s == null ? "" : s.strip().replaceAll("\\s+", " ").toLowerCase();
    }

    /** Output comparison: trim, normalise line endings + trailing spaces, keep case. */
    private boolean looseMatch(String actual, String expected) {
        if (expected == null) return false;
        return canon(actual).equals(canon(expected));
    }

    private String canon(String s) {
        if (s == null) return "";
        return s.replace("\r\n", "\n").strip()
                .lines().map(String::stripTrailing).collect(Collectors.joining("\n"));
    }

    public record StepResult(boolean correct, int xpAwarded, String feedback,
                             String expected, CodeRunnerService.RunResult run) {}

    // ---------------------------------------------------------------- seeding
    public void seedSteps() {
        boolean userBound = users.currentUserId().isPresent();
        if (userBound) {
            // Scoped progress cleanup is only valid inside an authenticated user context.
            Set<Integer> liveStepIds = stepRepository.findAll().stream()
                    .map(LessonStep::getId).collect(Collectors.toSet());
            List<StepProgress> orphans = progressRepository.findAll().stream()
                    .filter(sp -> !liveStepIds.contains(sp.getStepId()))
                    .collect(Collectors.toList());
            if (!orphans.isEmpty()) progressRepository.deleteAll(orphans);
        }

        List<CourseModule> modules = moduleRepository.findAllByOrderByOrderIndexAsc();
        for (CourseModule m : modules) {
            long count = stepRepository.countByModuleId(m.getId());
            // Re-seed when steps are missing, CONCEPT cards are absent, or the module is stale (< 10 steps).
            boolean needsReseed = count == 0 || !hasConceptSteps(m) || count < 10;
            if (!needsReseed) continue;
            if (count > 0) {
                // Startup has no authenticated tenant. Never destroy global steps (and
                // potentially every user's progress) from that unscoped context.
                if (!userBound) continue;
                progressRepository.deleteAll(progressRepository.findByModuleId(m.getId()));
                stepRepository.deleteAll(stepRepository.findByModuleIdOrderByOrderIndexAsc(m.getId()));
            }
            switch (m.getOrderIndex()) {
                case 1 -> seedModuleOne(m);
                case 2 -> seedModuleTwo(m);
                case 3 -> seedModuleThree(m);
                case 4 -> seedModuleFour(m);
                case 5 -> seedModuleFive(m);
                case 6 -> seedModuleSix(m);
                case 7 -> seedModuleSeven(m);
                case 8 -> seedModuleEight(m);
                case 9 -> seedModuleNine(m);
                default -> seedGeneric(m);
            }
        }
    }

    private boolean hasConceptSteps(CourseModule m) {
        return stepRepository.findByModuleIdOrderByOrderIndexAsc(m.getId()).stream()
                .anyMatch(s -> "CONCEPT".equals(s.getType()));
    }

    /** Module 1: Variables and Types - full teaching sequence. */
    private void seedModuleOne(CourseModule m) {
        int i = 0;

        // -- CONCEPT 1: The type system --------------------------------------
        step(m, i++, Type.CONCEPT, "Every variable has a type - and Java enforces it at compile time", XP_SMALL,
                "Java is **statically typed**: you declare the type of every variable upfront, and the "
                + "compiler rejects any mismatch before your code ever runs.\n\n"
                + "**The 8 primitive types** (lowercase, built-in):\n\n"
                + "| Type | What it stores | Example |\n"
                + "|------|---------------|---------|\n"
                + "| `int` | whole number (-2 billion -> +2 billion) | `int age = 25;` |\n"
                + "| `long` | bigger whole number | `long pop = 8_000_000_000L;` |\n"
                + "| `double` | decimal number (64-bit) | `double pi = 3.14;` |\n"
                + "| `float` | decimal number (32-bit, less precise) | `float temp = 98.6f;` |\n"
                + "| `boolean` | true or false | `boolean active = true;` |\n"
                + "| `char` | single character | `char grade = 'A';` |\n"
                + "| `byte` | tiny integer (-128 -> 127) | `byte flag = 1;` |\n"
                + "| `short` | small integer | `short year = 2026;` |\n\n"
                + "**`String`** is *not* a primitive - it's a class (capital S) that holds a sequence of characters:\n\n"
                + "```java\n"
                + "String name = \"Alice\";  // double quotes, capital S\n"
                + "char initial = 'A';     // char uses single quotes\n"
                + "```\n\n"
                + "The type is fixed when you declare the variable - you can't later assign a `boolean` into an `int`.").save();

        step(m, i++, Type.CODE_DEMO, "See types in action", XP_SMALL,
                "Press **Run** and watch each type print. Notice `int` division truncates - `7 / 2` gives `3`, not `3.5`.")
                .code("public class Types {\n"
                    + "    public static void main(String[] args) {\n"
                    + "        int year   = 2026;\n"
                    + "        double pi  = 3.14159;\n"
                    + "        boolean ok = true;\n"
                    + "        char   ch  = 'J';\n"
                    + "        String lang = \"Java\";\n"
                    + "        System.out.println(lang + \" \" + year);\n"
                    + "        System.out.println(\"pi = \" + pi);\n"
                    + "        System.out.println(\"ok = \" + ok);\n"
                    + "        System.out.println(\"ch = \" + ch);\n"
                    + "        System.out.println(7 / 2);   // int division: truncates\n"
                    + "    }\n"
                    + "}")
                .expected("Java 2026\npi = 3.14159\nok = true\nch = J\n3").save();

        step(m, i++, Type.PREDICT_OUTPUT, "What does integer division print?", XP_MEDIUM,
                "Both `a` and `b` are `int`. What does `a / b` print?\n\n"
                + "```java\n"
                + "int a = 7;\n"
                + "int b = 2;\n"
                + "System.out.println(a / b);\n"
                + "```\n\n"
                + "Hint: when both operands are `int`, Java keeps only the whole part.")
                .expected("3").save();

        step(m, i++, Type.MCQ, "Which of these is NOT a primitive type?", XP_SMALL,
                "Four options below - three are primitives, one is a class. Which is the class?")
                .options("[\"boolean\",\"String\",\"char\",\"double\"]").correct(1).save();

        // -- CONCEPT 2: final and var ----------------------------------------
        step(m, i++, Type.CONCEPT, "Two special keywords: final and var", XP_SMALL,
                "**`final`** - makes a variable a constant. The compiler refuses any later reassignment:\n\n"
                + "```java\n"
                + "final double PI = 3.14159;\n"
                + "PI = 3;  // Error compile error: cannot assign a value to final variable\n"
                + "```\n\n"
                + "Use `final` for values that should never change (math constants, config, lookup tables).\n\n"
                + "---\n\n"
                + "**`var`** (Java 10+) - *local variable type inference*. You skip writing the type and let "
                + "the compiler infer it from the right-hand side:\n\n"
                + "```java\n"
                + "var name    = \"Alice\";   // inferred: String\n"
                + "var count   = 42;        // inferred: int\n"
                + "var ratio   = 3.14;      // inferred: double\n"
                + "```\n\n"
                + "`var` is **still statically typed** - the type is locked at compile time, just not written out. "
                + "It only works for local variables (inside methods), never for fields or parameters.").save();

        step(m, i++, Type.CODE_DEMO, "final and var in action", XP_SMALL,
                "Press **Run**. Notice `var` infers the type - it is NOT dynamic like JavaScript. "
                + "The `final` line is commented out to allow the demo to compile.")
                .code("public class FinalVar {\n"
                    + "    public static void main(String[] args) {\n"
                    + "        final int MAX_SCORE = 100;\n"
                    + "        // MAX_SCORE = 200;  // would be a compile error\n\n"
                    + "        var greeting = \"Hello\";  // String inferred\n"
                    + "        var score    = 42;       // int inferred\n"
                    + "        var ratio    = 1.5;      // double inferred\n\n"
                    + "        System.out.println(MAX_SCORE);\n"
                    + "        System.out.println(greeting + \", score=\" + score + \", ratio=\" + ratio);\n"
                    + "    }\n"
                    + "}")
                .expected("100\nHello, score=42, ratio=1.5").save();

        step(m, i++, Type.FILL_BLANK, "Make PI a constant", XP_MEDIUM,
                "Fill in the blank to make `PI` a compile-time constant. Type just the keyword.")
                .code("____ double PI = 3.14159;").solution("final").save();

        step(m, i++, Type.FILL_BLANK, "var infers the type", XP_MEDIUM,
                "Replace the explicit type with `var` so the compiler infers it. "
                + "Fill in the blank with the single keyword that enables type inference.")
                .code("____ message = \"Hello, Java!\";").solution("var").save();

        step(m, i++, Type.FIX_THE_BUG, "Fix the reassignment of a constant", XP_MEDIUM,
                "This code won't compile because a `final` variable is being reassigned. "
                + "Fix it by removing the reassignment line (delete it entirely) and printing `MAX` instead.")
                .code("public class Fix {\n"
                    + "    public static void main(String[] args) {\n"
                    + "        final int MAX = 100;\n"
                    + "        MAX = 200;  // bug: can't reassign final\n"
                    + "        System.out.println(MAX);\n"
                    + "    }\n"
                    + "}")
                .expected("100").save();

        step(m, i++, Type.LIVE_CODE, "Declare variables for a student", XP_LARGE,
                "Declare three variables:\n"
                + "- `String name` = `\"Sam\"`\n"
                + "- `int age` = `20`\n"
                + "- `double gpa` = `3.8`\n\n"
                + "Print them on one line separated by spaces: `Sam 20 3.8`")
                .code("public class Main {\n"
                    + "    public static void main(String[] args) {\n"
                    + "        // declare name, age, gpa here\n\n"
                    + "        System.out.println(name + \" \" + age + \" \" + gpa);\n"
                    + "    }\n"
                    + "}")
                .expected("Sam 20 3.8").save();

        step(m, i++, Type.MCQ, "What does var actually do?", XP_SMALL,
                "Which statement about `var` is true?")
                .options("["
                    + "\"It makes the variable dynamically typed like JavaScript\","
                    + "\"It lets the compiler infer the type - the variable is still statically typed\","
                    + "\"It works on fields and method parameters\","
                    + "\"It was introduced in Java 8\""
                    + "]").correct(1).save();

        step(m, i++, Type.CHECKPOINT, "Boss: temperature converter", XP_CHECKPOINT,
                "Write a program that converts **100 degrees Celsius to Fahrenheit** and prints the result.\n\n"
                + "Formula: `fahrenheit = celsius * 9 / 5.0 + 32`\n\n"
                + "Declare `celsius` as a `double` equal to `100.0`, compute `fahrenheit`, then print it "
                + "cast to `int`. Expected output: `212`")
                .code("public class Main {\n"
                    + "    public static void main(String[] args) {\n"
                    + "        double celsius = 100.0;\n"
                    + "        // compute fahrenheit and print as int\n"
                    + "    }\n"
                    + "}")
                .expected("212").save();
    }

    // ---- MODULE 2: Classes and Objects ----------------------------------------
    private void seedModuleTwo(CourseModule m) {
        int i = 0;
        step(m, i++, Type.CONCEPT, "Classes are blueprints, objects are instances", XP_SMALL,
                "A **class** defines the shape - what data and behaviour objects have.\n"
                + "An **object** is a live instance built from that blueprint with `new`.\n\n"
                + "```java\n"
                + "Student alice = new Student();  // creates a new object\n"
                + "alice.name = \"Alice\";\n"
                + "```\n\n"
                + "Two objects built from the same class are **completely independent**: "
                + "changing `alice.name` has no effect on `bob.name`.").save();

        step(m, i++, Type.CODE_DEMO, "Your first class", XP_SMALL,
                "A class defines the shape; `new` builds an object from it. "
                + "Two separate objects - two separate copies of the fields. Press Run.")
                .code("class Student {\n    String name;\n    int age;\n}\n\npublic class Demo {\n    public static void main(String[] args) {\n        Student alice = new Student();\n        alice.name = \"Alice\";\n        alice.age = 22;\n        Student bob = new Student();\n        bob.name = \"Bob\";\n        bob.age = 20;\n        System.out.println(alice.name + \" \" + alice.age);\n        System.out.println(bob.name + \" \" + bob.age);\n    }\n}")
                .expected("Alice 22\nBob 20").save();

        step(m, i++, Type.PREDICT_OUTPUT, "alice or bob?", XP_MEDIUM,
                "alice.name was set to \"Alice\", bob.name to \"Bob\". They are separate objects. "
                + "What does `System.out.println(alice.name)` print?")
                .code("class Student { String name; }\npublic class Demo {\n    public static void main(String[] args) {\n        Student alice = new Student();\n        alice.name = \"Alice\";\n        Student bob = new Student();\n        bob.name = \"Bob\";\n        System.out.println(alice.name);\n    }\n}")
                .expected("Alice").save();

        step(m, i++, Type.MCQ, "Which keyword creates an object?", XP_SMALL,
                "To build a Student object from the class, which keyword goes before the class name?")
                .options("[\"new\",\"create\",\"build\",\"object\"]").correct(0).save();

        step(m, i++, Type.CONCEPT, "Constructors and this", XP_SMALL,
                "A **constructor** runs the instant `new` is called. It wires up fields at creation time.\n\n"
                + "Inside a constructor, `this.name` refers to the **field**, distinguishing it from a parameter with the same name:\n\n"
                + "```java\n"
                + "Student(String name) {\n"
                + "    this.name = name;  // this.name = field,  name = parameter\n"
                + "}\n"
                + "```\n\n"
                + "In Spring Boot, fields are always `private`. External code reads them via **getters** "
                + "(`getName()`) and writes via **setters** (`setName(...)`). "
                + "Jackson uses these automatically when serialising objects to JSON.").save();

        step(m, i++, Type.CODE_DEMO, "Constructor: set up at creation time", XP_SMALL,
                "A constructor runs the moment you call `new`. Inside it, `this.name` means "
                + "'the field on this object' - it distinguishes the field from the same-named parameter. Press Run.")
                .code("class Student {\n    String name;\n    int age;\n    Student(String name, int age) {\n        this.name = name;   // field = parameter\n        this.age  = age;\n    }\n}\npublic class Demo {\n    public static void main(String[] args) {\n        Student s = new Student(\"Alice\", 22);\n        System.out.println(s.name + \" \" + s.age);\n    }\n}")
                .expected("Alice 22").save();

        step(m, i++, Type.PREDICT_OUTPUT, "Trace the constructor", XP_MEDIUM,
                "The constructor runs `this.name = name`. Called with new Student(\"Zara\"), what does `s.name` hold?")
                .code("class Student {\n    String name;\n    Student(String name) { this.name = name; }\n}\npublic class Demo {\n    public static void main(String[] args) {\n        Student s = new Student(\"Zara\");\n        System.out.println(s.name);\n    }\n}")
                .expected("Zara").save();

        step(m, i++, Type.FILL_BLANK, "this inside the constructor", XP_MEDIUM,
                "The field and the parameter share the name `name`. "
                + "Use `____.name` to refer to the field - not the parameter.")
                .code("Student(String name) { ____.name = name; }")
                .solution("this").save();

        step(m, i++, Type.FIX_THE_BUG, "Missing this.", XP_MEDIUM,
                "`name = name` assigns the parameter to itself - the field stays null. "
                + "Add `this.` so the constructor stores the value. Output: Alice")
                .code("public class Student {\n    private String name;\n    public Student(String name) {\n        name = name;  // bug\n    }\n    public String getName() { return name; }\n    public static void main(String[] args) {\n        System.out.println(new Student(\"Alice\").getName());\n    }\n}")
                .expected("Alice").save();

        step(m, i++, Type.CODE_DEMO, "Private fields + getters/setters", XP_SMALL,
                "Spring Boot always uses `private` fields. Outside code reads them via getters, "
                + "writes them via setters. Jackson uses these to serialize objects to JSON. Press Run.")
                .code("class Student {\n    private String name;\n    public Student(String name) { this.name = name; }\n    public String getName()            { return name; }\n    public void   setName(String name) { this.name = name; }\n}\npublic class Demo {\n    public static void main(String[] args) {\n        Student s = new Student(\"Alice\");\n        System.out.println(s.getName());\n        s.setName(\"Alice Smith\");\n        System.out.println(s.getName());\n    }\n}")
                .expected("Alice\nAlice Smith").save();

        step(m, i++, Type.MCQ, "What does private enforce?", XP_SMALL,
                "With `private String name;` declared in Student, which line causes a compile error?")
                .options("[\"s.getName()\",\"s.name = \\\"Alice\\\"\",\"s.setName(\\\"Alice\\\")\",\"s.getName().length()\"]")
                .correct(1).save();

        step(m, i++, Type.FILL_BLANK, "Getter return type", XP_MEDIUM,
                "A getter for a `String` field named `name` must declare what return type?")
                .code("public ____ getName() { return name; }")
                .solution("String").save();

        step(m, i++, Type.LIVE_CODE, "Write a Book class", XP_LARGE,
                "Write a `Book` class with `private String title` and `private int pages`, "
                + "a constructor that sets both, and getters `getTitle()` / `getPages()`. "
                + "In main create `Book(\"Dune\", 688)` and print title then pages on separate lines.")
                .code("public class Book {\n    // TODO: private fields, constructor, getters\n\n    public static void main(String[] args) {\n        // print title then pages\n    }\n}")
                .expected("Dune\n688").save();

        step(m, i++, Type.CHECKPOINT, "Boss: Person class", XP_CHECKPOINT,
                "Write a `Person` class: private `String name` and `int age`, constructor for both, "
                + "and `toString()` returning `name + \" (age \" + age + \")\"`. "
                + "Print `new Person(\"Alice\", 25)`. Output: Alice (age 25)")
                .code("public class Person {\n    // TODO\n    public static void main(String[] args) {\n        System.out.println(new Person(\"Alice\", 25));\n    }\n}")
                .expected("Alice (age 25)").save();
    }

    // ---- MODULE 3: Methods ----------------------------------------------------
    private void seedModuleThree(CourseModule m) {
        int i = 0;
        step(m, i++, Type.CONCEPT, "Anatomy of a method", XP_SMALL,
                "Every method has four parts:\n\n"
                + "| Part | Example |\n"
                + "|------|---------|\n"
                + "| Access modifier | `public` / `static` |\n"
                + "| **Return type** | `int`, `String`, or `void` |\n"
                + "| Name | `add` |\n"
                + "| Parameters | `(int a, int b)` |\n\n"
                + "The body lives between `{ }`. Write it once, call it from anywhere.").save();

        step(m, i++, Type.CODE_DEMO, "Define and call a method", XP_SMALL,
                "A method has: access modifier (public/static), return type (int/void/String), name, and parameters. "
                + "The body is between { }. Write once, call anywhere. Press Run.")
                .code("public class Demo {\n    static int add(int a, int b) {\n        return a + b;\n    }\n    public static void main(String[] args) {\n        System.out.println(add(3, 4));\n        System.out.println(add(10, 20));\n    }\n}")
                .expected("7\n30").save();

        step(m, i++, Type.PREDICT_OUTPUT, "Trace the method call", XP_MEDIUM,
                "add(7, 3) passes a=7 and b=3. The body executes `return a + b`. What prints?")
                .code("public class Demo {\n    static int add(int a, int b) { return a + b; }\n    public static void main(String[] args) {\n        System.out.println(add(7, 3));\n    }\n}")
                .expected("10").save();

        step(m, i++, Type.MCQ, "Which token declares the return type?", XP_SMALL,
                "In `static int add(int a, int b)`, which token declares what type the method returns?")
                .options("[\"static\",\"int\",\"add\",\"(int a, int b)\"]").correct(1).save();

        step(m, i++, Type.CONCEPT, "void and return", XP_SMALL,
                "- If a method gives something back, declare its **return type** and use `return` to send the value.\n"
                + "- `return` exits the method **immediately** - any code after it on that path is unreachable.\n"
                + "- `void` means the method returns **nothing** - no return statement is required.\n\n"
                + "Every non-`void` method **must** have a `return` on every possible execution path "
                + "or the compiler rejects it.").save();

        step(m, i++, Type.PREDICT_OUTPUT, "Early return in a chain", XP_MEDIUM,
                "Java hits the first matching `return` and stops - the rest are skipped. "
                + "What does getGrade(85) print?")
                .code("public class Demo {\n    static String getGrade(int score) {\n        if (score >= 90) return \"A\";\n        if (score >= 80) return \"B\";\n        if (score >= 70) return \"C\";\n        return \"F\";\n    }\n    public static void main(String[] args) {\n        System.out.println(getGrade(85));\n    }\n}")
                .expected("B").save();

        step(m, i++, Type.MCQ, "What does void mean?", XP_SMALL,
                "A method declared with `void` as its return type does what?")
                .options("[\"Returns the integer 0\",\"Returns nothing - the caller gets no value back\",\"Must have an explicit return statement\",\"Cannot take parameters\"]").correct(1).save();

        step(m, i++, Type.FILL_BLANK, "void return type", XP_MEDIUM,
                "A method that just prints its argument and returns nothing - what return type goes in the blank?")
                .code("public ____ printName(String name) { System.out.println(name); }")
                .solution("void").save();

        step(m, i++, Type.FIX_THE_BUG, "Missing return on one branch", XP_MEDIUM,
                "max() returns `a` when a > b but falls off the end when b >= a - compile error. "
                + "Add the missing return. Output: 7")
                .code("public class Demo {\n    static int max(int a, int b) {\n        if (a > b) {\n            return a;\n        }\n        // missing return b;\n    }\n    public static void main(String[] args) {\n        System.out.println(max(3, 7));\n    }\n}")
                .expected("7").save();

        step(m, i++, Type.MCQ, "Method overloading", XP_SMALL,
                "Java has both `add(int a, int b)` and `add(double a, double b)`. "
                + "The call `add(2.0, 3.0)` will invoke which one?")
                .options("[\"The int version - Java always prefers int\",\"The double version - parameter types match\",\"Compile error - two methods cannot share a name\",\"Runtime error\"]").correct(1).save();

        step(m, i++, Type.LIVE_CODE, "Write multiply()", XP_LARGE,
                "Write a static method `multiply(int a, int b)` that returns a * b. "
                + "In main, call multiply(6, 7) and print the result. Output: 42")
                .code("public class Main {\n    // TODO: static int multiply(int a, int b)\n\n    public static void main(String[] args) {\n        // call multiply(6, 7) and print\n    }\n}")
                .expected("42").save();

        step(m, i++, Type.CHECKPOINT, "Boss: Calculator", XP_CHECKPOINT,
                "Write three static methods: add(int a, int b), subtract(int a, int b), multiply(int a, int b). "
                + "In main, print add(5,3), subtract(10,4), multiply(3,7) each on a separate line.\n\nOutput:\n8\n6\n21")
                .code("public class Main {\n    // TODO: add(), subtract(), multiply()\n\n    public static void main(String[] args) {\n        // print add(5,3), subtract(10,4), multiply(3,7)\n    }\n}")
                .expected("8\n6\n21").save();
    }

    // ---- MODULE 4: Control Flow -----------------------------------------------
    private void seedModuleFour(CourseModule m) {
        int i = 0;
        step(m, i++, Type.CONCEPT, "if / else - making decisions", XP_SMALL,
                "Java evaluates the condition in `( )` after `if`. If `true`, that block runs; "
                + "otherwise the `else` block runs.\n\n"
                + "```java\n"
                + "if (score >= 90)      { /* A */ }\n"
                + "else if (score >= 80) { /* B */ }\n"
                + "else                  { /* F */ }\n"
                + "```\n\n"
                + "Java checks conditions **top-to-bottom** and runs the **first** matching block - "
                + "the rest are skipped.").save();

        step(m, i++, Type.CODE_DEMO, "if/else chain - grade calculator", XP_SMALL,
                "Java evaluates each condition top-to-bottom and runs the first matching block. "
                + "score = 75 falls into the `>= 70` branch. Press Run to confirm.")
                .code("public class Demo {\n    public static void main(String[] args) {\n        int score = 75;\n        if (score >= 90) {\n            System.out.println(\"A\");\n        } else if (score >= 80) {\n            System.out.println(\"B\");\n        } else if (score >= 70) {\n            System.out.println(\"C\");\n        } else {\n            System.out.println(\"F\");\n        }\n    }\n}")
                .expected("C").save();

        step(m, i++, Type.PREDICT_OUTPUT, "Change the score", XP_MEDIUM,
                "Same chain, but now score = 62. Which branch fires? What prints?")
                .code("public class Demo {\n    public static void main(String[] args) {\n        int score = 62;\n        if (score >= 90) System.out.println(\"A\");\n        else if (score >= 80) System.out.println(\"B\");\n        else if (score >= 70) System.out.println(\"C\");\n        else System.out.println(\"F\");\n    }\n}")
                .expected("F").save();

        step(m, i++, Type.CODE_DEMO, "for loop accumulating a sum", XP_SMALL,
                "A `for` loop repeats a block a known number of times: `for (init; condition; update)`. "
                + "Each pass adds i to total. Press Run and trace the accumulation.")
                .code("public class Demo {\n    public static void main(String[] args) {\n        int total = 0;\n        for (int i = 1; i <= 3; i++) {\n            total += i;\n        }\n        System.out.println(total);\n    }\n}")
                .expected("6").save();

        step(m, i++, Type.PREDICT_OUTPUT, "Sum 1 through 4", XP_MEDIUM,
                "Same loop pattern but the condition is `i <= 4`. What is the final total?")
                .code("public class Demo {\n    public static void main(String[] args) {\n        int total = 0;\n        for (int i = 1; i <= 4; i++) total += i;\n        System.out.println(total);\n    }\n}")
                .expected("10").save();

        step(m, i++, Type.CONCEPT, "String comparison and logical AND", XP_SMALL,
                "**Never use `==` to compare Strings.** `==` checks if two references point to the "
                + "*same object in memory* - not the same characters. Always use `.equals()`:\n\n"
                + "```java\n"
                + "if (name.equals(\"Alice\")) { ... }   // correct\n"
                + "if (name == \"Alice\")      { ... }   // WRONG - may fail at runtime\n"
                + "```\n\n"
                + "The **`&&` operator** (logical AND) requires **both** conditions to be true: "
                + "`age >= 18 && hasTicket` is only `true` when the customer is both old enough **and** has a ticket.").save();

        step(m, i++, Type.MCQ, ".equals() vs ==", XP_SMALL,
                "`==` checks if two references point to the same object in memory - NOT the same characters. "
                + "Which must you use to compare String content?")
                .options("[\"==\",\".equals()\",\"Both work identically for Strings\",\".compareTo()\"]").correct(1).save();

        step(m, i++, Type.FIX_THE_BUG, "== on a String object", XP_MEDIUM,
                "`new String(\"Alice\")` is a separate object in memory. `==` compares references, not content, "
                + "so it returns false even though the text matches. Fix the comparison. Output: Hello Alice")
                .code("public class Demo {\n    public static void main(String[] args) {\n        String name = new String(\"Alice\");\n        if (name == \"Alice\") {\n            System.out.println(\"Hello Alice\");\n        } else {\n            System.out.println(\"Not Alice\");\n        }\n    }\n}")
                .expected("Hello Alice").save();

        step(m, i++, Type.FILL_BLANK, "String comparison method", XP_MEDIUM,
                "Fill in the method name that correctly compares a String's content to \"Alice\".")
                .code("if (name._____(\"Alice\")) { System.out.println(\"Hi Alice!\"); }")
                .solution("equals").save();

        step(m, i++, Type.MCQ, "&&: logical AND", XP_SMALL,
                "When does `age >= 18 && hasTicket` evaluate to true?")
                .options("[\"When either condition is true\",\"Only when BOTH conditions are true\",\"When neither condition is true\",\"Only when age > 18\"]").correct(1).save();

        step(m, i++, Type.LIVE_CODE, "Find the maximum in an array", XP_LARGE,
                "Write a static method `max(int[] nums)` that loops through the array and returns the largest value. "
                + "In main, call it with {3, 9, 2, 7, 5} and print the result. Output: 9")
                .code("public class Main {\n    static int max(int[] nums) {\n        // TODO: loop and return largest\n        return 0;\n    }\n    public static void main(String[] args) {\n        System.out.println(max(new int[]{3, 9, 2, 7, 5}));\n    }\n}")
                .expected("9").save();

        step(m, i++, Type.CHECKPOINT, "Boss: FizzBuzz", XP_CHECKPOINT,
                "Print numbers 1 through 6. For multiples of 3 print \"Fizz\", "
                + "for multiples of 5 print \"Buzz\", for multiples of both print \"FizzBuzz\".\n\nOutput:\n1\n2\nFizz\n4\nBuzz\nFizz")
                .code("public class Main {\n    public static void main(String[] args) {\n        // TODO: FizzBuzz 1..6\n    }\n}")
                .expected("1\n2\nFizz\n4\nBuzz\nFizz").save();
    }

    // ---- MODULE 5: Interfaces and Inheritance ---------------------------------
    private void seedModuleFive(CourseModule m) {
        int i = 0;
        step(m, i++, Type.CONCEPT, "extends - inheriting behaviour", XP_SMALL,
                "A child class `extends` a parent and automatically inherits all its fields and methods. "
                + "Java allows extending only **one** class at a time.\n\n"
                + "```java\n"
                + "class Dog extends Animal {\n"
                + "    // Dog inherits everything Animal has\n"
                + "    void bark() { ... }   // plus its own methods\n"
                + "}\n"
                + "```\n\n"
                + "Spring Boot uses this everywhere: `StudentRepository extends JpaRepository` gives you "
                + "`save()`, `findById()`, `findAll()` and 15+ more database methods - **for free**.").save();

        step(m, i++, Type.CODE_DEMO, "extends: inherit methods for free", XP_SMALL,
                "Dog extends Animal, so it automatically inherits breathe(). An empty Dog class gets a "
                + "method it never defined. Spring Boot uses this: `StudentRepository extends JpaRepository` "
                + "gives you save(), findById(), findAll() for free. Press Run.")
                .code("class Animal {\n    String name;\n    void breathe() { System.out.println(name + \" breathes.\"); }\n}\nclass Dog extends Animal {\n    void bark() { System.out.println(name + \" barks!\"); }\n}\npublic class Demo {\n    public static void main(String[] args) {\n        Dog d = new Dog();\n        d.name = \"Rex\";\n        d.breathe();   // inherited from Animal\n        d.bark();\n    }\n}")
                .expected("Rex breathes.\nRex barks!").save();

        step(m, i++, Type.PREDICT_OUTPUT, "Calling an inherited method", XP_MEDIUM,
                "Dog doesn't define breathe(), but Animal does. name was set to \"Rex\". What prints?")
                .code("class Animal {\n    String name;\n    void breathe() { System.out.println(name + \" breathes.\"); }\n}\nclass Dog extends Animal { }\npublic class Demo {\n    public static void main(String[] args) {\n        Dog d = new Dog();\n        d.name = \"Rex\";\n        d.breathe();\n    }\n}")
                .expected("Rex breathes.").save();

        step(m, i++, Type.MCQ, "Why add @Override?", XP_SMALL,
                "You write `void speel()` thinking you are overriding `void speak()` from the parent. "
                + "With @Override present, what happens at compile time?")
                .options("[\"Java silently creates a brand-new method named speel()\",\"Compile error - no method speel() in the parent to override\",\"Java renames speel() to speak() automatically\",\"The parent's speak() is deleted\"]").correct(1).save();

        step(m, i++, Type.PREDICT_OUTPUT, "Overridden method wins", XP_MEDIUM,
                "Cat overrides speak(). The object is a Cat. Which speak() runs?")
                .code("class Animal {\n    void speak() { System.out.println(\"...\"); }\n}\nclass Cat extends Animal {\n    @Override\n    void speak() { System.out.println(\"Meow!\"); }\n}\npublic class Demo {\n    public static void main(String[] args) {\n        Cat c = new Cat();\n        c.speak();\n    }\n}")
                .expected("Meow!").save();

        step(m, i++, Type.CONCEPT, "interface - a contract", XP_SMALL,
                "An **interface** declares *what* a class must do, not *how*. "
                + "It lists method signatures with no bodies. Any class that `implements` it must provide the bodies.\n\n"
                + "```java\n"
                + "interface Shape {\n"
                + "    int area();   // no body - just the contract\n"
                + "}\n"
                + "class Square implements Shape {\n"
                + "    public int area() { return side * side; }  // must implement\n"
                + "}\n"
                + "```\n\n"
                + "Key differences:\n"
                + "- `extends` -> inherits concrete code from **one** parent class\n"
                + "- `implements` -> fulfils a contract from **one or more** interfaces\n\n"
                + "`@Override` confirms you are replacing a parent's method - a typo becomes a **compile error** instead of a silent new method.").save();

        step(m, i++, Type.CODE_DEMO, "interface: a contract every implementer must honour", XP_SMALL,
                "Shape declares area() with no body. Square and Rectangle each provide the body. "
                + "printArea() accepts ANY Shape - it works with both implementations. Press Run.")
                .code("interface Shape {\n    int area();\n}\nclass Square implements Shape {\n    int side;\n    Square(int s) { this.side = s; }\n    public int area() { return side * side; }\n}\nclass Rectangle implements Shape {\n    int w, h;\n    Rectangle(int w, int h) { this.w = w; this.h = h; }\n    public int area() { return w * h; }\n}\npublic class Demo {\n    static void printArea(Shape s) { System.out.println(s.area()); }\n    public static void main(String[] args) {\n        printArea(new Square(4));\n        printArea(new Rectangle(3, 5));\n    }\n}")
                .expected("16\n15").save();

        step(m, i++, Type.MCQ, "extends vs implements", XP_SMALL,
                "Dog inherits concrete code from Animal. Report fulfills the Printable contract. "
                + "Which keyword pairs with which?")
                .options("[\"Dog implements Animal, Report extends Printable\",\"Dog extends Animal, Report implements Printable\",\"Both use implements\",\"Both use extends\"]").correct(1).save();

        step(m, i++, Type.FILL_BLANK, "implements keyword", XP_MEDIUM,
                "A class Printer must fulfil the Printable interface contract. Fill in the keyword.")
                .code("class Printer ________ Printable { public void print() { } }")
                .solution("implements").save();

        step(m, i++, Type.FIX_THE_BUG, "Missing interface method", XP_MEDIUM,
                "Triangle says it implements Shape but has no area() method - compile error. "
                + "Add `public int area() { return base * height / 2; }`. Output: 6")
                .code("interface Shape {\n    int area();\n}\npublic class Triangle implements Shape {\n    int base, height;\n    Triangle(int base, int height) {\n        this.base = base;\n        this.height = height;\n    }\n    // TODO: public int area() { return base * height / 2; }\n    public static void main(String[] args) {\n        Triangle t = new Triangle(4, 3);\n        System.out.println(t.area());\n    }\n}")
                .expected("6").save();

        step(m, i++, Type.LIVE_CODE, "Implement Flyable", XP_LARGE,
                "Create an interface `Flyable` with one method `void fly()`. "
                + "Write a `Bird` class that implements Flyable and prints `Bird flies` in fly(). "
                + "In main, create a Bird and call fly(). Output: Bird flies")
                .code("public class Main {\n    // TODO: interface Flyable with void fly()\n    // TODO: class Bird implements Flyable\n\n    public static void main(String[] args) {\n        // create Bird, call fly()\n    }\n}")
                .expected("Bird flies").save();

        step(m, i++, Type.CHECKPOINT, "Boss: Speaker interface", XP_CHECKPOINT,
                "Write an interface `Speaker` with `void speak()`. "
                + "Implement it in a `Robot` class (prints `Beep!`) and a `Human` class (prints `Hello!`). "
                + "In main, create one of each and call speak() on both.\n\nOutput:\nBeep!\nHello!")
                .code("public class Main {\n    // TODO: interface Speaker\n    // TODO: class Robot implements Speaker\n    // TODO: class Human implements Speaker\n\n    public static void main(String[] args) {\n        // create Robot and Human, call speak() on each\n    }\n}")
                .expected("Beep!\nHello!").save();
    }

    // ---- MODULE 6: Generics and Optional --------------------------------------
    private void seedModuleSix(CourseModule m) {
        int i = 0;
        step(m, i++, Type.CONCEPT, "Generics - type-safe collections", XP_SMALL,
                "Without generics, a `List` accepts *anything* and crashes at runtime with `ClassCastException`. "
                + "**Generics constrain the type at compile time:**\n\n"
                + "```java\n"
                + "List<Student> roster = new ArrayList<>();\n"
                + "roster.add(new Student());   // OK\n"
                + "roster.add(\"oops\");          // compile error - caught before you run\n"
                + "```\n\n"
                + "The `<T>` in `List<T>`, `Optional<T>`, and `JpaRepository<T, ID>` is a **type parameter** "
                + "you fill in with a concrete class.").save();

        step(m, i++, Type.CODE_DEMO, "List<String>: type enforced at compile time", XP_SMALL,
                "Without generics a List accepts anything and crashes at runtime with ClassCastException. "
                + "`List<String>` tells the compiler 'only Strings here' - wrong types are rejected at compile time "
                + "and no manual cast is needed when reading items back. Press Run.")
                .code("import java.util.*;\npublic class Demo {\n    public static void main(String[] args) {\n        List<String> names = new ArrayList<>();\n        names.add(\"Alice\");\n        names.add(\"Bob\");\n        for (String s : names) {\n            System.out.println(s.toUpperCase());\n        }\n    }\n}")
                .expected("ALICE\nBOB").save();

        step(m, i++, Type.MCQ, "What does List<Student> guarantee?", XP_SMALL,
                "You declare `List<Student> roster`. What does the compiler enforce?")
                .options("[\"The list sorts itself automatically\",\"Only Student objects can be added - wrong types are a compile error\",\"The list never throws exceptions\",\"Students are stored more efficiently in memory\"]").correct(1).save();

        step(m, i++, Type.CONCEPT, "Optional<T> - safe null handling", XP_SMALL,
                "`Optional<T>` is a container that either holds a value or holds nothing. "
                + "Spring's `findById()` returns `Optional<Student>` because the student **might not exist**.\n\n"
                + "Instead of returning `null` (and risking `NullPointerException` if the caller forgets to check):\n\n"
                + "| Method | What it does |\n"
                + "|--------|--------------|\n"
                + "| `.orElse(default)` | Return default value if empty |\n"
                + "| `.orElseThrow(() -> new ...)` | Throw an exception if empty |\n"
                + "| `.isPresent()` | Returns `true` if a value is held |\n\n"
                + "Returning `Optional` forces the caller to handle the \"not found\" case explicitly.").save();

        step(m, i++, Type.CODE_DEMO, "Optional: safe null handling", XP_SMALL,
                "findById() returns Optional<Student> because the student might not exist. "
                + "Instead of returning null (risking NullPointerException), Optional forces you to handle both cases. "
                + "Press Run to see orElse in action.")
                .code("import java.util.*;\npublic class Demo {\n    public static void main(String[] args) {\n        Optional<String> present = Optional.of(\"Alice\");\n        Optional<String> empty   = Optional.empty();\n        System.out.println(present.orElse(\"nobody\"));\n        System.out.println(empty.orElse(\"nobody\"));\n    }\n}")
                .expected("Alice\nnobody").save();

        step(m, i++, Type.PREDICT_OUTPUT, "Present Optional - what comes out?", XP_MEDIUM,
                "`Optional.of(42)` holds the value 42. `orElse(0)` only fires if empty. What prints?")
                .code("import java.util.*;\npublic class Demo {\n    public static void main(String[] args) {\n        Optional<Integer> val = Optional.of(42);\n        System.out.println(val.orElse(0));\n    }\n}")
                .expected("42").save();

        step(m, i++, Type.PREDICT_OUTPUT, "Empty Optional - fallback fires", XP_MEDIUM,
                "`Optional.empty()` holds nothing. What does `orElse(\"nobody\")` return?")
                .code("import java.util.*;\npublic class Demo {\n    public static void main(String[] args) {\n        Optional<String> empty = Optional.empty();\n        System.out.println(empty.orElse(\"nobody\"));\n    }\n}")
                .expected("nobody").save();

        step(m, i++, Type.MCQ, "Why does findById return Optional?", XP_SMALL,
                "Spring's `findById(id)` might not find the entity. Why return Optional instead of null?")
                .options("[\"Optional is faster than null\",\"Returning null compiles fine but crashes at runtime if the caller forgets to check; Optional forces the check\",\"JPA cannot return null from a query\",\"Null values are stored differently in PostgreSQL\"]").correct(1).save();

        step(m, i++, Type.FILL_BLANK, "JpaRepository type parameter", XP_MEDIUM,
                "A repository for Student entities with Long primary key. Fill in the entity type.")
                .code("interface StudentRepository extends JpaRepository<____, Long> {}")
                .solution("Student").save();

        step(m, i++, Type.FIX_THE_BUG, "Add the type parameter", XP_MEDIUM,
                "This raw List is unsafe - add `<String>` to both List and ArrayList to make it type-safe. Output: Alice")
                .code("import java.util.*;\npublic class Demo {\n    public static void main(String[] args) {\n        List names = new ArrayList();\n        names.add(\"Alice\");\n        String first = (String) names.get(0);\n        System.out.println(first);\n    }\n}")
                .expected("Alice").save();

        step(m, i++, Type.LIVE_CODE, "find() returning Optional", XP_LARGE,
                "Write a static method `find(String[] arr, String target)` that returns Optional<String>. "
                + "If target is in arr return Optional.of(target), otherwise Optional.empty(). "
                + "In main, call it with {\"Alice\", \"Bob\"} and \"Bob\", print with .orElse(\"Not found\"). Output: Bob")
                .code("import java.util.*;\npublic class Main {\n    static Optional<String> find(String[] arr, String target) {\n        // TODO: loop, return Optional.of(target) if found\n        return Optional.empty();\n    }\n    public static void main(String[] args) {\n        System.out.println(find(new String[]{\"Alice\", \"Bob\"}, \"Bob\").orElse(\"Not found\"));\n    }\n}")
                .expected("Bob").save();

        step(m, i++, Type.CHECKPOINT, "Boss: Optional safe lookup", XP_CHECKPOINT,
                "Write a method `safeName(String value)` using Optional.ofNullable(value). "
                + "If a value is present return \"Found: \" + value, if null return \"Not found\". "
                + "Call it with \"Java\" in main and print. Output: Found: Java")
                .code("import java.util.*;\npublic class Main {\n    static String safeName(String value) {\n        // TODO: Optional.ofNullable(value)\n        return \"\";\n    }\n    public static void main(String[] args) {\n        System.out.println(safeName(\"Java\"));\n    }\n}")
                .expected("Found: Java").save();
    }

    // ---- MODULE 7: Annotations ------------------------------------------------
    private void seedModuleSeven(CourseModule m) {
        int i = 0;
        step(m, i++, Type.CONCEPT, "Annotations - metadata labels", XP_SMALL,
                "An annotation starts with `@` and attaches **instructions** to a class, method, or field. "
                + "It doesn't change your logic - it adds metadata the **framework reads at startup**.\n\n"
                + "```java\n"
                + "@Entity                     // -> creates a DB table\n"
                + "public class Student {\n"
                + "    @Id                     // -> marks the primary key\n"
                + "    private Long id;\n"
                + "}\n"
                + "```\n\n"
                + "When Spring Boot starts, it scans every class for these labels and auto-configures your application.").save();

        step(m, i++, Type.CODE_DEMO, "@Override: the annotation Java itself checks", XP_SMALL,
                "Annotations start with `@` and attach instructions that frameworks read at startup. "
                + "@Override is special - Java verifies the method actually exists in a parent. "
                + "Misspell `speak()` as `speel()` with @Override and you get a compile error instead of a silent bug. Press Run.")
                .code("class Animal {\n    void speak() { System.out.println(\"...\"); }\n}\nclass Dog extends Animal {\n    @Override\n    void speak() { System.out.println(\"Woof!\"); }\n}\npublic class Demo {\n    public static void main(String[] args) {\n        new Dog().speak();\n    }\n}")
                .expected("Woof!").save();

        step(m, i++, Type.MCQ, "What does @Override protect against?", XP_SMALL,
                "You write `void speel()` with @Override thinking you are overriding `speak()`. What happens?")
                .options("[\"Java silently runs speel() when speak() is called\",\"A compile error: no method speel() in any parent class\",\"Java renames speel() to speak() at runtime\",\"The annotation is silently ignored\"]").correct(1).save();

        step(m, i++, Type.FIX_THE_BUG, "@Override on a non-existent method", XP_MEDIUM,
                "@Override here causes a compile error because `greet()` doesn't exist in any parent class. "
                + "Remove the incorrect @Override. Output: Hello")
                .code("public class Demo {\n    @Override\n    static void greet() {\n        System.out.println(\"Hello\");\n    }\n    public static void main(String[] args) {\n        greet();\n    }\n}")
                .expected("Hello").save();

        step(m, i++, Type.CONCEPT, "Spring's everyday annotations", XP_SMALL,
                "Annotations you will use every single day:\n\n"
                + "| Annotation | Where | What it does |\n"
                + "|-----------|-------|--------------|\n"
                + "| `@Entity` | Class | Maps class -> DB table |\n"
                + "| `@Id` | Field | Marks the primary key |\n"
                + "| `@Service` | Class | Registers as a Spring bean |\n"
                + "| `@RestController` | Class | Handles HTTP requests, returns JSON |\n"
                + "| `@GetMapping(\"/path\")` | Method | Maps to HTTP GET |\n"
                + "| `@PostMapping(\"/path\")` | Method | Maps to HTTP POST |\n"
                + "| `@RequestBody` | Parameter | Reads JSON body into a Java object |\n\n"
                + "Forget one and Spring gives a clear error at startup - usually `NoSuchBeanDefinitionException`.").save();

        step(m, i++, Type.MCQ, "@Entity - what it does", XP_SMALL,
                "When Spring Boot starts and sees `@Entity` on a class, what does it do?")
                .options("[\"Creates a Spring bean for the service layer\",\"Maps the class to a database table and manages its schema\",\"Makes the class handle HTTP requests\",\"Marks a field as the primary key\"]").correct(1).save();

        step(m, i++, Type.MCQ, "@Id placement", XP_SMALL,
                "Where do you place @Id to mark a field as the primary key in a JPA entity?")
                .options("[\"On the class declaration, above @Entity\",\"On the field that holds the primary key value\",\"On the constructor\",\"On every field in the class\"]").correct(1).save();

        step(m, i++, Type.MCQ, "Which annotation handles HTTP GET?", XP_SMALL,
                "Which annotation maps a controller method to handle incoming HTTP GET requests?")
                .options("[\"@Entity\",\"@Service\",\"@GetMapping\",\"@Transactional\"]").correct(2).save();

        step(m, i++, Type.FILL_BLANK, "REST controller annotation", XP_MEDIUM,
                "A class that handles REST API requests needs this annotation. Fill in the missing word (no @).")
                .code("@____Controller\npublic class StudentController { }")
                .solution("Rest").save();

        step(m, i++, Type.LIVE_CODE, "Override toString()", XP_LARGE,
                "Write a class `Widget` with a private `String name` field and constructor. "
                + "Add @Override and implement toString() to return the name. "
                + "In main, create Widget(\"Gadget\") and print it - println calls toString() automatically. Output: Gadget")
                .code("public class Widget {\n    private String name;\n    public Widget(String name) { this.name = name; }\n\n    // TODO: @Override toString() to return name\n\n    public static void main(String[] args) {\n        System.out.println(new Widget(\"Gadget\"));\n    }\n}")
                .expected("Gadget").save();

        step(m, i++, Type.CHECKPOINT, "Boss: Annotated Book class", XP_CHECKPOINT,
                "Write a class `Book` with private String title and String author fields, a constructor that sets both. "
                + "Add @Override and implement toString() to return title + \" by \" + author. "
                + "In main, print new Book(\"Dune\", \"Herbert\"). Output: Dune by Herbert")
                .code("public class Book {\n    // TODO: fields, constructor, @Override toString()\n\n    public static void main(String[] args) {\n        System.out.println(new Book(\"Dune\", \"Herbert\"));\n    }\n}")
                .expected("Dune by Herbert").save();
    }

    // ---- MODULE 8: Packages and Imports ---------------------------------------
    private void seedModuleEight(CourseModule m) {
        int i = 0;
        step(m, i++, Type.CONCEPT, "Packages - namespaced folders", XP_SMALL,
                "A **package** is a named namespace that maps directly to a folder on disk. "
                + "It organises your classes and prevents naming conflicts between libraries.\n\n"
                + "```java\n"
                + "package com.example.studentapi;   // must be the FIRST line\n"
                + "```\n\n"
                + "Packages use **reversed domain notation** (`com.example`) to guarantee global uniqueness - "
                + "two teams at different companies cannot accidentally share a package name.").save();

        step(m, i++, Type.CODE_DEMO, "Imports bring in classes from other packages", XP_SMALL,
                "A package is a namespace that maps to a folder on disk. "
                + "`import java.util.List;` tells Java where to find List - without it the compiler can't locate the class. "
                + "java.lang (String, System, Math) is auto-imported; everything else you must import explicitly. Press Run.")
                .code("import java.util.List;\nimport java.util.ArrayList;\n\npublic class Demo {\n    public static void main(String[] args) {\n        List<String> names = new ArrayList<>();\n        names.add(\"Alice\");\n        names.add(\"Bob\");\n        System.out.println(names.size());\n    }\n}")
                .expected("2").save();

        step(m, i++, Type.FIX_THE_BUG, "Missing imports", XP_MEDIUM,
                "List and ArrayList are in java.util but there are no import statements. Add both. Output: 1")
                .code("public class Demo {\n    public static void main(String[] args) {\n        List<String> names = new ArrayList<>();\n        names.add(\"Alice\");\n        System.out.println(names.size());\n    }\n}")
                .expected("1").save();

        step(m, i++, Type.CONCEPT, "Import rules", XP_SMALL,
                "**File structure in every Java file:**\n\n"
                + "```\n"
                + "1. package com.example.student;   <- must come first\n"
                + "2. import java.util.List;         <- then all imports\n"
                + "3. public class StudentService {  <- then the class\n"
                + "```\n\n"
                + "**Packages you will import most:**\n"
                + "- `java.util.*` - `List`, `Map`, `ArrayList`, `Optional`\n"
                + "- `org.springframework.*` - Spring annotations and types\n"
                + "- `jakarta.persistence.*` - JPA/Hibernate\n\n"
                + "`java.lang` (String, System, Math, Integer...) is **always imported automatically** - "
                + "never write `import java.lang.String`.").save();

        step(m, i++, Type.MCQ, "Order in a Java file", XP_SMALL,
                "What is the required order of package declaration, import statements, and class declaration?")
                .options("[\"imports -> package -> class\",\"class -> package -> imports\",\"package -> imports -> class\",\"Order does not matter\"]").correct(2).save();

        step(m, i++, Type.MCQ, "Where does List live?", XP_SMALL,
                "Which import brings in the standard Java List interface?")
                .options("[\"import java.lang.List;\",\"import java.collections.List;\",\"import java.util.List;\",\"import java.io.List;\"]").correct(2).save();

        step(m, i++, Type.FILL_BLANK, "Package declaration keyword", XP_MEDIUM,
                "Fill in the keyword that starts a package declaration at the very top of a Java file.")
                .code("_______ com.example.student;")
                .solution("package").save();

        step(m, i++, Type.MCQ, "Why reverse domain notation?", XP_SMALL,
                "Packages use reversed domain names (com.example.app) rather than plain names (app). Why?")
                .options("[\"It makes code run faster\",\"It guarantees global uniqueness - two teams at different companies cannot accidentally share a package name\",\"Java compilers require lowercase names\",\"It is a Spring Boot convention only\"]").correct(1).save();

        step(m, i++, Type.LIVE_CODE, "Use List<Integer> with imports", XP_LARGE,
                "Import java.util.List and java.util.ArrayList. "
                + "Create a List<Integer> called numbers, add 10, 20, 30, then print its size. Output: 3")
                .code("// TODO: add imports\n\npublic class Main {\n    public static void main(String[] args) {\n        // create List<Integer>, add 10, 20, 30, print size\n    }\n}")
                .expected("3").save();

        step(m, i++, Type.CHECKPOINT, "Boss: List of languages", XP_CHECKPOINT,
                "Import java.util.List and java.util.ArrayList. "
                + "Create a List<String>, add \"Java\", \"Python\", \"Go\" in that order, "
                + "then print the element at index 1. Output: Python")
                .code("// TODO: imports\n\npublic class Main {\n    public static void main(String[] args) {\n        // List<String> with \"Java\", \"Python\", \"Go\" - print index 1\n    }\n}")
                .expected("Python").save();
    }

    // ---- MODULE 9: Lambdas ----------------------------------------------------
    private void seedModuleNine(CourseModule m) {
        int i = 0;
        step(m, i++, Type.CONCEPT, "Lambdas - inline behaviour", XP_SMALL,
                "Before Java 8, passing a small block of code required a full anonymous class. "
                + "A **lambda** does the same thing on one line.\n\n"
                + "```java\n"
                + "// Old way - anonymous class\n"
                + "Runnable r = new Runnable() {\n"
                + "    public void run() { System.out.println(\"Hello!\"); }\n"
                + "};\n\n"
                + "// Lambda way\n"
                + "Runnable r = () -> System.out.println(\"Hello!\");\n"
                + "```\n\n"
                + "`->` separates **parameters** (left) from the **body** (right). "
                + "Same output, far less noise.").save();

        step(m, i++, Type.CODE_DEMO, "Anonymous class vs lambda", XP_SMALL,
                "Before lambdas, passing a small block of behaviour required a whole anonymous class. "
                + "A lambda writes the same thing on one line. `->` separates parameters (left) from the body (right). "
                + "Both produce identical output - the lambda is just less noise. Press Run.")
                .code("interface Greeter {\n    void greet();\n}\npublic class Demo {\n    static void run(Greeter g) { g.greet(); }\n    public static void main(String[] args) {\n        // Old: anonymous class\n        run(new Greeter() {\n            public void greet() { System.out.println(\"Hello from anon!\"); }\n        });\n        // New: lambda\n        run(() -> System.out.println(\"Hello from lambda!\"));\n    }\n}")
                .expected("Hello from anon!\nHello from lambda!").save();

        step(m, i++, Type.PREDICT_OUTPUT, "The lambda line alone", XP_MEDIUM,
                "`() -> System.out.println(\"Hello from lambda!\")` is a zero-parameter lambda. What does it print?")
                .code("interface Greeter { void greet(); }\npublic class Demo {\n    static void run(Greeter g) { g.greet(); }\n    public static void main(String[] args) {\n        run(() -> System.out.println(\"Hello from lambda!\"));\n    }\n}")
                .expected("Hello from lambda!").save();

        step(m, i++, Type.MCQ, "Lambda with two parameters", XP_SMALL,
                "Which is the correct lambda syntax for a BiFunction that adds two ints?")
                .options("[\"() -> a + b\",\"a, b -> a + b\",\"(a, b) -> a + b\",\"a -> b -> a + b\"]").correct(2).save();

        step(m, i++, Type.CODE_DEMO, "orElseThrow with a lambda", XP_SMALL,
                "`orElseThrow` takes a Supplier<Exception> - a zero-parameter lambda that produces an exception. "
                + "If the Optional is present the lambda never fires. If empty it runs and throws. Press Run.")
                .code("import java.util.*;\npublic class Demo {\n    public static void main(String[] args) {\n        Optional<String> present = Optional.of(\"Bob\");\n        String name = present.orElseThrow(() -> new RuntimeException(\"Not found\"));\n        System.out.println(name);\n    }\n}")
                .expected("Bob").save();

        step(m, i++, Type.CONCEPT, "Functional interfaces", XP_SMALL,
                "A lambda can only be used where Java expects a **functional interface** - "
                + "an interface with exactly **one** abstract method. The lambda becomes its implementation.\n\n"
                + "Common built-in functional interfaces:\n\n"
                + "| Interface | Signature | Use |\n"
                + "|-----------|-----------|-----|\n"
                + "| `Runnable` | `void run()` | Run a task |\n"
                + "| `Supplier<T>` | `T get()` | Produce a value |\n"
                + "| `Consumer<T>` | `void accept(T t)` | Consume a value |\n"
                + "| `Predicate<T>` | `boolean test(T t)` | Test a condition |\n"
                + "| `Function<T,R>` | `R apply(T t)` | Transform T -> R |\n\n"
                + "Lambda syntax rules:\n"
                + "- No params: `() -> body`\n"
                + "- One param: `name -> body` *(parens optional)*\n"
                + "- Multiple params: `(a, b) -> body` *(parens **required**)*").save();

        step(m, i++, Type.PREDICT_OUTPUT, "orElseThrow: value is present", XP_MEDIUM,
                "The Optional holds \"Bob\". orElseThrow's lambda never fires when the value is present. What prints?")
                .code("import java.util.*;\npublic class Demo {\n    public static void main(String[] args) {\n        System.out.println(\n            Optional.of(\"Bob\").orElseThrow(() -> new RuntimeException(\"err\")));\n    }\n}")
                .expected("Bob").save();

        step(m, i++, Type.MCQ, "Functional interface rule", XP_SMALL,
                "A lambda can only be used where Java expects a functional interface. What defines a functional interface?")
                .options("[\"It has no methods at all\",\"It has exactly one abstract method\",\"It uses the @FunctionalInterface annotation\",\"It only contains default methods\"]").correct(1).save();

        step(m, i++, Type.FILL_BLANK, "Lambda arrow operator", XP_MEDIUM,
                "Fill in the arrow operator that separates parameters from the body in a lambda.")
                .code("opt.orElseThrow(() ____ new RuntimeException(\"Not found\"));")
                .solution("->").save();

        step(m, i++, Type.FIX_THE_BUG, "Multi-parameter lambda missing parentheses", XP_MEDIUM,
                "A lambda with two or more parameters requires parentheses. Fix `a, b ->` to `(a, b) ->`. Output: 12")
                .code("import java.util.function.*;\npublic class Demo {\n    public static void main(String[] args) {\n        BiFunction<Integer, Integer, Integer> multiply = a, b -> a * b;\n        System.out.println(multiply.apply(3, 4));\n    }\n}")
                .expected("12").save();

        step(m, i++, Type.LIVE_CODE, "Lambda for string validation", XP_LARGE,
                "An interface `Validator` has one method `boolean isValid(String s)`. "
                + "Write a lambda that returns true if s is not empty (!s.isEmpty()). "
                + "Assign it to Validator v, then print v.isValid(\"hello\") and v.isValid(\"\") on separate lines.\n\nOutput:\ntrue\nfalse")
                .code("public class Main {\n    interface Validator {\n        boolean isValid(String s);\n    }\n    public static void main(String[] args) {\n        // TODO: Validator v = s -> ...\n        // System.out.println(v.isValid(\"hello\"));\n        // System.out.println(v.isValid(\"\"));\n    }\n}")
                .expected("true\nfalse").save();

        step(m, i++, Type.CHECKPOINT, "Boss: Execute a Runnable lambda", XP_CHECKPOINT,
                "Write a static method `execute(Runnable r)` that calls r.run(). "
                + "In main, create a lambda that implements Runnable and prints `Lambda running!`, "
                + "then pass it to execute. Output: Lambda running!")
                .code("public class Main {\n    static void execute(Runnable r) {\n        // TODO: call r.run()\n    }\n    public static void main(String[] args) {\n        // TODO: create Runnable lambda and pass to execute\n    }\n}")
                .expected("Lambda running!").save();
    }

    /** Fallback for any future modules without a dedicated seed method. */
    private void seedGeneric(CourseModule m) {
        int i = 0;
        step(m, i++, Type.CONCEPT, "What you'll learn", XP_SMALL,
                m.getDescription() != null ? m.getDescription() : "Key concepts for " + m.getTitle() + ".").save();
        step(m, i++, Type.CHECKPOINT, "Checkpoint: " + m.getTitle(), XP_CHECKPOINT,
                m.getChallengeInstructions() != null ? m.getChallengeInstructions()
                        : "Write Java code that demonstrates the concepts in this module.")
                .code(m.getChallengeTemplateCode() != null ? m.getChallengeTemplateCode()
                        : "public class Main {\n    public static void main(String[] args) {\n\n    }\n}").save();
    }

    private StepBuilder step(CourseModule m, int order, Type type, String title, int xp, String body) {
        LessonStep s = new LessonStep(m, order, type, title, xp);
        s.setBodyMarkdown(body);
        return new StepBuilder(s);
    }

    /** Tiny fluent builder so seeding reads cleanly and only sets what each step needs. */
    private final class StepBuilder {
        private final LessonStep s;
        StepBuilder(LessonStep s) { this.s = s; }
        StepBuilder code(String c) { s.setCode(c); return this; }
        StepBuilder solution(String v) { s.setSolution(v); return this; }
        StepBuilder options(String json) { s.setOptions(json); return this; }
        StepBuilder correct(int idx) { s.setCorrectIndex(idx); return this; }
        StepBuilder expected(String out) { s.setExpectedOutput(out); return this; }
        LessonStep save() { return stepRepository.save(s); }
    }
}
