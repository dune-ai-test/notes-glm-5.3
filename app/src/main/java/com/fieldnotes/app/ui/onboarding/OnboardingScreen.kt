package com.fieldnotes.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val accent: Color,
    val title: String,
    val body: String
)

private val Pages = listOf(
    OnboardingPage(
        icon = Icons.Outlined.Edit,
        accent = FN.butter,
        title = "Notes that feel like paper",
        body = "Write in blocks — text, checklists, highlights and voice memos — saved straight to your device."
    ),
    OnboardingPage(
        icon = Icons.Outlined.Folder,
        accent = FN.peach,
        title = "Made for organising",
        body = "Everything lands in folders. Pin what matters, pick a colour, and find it fast with search and filters."
    ),
    OnboardingPage(
        icon = Icons.Outlined.Lock,
        accent = FN.lilac,
        title = "Private by design",
        body = "No accounts, no cloud. Lock notes with your fingerprint, back up as a ZIP, and your data stays yours."
    )
)

/** Three-page first-launch intro. Shown once; [onFinish] marks it seen. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = 0) { Pages.size }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(FN.bg)
            .statusBarsPadding()
            .systemBarsPadding()
            .padding(horizontal = 32.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                "Skip",
                style = FT.button,
                color = FN.muted,
                modifier = Modifier.clickable(onClick = onFinish)
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val item = Pages[page]
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = item.accent,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = FN.inkFixed,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Text(
                    item.title,
                    style = FT.heroTitle.copy(fontSize = 28.sp),
                    color = FN.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp)
                )
                Text(
                    item.body,
                    style = FT.body,
                    color = FN.muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pages.indices.forEach { index ->
                val selected = pagerState.currentPage == index
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (selected) 22.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (selected) FN.text else FN.dotGray)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = FN.strong,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (pagerState.currentPage < Pages.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                }
        ) {
            Text(
                if (pagerState.currentPage < Pages.lastIndex) "Next" else "Get started",
                style = FT.button,
                color = FN.onStrong,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
            )
        }
        Text(
            "Local-first · No account needed",
            style = FT.monoTiny,
            color = FN.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp)
        )
    }
}
