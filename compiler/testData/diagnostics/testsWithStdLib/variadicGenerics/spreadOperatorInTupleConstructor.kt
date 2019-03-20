// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg val values: Ts) {
    fun <T> get(
        ix: @TypeIndex(Ts::class, T::class) Int
    ): T = values[ix] as T
}

fun test() {
    val args = arrayOf("str", 1)
    val tuple = Tuple<String, Int>(*args)
    val first: String = tuple.get(0)
    val second: Int = tuple.get(1)
}