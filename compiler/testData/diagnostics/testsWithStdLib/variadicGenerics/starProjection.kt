// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg values: Ts) {
    private val _values: Array<Any?> = arrayOf(*values)
    fun <T> get(ix: @TypeIndex(Ts::class, T::class) Int): T = _values[ix] as T
}

fun generator(): Tuple<*> {
    return Tuple(15, "5")
}

fun test() {
    val t = generator()
    val t2 = t as Tuple<Int, String>
    val fst = t2.get(0)
}

fun test2() {
    val multiple: <!VARIADIC_STAR_PROJECTION_MISUSE!>Tuple<*, *><!> = Tuple(1, 2)
    val mixed: <!VARIADIC_STAR_PROJECTION_MISUSE!>Tuple<Int, *><!> = Tuple(15, "")
    val multipleMixed: <!VARIADIC_STAR_PROJECTION_MISUSE!>Tuple<*, Int, *, *, String><!> = Tuple(1, 2, 3, 4, "5")
}