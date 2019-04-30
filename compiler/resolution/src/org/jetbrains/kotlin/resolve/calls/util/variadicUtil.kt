/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.util

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.resolveClassByFqName
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValueFactory
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun generateTypeArgumentsAnnotation(
    moduleDescriptor: ModuleDescriptor,
    types: List<UnwrappedType>
): AnnotationDescriptor? {
    val annotationClass = moduleDescriptor.resolveClassByFqName(
        FqName("kotlin.experimental.TypeArguments"),
        NoLookupLocation.WHEN_RESOLVING_DEFAULT_TYPE_ARGUMENTS
    )
    val kClassDescriptor = moduleDescriptor.resolveClassByFqName(
        FqName("kotlin.reflect.KClass"),
        NoLookupLocation.WHEN_RESOLVING_DEFAULT_TYPE_ARGUMENTS
    ) ?: return null
    val typeConstructor = annotationClass?.defaultType?.constructor ?: return null
    val annotationArgName = Name.identifier("types")
    val typesWrappedInKClass = types.map {
        KClassValue(KClassValue.Value.LocalClass(it))
    }
    val constValForTypesArray = ConstantValueFactory.createArrayValue(
        typesWrappedInKClass,
        kClassDescriptor.defaultType
    )
    return AnnotationDescriptorImpl(
        KotlinTypeFactory.simpleType(
            Annotations.EMPTY,
            typeConstructor,
            emptyList(),
            false
        ),
        mapOf(annotationArgName to constValForTypesArray),
        SourceElement.NO_SOURCE
    )
}

fun KotlinType.isTypeIndex() = annotations.hasAnnotation(FqName("kotlin.experimental.TypeIndex"))
fun KotlinType.typeIndexAnnotation() = annotations.findAnnotation(FqName("kotlin.experimental.TypeIndex"))
fun KotlinType.hasTypeArgumentsAnnotation() = annotations.hasAnnotation(FqName("kotlin.experimental.TypeArguments"))
fun KotlinType.typeArgumentsAnnotation() = annotations.findAnnotation(FqName("kotlin.experimental.TypeArguments"))

fun AnnotationDescriptor.targetedTypeParameterOfTypeIndex(): KotlinType? {
    assert(fqName == FqName("kotlin.experimental.TypeIndex")) { "Should only be called on @TypeIndex annotation" }
    return getClassLiteralType(Name.identifier("targetType"))
}

fun AnnotationDescriptor.targetedVariadicParameterOfTypeIndex(): KotlinType? {
    assert(fqName == FqName("kotlin.experimental.TypeIndex")) { "Should only be called on @TypeIndex annotation" }
    return getClassLiteralType(Name.identifier("variadicParameter"))
}

private fun AnnotationDescriptor.getClassLiteralType(argName: Name) =
    allValueArguments[argName]?.value?.safeAs<KClassValue.Value.LocalClass>()?.type
