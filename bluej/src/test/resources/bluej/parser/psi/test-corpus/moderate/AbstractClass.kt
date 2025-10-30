package test.moderate

abstract class Shape {
    abstract fun area(): Double
    abstract fun perimeter(): Double
    
    fun describe() {
        println("Area: ${area()}, Perimeter: ${perimeter()}")
    }
}

class Rectangle(val width: Double, val height: Double) : Shape() {
    override fun area(): Double = width * height
    override fun perimeter(): Double = 2 * (width + height)
}