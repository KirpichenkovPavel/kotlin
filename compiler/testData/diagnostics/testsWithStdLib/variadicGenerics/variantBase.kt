// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST
import kotlin.experimental.*

class Variant<Ts...> () {
    private var value: Any? = null
    private var index: Int = -1

    operator fun <T> set (
        ix: @TypeIndex(Ts::class, T::class) Int,
        value: T
    ) {
        this.value = value
        index = ix
    }

    operator fun <T> get (ix: @TypeIndex(Ts::class, T::class) Int): T? {
        if (index == -1) throw java.lang.Exception() // not initialized
        if (ix != index) return null
        return value as T
    }
}

fun test() {
    val variant = Variant<Int, String>()
    variant[0] = 15
    val present: Int? = variant[0]
    val absent: String? = variant[1]
    val typeMismatch: String? = <!TYPE_MISMATCH, TYPE_MISMATCH!>variant[0]<!>
}

fun test2() {
    val variant = Variant<Int, String>()
    <!TYPE_MISMATCH, TYPE_MISMATCH!>variant[0]<!> = "bar"
    variant[<!VARIADIC_TYPE_PARAMETER_INDEX_OUT_OF_BOUNDS!>2<!>] = Unit
}