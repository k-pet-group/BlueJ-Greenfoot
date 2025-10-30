package test.moderate

class Logger(val name: String = "DefaultLogger", val level: Int = 1) {
    fun log(message: String, priority: Int = level, timestamp: Long = System.currentTimeMillis()) {
        println("[$name] [$priority] [$timestamp] $message")
    }
}

fun greet(name: String, greeting: String = "Hello", punctuation: String = "!") {
    println("$greeting, $name$punctuation")
}