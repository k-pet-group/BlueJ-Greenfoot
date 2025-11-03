package bluej.parser.psi.simple

// Tailrec function test case  
tailrec fun factorial(n: Int, acc: Int = 1): Int {
    return if (n <= 1) acc else factorial(n - 1, n * acc)
}