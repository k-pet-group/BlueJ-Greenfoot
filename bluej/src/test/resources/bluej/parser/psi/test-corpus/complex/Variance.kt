package bluej.parser.psi.complex

// Variance annotation test case
class Box<out T>(val value: T)  // Covariance

interface Comparable<in T> {     // Contravariance
    fun compareTo(other: T): Int
}