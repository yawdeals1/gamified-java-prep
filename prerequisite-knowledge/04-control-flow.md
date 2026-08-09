# Control Flow

Control flow is how you make a program make decisions and repeat actions. Without it, code runs top to bottom, doing the exact same thing every time. With it, the program can react to different situations.

---

## `if` / `else if` / `else`

The most fundamental decision tool. Java evaluates a condition — if it is `true`, the block runs; if it is `false`, it is skipped.

```java
int age = 20;

if (age >= 18) {
    System.out.println("Adult");
} else {
    System.out.println("Minor");
}
```

### Multiple Conditions

```java
int score = 75;

if (score >= 90) {
    System.out.println("A");
} else if (score >= 80) {
    System.out.println("B");
} else if (score >= 70) {
    System.out.println("C");
} else {
    System.out.println("F");
}
```

Java checks each condition top to bottom and runs the *first* block whose condition is `true`. The rest are skipped.

---

## Comparison Operators

These produce a `boolean` (`true` or `false`) and are used inside conditions:

| Operator | Meaning | Example |
|---|---|---|
| `==` | Equal to | `age == 18` |
| `!=` | Not equal to | `age != 0` |
| `>` | Greater than | `score > 90` |
| `<` | Less than | `score < 50` |
| `>=` | Greater than or equal | `age >= 18` |
| `<=` | Less than or equal | `age <= 65` |

### Comparing Strings

Do **not** use `==` to compare Strings. Use `.equals()`:

```java
String name = "Alice";

if (name.equals("Alice")) {      // correct
    System.out.println("Hello, Alice");
}

if (name == "Alice") {           // unreliable — do not do this
    ...
}
```

`==` on objects checks whether they are the exact same object in memory, not whether they contain the same text.

---

## Logical Operators

Combine multiple conditions:

| Operator | Meaning | Example |
|---|---|---|
| `&&` | AND — both must be true | `age >= 18 && hasPaid` |
| `\|\|` | OR — at least one must be true | `isAdmin \|\| isOwner` |
| `!` | NOT — inverts the boolean | `!isEnrolled` |

```java
boolean hasTicket = true;
boolean isVIP = false;

if (hasTicket && isVIP) {
    System.out.println("VIP entrance");
} else if (hasTicket) {
    System.out.println("General entrance");
} else {
    System.out.println("No entry");
}
```

---

## String Methods Used in Conditions

The Spring Boot service layer uses these frequently:

```java
String name = "  Alice  ";

name.isBlank()           // true if empty or only whitespace
name.isEmpty()           // true if length is 0 (does not count whitespace)
name.equals("Alice")     // true if identical text
name.trim()              // "Alice" — removes leading/trailing whitespace
```

In the course you will see patterns like:

```java
if (name != null && !name.isBlank()) {
    student.setName(name);
}
```

Read: "if name is not null AND name is not blank, update the student's name."

---

## `for` Loop

Repeats a block of code a set number of times.

```java
for (int i = 0; i < 5; i++) {
    System.out.println("Count: " + i);
}
// Prints: Count: 0, Count: 1, Count: 2, Count: 3, Count: 4
```

The three parts of a `for` loop:
1. `int i = 0` — initialise a counter
2. `i < 5` — keep looping while this is true
3. `i++` — increment the counter after each loop (`i++` means `i = i + 1`)

---

## Enhanced `for` Loop (For-Each)

Iterates over every item in a collection — much simpler when you do not need the index:

```java
List<String> names = List.of("Alice", "Bob", "Charlie");

for (String name : names) {
    System.out.println(name);
}
// Alice
// Bob
// Charlie
```

Read `for (String name : names)` as "for each String named `name` in `names`."

---

## `while` Loop

Repeats as long as a condition is true. Use when you do not know in advance how many iterations you need.

```java
int attempts = 0;

while (attempts < 3) {
    System.out.println("Attempt " + (attempts + 1));
    attempts++;
}
```

---

## `return` — Exiting a Method Early

`return` immediately exits the current method and sends a value back to the caller. You can use it mid-method to exit early when a condition is met.

```java
public String getGrade(int score) {
    if (score < 0 || score > 100) {
        return "Invalid score";   // exits immediately
    }
    if (score >= 90) {
        return "A";
    }
    return "B or below";          // only reached if score is 0–89
}
```

In the Spring Boot service layer, early returns (or early exceptions) are used to guard against invalid states:

```java
public Student addStudent(Student student) {
    if (studentRepository.existsByEmail(student.getEmail())) {
        throw new IllegalStateException("Email already taken");  // exits early
    }
    return studentRepository.save(student);
}
```

---

## Throwing Exceptions

When something goes wrong, Java uses exceptions to signal the error. You `throw` an exception object:

```java
throw new RuntimeException("Student not found with id: " + id);
throw new IllegalStateException("Email already taken: " + email);
```

When an exception is thrown:
- The method stops immediately
- The exception travels up the call stack until something catches it
- If nothing catches it, the program prints the error and stops

In Spring Boot, the framework catches exceptions from your service and controller and converts them into HTTP error responses. You will see this in the course.

---

## `null` — The Absence of a Value

`null` means a reference variable holds nothing — it does not point to any object.

```java
String name = null;  // name exists but holds no value

System.out.println(name.length());  // NullPointerException — cannot call methods on null
```

Always check for `null` before using a reference:

```java
if (name != null) {
    System.out.println(name.length());
}
```

You will see `!= null` checks throughout the Spring Boot service layer to avoid crashing when optional fields are not provided.
