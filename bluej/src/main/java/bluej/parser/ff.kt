
        println("=== Kotlin Basics Demo ===\n");

        var name: String = "Alice";
        val age: Int = 25;

        println("Hello, $name! You are $age years old.");

        var category : String = "";

        println("Category: $category");

        val numbers = listOf(1, 2, 3, 4, 5);

        println("\nCounting with a loop:");
        for (n in numbers) {
            print("$n ");
        }

        println("\nDone!\n");

        val person = Person("Bob", 32);
        println("Created a person: $person");

        person.haveBirthday();
        println("After birthday: $person");

        var maybeName: String? = null;

        
        println("\nMaybe name length: ${maybeName?.length ?: "Unknown"}");
                println("\n=== End of Demo ===");
    