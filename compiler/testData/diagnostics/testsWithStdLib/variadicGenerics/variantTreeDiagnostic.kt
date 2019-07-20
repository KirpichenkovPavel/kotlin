// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Variant<Ts...> () {
    private var value: Any? = null
    private var index: Int = -1

    operator fun <T> set (
        ix: @TypeIndex(Ts::class, T::class) Int,
        value: T
    ) {
        this.value = value
        index = ix
    }

    operator fun <T> get (ix: @TypeIndex(Ts::class, T::class) Int): T? {
        if (index == -1) {
            // not initialized - throw exception
            throw java.lang.Exception("not initialized")
        }
        if (ix != index) {
            return null
        }
        return value as T
    }

    fun <T, R> let (
        ix: @TypeIndex(Ts::class, T::class) Int,
        block: (T) -> R
    ): R? = get(ix)?.let(block)
}

data class Leaf(val data: String)
data class Node(val left: Variant<Node, Leaf>, val right: Variant<Node, Leaf>, val data: String)

fun printValues(tree: Variant<Node, Leaf>): String {
    var result = ""
    tree.let(0) { node ->
        result = "${printValues(node.left)}${node.data}${node.right}"
    }
    tree.let(1) { leaf ->
        result = leaf.data
    }
    return result
}

fun box(): String {
    val left = Leaf("O")
    val right = Leaf("K")
    val leftLeaf = Variant<Node, Leaf>()
    val rightLeaf = Variant<Node, Leaf>()
    val root = Variant<Node, Leaf>()
    leftLeaf[1] = left
    rightLeaf[1] = right
    root[0] = Node(leftLeaf, rightLeaf, "")
    return printValues(root)
}