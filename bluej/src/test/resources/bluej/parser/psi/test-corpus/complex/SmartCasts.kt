package test.complex

interface Node
data class TextNode(val text: String) : Node
data class ElementNode(val tag: String, val children: List<Node>) : Node

fun renderNode(node: Node): String {
    return when (node) {
        is TextNode -> node.text
        is ElementNode -> {
            val childrenText = node.children.joinToString("") { renderNode(it) }
            "<${node.tag}>$childrenText</${node.tag}>"
        }
        else -> ""
    }
}