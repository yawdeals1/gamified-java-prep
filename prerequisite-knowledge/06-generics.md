# Generics

## The Problem Generics Solve

Imagine you want a list that holds students. Without generics, you would use a plain `List` that accepts *anything*:

```java
List students = new ArrayList();
students.add(new Student("Alice", "alice@x.com", 22));
students.add("oops, a String got in here");  // Java allows this — no type check

Student s = (Student) students.get(0);  // you must cast manually
Student s2 = (Student) students.get(1); // ClassCastException at runtime — crashes!
```

The bug only appears when the program is running, not when you compile it.

Generics fix this by letting you declare what type a collection (or class) works with:

```java
List<Student> students = new ArrayList<Student>();
students.add(new Student("Alice", "alice@x.com", 22));
students.add("oops, a String");  // compile error — caught immediately

Student s = students.get(0);  // no cast needed — Java already knows it's a Student
```

The mistake is caught at compile time, not at runtime. The code is safer and cleaner.

---

## The Angle Bracket Syntax `<T>`

The `<T>` you see in code is a **type parameter** — a placeholder that gets filled in with a real type when you use the class.

`T` is just a convention for "Type." You could write `<Banana>` and it would work the same way. Other common single-letter names:
- `T` — Type (general)
- `E` — Element (collections)
- `K` — Key (maps)
- `V` — Value (maps)
- `N` — Number

---

## Generic Classes

A class can be written to work with *any* type:

```java
class Box<T> {
    private T contents;

    public void put(T item) {
        this.contents = item;
    }

    public T get() {
        return contents;
    }
}
```

`T` is a stand-in. When you create a `Box`, you specify what `T` actually is:

```java
Box<String> nameBox = new Box<>();
nameBox.put("Alice");
String name = nameBox.get();   // returns a String — no cast

Box<Integer> ageBox = new Box<>();
ageBox.put(22);
int age = ageBox.get();        // returns an Integer — no cast
```

The same `Box` class works for `String`, `Integer`, `Student`, or any other type.

---

## Generic Interfaces

Interfaces can be generic too. `List<E>` is an interface:

```java
// Simplified version of how List<E> is defined internally
interface List<E> {
    void add(E element);
    E get(int index);
    int size();
}
```

When you write `List<Student>`, Java replaces every `E` with `Student`:
- `void add(Student element)`
- `Student get(int index)`

---

## Common Generic Types You Will See

### `List<T>`

An ordered collection of items.

```java
List<String> names = new ArrayList<>();
names.add("Alice");
names.add("Bob");

String first = names.get(0);    // "Alice"
int count = names.size();       // 2
```

`List` is an interface. `ArrayList` is the most common implementation — use it unless you have a specific reason not to.

### `Optional<T>`

A container that either holds one value or holds nothing. Used to avoid `null`.

```java
Optional<Student> result = studentRepository.findById(1L);

// Option A — get the value, throw if absent
Student student = result.orElseThrow();

// Option B — get the value or a default
Student student = result.orElse(new Student("Unknown", "", 0));

// Option C — check first
if (result.isPresent()) {
    Student student = result.get();
}
```

`findById()` in Spring Data returns `Optional<Student>` because the student with that ID might not exist. `Optional` forces you to handle the "not found" case rather than accidentally getting a `NullPointerException`.

### `Map<K, V>`

Stores key-value pairs. Both the key type and value type are specified.

```java
Map<String, Integer> scores = new HashMap<>();
scores.put("Alice", 95);
scores.put("Bob", 87);

int aliceScore = scores.get("Alice");  // 95
```

In the course, `Map.of("error", "Student not found")` creates a small map used in error responses.

---

## Generics in Spring Boot — The Key Appearances

### `JpaRepository<Student, Long>`

```java
public interface StudentRepository extends JpaRepository<Student, Long> {
```

Two type parameters:
- `Student` — the entity type this repository manages
- `Long` — the type of the entity's primary key

Spring Data uses these to generate the right SQL. `findById(Long id)` becomes a query on the `student` table, and the result is cast to `Student` automatically.

### `ResponseEntity<List<Student>>`

```java
public ResponseEntity<List<Student>> getAllStudents() {
    List<Student> students = studentService.getAllStudents();
    return ResponseEntity.ok(students);
}
```

`ResponseEntity<T>` wraps an HTTP response. The `T` is the type of the response body.
- `ResponseEntity<Student>` — body is a single student
- `ResponseEntity<List<Student>>` — body is a list of students
- `ResponseEntity<Void>` — no body (used for DELETE responses)

### `List<Student>` as a Return Type

```java
public List<Student> getAllStudents() {
    return studentRepository.findAll();
}
```

`findAll()` is defined in `JpaRepository` as `List<T> findAll()`. Because `T` is `Student`, this becomes `List<Student> findAll()` — returning a list of Student objects.

---

## The Diamond Operator `<>`

When creating a generic object, Java can infer the type from the variable declaration. Instead of:

```java
List<Student> students = new ArrayList<Student>();
```

You can write:

```java
List<Student> students = new ArrayList<>();  // <> is the "diamond" — type is inferred
```

Both are identical. The diamond `<>` just saves you from repeating the type on the right side.

---

## Summary

| Syntax | Meaning |
|---|---|
| `List<Student>` | A list that can only contain Student objects |
| `Optional<Student>` | Either a Student, or nothing — forces you to handle both cases |
| `JpaRepository<Student, Long>` | A repository for Student entities with Long primary keys |
| `ResponseEntity<List<Student>>` | An HTTP response whose body is a list of students |
| `Map<String, String>` | A map where both keys and values are Strings |

Generics are the reason you can write generic code once (like `JpaRepository`) and reuse it with any entity type without losing type safety.
