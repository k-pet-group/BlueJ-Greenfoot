package bluej.parser.kotlin.data

/**
 * A basic Kotlin class with various language constructs
 */
class KotlinBasicClass {
    // Properties
    private val name: String = "Default"
    var age: Int = 0
    
    // Constructor
    constructor(name: String, age: Int) {
        this.name = name
        this.age = age
    }
    
    // Member function
    fun greet(): String {
        return "Hello, $name! You are $age years old."
    }
    
    // Function with parameters and return type
    fun add(a: Int, b: Int): Int {
        return a + b
    }
    
    // Function with default parameters
    fun multiply(a: Int, b: Int = 1): Int {
        return a * b
    }
    
    // Companion object
    companion object {
        const val MAX_AGE = 120
        
        fun createDefault(): KotlinBasicClass {
            return KotlinBasicClass("Default", 0)
        }
    }
    
    // Nested class
    class NestedClass {
        fun doSomething() {
            println("Doing something in nested class")
        }
    }
    
    // Inner class
    inner class InnerClass {
        fun accessOuter() {
            println("Accessing outer class: $name")
        }
    }
}

/**
 * A Kotlin interface
 */
interface KotlinInterface {
    fun doSomething()
    
    // Default implementation
    fun doSomethingElse() {
        println("Default implementation")
    }
}

/**
 * A Kotlin data class
 */
data class KotlinDataClass(val id: Int, val name: String)

/**
 * A Kotlin enum class
 */
enum class KotlinEnum {
    ONE, TWO, THREE
}

/**
 * A Kotlin sealed class
 */
sealed class KotlinSealedClass {
    class FirstType : KotlinSealedClass()
    class SecondType : KotlinSealedClass()
}

/**
 * A Kotlin object (singleton)
 */
object KotlinSingleton {
    fun doSomething() {
        println("Doing something in singleton")
    }
}

/**
 * Extension function
 */
fun String.addExclamation(): String {
    return this + "!"
}

/**
 * Top-level function
 */
fun topLevelFunction() {
    println("This is a top-level function")
}

/**
 * Type alias
 */
typealias StringList = List<String>