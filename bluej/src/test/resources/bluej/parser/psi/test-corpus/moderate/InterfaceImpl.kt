package test.moderate

interface Clickable {
    fun click()
}

interface Draggable {
    fun drag(x: Int, y: Int)
}

class Button : Clickable, Draggable {
    override fun click() {
        println("Button clicked")
    }
    
    override fun drag(x: Int, y: Int) {
        println("Dragged to ($x, $y)")
    }
}