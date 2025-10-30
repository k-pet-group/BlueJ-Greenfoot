package test.complex

suspend fun fetchData(): String {
    return "data"
}

suspend fun computeResult(value: Int): Int {
    return value * 2
}

class CoroutineExample {
    suspend fun processData(): String {
        val result = fetchData()
        return result.uppercase()
    }
    
    suspend fun complexProcess(a: Int, b: Int): Int {
        val x = computeResult(a)
        val y = computeResult(b)
        return x + y
    }
}