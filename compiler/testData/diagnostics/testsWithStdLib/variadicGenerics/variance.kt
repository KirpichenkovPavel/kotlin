// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

abstract class Function<out R, Ts...> {
    protected var _arguments: Array<Any?> = emptyArray()

    fun setArguments(vararg arguments: Ts) {
        _arguments = arrayOf(*arguments)
    }

    fun <T> argument(
        index: @TypeIndex(Ts::class, T::class) Int
    ): T = _arguments[index] as T

    abstract operator fun invoke(): R
}

open class Foo<T1, T2> (val v1: T1, val v2: T2)

fun test1() {
    val foo = object: Foo<Int, Int>(1, 2) {
        fun bar() = v1 + v2
    }
    val sum = object: Function<Int, Int, Int>() {
        override operator fun invoke() = argument(0) + argument(1)
    }
    sum.setArguments(15, 7)
    val result = sum()
}
