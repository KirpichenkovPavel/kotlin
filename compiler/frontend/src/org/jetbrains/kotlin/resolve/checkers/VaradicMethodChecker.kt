/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getVariadicTypeArgsFromDispatchReceiver
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class VariadicMethodCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val typeIndexArguments = getTypeIndexArguments(resolvedCall)
        for ((valueParameter, valueArgument) in typeIndexArguments) {
            val reportTarget = valueArgument.safeAs<ExpressionValueArgument>()?.valueArgument?.asElement() ?: reportOn
            checkTypeIndex(valueArgument, resolvedCall, reportTarget, context)
        }
    }

    private fun getTypeIndexArguments(resolvedCall: ResolvedCall<*>) =
        resolvedCall.valueArguments.entries.filter { entry ->
            entry.key.type.annotations.hasAnnotation(FqName("kotlin.experimental.TypeIndex"))
        }

    private fun getIntegerArgumentValue(valueArgument: ResolvedValueArgument, context: CallCheckerContext) =
        context.trace.get(
            BindingContext.COMPILE_TIME_VALUE,
            valueArgument.arguments.first().getArgumentExpression()
        )?.safeAs<IntegerValueTypeConstant>()?.getNumberValue()?.safeAs<Long>()?.toInt()

    private fun checkTypeIndex(
        valueArgument: ResolvedValueArgument,
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val indexValue = getIntegerArgumentValue(valueArgument, context)
        if (indexValue == null) {
            context.trace.reportDiagnosticOnce(Errors.VARIADIC_TYPE_PARAMETER_NOT_COMPILE_TIME_CONSTANT_INDEX.on(reportOn))
            return
        }
        val receiverVariadicTypeArguments = resolvedCall.getVariadicTypeArgsFromDispatchReceiver()
        if (receiverVariadicTypeArguments == null) {
            context.trace.reportDiagnosticOnce(Errors.TYPE_INDEX_ON_NON_VARIADIC_RECEIVER.on(reportOn))
            return
        }
        if (indexValue < 0 || indexValue > receiverVariadicTypeArguments.lastIndex) {
            context.trace.reportDiagnosticOnce(Errors.VARIADIC_TYPE_PARAMETER_INDEX_OUT_OF_BOUNDS.on(reportOn))
            return
        }
        return
    }
}