package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun DesktopLoadingOperationDialog(feedOperation: FeedOperation) {
    val strings = LocalFeedFlowStrings.current
    val title = when (feedOperation) {
        FeedOperation.Deleting -> strings.deletingFeedDialogTitle
        FeedOperation.MarkingAllRead -> strings.markingAllReadDialogTitle
        FeedOperation.None -> return
    }
    Dialog(title = title, onCloseRequest = {}, alertStyle = true) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(title)
        }
    }
}

@Composable
internal fun ReaderEmptyState() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = rememberVectorPainter(Icons.Outlined.Article),
            contentDescription = null,
            tint = JewelTheme.globalColors.text.info.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 16.dp).size(48.dp),
        )
        Text(
            LocalFeedFlowStrings.current.selectArticleToRead,
            color = JewelTheme.globalColors.text.info,
            fontSize = 16.sp,
        )
    }
}
