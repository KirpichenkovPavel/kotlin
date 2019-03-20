// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg val values: Ts) {
    fun <T> get(ix: @TypeIndex(Ts::class, T::class) Int): T = values[ix] as T
}

fun producer(): Tuple<*> {
    return Tuple<Int, String>(17, "string")
}

fun consumer() {
    val tuple = producer()
    // Should this be allowed somehow?
    val int: Int = tuple.get(0)
    val string: String = tuple.get(1)
}