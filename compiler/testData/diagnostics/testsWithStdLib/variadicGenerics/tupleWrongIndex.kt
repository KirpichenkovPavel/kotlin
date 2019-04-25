// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg values: Ts) {
    private val _values: Array<Any?> = arrayOf(*values)
    fun <T> get(ix: @TypeIndex(Ts::class, T::class) Int): T = _values[ix] as T
}

fun testUpper() {
    val tuple = Tuple<Int, String>(15, "15")
    tuple.get(
        <!VARIADIC_TYPE_PARAMETER_INDEX_OUT_OF_BOUNDS!>2<!>
    )
}

fun testLower() {
    val tuple = Tuple<Int, String>(15, "15")
    tuple.get(
        <!VARIADIC_TYPE_PARAMETER_INDEX_OUT_OF_BOUNDS!>-1<!>
    )
}

fun testNonConstantIndex(notAConstant: Int) {
    val tuple = Tuple<Int, String>(15, "15")
    tuple.get(
        <!VARIADIC_TYPE_PARAMETER_NOT_COMPILE_TIME_CONSTANT_INDEX!>notAConstant<!>
    )
}
