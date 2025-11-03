package test.moderate

class Counter {
    var count: Int = 0
        set(value) {
            if (value >= 0) field = value
        }
}