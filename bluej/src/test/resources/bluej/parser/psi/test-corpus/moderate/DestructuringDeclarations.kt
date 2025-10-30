package test.moderate

data class Account(val username: String, val id: Int, val email: String)

fun processAccount() {
    val account = Account("john_doe", 12345, "john@example.com")
    val (username, id, email) = account
    
    println("Username: $username, ID: $id, Email: $email")
}

fun processMap() {
    val map = mapOf("key1" to "value1", "key2" to "value2")
    for ((key, value) in map) {
        println("$key -> $value")
    }
}