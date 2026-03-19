package com.soundless

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingStep(
    val emoji: String,
    val title: String,
    val items: List<String>,
)

private val emojis = listOf("\u2699\uFE0F", "\uD83D\uDD13", "\uD83D\uDCF6", "\uD83D\uDD17", "\uD83D\uDD07")

@Composable
private fun onboardingSteps(): List<OnboardingStep> {
    val s = LocalStrings.current
    return listOf(
        OnboardingStep(emojis[0], s.onboardingStep1Title, s.onboardingStep1Items),
        OnboardingStep(emojis[1], s.onboardingStep2Title, s.onboardingStep2Items),
        OnboardingStep(emojis[2], s.onboardingStep3Title, s.onboardingStep3Items),
        OnboardingStep(emojis[3], s.onboardingStep4Title, s.onboardingStep4Items),
        OnboardingStep(emojis[4], s.onboardingStep5Title, s.onboardingStep5Items),
    )
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit, langManager: LanguageManager) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    val steps = onboardingSteps()
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        // Header with language toggle
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        "Soundless",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp,
                    )
                }
                Text(
                    strings.onboardingSubtitle,
                    fontSize = 16.sp,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            LanguageToggleButton(
                currentLanguage = language,
                onLanguageSelected = langManager::setLanguage,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(24.dp))

        // Page indicator
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            repeat(steps.size) { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (i == pagerState.currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == pagerState.currentPage) Color(0xFF4A90D9)
                            else Color(0xFF3A3A3C)
                        )
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            val step = steps[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(step.emoji, fontSize = 32.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "${page + 1}/${steps.size}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF4A90D9),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    step.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))

                        step.items.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier.padding(bottom = 14.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    "${idx + 1}.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF4A90D9),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(24.dp),
                                )
                                Text(
                                    item,
                                    fontSize = 14.sp,
                                    color = Color(0xFFCCCCCC),
                                    lineHeight = 22.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom buttons
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Text(strings.previous, color = Color(0xFF8E8E93), fontSize = 16.sp)
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            if (pagerState.currentPage < steps.size - 1) {
                Button(
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90D9)),
                ) {
                    Text(strings.next, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onFinish,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                ) {
                    Text(strings.getStarted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
