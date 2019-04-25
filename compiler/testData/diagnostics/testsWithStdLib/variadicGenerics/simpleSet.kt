// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg values: Ts) {
    private val _values: Array<Any?> = arrayOf(*values)
    fun <T> set(
        ix: @TypeIndex(Ts::class, T::class) Int,
        value: T
    ) {
        _values[ix] = value
    }
}


fun test() {
    val tuple: Tuple<Number, String> = Tuple<Number, String>(4, "foo")
    tuple.set(1, "baz")
    tuple.<!TYPE_MISMATCH, TYPE_MISMATCH!>set(0, "baz")<!>
    tuple.<!TYPE_MISMATCH, TYPE_MISMATCH!>set(1, 13)<!>
}