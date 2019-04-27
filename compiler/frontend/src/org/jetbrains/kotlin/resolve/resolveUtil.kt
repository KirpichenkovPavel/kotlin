/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValueFactory
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun PropertyDescriptor.hasBackingField(bindingContext: BindingContext?): Boolean = when {
    kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> overriddenDescriptors.any { it.hasBackingField(bindingContext) }
    source is KotlinSourceElement && bindingContext != null -> bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, this) ?: false
    compileTimeInitializer != null -> true
    getter != null -> false
    else -> true
}

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
    val constValForTypesArray = ConstantValueFactory.createArrayValue(
        types.map {
            val arguments = listOf(TypeProjectionImpl(it))
            val type = KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, kClassDescriptor, arguments)
            KClassValue(type)
        },
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
fun KotlinType.typeIndex() = annotations.findAnnotation(FqName("kotlin.experimental.TypeIndex"))
fun AnnotationDescriptor.targetedTypeParameterOfTypeIndex(): KotlinType? {
    assert(fqName == FqName("kotlin.experimental.TypeIndex")) { "Should only be called on @TypeIndex annotation" }
    return allValueArguments.getValue(Name.identifier("targetType")).value.safeAs()
}

fun AnnotationDescriptor.targetedVariadicParameterOfTypeIndex(): KotlinType? {
    assert(fqName == FqName("kotlin.experimental.TypeIndex")) { "Should only be called on @TypeIndex annotation" }
    return allValueArguments.getValue(Name.identifier("variadicParameter")).value.safeAs()
}