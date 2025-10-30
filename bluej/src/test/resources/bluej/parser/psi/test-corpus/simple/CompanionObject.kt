package test.simple

class MyClass {
    companion object {
        const val CONSTANT = "value"
        
        fun create(): MyClass {
            return MyClass()
        }
    }
}