// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg var values: Ts) {
    fun <T> set(
        ix: @TypeIndex(Ts::class, T::class) Int,
        value: T
    ) {
        values[ix] = value
    }
}

fun test() {
    val tuple: Tuple<Number, String> = Tuple<Number, String>(4, "foo")
    tuple.set(1, "baz")
}