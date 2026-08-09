# Annotations

## What Is an Annotation?

An annotation is a label you attach to a class, method, field, or parameter. It starts with `@` and carries a message — usually to a framework like Spring Boot — about how that piece of code should be treated.

```java
@Entity
public class Student {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;
}
```

The annotations here — `@Entity`, `@Id`, `@Column` — do not change the logic of the code. They add *metadata*: information that Spring Boot reads at startup to configure your application.

Think of annotations like sticky notes on your code. You are not changing the code; you are leaving instructions for the framework.

---

## How Annotations Work (The Big Picture)

You write an annotation. At startup, Spring Boot scans your code, finds the annotation, and acts on it.

```
Your Class with @Entity annotation
          ↓
    Spring Boot scans
          ↓
    "This class maps to a DB table — I'll create one"
          ↓
    Hibernate generates CREATE TABLE SQL
```

You never write the code that *reads* annotations — the framework does. Your job is just to place the right annotation in the right place.

---

## Annotation Syntax

### No Arguments

```java
@Override
@Entity
@Service
@Transactional
```

Just the name. No parentheses needed when there are no arguments.

### With Arguments

```java
@Table(name = "student")
@Column(nullable = false, unique = true)
@RequestMapping(path = "api/v1/students")
```

Arguments go inside parentheses as `name = value` pairs.

### Single-Value Shorthand

When an annotation has exactly one argument and it is named `value`, you can skip the `value =` part:

```java
@PathVariable("id")       // same as @PathVariable(value = "id")
@RequestParam("name")     // same as @RequestParam(value = "name")
```

---

## Annotations You Will Use in the Course

### JPA / Database Annotations

| Annotation | Where it goes | What it does |
|---|---|---|
| `@Entity` | Class | Marks this class as a database table |
| `@Table(name = "...")` | Class | Sets the table name in the database |
| `@Id` | Field | Marks this field as the primary key |
| `@GeneratedValue(strategy = ...)` | Field | Tells JPA to auto-generate the ID value |
| `@Column(...)` | Field | Customises the column (name, nullable, unique, etc.) |

### Spring Component Annotations

These all mean "create a bean from this class and add it to the Spring container." They differ only in what they communicate about purpose:

| Annotation | Where it goes | Communicates |
|---|---|---|
| `@Component` | Class | Generic Spring-managed component |
| `@Service` | Class | Business logic layer |
| `@Repository` | Class or interface | Data access layer |
| `@RestController` | Class | HTTP REST API controller |
| `@Configuration` | Class | Contains bean definitions |

### Web / HTTP Annotations

| Annotation | Where it goes | What it does |
|---|---|---|
| `@RequestMapping(path = "...")` | Class | Sets the base URL path for all methods |
| `@GetMapping` | Method | Handles HTTP GET requests |
| `@PostMapping` | Method | Handles HTTP POST requests |
| `@PutMapping` | Method | Handles HTTP PUT requests |
| `@DeleteMapping` | Method | Handles HTTP DELETE requests |
| `@PathVariable` | Parameter | Extracts a value from the URL path (`/students/{id}`) |
| `@RequestBody` | Parameter | Reads the JSON request body and converts it to a Java object |
| `@RequestParam` | Parameter | Reads a query parameter from the URL (`?name=Alice`) |

### Transaction Annotation

| Annotation | Where it goes | What it does |
|---|---|---|
| `@Transactional` | Method or class | Wraps the method in a database transaction |

### Utility Annotations

| Annotation | Where it goes | What it does |
|---|---|---|
| `@Override` | Method | Confirms this method overrides a parent method |
| `@SpringBootApplication` | Main class | Combines three setup annotations in one |
| `@Bean` | Method | Declares that this method produces a Spring bean |

---

## Annotations Can Stack

Multiple annotations on the same element are fine:

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE)
private Long id;
```

```java
@GetMapping("/{id}")
public ResponseEntity<Student> getStudentById(@PathVariable Long id) { ... }
```

---

## Where Annotations Are Defined

Every annotation is a Java type defined somewhere. The annotations in this course come from:

- `jakarta.persistence.*` — JPA annotations (`@Entity`, `@Id`, `@Column`, etc.)
- `org.springframework.stereotype.*` — Spring component annotations (`@Service`, `@Repository`)
- `org.springframework.web.bind.annotation.*` — Web annotations (`@RestController`, `@GetMapping`, etc.)
- `org.springframework.transaction.annotation.*` — `@Transactional`

You do not need to memorise the packages — VS Code will suggest the correct import automatically when you type the annotation name. Just pick the right one from the list.

---

## A Common Mistake — Forgetting the Annotation

If you forget `@Service` on a class that another class depends on, Spring cannot find the bean and crashes on startup:

```
NoSuchBeanDefinitionException: No qualifying bean of type 'StudentService'
```

The fix is always the same: add the missing annotation. This is one of the most common beginner errors in Spring Boot.

---

## Summary

Annotations are instructions embedded in your code that the framework reads to configure itself. You will not always understand *exactly* what happens behind the scenes when you add one — and that is fine. The important thing is knowing:

1. What each annotation means (this file)
2. Where to place it (shown in each course module)
3. What breaks if you forget it (a startup error with a clear message)

As you work through the course, these annotations will become muscle memory.
