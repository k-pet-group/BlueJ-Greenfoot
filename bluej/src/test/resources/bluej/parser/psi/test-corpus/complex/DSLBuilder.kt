package test.complex

class HTML {
    private val elements = mutableListOf<String>()
    
    fun head(init: Head.() -> Unit) {
        val head = Head()
        head.init()
        elements.add(head.toString())
    }
    
    fun body(init: Body.() -> Unit) {
        val body = Body()
        body.init()
        elements.add(body.toString())
    }
}

class Head {
    fun title(text: String) = "<title>$text</title>"
}

class Body {
    fun p(text: String) = "<p>$text</p>"
}

fun html(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()
    return html
}