// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg values: Ts) {
    private val _values: Array<Any?> = arrayOf(*values)
    operator fun <T> get(ix: @TypeIndex(Ts::class, T::class) Int): T = _values[ix] as T
}

fun test () {
    val tuple = Tuple<Int, String>(42, "42")
    val first: Int = tuple[0]
    val second: String = tuple[1]
    val wrongType: Double = <!TYPE_MISMATCH, TYPE_MISMATCH!>tuple[0]<!>
    <!UNREACHABLE_CODE!>val third: Nothing =<!> <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>tuple[<!VARIADIC_TYPE_PARAMETER_INDEX_OUT_OF_BOUNDS!>2<!>]<!>
}

