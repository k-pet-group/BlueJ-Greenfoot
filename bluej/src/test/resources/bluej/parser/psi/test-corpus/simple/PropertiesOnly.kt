package test.simple

class Container {
    val readOnly = "constant"
    var mutable = 0
    
    val computed: Int
        get() = mutable * 2
}