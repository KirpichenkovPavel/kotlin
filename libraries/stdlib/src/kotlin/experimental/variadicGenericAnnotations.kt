/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.experimental

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class TypeArguments(vararg val types: kotlin.reflect.KClass<*>)

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class TypeIndex(val variadicParameter: kotlin.reflect.KClass<*>, val targetType: kotlin.reflect.KClass<*>)