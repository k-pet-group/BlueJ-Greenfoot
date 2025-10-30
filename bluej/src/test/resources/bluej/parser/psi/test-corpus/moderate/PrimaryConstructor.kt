package test.moderate

class User(val username: String, var email: String, private val password: String) {
    init {
        require(username.isNotEmpty()) { "Username cannot be empty" }
        require(email.contains("@")) { "Invalid email" }
    }
    
    fun updateEmail(newEmail: String) {
        email = newEmail
    }
}