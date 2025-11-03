package bluej.parser.psi.complex

// Destructuring declaration test case
data class Person(val name: String, val age: Int)

val (name, age) = Person("Alice", 30)