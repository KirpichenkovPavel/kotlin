// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg val values: Ts) {
    operator fun plus (another: Tuple<*>): Tuple<*> {
        return Tuple(*values, *another.values)
    }

    operator fun <T> get(
        ix: @TypeIndex(Ts::class, T::class) Int
    ): T = values[ix] as T
}

fun test() {
    val tuple1: Tuple<Int, String> = Tuple(0, "str")
    val tuple2: Tuple<Double, Unit> = Tuple(36.6, Unit)
    val tuple3: Tuple<Int, String, Double, Unit> = tuple1 + tuple2
    val tuple4 = tuple1 + tuple2
    tuple3[1].substring(0)
    tuple3[2].<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>substring<!>(1)
    tuple4[1].substring(0)
    tuple4[2].<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>substring<!>(1)
}