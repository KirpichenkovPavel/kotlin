// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg val values: Ts) {
    fun <T> get(ix: @TypeIndex(Ts::class, T::class) Int): T = values[ix] as T
}

fun test() {
    val string: String = "string"
    val int: Int = 2
    val tuple = Tuple<String, Int>(string, int)
    val first = tuple.get(0)
    val second = tuple.get(1)
}