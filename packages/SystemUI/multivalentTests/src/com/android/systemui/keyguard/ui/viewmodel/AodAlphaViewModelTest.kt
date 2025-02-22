/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.android.systemui.keyguard.ui.viewmodel

import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.systemui.Flags
import com.android.systemui.Flags as AConfigFlags
import com.android.systemui.SysuiTestCase
import com.android.systemui.coroutines.collectLastValue
import com.android.systemui.flags.DisableSceneContainer
import com.android.systemui.flags.EnableSceneContainer
import com.android.systemui.keyguard.data.repository.fakeKeyguardRepository
import com.android.systemui.keyguard.data.repository.fakeKeyguardTransitionRepository
import com.android.systemui.keyguard.shared.model.KeyguardState
import com.android.systemui.keyguard.shared.model.TransitionState
import com.android.systemui.keyguard.shared.model.TransitionStep
import com.android.systemui.kosmos.testScope
import com.android.systemui.scene.data.repository.Idle
import com.android.systemui.scene.data.repository.Transition
import com.android.systemui.scene.data.repository.setSceneTransition
import com.android.systemui.scene.shared.model.Scenes
import com.android.systemui.testKosmos
import com.android.systemui.util.mockito.whenever
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@SmallTest
@RunWith(AndroidJUnit4::class)
class AodAlphaViewModelTest : SysuiTestCase() {

    @Mock private lateinit var goneToAodTransitionViewModel: GoneToAodTransitionViewModel

    private val kosmos = testKosmos()
    private val testScope = kosmos.testScope
    private val keyguardRepository = kosmos.fakeKeyguardRepository
    private val keyguardTransitionRepository = kosmos.fakeKeyguardTransitionRepository

    private lateinit var underTest: AodAlphaViewModel

    private val enterFromTopAnimationAlpha = MutableStateFlow(0f)

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        whenever(goneToAodTransitionViewModel.enterFromTopAnimationAlpha)
            .thenReturn(enterFromTopAnimationAlpha)
        kosmos.goneToAodTransitionViewModel = goneToAodTransitionViewModel

        underTest = kosmos.aodAlphaViewModel
    }

    @Test
    @DisableSceneContainer
    @DisableFlags(Flags.FLAG_MIGRATE_CLOCKS_TO_BLUEPRINT)
    fun alpha_WhenNotGone_clockMigrationFlagIsOff_emitsKeyguardAlpha() =
        testScope.runTest {
            val alpha by collectLastValue(underTest.alpha)

            keyguardTransitionRepository.sendTransitionSteps(
                from = KeyguardState.AOD,
                to = KeyguardState.LOCKSCREEN,
                testScope = testScope,
            )

            keyguardRepository.setKeyguardAlpha(0.5f)
            assertThat(alpha).isEqualTo(0.5f)

            keyguardRepository.setKeyguardAlpha(0.8f)
            assertThat(alpha).isEqualTo(0.8f)
        }

    @Test
    @DisableSceneContainer
    fun alpha_WhenGoneToAod() =
        testScope.runTest {
            val alpha by collectLastValue(underTest.alpha)

            keyguardTransitionRepository.sendTransitionSteps(
                from = KeyguardState.AOD,
                to = KeyguardState.GONE,
                testScope = testScope,
            )
            assertThat(alpha).isEqualTo(0f)

            keyguardTransitionRepository.sendTransitionSteps(
                from = KeyguardState.GONE,
                to = KeyguardState.AOD,
                testScope = testScope,
            )
            enterFromTopAnimationAlpha.value = 0.5f
            assertThat(alpha).isEqualTo(0.5f)

            enterFromTopAnimationAlpha.value = 1f
            assertThat(alpha).isEqualTo(1f)
        }

    @Test
    @EnableSceneContainer
    fun alpha_WhenGoneToAod_scene_container() =
        testScope.runTest {
            val alpha by collectLastValue(underTest.alpha)

            kosmos.setSceneTransition(Transition(from = Scenes.Lockscreen, to = Scenes.Gone))
            keyguardTransitionRepository.sendTransitionSteps(
                from = KeyguardState.AOD,
                to = KeyguardState.UNDEFINED,
                testScope = testScope,
            )
            kosmos.setSceneTransition(Idle(Scenes.Gone))
            assertThat(alpha).isEqualTo(0f)

            kosmos.setSceneTransition(Transition(from = Scenes.Gone, to = Scenes.Lockscreen))
            keyguardTransitionRepository.sendTransitionSteps(
                from = KeyguardState.UNDEFINED,
                to = KeyguardState.AOD,
                testScope = testScope,
            )
            enterFromTopAnimationAlpha.value = 0.5f
            assertThat(alpha).isEqualTo(0.5f)

            enterFromTopAnimationAlpha.value = 1f
            assertThat(alpha).isEqualTo(1f)
        }

    @Test
    @DisableSceneContainer
    fun alpha_WhenGoneToDozing() =
        testScope.runTest {
            val alpha by collectLastValue(underTest.alpha)

            keyguardTransitionRepository.sendTransitionSteps(
                from = KeyguardState.AOD,
                to = KeyguardState.GONE,
                testScope = testScope,
            )
            assertThat(alpha).isEqualTo(0f)

            keyguardTransitionRepository.sendTransitionSteps(
                from = KeyguardState.GONE,
                to = KeyguardState.DOZING,
                testScope = testScope,
            )
            assertThat(alpha).isEqualTo(1f)
        }

    @Test
    @EnableSceneContainer
    fun alpha_WhenGoneToDozing_scene_container() =
        testScope.runTest {
            val alpha by collectLastValue(underTest.alpha)

            kosmos.setSceneTransition(Idle(Scenes.Gone))
            assertThat(alpha).isEqualTo(0f)

            kosmos.setSceneTransition(Transition(from = Scenes.Gone, to = Scenes.Lockscreen))
            keyguardTransitionRepository.sendTransitionSteps(
                from = KeyguardState.UNDEFINED,
                to = KeyguardState.DOZING,
                testScope = testScope,
            )
            assertThat(alpha).isEqualTo(1f)
        }

    @Test
    @DisableSceneContainer
    @EnableFlags(Flags.FLAG_MIGRATE_CLOCKS_TO_BLUEPRINT)
    fun alpha_whenGone_equalsZero() =
        testScope.runTest {
            val alpha by collectLastValue(underTest.alpha)

            keyguardTransitionRepository.sendTransitionStep(
                TransitionStep(
                    from = KeyguardState.LOCKSCREEN,
                    to = KeyguardState.GONE,
                    transitionState = TransitionState.STARTED,
                )
            )
            assertThat(alpha).isNull()

            keyguardTransitionRepository.sendTransitionStep(
                TransitionStep(
                    from = KeyguardState.LOCKSCREEN,
                    to = KeyguardState.GONE,
                    transitionState = TransitionState.RUNNING,
                    value = 0.5f,
                )
            )
            assertThat(alpha).isNull()

            keyguardTransitionRepository.sendTransitionStep(
                TransitionStep(
                    from = KeyguardState.LOCKSCREEN,
                    to = KeyguardState.GONE,
                    transitionState = TransitionState.RUNNING,
                    value = 1f,
                )
            )
            assertThat(alpha).isEqualTo(0f)
        }

    @Test
    @DisableSceneContainer
    fun enterFromTopAlpha() =
        testScope.runTest {
            val alpha by collectLastValue(underTest.alpha)

            keyguardTransitionRepository.sendTransitionStep(
                TransitionStep(
                    from = KeyguardState.GONE,
                    to = KeyguardState.AOD,
                    transitionState = TransitionState.STARTED,
                )
            )

            enterFromTopAnimationAlpha.value = 0.2f
            assertThat(alpha).isEqualTo(0.2f)

            enterFromTopAnimationAlpha.value = 1f
            assertThat(alpha).isEqualTo(1f)
        }

    @Test
    @EnableSceneContainer
    fun enterFromTopAlpha_scene_container() =
        testScope.runTest {
            val alpha by collectLastValue(underTest.alpha)

            kosmos.setSceneTransition(Transition(from = Scenes.Gone, to = Scenes.Lockscreen))
            keyguardTransitionRepository.sendTransitionStep(
                TransitionStep(
                    from = KeyguardState.UNDEFINED,
                    to = KeyguardState.AOD,
                    transitionState = TransitionState.STARTED,
                )
            )

            enterFromTopAnimationAlpha.value = 0.2f
            assertThat(alpha).isEqualTo(0.2f)

            enterFromTopAnimationAlpha.value = 1f
            assertThat(alpha).isEqualTo(1f)
        }
}
