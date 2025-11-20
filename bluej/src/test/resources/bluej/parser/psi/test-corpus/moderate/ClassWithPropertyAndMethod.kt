class Dog {
    val name: String
        get() {
            return "sparky"
        }

    fun bark() {
        var i = 0;
        while (i < 5) {
            print(name)
        }
        for (x in 1..5) {
            print(name + "!");
        }
    }
}
