// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg val values: Ts) {
    fun <T> get(ix: @TypeIndex(Ts::class, T::class) Int): T = values[ix] as T
}

fun inputTypeMismatch() {
    val notInt = 25.0
    val tuple = Tuple<Int, String>(
        <!TYPE_MISMATCH!>notInt<!>,
        "string"
    )
}

fun outputTypeMismatch() {
    val notInt = 25.0
    val tuple = Tuple<Double, String>(notInt, "string")
    val int: Int = <!TYPE_MISMATCH!>tuple.<!TYPE_MISMATCH!>get(0)<!><!>
    val string: String = tuple.get(1)
}
