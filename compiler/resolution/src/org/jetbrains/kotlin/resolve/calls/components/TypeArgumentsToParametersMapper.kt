/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateApplicability
import org.jetbrains.kotlin.utils.SmartList

class TypeArgumentsToParametersMapper {

    sealed class TypeArgumentsMapping(val diagnostics: List<KotlinCallDiagnostic>) {

        abstract fun getTypeArguments(typeParameterDescriptor: TypeParameterDescriptor): List<TypeArgument>

        object NoExplicitArguments : TypeArgumentsMapping(emptyList()) {
            override fun getTypeArguments(typeParameterDescriptor: TypeParameterDescriptor): List<TypeArgument> =
                SmartList(TypeArgumentPlaceholder)
        }

        class TypeArgumentsMappingImpl(
            diagnostics: List<KotlinCallDiagnostic>,
            private val typeParameterToArgumentMap: Map<TypeParameterDescriptor, List<TypeArgument>>
        ) : TypeArgumentsMapping(diagnostics) {
            override fun getTypeArguments(typeParameterDescriptor: TypeParameterDescriptor): List<TypeArgument> =
                typeParameterToArgumentMap[typeParameterDescriptor] ?: SmartList(TypeArgumentPlaceholder)
        }
    }

    fun mapTypeArguments(call: KotlinCall, descriptor: CallableDescriptor): TypeArgumentsMapping {
        if (call.typeArguments.isEmpty()) {
            return TypeArgumentsMapping.NoExplicitArguments
        }

        val last = descriptor.typeParameters.lastOrNull()
        if (last?.isVariadic != true) {
            if (call.typeArguments.size != descriptor.typeParameters.size) {
                return TypeArgumentsMapping.TypeArgumentsMappingImpl(
                    listOf(WrongCountOfTypeArguments(descriptor, call.typeArguments.size)), emptyMap()
                )
            } else {
                val typeParameterToArgumentMap = descriptor.typeParameters.zip(call.typeArguments).associate {
                    Pair(it.first, SmartList(it.second))
                }
                return TypeArgumentsMapping.TypeArgumentsMappingImpl(listOf(), typeParameterToArgumentMap)
            }
        } else {
            if (call.typeArguments.size < descriptor.typeParameters.size - 1) {
                return TypeArgumentsMapping.TypeArgumentsMappingImpl(
                    listOf(WrongCountOfTypeArguments(descriptor, call.typeArguments.size)), emptyMap()
                )
            } else {
                val typeParameterToArgumentMap = descriptor.typeParameters.dropLast(1).zip(call.typeArguments).associate {
                    Pair(it.first, SmartList(it.second))
                }.toMutableMap<TypeParameterDescriptor, List<TypeArgument>>()
                typeParameterToArgumentMap[last] = call.typeArguments.subList(descriptor.typeParameters.lastIndex, call.typeArguments.size)
                return TypeArgumentsMapping.TypeArgumentsMappingImpl(listOf(), typeParameterToArgumentMap)
            }
        }
    }

}

