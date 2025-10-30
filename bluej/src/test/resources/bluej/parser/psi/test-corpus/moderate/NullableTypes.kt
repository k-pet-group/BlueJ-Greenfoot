package test.moderate

class NullableExample {
    var nullableString: String? = null
    
    fun safePrint(value: String?) {
        println(value ?: "null value")
    }
    
    fun processNullable(input: String?): Int {
        return input?.length ?: 0
    }
    
    fun requireNonNull(value: String?): String {
        return value ?: throw IllegalArgumentException("Value cannot be null")
    }
}