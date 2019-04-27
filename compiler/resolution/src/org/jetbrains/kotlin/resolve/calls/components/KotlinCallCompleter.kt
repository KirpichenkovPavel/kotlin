/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage.Empty.hasContradiction
import org.jetbrains.kotlin.resolve.calls.inference.model.ExpectedTypeConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.VariadicTypeParameterConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.VariadicTypeVariableFromCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.forceResolution
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KotlinCallCompleter(
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    private val kotlinConstraintSystemCompleter: KotlinConstraintSystemCompleter
) {

    fun runCompletion(
        factory: SimpleCandidateFactory,
        candidates: Collection<KotlinResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): CallResolutionResult {
        val diagnosticHolder = KotlinDiagnosticsHolder.SimpleHolder()
        if (candidates.isEmpty()) {
            diagnosticHolder.addDiagnostic(NoneCandidatesCallDiagnostic(factory.kotlinCall))
        }
        if (candidates.size > 1) {
            diagnosticHolder.addDiagnostic(ManyCandidatesCallDiagnostic(factory.kotlinCall, candidates))
        }

        val candidate = prepareCandidateForCompletion(factory, candidates, resolutionCallbacks)
        val completionType = candidate.prepareForCompletion(expectedType, resolutionCallbacks)

        return if (resolutionCallbacks.inferenceSession.shouldRunCompletion(candidate))
            candidate.runCompletion(completionType, diagnosticHolder, resolutionCallbacks)
        else
            candidate.asCallResolutionResult(ConstraintSystemCompletionMode.PARTIAL, diagnosticHolder)
    }

    fun createAllCandidatesResult(
        candidates: Collection<KotlinResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): CallResolutionResult {
        val diagnosticsHolder = KotlinDiagnosticsHolder.SimpleHolder()
        for (candidate in candidates) {
            candidate.prepareForCompletion(expectedType, resolutionCallbacks)
            runCompletion(
                candidate.resolvedCall,
                ConstraintSystemCompletionMode.FULL,
                diagnosticsHolder,
                candidate.getSystem(),
                resolutionCallbacks,
                collectAllCandidatesMode = true
            )
        }
        return AllCandidatesResolutionResult(candidates)
    }

    private fun KotlinResolutionCandidate.runCompletion(
        completionType: ConstraintSystemCompletionMode,
        diagnosticHolder: KotlinDiagnosticsHolder.SimpleHolder,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): CallResolutionResult {
        if (isErrorCandidate()) {
            runCompletion(resolvedCall, ConstraintSystemCompletionMode.FULL, diagnosticHolder, getSystem(), resolutionCallbacks)
            return asCallResolutionResult(completionType, diagnosticHolder)
        }

        runCompletion(resolvedCall, completionType, diagnosticHolder, getSystem(), resolutionCallbacks)

        return asCallResolutionResult(completionType, diagnosticHolder)
    }

    private fun runCompletion(
        resolvedCallAtom: ResolvedCallAtom,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: KotlinDiagnosticsHolder,
        constraintSystem: NewConstraintSystem,
        resolutionCallbacks: KotlinResolutionCallbacks,
        collectAllCandidatesMode: Boolean = false
    ) {
        val returnType = resolvedCallAtom.freshReturnType ?: constraintSystem.builtIns.unitType
        kotlinConstraintSystemCompleter.runCompletion(
            constraintSystem.asConstraintSystemCompleterContext(),
            completionMode,
            listOf(resolvedCallAtom),
            returnType
        ) {
            if (collectAllCandidatesMode) {
                it.setEmptyAnalyzedResults()
            } else {
                postponedArgumentsAnalyzer.analyze(
                    constraintSystem.asPostponedArgumentsAnalyzerContext(),
                    resolutionCallbacks,
                    it,
                    diagnosticsHolder
                )
            }
        }

        constraintSystem.diagnostics.forEach(diagnosticsHolder::addDiagnostic)
    }

    private fun prepareCandidateForCompletion(
        factory: SimpleCandidateFactory,
        candidates: Collection<KotlinResolutionCandidate>,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): KotlinResolutionCandidate {
        val candidate = candidates.singleOrNull()

        // this is needed at least for non-local return checker, because when we analyze lambda we should already bind descriptor for outer call
        candidate?.resolvedCall?.let { resolutionCallbacks.bindStubResolvedCallForCandidate(it) }

        return candidate ?: factory.createErrorCandidate().forceResolution()
    }

    // true if we should complete this call
    private fun KotlinResolutionCandidate.prepareForCompletion(
        expectedType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): ConstraintSystemCompletionMode {
        addConstraintsForVariadicGenerics(expectedType)
        if (expectedType != null && TypeUtils.noExpectedType(expectedType)) return ConstraintSystemCompletionMode.FULL

        val returnType = resolvedCall.candidateDescriptor.returnType?.unwrap() ?: return ConstraintSystemCompletionMode.PARTIAL
        val substitutedType: UnwrappedType
        if (expectedType != null) {
            val returnTypeWithSmartCastInfo = computeReturnTypeWithSmartCastInfo(returnType, resolutionCallbacks)
            substitutedType = resolvedCall.substitutor.substituteKeepAnnotations(returnTypeWithSmartCastInfo)

            if (!resolutionCallbacks.isCompileTimeConstant(resolvedCall, expectedType)) {
                csBuilder.addSubtypeConstraint(substitutedType, expectedType, ExpectedTypeConstraintPosition(resolvedCall.atom))
            }
        } else {
            substitutedType = resolvedCall.substitutor.substituteKeepAnnotations(returnType)
        }
        return if (expectedType != null || csBuilder.isProperType(substitutedType))
            ConstraintSystemCompletionMode.FULL
        else
            ConstraintSystemCompletionMode.PARTIAL
    }

    private fun KotlinResolutionCandidate.computeReturnTypeWithSmartCastInfo(
        returnType: UnwrappedType,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): UnwrappedType {
        if (resolvedCall.atom.callKind != KotlinCallKind.VARIABLE) return returnType
        return resolutionCallbacks.createReceiverWithSmartCastInfo(resolvedCall)?.stableType ?: returnType
    }

    private fun KotlinResolutionCandidate.addConstraintsForVariadicGenerics(expectedType: UnwrappedType?) {
        processTypeIndexes()
        processExpectedType(expectedType)
    }

    private fun KotlinResolutionCandidate.processTypeIndexes() {
        for ((parameter, argument) in typeIndexesFromResolvedCall()) {
            val typeIndexAnnotation = parameter.type.typeIndexAnnotation()!!
            val type = typeIndexAnnotation.targetedTypeParameterOfTypeIndex() ?: continue
            val typeVariableForAdditionalConstraint = resolvedCall.substitutor.freshVariables.singleOrNull { typeVariable ->
                /* TODO candidate descriptor and function descriptor are different instances (why?)
                   Can't use equality check for types or descriptors.*/
                typeVariable.originalTypeParameter.name == type.unwrap().constructor.declarationDescriptor?.name
            } ?: continue
            val indexArgumentType = argumentReceiverType(argument)
            val typeForConstraint = indexArgumentType?.let { indexType ->
                val indexTypeConstructor = indexType.constructor
                when {
                    indexTypeConstructor is IntegerValueTypeConstructor -> expectedTypeForIntegerIndex(indexTypeConstructor)
                    indexType.isTypeIndex() -> expectedTypeForVariableIndex(indexType)
                    else -> null
                }
            } ?: continue
            csBuilder.addEqualityConstraint(
                typeVariableForAdditionalConstraint.defaultType,
                typeForConstraint.unwrap(),
                VariadicTypeParameterConstraintPosition(resolvedCall.atom)
            )
        }
    }

    private fun KotlinResolutionCandidate.typeIndexesFromResolvedCall() =
        resolvedCall.argumentMappingByOriginal.entries.filter { parameterToArgument ->
            parameterToArgument.key.type.annotations.hasAnnotation(
                FqName("kotlin.experimental.TypeIndex")
            )
        }

    private fun KotlinType.isTypeIndex() = annotations.hasAnnotation(FqName("kotlin.experimental.TypeIndex"))
    private fun KotlinType.typeIndexAnnotation() = annotations.findAnnotation(FqName("kotlin.experimental.TypeIndex"))
    private fun AnnotationDescriptor.targetedTypeParameterOfTypeIndex(): KotlinType? {
        assert(fqName == FqName("kotlin.experimental.TypeIndex")) { "Should only be called on @TypeIndex annotation" }
        return allValueArguments.getValue(Name.identifier("targetType")).value.safeAs()
    }

    private fun AnnotationDescriptor.targetedVariadicParameterOfTypeIndex(): KotlinType? {
        assert(fqName == FqName("kotlin.experimental.TypeIndex")) { "Should only be called on @TypeIndex annotation" }
        return allValueArguments.getValue(Name.identifier("variadicParameter")).value.safeAs()
    }

    private fun KotlinResolutionCandidate.expectedTypeForIntegerIndex(argument: IntegerValueTypeConstructor): KotlinType? =
        typeIndexArgumentIntegerValue(argument)?.let { typeFromReceiverAnnotation(it) }

    private fun expectedTypeForVariableIndex(indexType: KotlinType): KotlinType? =
        indexType.typeIndexAnnotation()?.targetedTypeParameterOfTypeIndex()

    private fun argumentReceiverType(argument: ResolvedCallArgument): KotlinType? =
        argument.arguments.singleOrNull().safeAs<ExpressionKotlinCallArgument>()?.receiver?.receiverValue?.type

    private fun typeIndexArgumentIntegerValue(argument: IntegerValueTypeConstructor): Int? = argument.getValue().toInt()

    private fun KotlinResolutionCandidate.typeArgumentsAnnotationFromReceiver(): AnnotationDescriptor? =
        resolvedCall.dispatchReceiverArgument?.receiver?.receiverValue?.originalReceiver()
            ?.type?.annotations?.findAnnotation(FqName("kotlin.experimental.TypeArguments"))

    private fun KotlinResolutionCandidate.typeFromReceiverAnnotation(indexValueArgument: Int): KotlinType? {
        val typeArgsAnnotation = typeArgumentsAnnotationFromReceiver() ?: return null
        val typesArray = typeArgsAnnotation.allValueArguments.get(Name.identifier("types"))?.value
        return typesArray.safeAs<ArrayList<KClassValue>>()?.getOrNull(indexValueArgument)?.value
    }

    private fun KotlinResolutionCandidate.processExpectedType(expectedType: UnwrappedType?) {
        val variadicTypeParameter = resolvedCall.candidateDescriptor.typeParameters.lastOrNull()?.takeIf { it.isVariadic }
        val typeArgumentsFromExpectedType = expectedType?.takeIf { !TypeUtils.noExpectedType(it) }
            ?.annotations?.findAnnotation(FqName("kotlin.experimental.TypeArguments"))
        if (variadicTypeParameter == null || typeArgumentsFromExpectedType == null)
            return
        val extractedTypeArguments = typeArgumentsFromExpectedType.allValueArguments[Name.identifier("types")]?.value
            ?.safeAs<ArrayList<KClassValue>>()?.map { it.value.unwrap() } ?: return
        val variadicTypeVariables = resolvedCall.substitutor.freshVariables
            .mapNotNull { variable -> variable as? VariadicTypeVariableFromCallableDescriptor }
            .filter { variable -> variable.originalTypeParameter == variadicTypeParameter }
            .sortedBy { variable -> variable.index }
        if (extractedTypeArguments.size != variadicTypeVariables.size) {
            // TODO report diagnostic
        }
        variadicTypeVariables.zip(extractedTypeArguments).forEach { (variable, type) ->
            csBuilder.addEqualityConstraint(
                variable.defaultType,
                type,
                ExpectedTypeConstraintPosition(resolvedCall.atom)
            )
        }
    }

    private fun ReceiverValue.originalReceiver(): ReceiverValue {
        var current = original
        while (current !== current.original)
            current = current.original
        return current
    }

    fun KotlinResolutionCandidate.asCallResolutionResult(
        type: ConstraintSystemCompletionMode,
        diagnosticsHolder: KotlinDiagnosticsHolder.SimpleHolder
    ): CallResolutionResult {
        val systemStorage = getSystem().asReadOnlyStorage()
        val allDiagnostics = diagnosticsHolder.getDiagnostics() + this.diagnosticsFromResolutionParts

        if (isErrorCandidate()) {
            return ErrorCallResolutionResult(resolvedCall, allDiagnostics, systemStorage)
        }

        return if (type == ConstraintSystemCompletionMode.FULL) {
            CompletedCallResolutionResult(resolvedCall, allDiagnostics, systemStorage)
        } else {
            PartialCallResolutionResult(resolvedCall, allDiagnostics, systemStorage)
        }
    }

    private fun KotlinResolutionCandidate.isErrorCandidate(): Boolean {
        return ErrorUtils.isError(resolvedCall.candidateDescriptor) || hasContradiction
    }
}
