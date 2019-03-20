// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Tuple<Ts...> (vararg val values: Ts) {
    fun <T> get(
        ix: @TypeIndex(Ts::class, T::class) Int
    ): T = values[ix] as T
}

data class MyClass1(val first: Int, val second: String)
data class MyClass2(val first: List<Int>, val second: List<MyClass1>)

fun test() {
    val tuple = Tuple<MyClass1, MyClass2, Int>(
        MyClass1(11, "str"),
        MyClass2(
            listOf(1,2,3),
            listOf(MyClass1(12, "str2"))
        ),
        33
    )
    val first: MyClass1 = tuple.get(0)
    val second: MyClass2 = tuple.get(1)
    val third: Int = tuple.get(2)
}