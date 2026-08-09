# Interfaces and Inheritance

These two concepts are how Java shares and reuses behaviour across classes. Both appear constantly in Spring Boot — understanding them removes a lot of the "why does this work?" mystery.

---

## Inheritance — The `extends` Keyword

Inheritance lets one class absorb the fields and methods of another.

```java
// Parent class (superclass)
class Animal {
    String name;

    void breathe() {
        System.out.println(name + " is breathing.");
    }
}

// Child class (subclass) — inherits everything from Animal
class Dog extends Animal {
    void bark() {
        System.out.println(name + " says: Woof!");
    }
}
```

`Dog` automatically gets the `name` field and `breathe()` method from `Animal` — you did not have to repeat them.

```java
Dog dog = new Dog();
dog.name = "Rex";
dog.breathe();   // Rex is breathing.  ← inherited from Animal
dog.bark();      // Rex says: Woof!    ← defined in Dog
```

### Why This Matters for Spring Boot

`JpaRepository` is a class (actually an interface — explained below) that already has 18+ methods written for you: `save()`, `findById()`, `findAll()`, `deleteById()`, and many more.

When you write:

```java
public interface StudentRepository extends JpaRepository<Student, Long> {
}
```

Your `StudentRepository` *inherits* all those methods without you writing a single one. `extends` is what makes that happen.

---

## Overriding — Replacing Inherited Behaviour

A child class can replace (override) a method it inherited:

```java
class Animal {
    void speak() {
        System.out.println("...");
    }
}

class Cat extends Animal {
    @Override
    void speak() {
        System.out.println("Meow!");   // replaces Animal's version
    }
}
```

`@Override` is an annotation (covered in its own file) that tells Java: "I am intentionally replacing the parent's version of this method." Java will give you a compile error if you misspell the method name — a safety net.

---

## Interfaces — Defining a Contract

An interface is a list of method *signatures* with no implementation. It says "any class that claims to implement me must provide these methods."

```java
interface Printable {
    void print();         // no body — just the signature
    String getSummary();  // no body
}
```

A class *implements* an interface by providing the method bodies:

```java
class Report implements Printable {
    @Override
    public void print() {
        System.out.println("Printing report...");
    }

    @Override
    public String getSummary() {
        return "Q4 Financial Report";
    }
}
```

If `Report` claims to implement `Printable` but forgets to write `getSummary()`, Java refuses to compile.

### The Difference Between `extends` and `implements`

| Keyword | Used for | What it means |
|---|---|---|
| `extends` | Class → Class, or Interface → Interface | Inherit code from a parent |
| `implements` | Class → Interface | Promise to provide the listed methods |

A class can only `extend` one other class, but it can `implement` multiple interfaces.

---

## Why Interfaces Exist

Interfaces let you write code that works with *any* class that fulfils the contract, without caring about the specific class.

```java
interface Shape {
    double area();
}

class Circle implements Shape {
    double radius;
    Circle(double radius) { this.radius = radius; }

    public double area() { return Math.PI * radius * radius; }
}

class Rectangle implements Shape {
    double width, height;
    Rectangle(double w, double h) { this.width = w; this.height = h; }

    public double area() { return width * height; }
}

// This method works for ANY Shape — it does not care which one
void printArea(Shape shape) {
    System.out.println("Area: " + shape.area());
}

printArea(new Circle(5));        // Area: 78.54
printArea(new Rectangle(4, 6));  // Area: 24.0
```

Spring Boot uses this heavily. Your controller does not care whether the `StudentService` fetches data from PostgreSQL, a file, or a test fake — it just calls `service.getAllStudents()` because the service implements that method.

---

## Interface Extending Interface

Interfaces can extend other interfaces:

```java
interface Repository {
    void save(Object item);
}

interface JpaRepository extends Repository {
    Object findById(long id);
    void deleteById(long id);
    // ... plus everything Repository defines
}
```

When you write `StudentRepository extends JpaRepository`, your interface inherits the entire chain. This is exactly what happens in the course — `JpaRepository` itself extends several other interfaces.

---

## Abstract Classes — The Middle Ground

An abstract class is halfway between a regular class and an interface. It can have some methods fully implemented and some left abstract (no body):

```java
abstract class Vehicle {
    String brand;

    // Fully implemented — every Vehicle gets this for free
    void startEngine() {
        System.out.println(brand + " engine started.");
    }

    // Abstract — each subclass must implement this itself
    abstract int getWheelCount();
}

class Car extends Vehicle {
    public int getWheelCount() { return 4; }
}

class Motorcycle extends Vehicle {
    public int getWheelCount() { return 2; }
}
```

You cannot create an instance of an abstract class directly (`new Vehicle()` is a compile error). You can only instantiate its concrete subclasses.

Abstract classes are less common in Spring Boot day-to-day work than interfaces, but they appear in the framework internals.

---

## Putting It Together — What You See in the Course

```java
// StudentRepository is an interface that extends JpaRepository
// JpaRepository is itself an interface
// Spring Data provides the actual implementation at runtime

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmail(String email);  // you add custom methods here
}
```

- `interface` → it is a contract, not a class
- `extends JpaRepository` → inherits all CRUD methods from JpaRepository
- `<Student, Long>` → generics, covered in the next file
- Spring Data generates a real implementation class automatically — you never write it
