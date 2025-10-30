package test.complex

inline fun <T> measureTime(block: () -> T): kotlin.Pair<T, Long> {
    val start = System.currentTimeMillis()
    val result = block()
    val end = System.currentTimeMillis()
    return kotlin.Pair(result, end - start)
}

inline fun <reified T> isInstance(value: Any): Boolean {
    return value is T
}

fun demonstrate() {
    val (result, time) = measureTime { 
        Thread.sleep(100)
        "Done"
    }
    println("Result: $result, Time: $time ms")
}