// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg values: Ts) {
    private val _values: Array<Any?> = arrayOf(*values)
    operator fun <T> get(ix: @TypeIndex(Ts::class, T::class) Int): T = _values[ix] as T

    operator fun <T> set(
        ix: @TypeIndex(Ts::class, T::class) Int,
        value: T
    ) {
        _values[ix] = value
    }
}

fun test() {
    val tuple: Tuple<Int, String> = Tuple(5, "baz")
    tuple[0] += 1
    tuple[1] = tuple[1].substring(2)
    val int: Int = tuple[0]
    val string: String = tuple[1]

    <!TYPE_MISMATCH, TYPE_MISMATCH!>tuple[0]<!> = "42"
    <!TYPE_MISMATCH, TYPE_MISMATCH!>tuple[1]<!> = 15
    tuple[<!VARIADIC_TYPE_PARAMETER_INDEX_OUT_OF_BOUNDS!>2<!>] = Unit
}