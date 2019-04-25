// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg values: Ts) {
    private val _values: Array<Any?> = arrayOf(*values)
    fun <T> get(ix: @TypeIndex(Ts::class, T::class) Int): T = _values[ix] as T
}

fun test() {
    val string: String = "string"
    val int: Int = 2
    val tuple = Tuple<String, Int>(string, int)
    val first = tuple.get(0)
    val second = tuple.get(1)
}