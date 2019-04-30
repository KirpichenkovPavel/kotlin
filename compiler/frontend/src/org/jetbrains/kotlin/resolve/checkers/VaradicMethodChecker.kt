/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.callUtil.getVariadicTypeArgsFromDispatchReceiver
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.util.isTypeIndex
import org.jetbrains.kotlin.resolve.calls.util.targetedVariadicParameterOfTypeIndex
import org.jetbrains.kotlin.resolve.calls.util.typeIndexAnnotation
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class VariadicMethodCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val typeIndexArguments = getTypeIndexArguments(resolvedCall)
        for ((valueParameter, valueArgument) in typeIndexArguments) {
            val reportTarget = valueArgument.safeAs<ExpressionValueArgument>()?.valueArgument?.asElement() ?: reportOn
            checkTypeIndex(valueParameter, valueArgument, resolvedCall, reportTarget, context)
        }
    }

    private fun checkTypeIndex(
        valueParameter: ValueParameterDescriptor,
        valueArgument: ResolvedValueArgument,
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val indexValue = integerArgumentValueIfConstant(valueArgument, context)
        val argumentIsCorrectTypeIndex = argumentTypeIndexMatches(valueParameter, valueArgument, context)
        if (indexValue == null && !argumentIsCorrectTypeIndex) {
            context.trace.reportDiagnosticOnce(Errors.VARIADIC_TYPE_PARAMETER_NOT_COMPILE_TIME_CONSTANT_INDEX.on(reportOn))
            return
        }
        val receiverVariadicTypeArguments = resolvedCall.getVariadicTypeArgsFromDispatchReceiver()
        if (receiverVariadicTypeArguments == null) {
            context.trace.reportDiagnosticOnce(Errors.TYPE_INDEX_ON_NON_VARIADIC_RECEIVER.on(reportOn))
            return
        }
        if (indexValue != null && (indexValue < 0 || indexValue > receiverVariadicTypeArguments.lastIndex)) {
            context.trace.reportDiagnosticOnce(Errors.VARIADIC_TYPE_PARAMETER_INDEX_OUT_OF_BOUNDS.on(reportOn))
        }
    }

    private fun getTypeIndexArguments(resolvedCall: ResolvedCall<*>) =
        resolvedCall.valueArguments.entries.filter { entry -> entry.key.type.isTypeIndex() }

    private fun integerArgumentValueIfConstant(valueArgument: ResolvedValueArgument, context: CallCheckerContext) =
        context.trace.get(
            BindingContext.COMPILE_TIME_VALUE,
            valueArgument.arguments.first().getArgumentExpression()
        )?.safeAs<IntegerValueTypeConstant>()?.getNumberValue()?.toInt()

    private fun argumentTypeIndexMatches(
        valueParameter: ValueParameterDescriptor,
        valueArgument: ResolvedValueArgument,
        context: CallCheckerContext
    ): Boolean {
        val typeIndexFromParameter = valueParameter.type.typeIndexAnnotation()
        val typeIndexFromArgument = valueArgument.arguments.singleOrNull()
            ?.getArgumentExpression()?.getType(context.trace.bindingContext)?.typeIndexAnnotation()
        return typeIndexFromArgument?.targetedVariadicParameterOfTypeIndex() == typeIndexFromParameter?.targetedVariadicParameterOfTypeIndex()
    }
}