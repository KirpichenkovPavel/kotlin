// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...>(vararg values: Ts) {
    private val _values: Array<Any?> = arrayOf(*values)
    fun <T1> get1(ix: @TypeIndex(Ts::class, T1::class) Int): T1 = _values[ix] as T1
    fun <T2> get2(ix: @TypeIndex(Ts::class, T2::class) Int): T2 = get1(ix)
    fun <T3> get3(ix: @TypeIndex(Ts::class, T3::class) Int) = get2(ix)
}

fun test() {
    val tuple = Tuple("s", 66)
    val v1: Int = tuple.get2(1)
    val v2 = tuple.get2(1)
    val v3 = tuple.get3(1)
}
