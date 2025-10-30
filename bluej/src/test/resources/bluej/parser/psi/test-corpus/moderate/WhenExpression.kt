package test.moderate

sealed class Result {
    data class Success(val data: String) : Result()
    data class Error(val message: String) : Result()
    object Loading : Result()
}

fun handleResult(result: Result): String {
    return when (result) {
        is Result.Success -> "Success: ${result.data}"
        is Result.Error -> "Error: ${result.message}"
        Result.Loading -> "Loading..."
    }
}