// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg values: Ts) {
    private val _values: Array<Any?> = arrayOf(*values)
    fun <T> get(ix: @TypeIndex(Ts::class, T::class) Int): T = _values[ix] as T
}

fun test() {
    val tuple = Tuple()
    <!UNREACHABLE_CODE!>val outOfBounds =<!> tuple.get(
        <!VARIADIC_TYPE_PARAMETER_INDEX_OUT_OF_BOUNDS!>0<!>
    )
}
