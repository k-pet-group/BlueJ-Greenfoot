package test.complex

fun demonstrateScope() {
    val result = "hello".let { it.uppercase() }
    val obj = StringBuilder().apply { append("text") }
    with(ArrayList<String>()) { add("item") }
    val computed = "world".run { length }
}