// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg val values: Ts) {
    fun <T> get(ix: @TypeIndex(Ts::class, T::class) Int): T = values[ix] as T
}

fun test() {
    val tuple = Tuple()
    val outOfBounds = tuple.get(
        <!VARIADIC_TYPE_PARAMETER_INDEX_OUT_OF_BOUNDS!>0<!>
    )
}
