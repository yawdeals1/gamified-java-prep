package com.gamifiedjava.service;

import com.gamifiedjava.model.CourseModule;
import com.gamifiedjava.model.ModuleProgress;
import com.gamifiedjava.repository.ModuleProgressRepository;
import com.gamifiedjava.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final ModuleProgressRepository progressRepository;
    private final GamificationService gamificationService;

    @Value("${prerequisite.path:prerequisite-knowledge}")
    private String prereqPath;

    public ModuleService(ModuleRepository moduleRepository,
                         ModuleProgressRepository progressRepository,
                         GamificationService gamificationService) {
        this.moduleRepository = moduleRepository;
        this.progressRepository = progressRepository;
        this.gamificationService = gamificationService;
    }

    @Transactional
    public void seedModules() {
        if (moduleRepository.count() > 0) {
            ensureProgressRows();
            return;
        }

        String[][] modules = {
            {"Variables and Data Types", "variables-and-data-types", "Java's built-in types, primitives vs references, constants, and var", "01-variables-and-data-types.md"},
            {"Classes and Objects", "classes-and-objects", "Fields, constructors, methods, access modifiers, getters/setters, static", "02-classes-and-objects.md"},
            {"Methods", "methods", "Anatomy, return types, parameters, overloading, chaining, static methods", "03-methods.md"},
            {"Control Flow", "control-flow", "if/else, loops, comparisons, logical operators, exceptions, null", "04-control-flow.md"},
            {"Interfaces and Inheritance", "interfaces-and-inheritance", "extends, implements, abstract classes, method overriding", "05-interfaces-and-inheritance.md"},
            {"Generics", "generics", "Type parameters, generic classes/interfaces, Optional, List, Map, diamond operator", "06-generics.md"},
            {"Annotations", "annotations", "What annotations are, syntax, JPA/Spring annotations, stacking", "07-annotations.md"},
            {"Packages and Imports", "packages-and-imports", "Package declarations, imports, java.util, Spring Boot packages", "08-packages-and-imports.md"},
            {"Lambdas", "lambdas", "Functional interfaces, lambda syntax, orElseThrow, CommandLineRunner", "09-lambdas.md"}
        };

        for (int i = 0; i < modules.length; i++) {
            CourseModule m = new CourseModule(modules[i][0], modules[i][1], modules[i][2], i + 1, 100);
            m.setContentMarkdown(loadMarkdown(modules[i][3]));
            m.setChallengeInstructions(getChallengeInstructions(i + 1));
            m.setChallengeTemplateCode(getChallengeTemplate(i + 1));
            moduleRepository.save(m);

            ModuleProgress prog = new ModuleProgress(m);
            progressRepository.save(prog);
        }
    }

    private void ensureProgressRows() {
        List<CourseModule> modules = moduleRepository.findAllByOrderByOrderIndexAsc();
        for (CourseModule module : modules) {
            if (progressRepository.findByModuleId(module.getId()).isEmpty()) {
                progressRepository.save(new ModuleProgress(module));
            }
        }
    }

    private String loadMarkdown(String filename) {
        try {
            Path mdPath = Path.of(prereqPath, filename).toAbsolutePath().normalize();
            if (Files.exists(mdPath)) {
                return Files.readString(mdPath);
            }
            return "# " + filename.replace(".md", "").replace("-", " ").substring(3) + "\n\nContent not found at: " + mdPath;
        } catch (IOException e) {
            return "# Error\n\nCould not load content: " + e.getMessage();
        }
    }

    public List<CourseModule> getAllModules() {
        return moduleRepository.findAllByOrderByOrderIndexAsc();
    }

    public List<ModuleProgress> getAllProgress() {
        return progressRepository.findAllByOrderByIdAsc();
    }

    public CourseModule getBySlug(String slug) {
        return moduleRepository.findBySlug(slug).orElse(null);
    }

    public ModuleProgress getProgress(Integer moduleId) {
        return progressRepository.findByModuleId(moduleId).orElse(null);
    }

    @Transactional
    public void unlockNextModule(Integer currentModuleId) {
        CourseModule current = moduleRepository.findById(currentModuleId).orElse(null);
        if (current == null) return;

        CourseModule next = moduleRepository.findBySlug(getSlugForOrder(current.getOrderIndex() + 1)).orElse(null);
        if (next == null) return;

        ModuleProgress nextProg = progressRepository.findByModuleId(next.getId()).orElse(null);
        if (nextProg != null && "locked".equals(nextProg.getStatus())) {
            nextProg.setStatus("available");
            nextProg.setUpdatedAt(java.time.LocalDateTime.now());
            progressRepository.save(nextProg);
        }
    }

    @Transactional
    public ModuleProgress completeModuleReading(Integer moduleId) {
        ModuleProgress prog = getProgress(moduleId);
        if (prog != null && ("available".equals(prog.getStatus()) || "reading".equals(prog.getStatus()))) {
            prog.setStatus("quiz_ready");
            prog.setUpdatedAt(java.time.LocalDateTime.now());
            progressRepository.save(prog);
            gamificationService.addXp("module_read", 50, "Finished reading CourseModule " + moduleId);
        }
        return prog;
    }

    @Transactional
    public ModuleProgress markModuleComplete(Integer moduleId) {
        ModuleProgress prog = getProgress(moduleId);
        if (prog != null) {
            prog.setStatus("completed");
            prog.setCompletedAt(java.time.LocalDateTime.now());
            prog.setUpdatedAt(java.time.LocalDateTime.now());
            progressRepository.save(prog);
            gamificationService.addXp("module_completed", 100, "Completed CourseModule " + moduleId);
            unlockNextModule(moduleId);
        }
        return prog;
    }

    private String getSlugForOrder(int order) {
        return switch (order) {
            case 1 -> "variables-and-data-types";
            case 2 -> "classes-and-objects";
            case 3 -> "methods";
            case 4 -> "control-flow";
            case 5 -> "interfaces-and-inheritance";
            case 6 -> "generics";
            case 7 -> "annotations";
            case 8 -> "packages-and-imports";
            case 9 -> "lambdas";
            default -> null;
        };
    }

    private String getChallengeInstructions(int moduleNum) {
        return switch (moduleNum) {
            case 1 -> "Write a Java class called `Person` with:\n- A `String` field `name`\n- An `int` field `age`\n- A `double` field `height`\n- A `final` field `species` set to \"Homo sapiens\"\n- A constructor that sets name, age, and height\n- Getters for all fields";
            case 2 -> "Create a `Car` class with private fields, a constructor, getters, and a `displayInfo()` method. Then create a `Garage` class that holds a list of cars and a method to find the fastest car.";
            case 3 -> "Write a `Calculator` class with:\n- An overloaded `add` method (int + int, double + double, int + double)\n- A static `square` method\n- A `void` method `printOperations` that prints all stored results\n- Method chaining: `add(3).add(4).printResults()` pattern";
            case 4 -> "Implement a `GradeBook` class that:\n- Takes an array of scores\n- Returns letter grades (A/B/C/D/F) using if/else\n- Calculates the average using a for loop\n- Throws an exception for invalid scores (<0 or >100)\n- Handles null input gracefully";
            case 5 -> "Create an interface `Drawable` with a `draw()` method. Implement it in `Circle` and `Rectangle` classes. Then write a method that accepts a list of `Drawable` objects and calls `draw()` on each one.";
            case 6 -> "Write a generic `Storage<T>` class that stores one item of type T. Then create a generic method `swap<T>` that swaps two elements in a List<T>. Use Optional to handle the case when storage is empty.";
            case 7 -> "Given a skeleton Spring Boot entity class, add the correct JPA and validation annotations: @Entity, @Id, @GeneratedValue, @Column with nullable/unique constraints. Also add @Override where appropriate.";
            case 8 -> "Organize the following classes into the correct package structure: User (entity layer), UserRepository (data layer), UserService (service layer), UserController (controller layer). Write the package declarations and import statements.";
            case 9 -> "Convert the following anonymous class to a lambda:\n```\nbutton.addActionListener(new ActionListener() {\n    public void actionPerformed(ActionEvent e) {\n        System.out.println(\"Clicked!\");\n    }\n});\n```\nThen write a method that uses `orElseThrow` with a lambda. Finally, create a list of strings and sort it using a lambda.";
            default -> "Write Java code that demonstrates the concepts covered in this CourseModule.";
        };
    }

    private String getChallengeTemplate(int moduleNum) {
        return switch (moduleNum) {
            case 1 -> "public class Person {\n    // TODO: Add fields\n\n    // TODO: Add constructor\n\n    // TODO: Add getters\n}";
            case 2 -> "public class Car {\n    // TODO: Implement Car class\n}";
            case 3 -> "public class Calculator {\n    // TODO: Implement Calculator with overloaded methods\n}";
            case 4 -> "public class GradeBook {\n    // TODO: Implement GradeBook\n}";
            case 5 -> "interface Drawable {\n    // TODO: Define interface\n}";
            case 6 -> "public class Storage<T> {\n    // TODO: Implement generic Storage\n}";
            case 7 -> "// TODO: Add annotations to make this a JPA entity\npublic class Product {\n    private Long id;\n    private String name;\n    private String email;\n}";
            case 8 -> "// TODO: Write the package declaration and imports\npublic class UserController {\n    // ...\n}";
            case 9 -> "import java.util.*;\n\npublic class LambdaPractice {\n    public static void main(String[] args) {\n        // TODO: Convert anonymous class to lambda\n        // TODO: Use orElseThrow with lambda\n        // TODO: Sort list with lambda\n    }\n}";
            default -> "public class Challenge {\n    public static void main(String[] args) {\n        // Your code here\n    }\n}";
        };
    }
}
