package test.complex

class Outer {
    private val outerProperty = "outer"
    
    class Nested {
        fun nestedFunction() = "nested"
    }
    
    inner class Inner {
        fun accessOuter() = outerProperty
        
        inner class DeeplyNested {
            fun accessOuterFromDeep() = outerProperty
        }
    }
}