package test.edgecases

class Outer {
    class Level1 {
        class Level2 {
            class Level3 {
                class Level4 {
                    fun deepMethod() = "deeply nested"
                }
            }
        }
    }
}