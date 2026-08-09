package com.gamifiedjava.config;

import com.gamifiedjava.model.CourseModule;
import com.gamifiedjava.model.QuizQuestion;
import com.gamifiedjava.repository.ModuleRepository;
import com.gamifiedjava.repository.QuizQuestionRepository;
import com.gamifiedjava.service.GamificationService;
import com.gamifiedjava.service.LessonStepService;
import com.gamifiedjava.service.ModuleService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ModuleService moduleService;
    private final GamificationService gamificationService;
    private final LessonStepService lessonStepService;
    private final ModuleRepository moduleRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    public DataInitializer(ModuleService moduleService,
                           GamificationService gamificationService,
                           LessonStepService lessonStepService,
                           ModuleRepository moduleRepository,
                           QuizQuestionRepository quizQuestionRepository) {
        this.moduleService = moduleService;
        this.gamificationService = gamificationService;
        this.lessonStepService = lessonStepService;
        this.moduleRepository = moduleRepository;
        this.quizQuestionRepository = quizQuestionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        moduleService.seedModules();
        gamificationService.seedAchievements();
        seedQuizQuestions();
        lessonStepService.seedSteps();
    }

    private void seedQuizQuestions() {
        if (quizQuestionRepository.count() > 0) return;

        seedModule1Questions();
        seedModule2Questions();
        seedModule3Questions();
        seedModule4Questions();
        seedModule5Questions();
        seedModule6Questions();
        seedModule7Questions();
        seedModule8Questions();
        seedModule9Questions();
    }

    private CourseModule mod(String slug) {
        return moduleRepository.findBySlug(slug).orElse(null);
    }

    private void saveQ(CourseModule m, String q, String[] opts, int correct, String explanation, String difficulty) {
        QuizQuestion qq = new QuizQuestion();
        qq.setModule(m);
        qq.setQuestionText(q);
        qq.setOptions(toJson(opts));
        qq.setCorrectIndex(correct);
        qq.setExplanation(explanation);
        qq.setDifficulty(difficulty);
        quizQuestionRepository.save(qq);
    }

    private String toJson(String[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(arr[i].replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private void seedModule1Questions() {
        CourseModule m = mod("variables-and-data-types");
        if (m == null) return;
        saveQ(m, "Which of these is a valid Java variable declaration?", new String[]{"int 1stPlace = 1;", "int firstPlace = 1;", "int first-place = 1;", "int first.place = 1;"}, 1, "Variable names cannot start with a digit, and cannot contain hyphens or dots.", "easy");
        saveQ(m, "What is the difference between `int` and `Integer` in Java?", new String[]{"int is a primitive, Integer is a reference type that can be null", "Integer is faster than int", "int can hold decimals, Integer cannot", "There is no difference"}, 0, "int is a primitive (cannot be null). Integer is a wrapper class that allows null values.", "easy");
        saveQ(m, "What does the `L` suffix mean in `long population = 8000000000L`?", new String[]{"It marks the variable as final", "It tells Java this is a long literal, not an int", "It means the value is stored in lowercase", "It's optional when the value is large"}, 1, "Java requires L at the end of a long literal to distinguish it from an int literal.", "easy");
        saveQ(m, "Which keyword makes a variable unchangeable after assignment?", new String[]{"static", "constant", "final", "const"}, 2, "The `final` keyword prevents reassignment. By convention final variables use ALL_CAPS naming.", "easy");
        saveQ(m, "What will `var name = \"Alice\";` do?", new String[]{"Create a dynamically-typed variable", "Cause a compile error", "Infer that name is a String at compile time", "Create a variable with no type"}, 2, "var infers the type at compile time. name is still fixed as a String.", "medium");
        saveQ(m, "Which of the following is NOT a primitive type in Java?", new String[]{"boolean", "String", "char", "double"}, 1, "String is a class (reference type), not a primitive. The eight primitives are: byte, short, int, long, float, double, boolean, char.", "easy");
        saveQ(m, "What happens if you try to assign a String to an int variable?", new String[]{"The String is converted to a number automatically", "The program crashes at runtime", "The code does not compile", "The int becomes null"}, 2, "Java is statically typed — once a variable is declared as int, it can only hold int values.", "easy");
        saveQ(m, "Spring Boot uses wrapper types like Long and Integer for entity fields because:", new String[]{"They are faster than primitives", "They can be null, representing 'no value provided'", "They use less memory", "Databases require them"}, 1, "Wrapper types allow null in database columns where a value might not be provided.", "medium");
    }

    private void seedModule2Questions() {
        CourseModule m = mod("classes-and-objects");
        if (m == null) return;
        saveQ(m, "What is a constructor?", new String[]{"A method that destroys an object", "A special method that runs when creating an object with new", "A method that returns the class name", "A static method that creates objects"}, 1, "A constructor initializes the object's state when it's created with new.", "easy");
        saveQ(m, "What does `this` refer to inside a constructor?", new String[]{"The class itself", "The current object being created", "The parent class", "The constructor parameter"}, 1, "this refers to the current object, used to distinguish fields from parameters.", "easy");
        saveQ(m, "A getter method is always:", new String[]{"void", "static", "public and returns the field's type", "private"}, 2, "Getters are public methods that return the value of a private field.", "easy");
        saveQ(m, "If a field is marked `private`, who can access it?", new String[]{"Any class in the same package", "Only code inside the same class", "Any class anywhere", "Only subclasses"}, 1, "Private fields/methods are only accessible within the same class.", "easy");
        saveQ(m, "What does the `static` keyword mean?", new String[]{"The member belongs to the class, not to any instance", "The member is private", "The member cannot be changed", "The member is inherited"}, 0, "Static members belong to the class itself. You call them on the class name, not on an object.", "medium");
        saveQ(m, "Why is `main` declared as `public static void main(String[] args)`?", new String[]{"So it can be called before any objects exist", "To make it run faster", "Because Java requires it for threading", "To prevent inheritance"}, 0, "main is static because Java needs to call it before creating any objects.", "medium");
    }

    private void seedModule3Questions() {
        CourseModule m = mod("methods");
        if (m == null) return;
        saveQ(m, "What is method overloading?", new String[]{"Writing a method that takes too many parameters", "Defining multiple methods with the same name but different parameters", "Overriding a method from a parent class", "Calling a method multiple times"}, 1, "Overloading means same method name, different parameter lists (type or count).", "easy");
        saveQ(m, "What does `void` mean as a return type?", new String[]{"The method returns null", "The method returns no value", "The method is empty", "The method is private"}, 1, "void means the method performs work but does not return a value.", "easy");
        saveQ(m, "What is method chaining?", new String[]{"Calling methods in a specific order", "Calling a method on the result of another method, on one line", "Defining methods that call each other", "Using multiple threads to run methods"}, 1, "Method chaining chains method calls: object.method1().method2() — each dot calls a method on the returned object.", "medium");
        saveQ(m, "When a method's `return` statement executes, what happens?", new String[]{"The method continues to the next line", "The method exits immediately with the returned value", "Java saves the return value and continues", "The program restarts"}, 1, "return exits the method immediately and sends the value back to the caller.", "easy");
        saveQ(m, "What distinguishes a parameter from an argument?", new String[]{"They are the same thing", "Parameter = the variable in the method definition; Argument = the value passed when calling", "Argument = the variable in the definition; Parameter = the value passed", "Parameters are for static methods only"}, 1, "Parameters are defined in the method signature; arguments are the actual values passed when calling.", "medium");
    }

    private void seedModule4Questions() {
        CourseModule m = mod("control-flow");
        if (m == null) return;
        saveQ(m, "What is the correct way to compare two Strings in Java?", new String[]{"str1 == str2", "str1.equals(str2)", "str1 = str2", "compare(str1, str2)"}, 1, "Use .equals() to compare string content. == checks if they're the same object in memory.", "easy");
        saveQ(m, "What does `&&` mean in Java?", new String[]{"OR — at least one condition must be true", "AND — both conditions must be true", "NOT — inverts a boolean", "XOR — only one must be true"}, 1, "&& is the logical AND operator: condition1 && condition2 is true only if both are true.", "easy");
        saveQ(m, "What does `i++` do?", new String[]{"Increments i by 1 after the current operation", "Increments i by 1 before the current operation", "Adds 1 to i permanently", "Creates a new variable"}, 0, "i++ is post-increment: it uses the current value, then adds 1.", "easy");
        saveQ(m, "What does the enhanced for-loop `for (String name : names)` do?", new String[]{"Loops with an index counter", "Iterates over each element in the names collection", "Creates a new String for each name", "Only works with arrays"}, 1, "The for-each loop iterates over every element in a collection without needing an index.", "easy");
        saveQ(m, "What happens when you throw an exception?", new String[]{"The program continues to the next line", "The method stops and the exception travels up the call stack", "The program prints a warning but continues", "Java logs it internally"}, 1, "Throwing an exception exits the method immediately. The exception propagates up until something catches it.", "medium");
        saveQ(m, "What is a NullPointerException?", new String[]{"An exception thrown when you try to use a null reference", "An exception for invalid method arguments", "An exception for division by zero", "An exception for buffer overflow"}, 0, "NullPointerException occurs when you call a method or access a field on a null reference.", "easy");
    }

    private void seedModule5Questions() {
        CourseModule m = mod("interfaces-and-inheritance");
        if (m == null) return;
        saveQ(m, "Which keyword is used by a class to inherit from another class?", new String[]{"implements", "extends", "inherits", "super"}, 1, "extends is used for class-to-class inheritance. implements is for class-to-interface.", "easy");
        saveQ(m, "An interface defines:", new String[]{"Complete implementations of every method", "Only method signatures (a contract)", "Private helper methods", "Concrete field values"}, 1, "An interface defines method signatures without bodies — a contract for implementing classes.", "easy");
        saveQ(m, "Can a class extend multiple classes?", new String[]{"Yes, using commas", "No, Java single-inheritance only", "Yes, using the extends keyword multiple times", "Only if they're interfaces"}, 1, "Java does not allow multiple class inheritance. A class can only extend one parent class.", "medium");
        saveQ(m, "What does `@Override` do?", new String[]{"It's a comment for documentation", "It tells Java 'I am intentionally replacing a parent method' — Java gives a compile error if misspelled", "It forces the method to run faster", "It marks the method as the final version"}, 1, "Override is an annotation that confirms you're overriding a parent method. Java will error if the method doesn't actually override anything.", "easy");
        saveQ(m, "What is the difference between an abstract class and an interface?", new String[]{"There is no difference", "Abstract classes can have implemented methods; interfaces traditionally only had signatures", "Interfaces can have fields; abstract classes cannot", "Abstract classes are faster"}, 1, "Abstract classes can have both implemented and abstract methods. Interfaces define a contract. (Java 8+ interfaces can have default methods.)", "medium");
    }

    private void seedModule6Questions() {
        CourseModule m = mod("generics");
        if (m == null) return;
        saveQ(m, "What problem do generics solve?", new String[]{"Memory management", "Type safety — catch type mismatches at compile time", "Code indentation", "Database connectivity"}, 1, "Generics let you catch type mismatches at compile time instead of runtime ClassCastException.", "easy");
        saveQ(m, "What does `List<Student>` mean?", new String[]{"A list that can hold any type of object", "A list that can only hold Student objects", "A list of Student objects and their subclasses", "A list that holds Student strings"}, 1, "List<Student> is a list that can only contain Student objects. Java checks this at compile time.", "easy");
        saveQ(m, "What does the diamond operator `<>` do?", new String[]{"Creates a diamond-shaped data structure", "Lets Java infer the type from the left side — saves repeating it", "Marks generic methods", "Creates a new type parameter"}, 1, "The diamond <> lets the compiler infer the type from the variable declaration: List<Student> list = new ArrayList<>();", "easy");
        saveQ(m, "In `JpaRepository<Student, Long>`, what do the two type parameters mean?", new String[]{"The entity type and its primary key type", "The table name and column count", "The package and class name", "The query type and return type"}, 0, "JpaRepository<EntityType, IdType> — Student is the entity, Long is the type of its @Id field.", "medium");
        saveQ(m, "What does `Optional<Student>` help prevent?", new String[]{"Slow database queries", "NullPointerException by forcing you to handle the 'no value' case", "Memory leaks", "SQL injection"}, 1, "Optional forces you to handle both cases: when a value is present and when it's absent.", "medium");
    }

    private void seedModule7Questions() {
        CourseModule m = mod("annotations");
        if (m == null) return;
        saveQ(m, "What is an annotation in Java?", new String[]{"A type of comment that is ignored by the compiler", "A label starting with @ that adds metadata to code for the framework to read", "A method that runs automatically", "A special class that cannot be instantiated"}, 1, "Annotations are metadata labels (@...) that frameworks like Spring Boot read to configure behavior.", "easy");
        saveQ(m, "What does `@Entity` do?", new String[]{"Makes a class a REST controller", "Marks a class as a database table", "Creates a new instance of the class", "Enables debugging"}, 1, "@Entity marks a class as a JPA entity that maps to a database table.", "easy");
        saveQ(m, "What does `@GetMapping` mean?", new String[]{"Handles HTTP PUT requests", "Handles HTTP GET requests", "Maps a Java object to JSON", "Gets a database connection"}, 1, "@GetMapping maps a controller method to HTTP GET requests.", "easy");
        saveQ(m, "Where would you place `@Id`?", new String[]{"On the class declaration", "On the field that is the primary key", "On the constructor", "On the repository interface"}, 1, "@Id marks a field as the database primary key, typically on a Long field.", "easy");
        saveQ(m, "What error do you get if you forget `@Service` on a service class?", new String[]{"NoSuchBeanDefinitionException — Spring can't find the bean", "ClassNotFoundException", "Compile error", "NullPointerException"}, 0, "If you forget a stereotype annotation (@Service, @Repository, etc.), Spring can't create the bean and throws NoSuchBeanDefinitionException on startup.", "medium");
    }

    private void seedModule8Questions() {
        CourseModule m = mod("packages-and-imports");
        if (m == null) return;
        saveQ(m, "What is the purpose of a Java package?", new String[]{"To compress the code", "To organize classes and avoid name collisions", "To make the code run faster", "To encrypt the class files"}, 1, "Packages organize classes into namespaces, preventing name collisions between different libraries.", "easy");
        saveQ(m, "Where does the `package` statement go in a Java file?", new String[]{"After the imports", "At the very top, before imports", "Anywhere in the file", "After the class declaration"}, 1, "The package declaration must be the first statement in a Java file, before any imports.", "easy");
        saveQ(m, "What package does `List` belong to?", new String[]{"java.lang", "java.util", "java.io", "java.collections"}, 1, "List (and ArrayList, Optional, Map, etc.) is in java.util.", "easy");
        saveQ(m, "Why do package names use reversed domain names like `com.example.studentapi`?", new String[]{"To make them longer", "To ensure uniqueness across the entire Java ecosystem", "Because Java requires it", "To match folder structure rules"}, 1, "Reverse domain naming ensures uniqueness. No two organizations can accidentally use the same package name.", "medium");
        saveQ(m, "What happens if you import two classes with the same name?", new String[]{"Java uses the last import", "You must use fully-qualified names to disambiguate", "Java randomly picks one", "Compile error"}, 1, "When two imports conflict, you must use the fully-qualified name (e.g., java.util.List or java.awt.List) to specify which one you mean.", "medium");
    }

    private void seedModule9Questions() {
        CourseModule m = mod("lambdas");
        if (m == null) return;
        saveQ(m, "What is a lambda expression?", new String[]{"A named block of code that runs once", "A compact way to pass behavior (a function) as an argument", "A new type of loop", "A way to declare variables"}, 1, "A lambda is a compact way to represent a function (behavior) that can be passed to another method.", "easy");
        saveQ(m, "What does `->` mean in a lambda?", new String[]{"Assignment operator", "Arrow that separates parameters (left) from body (right)", "Comparison operator", "Method reference"}, 1, "The arrow operator -> separates the parameter list from the body in a lambda expression.", "easy");
        saveQ(m, "What is a functional interface?", new String[]{"An interface with zero methods", "An interface with exactly one abstract method", "An interface with only default methods", "Any interface that contains 'function' in its name"}, 1, "A functional interface has exactly one abstract method, which is what a lambda implements.", "medium");
        saveQ(m, "What does `orElseThrow(() -> new RuntimeException(...))` do?", new String[]{"Always throws an exception", "Returns the value if present, otherwise calls the lambda to produce an exception to throw", "Creates a new Optional", "Catches exceptions"}, 1, "orElseThrow returns the Optional's value if present, or calls the supplier lambda to create the exception if empty.", "medium");
        saveQ(m, "Lambda for `(a, b) -> a + b` with a multi-line body would be:", new String[]{"(a, b) -> { int sum = a + b; return sum; }", "(a, b) -> a + b; return;", "a, b -> a + b", "(a, b) -> int sum = a + b; return sum;"}, 0, "Multi-line lambda bodies need curly braces and an explicit return statement.", "medium");
    }
}
