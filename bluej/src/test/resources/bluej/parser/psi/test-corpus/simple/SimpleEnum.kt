package test.simple

enum class Color {
    RED,
    GREEN,
    BLUE
}

enum class Direction(val degrees: Int) {
    NORTH(0),
    EAST(90),
    SOUTH(180),
    WEST(270)
}