package test.complex

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CustomAnnotation(val value: String, val priority: Int = 0)

@CustomAnnotation("important", priority = 1)
class AnnotatedClass {
    @CustomAnnotation("method")
    fun annotatedMethod() {
        println("Annotated")
    }
    
    @Deprecated("Use newMethod instead", ReplaceWith("newMethod()"))
    fun oldMethod() {}
    
    fun newMethod() {}
}