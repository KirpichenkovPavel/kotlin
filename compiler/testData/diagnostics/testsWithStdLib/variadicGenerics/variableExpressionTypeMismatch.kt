// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST

import kotlin.experimental.*

class Variadic<Ts...>()

val test: Variadic<Int, String> = <!TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!>Variadic<Int, Double>()<!>
