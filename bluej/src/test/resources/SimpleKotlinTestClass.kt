/**
 * Simple Kotlin test class for validating object workbench menu integration.
 * 
 * This class tests Phase 1 functionality:
 * - Basic method visibility
 * - Kotlin-specific syntax
 * - Integration with existing BlueJ infrastructure
 */
class SimpleKotlinTestClass {
    
    // Public methods that should appear in menu
    fun publicMethod(): String {
        return "public result"
    }
    
    fun methodWithParams(name: String, age: Int): String {
        return "Hello $name, age $age"
    }
    
    fun methodWithReturnType(): Int {
        return 42
    }
    
    // Private method that should not appear (filtered by ViewFilter)
    private fun privateMethod(): String {
        return "private"
    }
    
    // Protected method
    protected fun protectedMethod(): String {
        return "protected"
    }
}