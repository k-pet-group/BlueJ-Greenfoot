// BasicsDemo.kt
// A simple Kotlin file demonstrating core language features, all wrapped in a class

class BasicDemoApp {

    fun runDemo() {
        println("=== Kotlin Basics Demo ===\n")

        // 1. Variables and Constants
        var name: String = "Alice"      // mutable variable
        val age: Int = 25               // immutable (read-only) variable

        println("Hello, $name! You are $age years old.")

        // 2. Conditionals
        val category = if (age < 18) {
            "Minor"
        } else if (age in 18..65) {
            "Adult"
        } else {
            "Senior"
        }
        println("Category: $category")

        // 3. Functions and String Templates
        fun greet(person: String, excited: Boolean = false): String {
            val message = "Hello, $person"
            return if (excited) "$message!!!" else "$message."
        }
        println(greet("Kotlin Learner"))
        println(greet("Kotlin Enthusiast", excited = true))

        // 4. Collections and Higher-Order Functions
        val numbers = listOf(1, 2, 3, 4, 5)

        // Using map, filter, forEach — functional style
        val squares = numbers.map { it * it }
        val evenSquares = squares.filter { it % 2 == 0 }

        println("\nNumbers: $numbers")
        println("Squares: $squares")
        println("Even Squares: $evenSquares")

        // 5. Loops
        println("\nCounting with a loop:")
        for (n in numbers) {
            print("$n ")
        }
        println("\nDone!\n")

        // 6. Classes and Data Classes
        val person = Person("Bob", 32)
        println("Created a person: $person")

        person.haveBirthday()
        println("After birthday: $person")

        // 7. Null Safety
        val maybeName: String? = if (person.age > 30) null else person.name
        println("\nMaybe name length: ${maybeName?.length ?: "Unknown"}")

        // 8. Using a simple function that takes a higher-order function
        val total = applyToList(numbers) { list -> list.sum() }
        println("\nSum of numbers (using higher-order function): $total")

        println("\n=== End of Demo ===")
    }

    class Trivial(test: String)

    // 9. A simple class with properties and a method
    class Person(var name: String, var age: Int) {
        fun haveBirthday() {
            age++
            println("🎉 Happy birthday, $name!")
        }

        override fun toString(): String {
            return "Person(name='$name', age=$age)"
        }
    }

    // 10. A function taking a higher-order function
    fun <T, R> applyToList(list: List<T>, operation: (List<T>) -> R): R {
        return operation(list)
    }
}
