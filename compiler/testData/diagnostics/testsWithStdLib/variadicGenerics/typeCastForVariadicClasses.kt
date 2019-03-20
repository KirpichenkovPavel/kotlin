// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg val values: Ts) {
    fun <T> get(
        ix: @TypeIndex(Ts::class, T::class) Int
    ): T = values[ix] as T
}

fun foo() {
    val tuple = Tuple<Int, String>(5, "5")
    val asAny: Any = tuple
    var asTuple = asAny as Tuple<Int, String>
    val first: Int = asTuple.get(0)
    val second: String = asTuple.get(1)
}