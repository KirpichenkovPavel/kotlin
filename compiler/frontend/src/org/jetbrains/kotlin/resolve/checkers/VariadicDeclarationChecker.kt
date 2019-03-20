/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class VariadicDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val descriptorTypeParameters = descriptor.safeAs<ClassifierDescriptorWithTypeParameters>()?.declaredTypeParameters ?: return
        if (descriptorTypeParameters.isEmpty()) return
        val allExceptLast = descriptorTypeParameters.subList(0, descriptorTypeParameters.lastIndex)
        for (typeParameter in allExceptLast) {
            if (typeParameter.isVariadic) {
                val reportTarget = typeParameter.source.getPsi() ?: declaration
                context.trace.reportDiagnosticOnce(Errors.VARIADIC_PARAMETER_IS_NOT_LAST.on(reportTarget))
            }
        }
    }
}