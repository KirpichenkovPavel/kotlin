// !LANGUAGE: +NewInference

class Correct<T1, T2, Ts...>

class VariadicIsNotLast <
    <!VARIADIC_PARAMETER_IS_NOT_LAST!>Ts...<!>,
    T
>

class MultipleVariadicDeclarations <
    <!VARIADIC_PARAMETER_IS_NOT_LAST!>Ts1...<!>,
    T,
    <!VARIADIC_PARAMETER_IS_NOT_LAST!>Ts2...<!>,
    Ts3...
>
