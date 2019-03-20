// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg val values: Ts) {
    operator fun <T> get(
        ix: @TypeIndex(Ts::class, T::class) Int
    ): T = values[ix] as T
}

fun test () {
    val tuple = Tuple<Int, String>(42, "42")
    val first = tuple[0]
    val second = tuple[1]
}

