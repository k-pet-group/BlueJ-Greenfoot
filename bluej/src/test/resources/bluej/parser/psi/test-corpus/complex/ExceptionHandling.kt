package test.complex

class CustomException(message: String) : Exception(message)

class Resource : AutoCloseable {
    override fun close() {
        println("Closing resource")
    }
}

fun riskyOperation(value: Int): Int {
    if (value < 0) throw CustomException("Negative value")
    return value * 2
}

fun handleExceptions() {
    try {
        val result = riskyOperation(-1)
        println(result)
    } catch (e: CustomException) {
        println("Custom: ${e.message}")
    } catch (e: Exception) {
        println("General: ${e.message}")
    } finally {
        println("Cleanup")
    }
}

fun useResource() {
    Resource().use { resource ->
        println("Using resource")
    }
}