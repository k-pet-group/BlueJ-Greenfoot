class Outer {
    class Inner {
        class DeepNested {
            fun deepMethod() = 1
            
            companion object {
                const val CONSTANT = "value"
            }
        }
    }
    
    object SingletonObject {
        fun objectMethod() {}
    }
}

interface OuterInterface {
    interface NestedInterface {
        fun nestedMethod()
    }
}