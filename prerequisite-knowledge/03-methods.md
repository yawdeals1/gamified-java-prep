# Methods

## What Is a Method?

A method is a named block of code that performs a specific task. You define it once and call it as many times as you need.

```java
// Defining a method
int add(int a, int b) {
    return a + b;
}

// Calling it
int result = add(3, 4);  // result = 7
```

Without methods, you would have to copy and paste the same logic everywhere it is needed. Methods let you write it once and reuse it by name.

---

## Anatomy of a Method

```java
public int add(int a, int b) {
    return a + b;
}
```

Every part has a name:

```
public   int      add    (int a, int b)   { return a + b; }
  │        │        │          │                  │
  │        │        │          │                  └─ body
  │        │        │          └─ parameters (inputs)
  │        │        └─ method name
  │        └─ return type (what the method gives back)
  └─ access modifier (who can call this)
```

### Access Modifier

Controls who can call the method. The same modifiers from classes apply:

| Modifier | Who can call it |
|---|---|
| `public` | Anyone |
| `private` | Only code inside the same class |
| `protected` | Same class and subclasses |

In Spring Boot, service and controller methods are `public` so other layers can call them. Internal helper methods are `private`.

### Return Type

The type of value the method sends back to the caller.

```java
String getName() {
    return "Alice";       // returns a String
}

int getAge() {
    return 22;            // returns an int
}

boolean isEnrolled() {
    return true;          // returns a boolean
}
```

The type you declare must match what you actually `return`. Java will not compile if they differ.

### `void` — No Return Value

When a method does not send anything back, its return type is `void`. You can omit `return`, or use a bare `return;` to exit early.

```java
void printName(String name) {
    System.out.println(name);
    // no return statement needed
}
```

Setters are always `void`:

```java
public void setName(String name) {
    this.name = name;
}
```

### Parameters

Parameters are the inputs a method accepts. You list them inside the parentheses, each with a type and a name.

```java
void greet(String name, int age) {
    System.out.println("Hello, " + name + ". You are " + age + ".");
}
```

When you call the method, you pass **arguments** — the actual values for those parameters:

```java
greet("Alice", 22);   // name = "Alice", age = 22
```

Parameters and arguments are often used interchangeably in conversation, but technically:
- **Parameter** — the variable in the method definition
- **Argument** — the value you pass when calling the method

### No Parameters

If a method needs no inputs, leave the parentheses empty:

```java
void sayHello() {
    System.out.println("Hello!");
}

sayHello();  // called with empty parentheses too
```

---

## The `return` Statement

`return` does two things at once: it exits the method and sends a value back to the caller.

```java
int max(int a, int b) {
    if (a > b) {
        return a;   // exits immediately with the value of a
    }
    return b;       // only reached if a <= b
}
```

Once `return` executes, no more code in that method runs. This makes it useful for exiting early when a condition is met — a pattern used constantly in the Spring Boot service layer:

```java
public Student addStudent(Student student) {
    if (studentRepository.existsByEmail(student.getEmail())) {
        throw new IllegalStateException("Email already taken");  // exits early
    }
    return studentRepository.save(student);  // only reached if email is free
}
```

---

## Method Overloading

You can define multiple methods with the same name as long as their parameter lists differ. Java picks the right one based on the arguments you pass.

```java
void print(String message) {
    System.out.println(message);
}

void print(String message, int times) {
    for (int i = 0; i < times; i++) {
        System.out.println(message);
    }
}

print("Hello");        // calls the first one
print("Hello", 3);     // calls the second one
```

This is called **overloading**. The method name is the same; the signature (parameter types and count) differs.

---

## Calling Methods on Objects

Methods defined inside a class are called on an instance of that class using the dot operator:

```java
Student alice = new Student("Alice", "alice@x.com", 22);

String name = alice.getName();    // calls getName() on the alice object
alice.setName("Alice Smith");     // calls setName() on the alice object
```

The object before the dot is called the **receiver** — the object the method runs on.

---

## Static Methods

A `static` method belongs to the class itself, not to any instance. You call it on the class name:

```java
class MathUtils {
    static int square(int n) {
        return n * n;
    }
}

int result = MathUtils.square(5);  // 25 — no object needed
```

Static methods cannot access instance fields (they have no `this`). They are useful for utility operations that do not depend on any object's state.

The `main` method is always static because Java calls it before creating any objects:

```java
public static void main(String[] args) {
    SpringApplication.run(StudentapiApplication.class, args);
}
```

---

## Method Chaining

When a method returns an object, you can immediately call another method on the result — all on one line. This is called **method chaining**.

```java
ResponseEntity.status(HttpStatus.CREATED).body(saved);
```

Read left to right:
1. `ResponseEntity.status(HttpStatus.CREATED)` — calls `status()`, returns a builder object
2. `.body(saved)` — calls `body()` on that builder, returns the final `ResponseEntity`

You will see this throughout the Spring Boot course. It is not magic — each `.` is just another method call on the object returned by the previous one.

Another example from the service layer:

```java
studentRepository.findById(id)
                 .orElseThrow(() -> new RuntimeException("Not found"));
```

1. `findById(id)` — returns an `Optional<Student>`
2. `.orElseThrow(...)` — called on that `Optional`, either returns the `Student` or throws

---

## Methods You Will See in the Course

| Method signature | Where | What it does |
|---|---|---|
| `public List<Student> getAllStudents()` | Service | Returns every student from the DB |
| `public Student addStudent(Student student)` | Service | Validates and saves a student |
| `public void deleteStudent(Long id)` | Service | Deletes a student by ID |
| `public String getName()` | Entity | Returns the student's name |
| `public void setName(String name)` | Entity | Updates the student's name |
| `public ResponseEntity<Student> getStudentById(@PathVariable Long id)` | Controller | Handles GET /students/{id} |

Each of these follows the exact same structure: access modifier, return type, name, parameters, body. Once you can read one, you can read them all.
