# Classes and Objects

## The Core Idea

A **class** is a blueprint. An **object** is something built from that blueprint.

Think of a class like an architectural drawing for a house. The drawing itself is not a house — but you can use it to build as many houses as you want. Each house built from the drawing is an object (also called an *instance*).

```java
// The blueprint
class Student {
    String name;
    String email;
    int age;
}

// Building objects from the blueprint
Student alice = new Student();
Student bob   = new Student();
```

`alice` and `bob` are two separate objects, both created from the same `Student` blueprint. Each has its own `name`, `email`, and `age`.

---

## Fields

Fields are the data that each object holds — the variables defined inside a class.

```java
class Student {
    String name;   // field
    String email;  // field
    int age;       // field
}
```

Every object of type `Student` gets its own copy of these three fields.

---

## Constructors

A constructor is a special method that runs when you create a new object with `new`. Its job is to set up the object's initial state.

```java
class Student {
    String name;
    String email;
    int age;

    // Constructor — same name as the class, no return type
    Student(String name, String email, int age) {
        this.name = name;    // this.name = the field; name = the parameter
        this.email = email;
        this.age = age;
    }
}
```

### `this`

`this` refers to the current object. In the constructor above, `this.name` means "the `name` field of this specific object." It distinguishes the field from the constructor parameter, which happens to share the same name.

### Creating Objects with a Constructor

```java
Student alice = new Student("Alice Smith", "alice@example.com", 22);
```

`new Student(...)` runs the constructor with those three arguments and returns a fully set-up `Student` object. That object is stored in the variable `alice`.

---

## Methods

Methods are the behaviours an object can perform — functions defined inside a class.

```java
class Student {
    String name;
    String email;
    int age;

    Student(String name, String email, int age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // A method — takes no arguments, returns a String
    String introduce() {
        return "Hi, I'm " + name + " and I'm " + age + " years old.";
    }

    // A method that takes an argument
    boolean isOlderThan(int otherAge) {
        return this.age > otherAge;
    }
}
```

Calling a method on an object:

```java
Student alice = new Student("Alice", "alice@example.com", 22);
String message = alice.introduce();       // "Hi, I'm Alice and I'm 22 years old."
boolean result = alice.isOlderThan(18);   // true
```

The dot (`.`) is how you access a field or method on an object.

---

## Access Modifiers

Access modifiers control who can see and use a field or method.

| Modifier | Who can access it |
|---|---|
| `public` | Anyone, from anywhere |
| `private` | Only code inside the same class |
| `protected` | Same class and subclasses |
| *(none)* | Same package only |

In Spring Boot, fields are almost always `private`, and you provide `public` methods to read and write them. This is called **encapsulation** — hiding the internal data and controlling access through methods.

```java
class Student {
    private String name;   // only Student's own code can touch this
    private String email;
    private int age;

    public Student(String name, String email, int age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // Public method to READ the name (getter)
    public String getName() {
        return name;
    }

    // Public method to WRITE the name (setter)
    public void setName(String name) {
        this.name = name;
    }
}
```

### Getters and Setters

The `getName()` / `setName()` pattern is called getters and setters. By convention:
- Getter: `get` + FieldName (capitalised) → `getName()`, `getEmail()`, `getAge()`
- Setter: `set` + FieldName (capitalised) → `setName()`, `setEmail()`, `setAge()`

These appear in every entity class in the Spring Boot course. Jackson (the JSON library) uses them to convert objects to/from JSON.

---

## `void` Return Type

When a method does not return a value, its return type is `void`:

```java
public void printDetails() {
    System.out.println(name + " — " + email);
    // no return statement needed
}
```

Setters are always `void` because their job is to store a value, not return one.

---

## `static` Members

A `static` field or method belongs to the class itself, not to any particular object. You call it on the class name, not on an instance.

```java
class MathHelper {
    static int add(int a, int b) {
        return a + b;
    }
}

int result = MathHelper.add(3, 4);  // 7 — no object needed
```

The most important `static` method in this course is `main`:

```java
public static void main(String[] args) {
    // Java starts here
}
```

`main` is static because Java needs to call it before any objects exist.

---

## Putting It Together — The `Student` Class

Here is what the complete `Student` class looks like in the Spring Boot course:

```java
public class Student {
    private Long id;
    private String name;
    private String email;
    private Integer age;

    // No-args constructor (required by JPA)
    protected Student() {}

    // Constructor for creating new students
    public Student(String name, String email, Integer age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // Getters
    public Long getId()      { return id; }
    public String getName()  { return name; }
    public String getEmail() { return email; }
    public Integer getAge()  { return age; }

    // Setters
    public void setName(String name)   { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setAge(Integer age)    { this.age = age; }
}
```

Every piece of this class — fields, constructors, getters, setters, access modifiers — is explained in this file. When you see it in the course, nothing should be new.
