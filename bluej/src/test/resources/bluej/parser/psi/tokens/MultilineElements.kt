// Multi-line function - spans Lines 2-6
fun multilineFunction(
    param1: String,
    param2: Int,
    param3: Boolean
): String {
    return """
        Multi-line
        String literal
        spanning lines
    """
}

// Multi-line class with long supertype list
class MultilineClass(
    val property1: String,
    val property2: Int
) : Interface1,
    Interface2,
    Interface3 {
    // Body
}