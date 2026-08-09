# Packages and Imports

## What Is a Package?

A package is a named folder that organises Java classes. Just as you organise files into folders on your computer, Java organises classes into packages.

```
com.example.studentapi           ← package (maps to a folder on disk)
└── StudentapiApplication.java   ← class inside that package

com.example.studentapi.student   ← sub-package
├── Student.java
├── StudentRepository.java
├── StudentService.java
└── StudentController.java
```

### Why Packages Exist

1. **Organisation** — related classes live together
2. **Avoiding name collisions** — two libraries can both have a class called `List` as long as they are in different packages
3. **Access control** — the default (package-private) access modifier only allows classes in the same package to see each other

### Package Declaration

The very first line of every Java file declares its package:

```java
package com.example.studentapi.student;

public class Student {
    // ...
}
```

The package name must match the folder structure on disk. `com.example.studentapi.student` maps to the folder path `com/example/studentapi/student/`.

### Naming Convention

Package names are:
- All lowercase
- Reverse domain name notation — e.g., `com.example` (reversed from `example.com`)
- Dot-separated levels, each level being a folder

This convention ensures uniqueness across the entire Java ecosystem. Two different companies cannot accidentally create packages with the same name if they use their own domain.

---

## What Is an Import?

When you use a class from a different package, Java needs to know where to find it. You declare this at the top of the file with an `import` statement.

```java
package com.example.studentapi.student;

import jakarta.persistence.Entity;        // import the @Entity annotation
import jakarta.persistence.Id;            // import the @Id annotation
import java.util.List;                    // import the List class
import java.util.Optional;               // import the Optional class

@Entity
public class Student {
    @Id
    private Long id;
}
```

Without the import, Java does not know what `@Entity` or `List` refers to and refuses to compile.

### Wildcard Imports

You can import all classes from a package at once using `*`:

```java
import jakarta.persistence.*;  // imports Entity, Id, Table, Column, etc.
```

This is convenient but considered poor style in large projects because it hides which classes are actually used. VS Code manages imports for you, so this rarely matters in practice.

---

## VS Code Manages Imports Automatically

You almost never type import statements by hand. When you use a class name in your code:

1. VS Code underlines it in red (cannot find the class)
2. Hover over it, or press `Ctrl+.`
3. A list of matching classes from different packages appears
4. Click the correct one — VS Code adds the import line automatically

If VS Code adds the wrong import (e.g., there are two classes called `List`), delete the import and try again, choosing the other option.

The class you almost always want is the one from `java.util` or `org.springframework.*`, not third-party alternatives.

---

## The `java.util` Package — What You Will Import

The standard library's utility classes live in `java.util`. You will import from here constantly:

| Class | What it is |
|---|---|
| `java.util.List` | Ordered collection interface |
| `java.util.ArrayList` | The standard `List` implementation |
| `java.util.Optional` | A value-or-nothing container |
| `java.util.Map` | Key-value pairs |
| `java.util.HashMap` | The standard `Map` implementation |

---

## Spring Boot's Packages — Where Things Come From

When you see an annotation or class in the course, it comes from one of these packages:

| Package | Contains |
|---|---|
| `org.springframework.stereotype` | `@Service`, `@Repository`, `@Component` |
| `org.springframework.web.bind.annotation` | `@RestController`, `@GetMapping`, `@RequestBody`, etc. |
| `org.springframework.http` | `ResponseEntity`, `HttpStatus` |
| `org.springframework.data.jpa.repository` | `JpaRepository` |
| `org.springframework.transaction.annotation` | `@Transactional` |
| `jakarta.persistence` | `@Entity`, `@Table`, `@Id`, `@Column`, `@GeneratedValue` |

You do not need to memorise these — VS Code will offer the import. But recognising `org.springframework.*` and `jakarta.persistence.*` helps you pick the right one when two options appear.

---

## The Default Package (Avoid It)

If you create a Java file without a `package` declaration, it belongs to the "default package." This seems simpler but causes problems:

- Classes in the default package cannot be imported by named packages
- Spring Boot's `@ComponentScan` may not find them
- It is considered bad practice in any non-trivial project

Always declare a package at the top of every file. In this course, every file lives in `com.example.studentapi` or one of its sub-packages.

---

## Putting It Together

Here is a complete file showing package declaration and imports in context:

```java
package com.example.studentapi.student;          // 1. Declare this file's package

import jakarta.persistence.Entity;                // 2. Import what you use
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity                                           // 3. Use the imported annotations
@Table(name = "student")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;
    private String email;
    private Integer age;
}
```

The structure is always:
1. `package` statement (one line, at the very top)
2. `import` statements (as many as needed)
3. The class itself

VS Code enforces this order and auto-sorts imports — you just write the code.
