package test.complex

interface Base {
    fun print()
    fun getValue(): Int
}

class BaseImpl(val value: Int) : Base {
    override fun print() { println("BaseImpl: $value") }
    override fun getValue(): Int = value
}

class Derived(b: Base) : Base by b {
    fun additionalMethod() {
        print()
        println("Additional: ${getValue()}")
    }
}