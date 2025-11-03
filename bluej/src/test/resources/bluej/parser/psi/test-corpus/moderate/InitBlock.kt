package test.moderate

class InitExample {
    val data: String
    
    init {
        data = "initialized"
        println("Init block executed")
    }
}