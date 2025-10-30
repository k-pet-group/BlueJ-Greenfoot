package test.moderate

open class Base {
    open fun overrideMe() {
        println("Base implementation")
    }
    
    fun finalMethod() {
        println("Cannot override")
    }
}

class Derived : Base() {
    override fun overrideMe() {
        super.overrideMe()
        println("Derived implementation")
    }
}