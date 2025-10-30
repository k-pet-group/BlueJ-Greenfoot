package test.complex

typealias StringMap = Map<String, String>
typealias IntPredicate = (Int) -> Boolean
typealias Handler<T> = (T) -> Unit

class TypeAliasExample {
    val config: StringMap = mapOf("key" to "value")
    
    fun filter(predicate: IntPredicate): List<Int> {
        return listOf(1, 2, 3, 4, 5).filter(predicate)
    }
    
    fun process(data: String, handler: Handler<String>) {
        handler(data)
    }
}