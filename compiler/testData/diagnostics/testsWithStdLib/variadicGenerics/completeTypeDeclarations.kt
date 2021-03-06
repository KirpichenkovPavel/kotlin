// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg values: Ts) {
    private val _values: Array<Any?> = arrayOf(*values)
    fun <T> get(ix: @TypeIndex(Ts::class, T::class) Int): T = _values[ix] as T
}

fun test() {
    val inInt: Int = 5
    val inDouble: Double = 0.25
    val tuple: Tuple<Int, Double> = Tuple<Int, Double>(inInt, inDouble)
    val outInt: Int = tuple.get(0)
    val outDouble: Double = tuple.get(1)
}