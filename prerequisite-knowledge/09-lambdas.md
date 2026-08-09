# Lambdas

## The Problem They Solve

Sometimes you need to pass a small piece of behaviour — a few lines of code — into a method as an argument. Before Java 8, the only way to do this was to create a whole class just to hold that behaviour. Lambdas let you write it inline, in a fraction of the space.

---

## Starting Point — Passing Behaviour the Old Way

Imagine a method that takes a task and runs it:

```java
interface Task {
    void run();
}

void execute(Task task) {
    task.run();
}
```

To call `execute`, you need a `Task`. Before lambdas you had two options:

**Option A — A full class:**
```java
class PrintHello implements Task {
    public void run() {
        System.out.println("Hello!");
    }
}

execute(new PrintHello());
```

**Option B — An anonymous class (shorter, but still verbose):**
```java
execute(new Task() {
    public void run() {
        System.out.println("Hello!");
    }
});
```

Both work, but both are noisy for what is essentially one line of logic.

---

## The Lambda — Writing Only What Matters

A lambda strips away everything but the parameters and the body:

```java
execute(() -> System.out.println("Hello!"));
```

That is the same thing as Option B above. The `->` arrow separates inputs (left) from behaviour (right).

```
() -> System.out.println("Hello!")
│         │
│         └─ body: what to do
└─ parameters: none (empty parentheses)
```

---

## Lambda Syntax

```
(parameters) -> body
```

### No Parameters

```java
() -> System.out.println("Hello!")
```

### One Parameter

Parentheses are optional with a single parameter:

```java
name -> System.out.println(name)
// or
(name) -> System.out.println(name)
```

### Multiple Parameters

Parentheses are required:

```java
(a, b) -> a + b
```

### Multi-Line Body

Use curly braces when the body is more than one expression:

```java
(a, b) -> {
    int sum = a + b;
    System.out.println("Sum: " + sum);
    return sum;
}
```

Single-expression bodies do not need curly braces or `return` — the result of the expression is returned automatically:

```java
(a, b) -> a + b   // implicitly returns a + b
```

---

## Functional Interfaces — Why Lambdas Work

A lambda can only be used where Java expects a **functional interface** — an interface with exactly one abstract method.

```java
interface Task {
    void run();   // exactly one method — this is a functional interface
}
```

Java sees a lambda and matches it to the single method of the expected interface. The lambda *becomes* the implementation of that method.

If an interface has two or more abstract methods, it is not a functional interface and lambdas cannot be used with it.

Common functional interfaces from the Java standard library:

| Interface | Method signature | Used when |
|---|---|---|
| `Runnable` | `void run()` | Running a task with no input/output |
| `Supplier<T>` | `T get()` | Producing a value with no input |
| `Consumer<T>` | `void accept(T t)` | Consuming a value with no output |
| `Function<T, R>` | `R apply(T t)` | Transforming one value to another |
| `Predicate<T>` | `boolean test(T t)` | Testing a condition |

---

## Lambdas in the Course

### `orElseThrow` — Supplying an Exception

```java
studentRepository.findById(id)
    .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
```

`orElseThrow` expects a `Supplier<Exception>` — a functional interface whose single method takes no arguments and returns something. The lambda `() -> new RuntimeException(...)` is that supplier.

Read it as: "If the Optional is empty, call this lambda to produce the exception to throw."

The lambda is only evaluated if the Optional is empty — if the student is found, the lambda is never called.

### `CommandLineRunner` — Running Code at Startup

```java
@Bean
CommandLineRunner commandLineRunner(StudentRepository repository) {
    return args -> {
        Student alice = new Student("Alice", "alice@example.com", 22);
        repository.save(alice);
        System.out.println(repository.findAll());
    };
}
```

`CommandLineRunner` is a functional interface with one method: `void run(String... args)`. The lambda `args -> { ... }` is the implementation of that method. Spring Boot calls it once after the application finishes starting up.

The `String... args` parameter (command-line arguments) is rarely used, so the body ignores `args` and goes straight to the logic.

### `List.of` and `saveAll` — Not a Lambda, But Often Nearby

```java
repository.saveAll(List.of(alice, bob));
```

`List.of(...)` is just a static method that creates an immutable list from the arguments you pass. Not a lambda, but it appears next to them often enough to mention.

---

## Lambdas vs Anonymous Classes — Side by Side

```java
// Anonymous class
repository.findById(id).orElseThrow(new Supplier<RuntimeException>() {
    public RuntimeException get() {
        return new RuntimeException("Student not found with id: " + id);
    }
});

// Lambda — identical behaviour
repository.findById(id).orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
```

They compile to the same thing. The lambda is just less noise.

---

## The `->` Is Not Scary

When you see `->` in the course, ask yourself three questions:

1. **What goes in?** — everything to the left of `->`
2. **What comes out / happens?** — everything to the right of `->`
3. **When is it called?** — whatever the receiving method decides (immediately, lazily, repeatedly)

For `orElseThrow(() -> new RuntimeException(...))`:
1. Nothing goes in — `()`
2. A new `RuntimeException` comes out
3. It is called only if the Optional is empty

That is all a lambda is — a compact way to hand a small piece of behaviour to something else.
