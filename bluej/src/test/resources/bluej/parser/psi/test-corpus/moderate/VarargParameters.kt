package test.moderate

fun printAll(vararg items: String) {
    for (item in items) {
        println(item)
    }
}

fun sum(vararg numbers: Int): Int {
    return numbers.sum()
}

class Builder {
    fun addItems(vararg items: String): Builder {
        items.forEach { println("Added: $it") }
        return this
    }
}