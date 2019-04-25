// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg values: Ts) {
    private val _values: Array<Any?> = arrayOf(*values)
    fun <T, R> let(
        ix: @TypeIndex(Ts::class, T::class) Int,
        block: Tuple<Ts>.(value: T) -> R
    ): R = this.block(_values[ix] as T)
}

fun test() {
    val tuple: Tuple<Int, String> = Tuple(5, "s")
    tuple.let(0) { int -> int + 1}
    tuple.let(1) { string -> string.substring(0) }
    tuple.let(0) { notString -> notString.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>substring<!>(0) }
}