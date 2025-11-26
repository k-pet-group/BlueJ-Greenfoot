// BasicsDemo.java
// A simple Java 21 example covering fundamental features

import java.util.*;
import java.util.stream.*;

public class BasicsDemo {

    public static void main(String[] args) {
        new BasicsDemo().runDemo();
    }

    public void runDemo() {
        System.out.println("=== Java Basics Demo ===\n");

        // 1. Variables and Constants
        var name = "Alice";       // local variable type inference (Java 10+)
        final int age = 25;       // final == val in Kotlin (immutable)

        System.out.println(String.format("Hello, %s! You are %d years old.", name, age));

        // 2. Conditionals
        String category = (age < 18) ? "Minor"
                : (age <= 65) ? "Adult"
                : "Senior";
        System.out.println("Category: " + category);

        // 3. Methods and Defaults
        System.out.println(greet("Java Learner"));
        System.out.println(greet("Java Enthusiast", true));

        // 4. Collections and Functional Transformations
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        List<Integer> squares = numbers.stream()
                .map(n -> n * n)
                .toList();

        List<Integer> evenSquares = squares.stream()
                .filter(n -> n % 2 == 0)
                .toList();

        System.out.println("\nNumbers: " + numbers);
        System.out.println("Squares: " + squares);
        System.out.println("Even Squares: " + evenSquares);

        // 5. Loops
        System.out.println("\nCounting with a loop:");
        for (int n : numbers) {
            System.out.print(n + " ");
        }
        System.out.println("\nDone!\n");

        // 6. Classes and Records
        Person person = new Person("Bob", 32);
        System.out.println("Created a person: " + person);

        person.haveBirthday();
        System.out.println("After birthday: " + person);

        // 7. Null Safety (using Optional)
        Optional<String> maybeName = (person.age() > 30)
                ? Optional.empty()
                : Optional.of(person.name());
        System.out.println("\nMaybe name length: " +
                maybeName.map(String::length).orElse(-1));

        // 8. Using a higher-order-like function (with a lambda)
        int total = applyToList(numbers, list -> list.stream().mapToInt(Integer::intValue).sum());
        System.out.println("\nSum of numbers (using lambda): " + total);

        // 9. Pattern Matching (Java 21)
        Object something = person;
        String description = switch (something) {
            case Person(var n, var a) -> String.format("It's a person named %s, age %d", n, a);
            case String s -> "It's a string: " + s;
            case null -> "It's null!";
            default -> "Something else: " + something.getClass().getSimpleName();
        };
        System.out.println("\nPattern matching result: " + description);

        System.out.println("\n=== End of Demo ===");
    }

    // Method with optional parameter (simulated with overloads)
    public String greet(String person) {
        return greet(person, false);
    }

    public String greet(String person, boolean excited) {
        var message = "Hello, " + person;
        return excited ? message + "!!!" : message + ".";
    }

    // Higher-order-like method that takes a function
    public <T, R> R applyToList(List<T> list, java.util.function.Function<List<T>, R> operation) {
        return operation.apply(list);
    }

    // Record — immutable data carrier (like Kotlin data class)
    record Person(String name, int age) {
        void haveBirthday() {
            System.out.println("🎉 Happy birthday, " + name + "!");
            // Records are immutable, but you can return a new instance
            // If you want to mutate, use a normal class instead
        }
    }
}
