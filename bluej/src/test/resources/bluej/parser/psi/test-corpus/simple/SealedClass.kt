package bluej.parser.psi.simple

// Simple sealed class for testing
sealed class Result {
    data class Success(val value: String) : Result()
    data class Error(val message: String) : Result()
}