package com.prof18.feedflow.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.resources.Res
import com.prof18.feedflow.desktop.resources.icon
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.desktop.ui.rememberDesktopDarkTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.HorizontalProgressBar

@Composable
@Suppress("MagicNumber")
fun SplashContent(
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    useRoundedCorners: Boolean = true,
    darkTheme: Boolean = rememberDesktopDarkTheme(),
    useOledTheme: Boolean = false,
) {
    val shape = if (useRoundedCorners) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp)
    FeedFlowTheme(darkTheme = darkTheme, useOledTheme = useOledTheme) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(shape)
                .background(JewelTheme.globalColors.panelBackground)
                .border(
                    width = 1.dp,
                    color = JewelTheme.globalColors.borders.normal,
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.icon),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                )
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalProgressBar(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.size(width = 160.dp, height = 6.dp),
                )
            }
        }
    }
}
