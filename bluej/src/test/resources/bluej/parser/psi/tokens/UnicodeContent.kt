// Unicode in identifiers
class ClassWithÜnicode {
    val émoji = "😀🎉"
    val chinese = "你好世界"
    val japanese = "こんにちは"
}

// Unicode in strings and comments
fun testUnicode() {
    val mixed = "Hello 世界 🌍"
    // Comment with émojis 😀
    val symbols = "€£¥₹"
}