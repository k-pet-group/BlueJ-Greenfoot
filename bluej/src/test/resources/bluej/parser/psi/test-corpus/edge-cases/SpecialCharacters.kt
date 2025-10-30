package test.edge

class SpecialCharacters {
    val escaped = "String with \"quotes\" and \n newlines \t tabs"
    val rawString = """Raw string with "quotes" without escaping"""
    val dollarSign = "Price: \$100"
    
    fun testBackticks() {
        val `variable with spaces` = 42
        println(`variable with spaces`)
    }
}