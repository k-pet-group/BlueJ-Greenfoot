package bluej.parser.psi.complex

// Lambda expression test case
val square = { x: Int -> x * x }

fun applyFunction(x: Int, fn: (Int) -> Int) = fn(x)

val result = applyFunction(5, square)