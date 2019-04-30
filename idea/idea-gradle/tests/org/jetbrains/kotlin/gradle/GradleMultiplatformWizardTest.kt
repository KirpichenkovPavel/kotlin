/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.ide.konan.gradle.KotlinGradleNativeMultiplatformModuleBuilder
import org.jetbrains.kotlin.idea.configuration.KotlinGradleMobileMultiplatformModuleBuilder
import org.jetbrains.kotlin.idea.configuration.KotlinGradleMobileSharedMultiplatformModuleBuilder
import org.jetbrains.kotlin.idea.configuration.KotlinGradleSharedMultiplatformModuleBuilder
import org.jetbrains.kotlin.idea.configuration.KotlinGradleWebMultiplatformModuleBuilder
import org.jetbrains.kotlin.idea.test.KotlinSdkCreationChecker
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class GradleMultiplatformWizardTest : AbstractGradleMultiplatformWizardTest() {

    lateinit var sdkCreationChecker: KotlinSdkCreationChecker

    override fun setUp() {
        super.setUp()
        sdkCreationChecker = KotlinSdkCreationChecker()
    }

    override fun tearDown() {
        sdkCreationChecker.removeNewKotlinSdk()
        super.tearDown()
    }

    fun testMobile() {
        // TODO: add import & tests here when we will be able to locate Android SDK automatically (see KT-27635)
        testImportFromBuilder(KotlinGradleMobileMultiplatformModuleBuilder(), performImport = false)
    }

    fun testMobileShared() {
        val builder = KotlinGradleMobileSharedMultiplatformModuleBuilder()
        val project = testImportFromBuilder(builder, "SampleTests", "SampleTestsJVM", metadataInside = true)
        // Native tests can be run on Mac only
        if (HostManager.hostIsMac) {
            runTaskInProject(project, builder.nativeTestName)
        }
    }

    fun testNative() {
        val builder = KotlinGradleNativeMultiplatformModuleBuilder()
        val project = testImportFromBuilder(builder)
        runTaskInProject(project, builder.nativeTestName)
    }

    fun testShared() {
        val builder = KotlinGradleSharedMultiplatformModuleBuilder()
        val project = testImportFromBuilder(builder, "SampleTests", "SampleTestsJVM", "SampleTestsNative", metadataInside = true)
        runTaskInProject(project, builder.nativeTestName)
    }

    fun testSharedWithQualifiedName() {
        val builder = KotlinGradleSharedMultiplatformModuleBuilder()
        val project = testImportFromBuilder(builder, "SampleTests", "SampleTestsJVM", "SampleTestsNative", metadataInside = true, useQualifiedModuleNames = true)
        runTaskInProject(project, builder.nativeTestName)
    }

    fun testWeb() {
        testImportFromBuilder(KotlinGradleWebMultiplatformModuleBuilder(), "SampleTests", "SampleTestsJVM")
    }
}