package test.complex

@JvmInline
value class UserId(val value: Int)

@JvmInline
value class Password(private val value: String) {
    fun isStrong(): Boolean = value.length >= 8
}

fun authenticate(userId: UserId, password: Password): Boolean {
    return password.isStrong()
}