package test.complex

interface Flyable {
    fun fly() {
        println("Flying")
    }
}

interface Swimmable {
    fun swim() {
        println("Swimming")
    }
}

open class Animal(val name: String)

class Duck(name: String) : Animal(name), Flyable, Swimmable {
    override fun fly() {
        println("$name is flying")
    }
}