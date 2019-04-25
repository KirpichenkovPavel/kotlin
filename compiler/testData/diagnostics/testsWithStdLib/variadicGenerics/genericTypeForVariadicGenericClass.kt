// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg values: Ts) {
    private val _values: Array<Any?> = arrayOf(*values)
    fun <T> get(ix: @TypeIndex(Ts::class, T::class) Int): T = _values[ix] as T
}

fun test() {
    val list1: List<Int> = listOf(1, 2, 3)
    val list2: List<String> = listOf("foo", "bar")
    val tuple: Tuple<List<Int>, List<String>> = Tuple<List<Int>, List<String>>(list1, list2)

    val correct1: List<Int> = tuple.get(0)
    val correct2: List<String> = tuple.get(1)
    val correctBase: Collection<String> = tuple.get(1)

    val wrongType: String = <!TYPE_MISMATCH!>tuple.<!TYPE_MISMATCH!>get(1)<!><!>
    val wrongParameterType: List<Double> = <!TYPE_MISMATCH!>tuple.<!TYPE_MISMATCH!>get(0)<!><!>
}