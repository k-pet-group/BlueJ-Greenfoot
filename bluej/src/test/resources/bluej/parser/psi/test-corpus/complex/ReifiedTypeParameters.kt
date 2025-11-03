package test.complex

inline fun <reified T> isInstance(value: Any): Boolean = value is T

inline fun <reified T> create(): T = T::class.java.getDeclaredConstructor().newInstance()