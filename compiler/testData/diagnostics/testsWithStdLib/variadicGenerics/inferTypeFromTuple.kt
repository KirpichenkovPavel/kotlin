// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg val values: Ts) {
    fun <T> get(
        ix: @TypeIndex(Ts::class, T::class) Int
    ): T = values[ix] as T
}

fun test() {
    val tuple = Tuple<Int, String>(42, "42")
    val first: Int = tuple.get(0)
    val second: Double = <!TYPE_MISMATCH!>tuple.<!TYPE_MISMATCH!>get(1)<!><!>
}