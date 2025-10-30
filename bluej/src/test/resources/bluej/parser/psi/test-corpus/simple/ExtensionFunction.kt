package test.simple

fun String.addExclamation(): String {
    return this + "!"
}

fun Int.isEven(): Boolean = this % 2 == 0