package test.edge

// Test file with very long lines to verify parser handles them correctly
class VeryLongClassName_WithManyWords_ToTestParserHandlingOfLongIdentifiers_ThatMightCauseIssuesInSomeParsers {
    fun veryLongMethodName_WithManyWords_ToTestParserHandlingOfLongIdentifiers_ThatMightCauseIssuesInSomeParsers() {
        val veryLongVariableName_WithManyWords_ToTestParserHandlingOfLongIdentifiers_ThatMightCauseIssuesInSomeParsers = "This is a very long string literal that contains a lot of text to test how the parser handles long string literals which might span significant portions of a line and could potentially cause issues in some parsing scenarios"
        println(veryLongVariableName_WithManyWords_ToTestParserHandlingOfLongIdentifiers_ThatMightCauseIssuesInSomeParsers)
    }
}