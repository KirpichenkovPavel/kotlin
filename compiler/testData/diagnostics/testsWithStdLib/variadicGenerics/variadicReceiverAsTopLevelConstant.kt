// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg val values: Ts) {
    fun <T> get(ix: @TypeIndex(Ts::class, T::class) Int): T = values[ix] as T
}

val tuple = Tuple<String, Double>("a", 26.5)

fun test() {
    // Have to handle captured types to fix this test and allow non-local instances of variadic classes
    val b: String = tuple.get(0)
}