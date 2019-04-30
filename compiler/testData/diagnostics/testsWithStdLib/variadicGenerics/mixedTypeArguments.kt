// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<T1, T2, Ts...> (val v1: T1, val v2: T2, vararg values: Ts) {
    private val _values: Array<Any?> = arrayOf(*values)
    fun <T> get(
        ix: @TypeIndex(Ts::class, T::class) Int
    ): T = _values[ix] as T
}

fun test() {
    val tuple = Tuple<Int, String, Double, Byte>(2, "3", 4.0, 5)
    val val1: Double = tuple.get(0)
    val val2: Byte = tuple.get(1)
    val val3 = tuple.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>get<!>(<!VARIADIC_TYPE_PARAMETER_INDEX_OUT_OF_BOUNDS!>2<!>)
}