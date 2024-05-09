package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.prof18.feedflow.desktop.openInBrowser
import com.prof18.feedflow.shared.ui.readermode.ReaderModeContent
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
internal fun ReaderModeScreen(
    readerModeViewModel: ReaderModeViewModel,
    navigateBack: () -> Unit,
) {
    val state by readerModeViewModel.readerModeState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val message = LocalFeedFlowStrings.current.linkCopiedSuccess

    ReaderModeContent(
        readerModeState = state,
        navigateBack = navigateBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        openInBrowser = { url ->
            openInBrowser(url)
        },
        onShareClick = { url ->
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(url), null)

            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short,
                )
            }
        },
        readerModeSuccessView = { contentPadding, successState ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState()),

            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.regular),
                    text = LocalFeedFlowStrings.current.readerModeWarning,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Light,
                )

                Markdown(
                    modifier = Modifier
                        .padding(Spacing.regular),
                    content = successState.readerModeData.content,
                    typography = markdownTypography(
                        h1 = MaterialTheme.typography.displaySmall,
                        h2 = MaterialTheme.typography.titleLarge,
                        h3 = MaterialTheme.typography.titleLarge,
                        h4 = MaterialTheme.typography.titleMedium,
                        h5 = MaterialTheme.typography.titleMedium,
                        h6 = MaterialTheme.typography.titleMedium,
                        paragraph = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 26.sp,
                        ),
                    ),
                )
            }
        },
    )
}
