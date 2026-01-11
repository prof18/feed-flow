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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.resources.Res
import com.prof18.feedflow.desktop.resources.icon
import com.prof18.feedflow.shared.ui.theme.rememberDesktopDarkTheme
import org.jetbrains.compose.resources.painterResource

@Composable
@Suppress("MagicNumber")
fun SplashContent(
    modifier: Modifier = Modifier,
    progress: Float = 0f,
) {
    val isDark = rememberDesktopDarkTheme()

    val backgroundColor = if (isDark) Color(0xFF1A1B1F) else Color(0xFFFDFBFF)
    val primaryColor = if (isDark) Color(0xFFABC7FF) else Color(0xFF2C5EA7)
    val borderColor = if (isDark) Color(0xFF3A3B3F) else Color(0xFFE0E0E0)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.icon),
                contentDescription = "FeedFlow Icon",
                modifier = Modifier.size(120.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                progress = { progress },
                color = primaryColor,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}
