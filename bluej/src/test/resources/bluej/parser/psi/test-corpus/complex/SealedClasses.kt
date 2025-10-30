package test.complex

sealed class Expression {
    data class Const(val number: Double) : Expression()
    data class Sum(val e1: Expression, val e2: Expression) : Expression()
    object NotANumber : Expression()
}

fun eval(expr: Expression): Double = when (expr) {
    is Expression.Const -> expr.number
    is Expression.Sum -> eval(expr.e1) + eval(expr.e2)
    Expression.NotANumber -> Double.NaN
}