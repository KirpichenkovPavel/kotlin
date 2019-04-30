// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...>(vararg values: Ts) {
    private val _values: Array<Any?> = arrayOf(*values)
    fun asValuesList(): List<Any?> = _values.toList()
}

fun <T, Ts...: T> Tuple<Ts>.asList(): List<T> =
    asValuesList().map { it as T }

fun test() {
    val tuple = Tuple(42.7, 15)
    val list: List<Number> = tuple.asList()
    val list2 = tuple.asList()
    val list3 = tuple.asList<Number, Double, Int>()
    val list4 = Tuple(143, "baz").asList()

    val floats = list2.map { it.toFloat() }
    val notFloats = list4.map {
        it.<!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toFloat<!>()
    }
}
