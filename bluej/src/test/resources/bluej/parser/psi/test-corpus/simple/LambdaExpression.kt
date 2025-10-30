package test.simple

val numbers = listOf(1, 2, 3, 4, 5)

val doubled = numbers.map { it * 2 }

val filtered = numbers.filter { n -> n > 2 }

fun processItems(items: List<Int>, action: (Int) -> Unit) {
    items.forEach(action)
}