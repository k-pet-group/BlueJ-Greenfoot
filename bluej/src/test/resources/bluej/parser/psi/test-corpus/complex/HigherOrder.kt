package bluej.parser.psi.complex

// Higher-order function test case
fun <T, R> map(items: List<T>, transform: (T) -> R): List<R> {
    return items.map(transform)
}

val doubled = map(listOf(1, 2, 3)) { it * 2 }