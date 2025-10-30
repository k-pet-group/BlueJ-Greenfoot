package test.simple

object Singleton {
    val value = 42
    
    fun getInstance(): Singleton = this
}