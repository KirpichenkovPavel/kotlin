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
data class Node(val left: Variant<Node, Leaf>, val right: Variant<Node, Leaf>, val data: Int)

fun printValues(tree: Variant<Node, Leaf>) {
    tree.let(0) { node ->
        printValues(node.left)
        val int: Int = node.data
        printValues(node.right)
    }
    tree.let(1) { leaf ->
        val string: String = leaf.data
    }
}
