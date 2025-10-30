package test.complex

class Box<T>(val value: T) {
    fun <R> map(transform: (T) -> R): Box<R> {
        return Box(transform(value))
    }
}

interface Repository<T, ID> {
    fun findById(id: ID): T?
    fun save(entity: T): T
}

class UserRepository : Repository<String, Int> {
    override fun findById(id: Int): String? = "User$id"
    override fun save(entity: String): String = entity
}

fun <T : Comparable<T>> max(a: T, b: T): T {
    return if (a > b) a else b
}