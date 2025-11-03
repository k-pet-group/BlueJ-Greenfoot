package bluej.parser.psi.simple

// Simple annotation class
annotation class MyAnnotation(val value: String = "")

@MyAnnotation("test")
class AnnotatedClass