package test.moderate

fun applyOperation(x: Int, y: Int, operation: (Int, Int) -> Int): Int {
    return operation(x, y)
}

fun filterAndTransform(items: List<Int>, predicate: (Int) -> Boolean, transform: (Int) -> String): List<String> {
    return items.filter(predicate).map(transform)
}

class Calculator {
    fun calculate(a: Int, b: Int, op: (Int, Int) -> Int): Int = op(a, b)
}