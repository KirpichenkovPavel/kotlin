/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage.Empty.hasContradiction
import org.jetbrains.kotlin.resolve.calls.inference.model.ExpectedTypeConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.VariadicTypeParameterConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.VariadicTypeVariableFromCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.forceResolution
import org.jetbrains.kotlin.resolve.calls.util.*
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.types.model.isIntegerLiteralTypeConstructor
import org.jetbrains.kotlin.types.model.typeConstructor

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
        when {
            candidates.isEmpty() -> diagnosticHolder.addDiagnostic(NoneCandidatesCallDiagnostic(factory.kotlinCall))
            candidates.size > 1 -> diagnosticHolder.addDiagnostic(ManyCandidatesCallDiagnostic(factory.kotlinCall, candidates))
        }

        val candidate = prepareCandidateForCompletion(factory, candidates, resolutionCallbacks)
        val returnType = candidate.returnTypeWithSmartCastInfo(resolutionCallbacks)

        candidate.addExpectedTypeConstraint(returnType, expectedType, resolutionCallbacks)
        candidate.addExpectedTypeFromCastConstraint(returnType, resolutionCallbacks)
        candidate.addConstraintsForVariadicGenerics(expectedType)

        return if (resolutionCallbacks.inferenceSession.shouldRunCompletion(candidate))
            candidate.runCompletion(
                candidate.computeCompletionMode(expectedType, returnType),
                diagnosticHolder,
                resolutionCallbacks
            )
        else
            candidate.asCallResolutionResult(ConstraintSystemCompletionMode.PARTIAL, diagnosticHolder)
    }

    fun createAllCandidatesResult(
        candidates: Collection<KotlinResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): CallResolutionResult {
        val completedCandidates = candidates.map { candidate ->
            val diagnosticsHolder = KotlinDiagnosticsHolder.SimpleHolder()

            candidate.addExpectedTypeConstraint(
                candidate.returnTypeWithSmartCastInfo(resolutionCallbacks), expectedType, resolutionCallbacks
            )

            runCompletion(
                candidate.resolvedCall,
                ConstraintSystemCompletionMode.FULL,
                diagnosticsHolder,
                candidate.getSystem(),
                resolutionCallbacks,
                collectAllCandidatesMode = true
            )

            CandidateWithDiagnostics(candidate, diagnosticsHolder.getDiagnostics() + candidate.diagnosticsFromResolutionParts)
        }
        return AllCandidatesResolutionResult(completedCandidates)
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

    private fun KotlinResolutionCandidate.returnTypeWithSmartCastInfo(resolutionCallbacks: KotlinResolutionCallbacks): UnwrappedType? {
        val returnType = resolvedCall.candidateDescriptor.returnType?.unwrap() ?: return null
        val returnTypeWithSmartCastInfo = computeReturnTypeWithSmartCastInfo(returnType, resolutionCallbacks)
        return resolvedCall.substitutor.substituteKeepAnnotations(returnTypeWithSmartCastInfo)
    }

    private fun KotlinResolutionCandidate.addExpectedTypeConstraint(
        returnType: UnwrappedType?,
        expectedType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ) {
        if (returnType == null) return
        if (expectedType == null || TypeUtils.noExpectedType(expectedType)) return

        // This is needed to avoid multiple mismatch errors as we type check resulting type against expected one later
        // Plus, it helps with IDE-tests where it's important to have particular diagnostics.
        // Note that it aligns with the old inference, see CallCompleter.completeResolvedCallAndArguments
        if (csBuilder.currentStorage().notFixedTypeVariables.isEmpty()) return

        // We don't add expected type constraint for constant expression like "1 + 1" because of type coercion for numbers:
        // val a: Long = 1 + 1, note that result type of "1 + 1" will be Int and adding constraint with Long will produce type mismatch
        if (!resolutionCallbacks.isCompileTimeConstant(resolvedCall, expectedType)) {
            csBuilder.addSubtypeConstraint(returnType, expectedType, ExpectedTypeConstraintPosition(resolvedCall.atom))
        }
    }

    private fun KotlinResolutionCandidate.addExpectedTypeFromCastConstraint(
        returnType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ) {
        if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.ExpectedTypeFromCast)) return
        if (returnType == null) return
        val expectedType = resolutionCallbacks.getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedCall) ?: return
        csBuilder.addSubtypeConstraint(returnType, expectedType, ExpectedTypeConstraintPosition(resolvedCall.atom))
    }

    private fun KotlinResolutionCandidate.computeCompletionMode(
        expectedType: UnwrappedType?,
        currentReturnType: UnwrappedType?
    ): ConstraintSystemCompletionMode {
        // Presence of expected type means that we trying to complete outermost call => completion mode should be full
        if (expectedType != null) return ConstraintSystemCompletionMode.FULL

        // This is questionable as null return type can be only for error call
        if (currentReturnType == null) return ConstraintSystemCompletionMode.PARTIAL

        return when {
            // Consider call foo(bar(x)), if return type of bar is a proper one, then we can complete resolve for bar => full completion mode
            // Otherwise, we shouldn't complete bar until we process call foo
            csBuilder.isProperType(currentReturnType) -> ConstraintSystemCompletionMode.FULL

            // Nested call is connected with the outer one through the UPPER constraint (returnType <: expectedOuterType)
            // This means that there will be no new LOWER constraints =>
        //   it's possible to complete call now if there are proper LOWER constraints
            csBuilder.isTypeVariable(currentReturnType) ->
                if (hasProperNonTrivialLowerConstraints(currentReturnType))
                    ConstraintSystemCompletionMode.FULL
                else
                    ConstraintSystemCompletionMode.PARTIAL

            else -> ConstraintSystemCompletionMode.PARTIAL
        }
    }

    private fun KotlinResolutionCandidate.hasProperNonTrivialLowerConstraints(typeVariable: UnwrappedType): Boolean {
        assert(csBuilder.isTypeVariable(typeVariable)) { "$typeVariable is not a type variable" }

        val context = getSystem() as TypeSystemInferenceExtensionContext
        val constructor = typeVariable.constructor
        val variableWithConstraints = csBuilder.currentStorage().notFixedTypeVariables[constructor] ?: return false
        val constraints = variableWithConstraints.constraints
        return constraints.isNotEmpty() && constraints.all {
            !it.type.typeConstructor(context).isIntegerLiteralTypeConstructor(context) &&
                    it.kind.isLower() && csBuilder.isProperType(it.type)
        }
    }

    private fun KotlinResolutionCandidate.computeReturnTypeWithSmartCastInfo(
        returnType: UnwrappedType,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): UnwrappedType {
        if (resolvedCall.atom.callKind != KotlinCallKind.VARIABLE) return returnType
        return resolutionCallbacks.createReceiverWithSmartCastInfo(resolvedCall)?.stableType ?: returnType
    }

    private fun KotlinResolutionCandidate.addConstraintsForVariadicGenerics(expectedType: UnwrappedType?) {
        processTypeIndices()
        processExpectedType(expectedType)
    }

    private fun KotlinResolutionCandidate.processTypeIndices() {
        for ((parameter, argument) in typeIndicesFromResolvedCall()) {
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
                    indexTypeConstructor is IntegerLiteralTypeConstructor -> expectedTypeForIntegerIndex(indexTypeConstructor)
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

    private fun KotlinResolutionCandidate.typeIndicesFromResolvedCall() =
        resolvedCall.argumentMappingByOriginal.entries.filter { parameterToArgument ->
            parameterToArgument.key.type.annotations.hasAnnotation(
                FqName("kotlin.experimental.TypeIndex")
            )
        }

    private fun KotlinResolutionCandidate.expectedTypeForIntegerIndex(argument: IntegerValueTypeConstructor) =
        typeFromReceiverAnnotation(argument.value.toInt())

    private fun KotlinResolutionCandidate.expectedTypeForIntegerIndex(argument: IntegerLiteralTypeConstructor) =
        typeFromReceiverAnnotation(argument.value.toInt())

    private fun expectedTypeForVariableIndex(indexType: KotlinType): KotlinType? =
        indexType.typeIndexAnnotation()?.targetedTypeParameterOfTypeIndex()

    private fun argumentReceiverType(argument: ResolvedCallArgument): KotlinType? =
        argument.arguments.singleOrNull().safeAs<ExpressionKotlinCallArgument>()?.receiver?.receiverValue?.type

    private fun KotlinResolutionCandidate.typeArgumentsAnnotationFromReceiver(): AnnotationDescriptor? =
        resolvedCall.dispatchReceiverArgument?.receiver?.receiverValue
            ?.originalReceiver()?.type?.typeArgumentsAnnotation()

    private fun KotlinResolutionCandidate.typeFromReceiverAnnotation(indexValueArgument: Int): KotlinType? {
        val typeArgsAnnotation = typeArgumentsAnnotationFromReceiver() ?: return null
        val typesArray = typeArgsAnnotation.allValueArguments[Name.identifier("types")]?.value
        val kClassValueByIndex = typesArray.safeAs<ArrayList<KClassValue>>()?.getOrNull(indexValueArgument)
        return kClassValueByIndex?.getArgumentType(resolvedCall.candidateDescriptor.module)
    }

    private fun KotlinResolutionCandidate.processExpectedType(expectedType: UnwrappedType?) {
        val variadicTypeParameter = resolvedCall.candidateDescriptor.typeParameters.lastOrNull()?.takeIf { it.isVariadic }
        val typeArgumentsFromExpectedType = expectedType?.takeIf { !TypeUtils.noExpectedType(it) }
            ?.annotations?.findAnnotation(FqName("kotlin.experimental.TypeArguments"))
        if (variadicTypeParameter == null || typeArgumentsFromExpectedType == null)
            return
        val extractedTypeArguments = typeArgumentsFromExpectedType.allValueArguments[Name.identifier("types")]?.value
            ?.safeAs<ArrayList<KClassValue>>()?.mapNotNull {
                it.getArgumentType(resolvedCall.candidateDescriptor.module).unwrap()
            } ?: return
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
