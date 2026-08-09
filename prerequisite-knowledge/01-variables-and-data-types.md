# Variables and Data Types

## What Is a Variable?

A variable is a named container that holds a value. You give it a name, declare what kind of value it holds, and then use it throughout your code.

```java
int age = 22;
```

This single line does three things:
1. `int` — declares the *type* (this variable holds whole numbers)
2. `age` — gives it a name
3. `= 22` — stores the value 22 inside it

You can then use `age` anywhere you would have written `22`.

---

## Java's Built-In Data Types

Java requires you to declare the type of every variable upfront. The type tells Java how much memory to reserve and what operations are allowed.

### Whole Numbers

| Type | Size | Range | Example |
|---|---|---|---|
| `int` | 32-bit | –2 billion to 2 billion | `int score = 100;` |
| `long` | 64-bit | Much larger range | `long population = 8000000000L;` |

Notice the `L` at the end of the `long` value — Java requires it to distinguish a `long` literal from an `int`.

In Spring Boot, database primary keys (ID columns) are almost always `Long` because databases can have more rows than `int` can count.

### Decimal Numbers

| Type | Size | Use case | Example |
|---|---|---|---|
| `double` | 64-bit | Most decimal math | `double price = 9.99;` |
| `float` | 32-bit | Less precise, less memory | `float temp = 36.6f;` |

### Text

```java
String name = "Alice";
```

`String` is not a primitive — it is a class (you will learn about classes next). It holds any sequence of characters. Text values are always wrapped in double quotes.

### True / False

```java
boolean isEnrolled = true;
boolean hasPaid = false;
```

A `boolean` holds exactly one of two values: `true` or `false`. Used constantly in conditions (`if` statements).

### Single Characters

```java
char grade = 'A';
```

A `char` holds a single character, wrapped in single quotes. Rarely used directly; `String` is far more common.

---

## Primitive vs Reference Types

Java has two categories of types:

**Primitives** — simple values stored directly in the variable:
`int`, `long`, `double`, `float`, `boolean`, `char`, `byte`, `short`

**Reference types** — the variable holds a *reference* (a pointer) to an object stored elsewhere in memory:
`String`, `List`, `Student`, and every other class

The practical difference you will notice: primitives can never be `null`. Reference types can.

```java
int age = null;      // compile error — int cannot be null
Integer age = null;  // fine — Integer is the reference type wrapper for int
```

Spring Boot uses the wrapper classes (`Integer`, `Long`, `Double`, `Boolean`) for entity fields so that null is possible — a null field means "no value provided."

---

## Declaring vs Assigning

You can split declaration and assignment:

```java
String email;           // declared — no value yet
email = "alice@x.com";  // assigned
```

Or do both at once (more common):

```java
String email = "alice@x.com";
```

Once a variable has a type, it keeps that type forever. You cannot do this:

```java
int age = 22;
age = "twenty-two";  // compile error — age is an int, not a String
```

This is what "statically typed" means — types are checked at compile time, not at runtime.

---

## `var` — Type Inference (Java 10+)

When the type is obvious from the right side, you can write `var` instead:

```java
var name = "Alice";   // Java infers: this is a String
var age = 22;         // Java infers: this is an int
var students = new ArrayList<Student>(); // Java infers the full type
```

The type is still fixed at compile time — `var` just saves you from typing it out. You will see both styles in Java code.

---

## Constants

If a value should never change, declare it `final`:

```java
final int MAX_STUDENTS = 100;
MAX_STUDENTS = 200; // compile error — cannot reassign a final variable
```

By convention, constants are written in ALL_CAPS with underscores.

---

## Quick Reference

```java
// Whole numbers
int score = 95;
long id = 1234567890L;

// Decimals
double price = 19.99;

// Text
String name = "Alice Smith";

// True/false
boolean active = true;

// Wrapper types (nullable versions of primitives)
Integer age = null;
Long studentId = 42L;
```

These types appear constantly in the Spring Boot course — especially `String`, `Long`, `Integer`, and `boolean`.
