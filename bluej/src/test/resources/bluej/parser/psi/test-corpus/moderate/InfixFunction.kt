package test.moderate

class Pair<A, B>(val first: A, val second: B)

infix fun <A, B> A.to(that: B): Pair<A, B> = Pair(this, that)

fun demonstrate() {
    val pair = "key" to "value"
    println("${pair.first} -> ${pair.second}")
}