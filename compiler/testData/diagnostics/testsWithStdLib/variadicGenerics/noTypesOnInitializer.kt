// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg val values: Ts) {
    operator fun <T> get(
        ix: @TypeIndex(Ts::class, T::class) Int
    ): T = values[ix] as T
}

fun test() {
    val tuple: Tuple<Int, String> = Tuple(5, "str")
    val int: Int = tuple[0]
    val string: String = tuple[1]
}

fun test2() {
    val tuple: Tuple<Int, String> = Tuple(5, <!TYPE_MISMATCH!>11.7<!>)
}