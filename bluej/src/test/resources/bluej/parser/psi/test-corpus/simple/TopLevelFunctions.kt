package test.simple

fun topLevelFunction() {
    println("Top level")
}

fun anotherTopLevel(): Int = 42

fun withParameters(name: String, value: Int): String {
    return "$name: $value"
}