// !LANGUAGE: +NewInference
// TARGET_BACKEND: JVM
// WITH_RUNTIME

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

    fun <T> with (
        ix: @TypeIndex(Ts::class, T::class) Int,
        value: T
    ): Variant<Ts> {
        set(ix, value)
        return this
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

fun collectValues(tree: Variant<Node, Leaf>): String {
    var result = ""
    tree.let(0) { node ->
        result = "${collectValues(node.left)}${node.data}${collectValues(node.right)}"
    }
    tree.let(1) { leaf ->
        result = leaf.data
    }
    return result
}

fun box(): String {
    val leftLeaf = Variant<Node?, Leaf>().with(1, Leaf("O"))
    val rightLeaf = Variant<Node?, Leaf>().with(1, Leaf("K"))
    val root = Variant<Node?, Leaf>().with(0, Node(leftLeaf, rightLeaf, ""))
    return collectValues(root)
}