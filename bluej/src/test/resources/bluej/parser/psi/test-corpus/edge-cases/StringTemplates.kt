package test.edge

class StringTemplateEdgeCases {
    val name = "World"
    val nested = "Value: ${1 + 2}"
    val complex = "Name: $name, Nested: ${nested.uppercase()}"
    
    fun multilineString(): String {
        val text = """
            Line 1
            Line 2 with $name
            Line 3 with ${nested}
        """.trimIndent()
        
        return text
    }
}