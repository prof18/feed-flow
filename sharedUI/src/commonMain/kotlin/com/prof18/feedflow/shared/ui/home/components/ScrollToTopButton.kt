package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.prof18.feedflow.shared.ui.style.MotionDurations
import com.prof18.feedflow.shared.ui.utils.ConditionalAnimatedVisibility
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun ScrollToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    shape: Shape = FloatingActionButtonDefaults.shape,
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    border: BorderStroke? = null,
) {
    ConditionalAnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = MotionDurations.Standard)) +
            scaleIn(animationSpec = tween(durationMillis = MotionDurations.Standard)),
        exit = fadeOut(animationSpec = tween(durationMillis = MotionDurations.Standard)) +
            scaleOut(animationSpec = tween(durationMillis = MotionDurations.Standard)),
    ) {
        val buttonModifier = if (border != null) {
            modifier.border(border, shape)
        } else {
            modifier
        }

        FloatingActionButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = elevation,
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = LocalFeedFlowStrings.current.scrollToTopButtonContentDescription,
            )
        }
    }
}
