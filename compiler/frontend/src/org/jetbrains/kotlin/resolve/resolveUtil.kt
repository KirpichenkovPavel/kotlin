/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentCall
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun PropertyDescriptor.hasBackingField(bindingContext: BindingContext?): Boolean = when {
    kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> overriddenDescriptors.any { it.hasBackingField(bindingContext) }
    source is KotlinSourceElement && bindingContext != null -> bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, this) ?: false
    compileTimeInitializer != null -> true
    getter != null -> false
    else -> true
}

fun KtExpression.isInsideTypeIndexAnnotation(context: BindingContext): Boolean {
    val parentAnnotationIfAny = this.getParentCall(context)?.callElement?.safeAs<KtAnnotationEntry>()
        ?: return false
    return context.get(BindingContext.ANNOTATION, parentAnnotationIfAny)?.fqName?.equals(
        FqName("kotlin.experimental.TypeIndex")
    ) ?: false
}
