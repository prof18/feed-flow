package com.prof18.feedflow.about

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.MR
import com.prof18.feedflow.openInBrowser
import com.prof18.feedflow.scrollbarStyle
import com.prof18.feedflow.ui.style.FeedFlowTheme
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutContent() {
    FeedFlowTheme {
        Scaffold { paddingValues ->

            var showLicensesScreen by remember { mutableStateOf(false) }

            if (showLicensesScreen) {
                LicensesScreen(
                    onBackClick = {
                        showLicensesScreen = false
                    },
                )
            } else {
                val listState = rememberLazyListState()

                Column(
                    modifier = Modifier
                        .padding(paddingValues),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues)
                            .weight(1f)
                            .padding(end = 4.dp),
                    ) {
                        LazyColumn(
                            modifier = Modifier,
                            state = listState,
                        ) {
                            item {
                                AboutTextItem(
                                    modifier = Modifier.padding(Spacing.regular),
                                )
                            }
                            item {
                                AboutButtonItem(
                                    onClick = {
                                        showLicensesScreen = true
                                    },
                                    buttonText = stringResource(MR.strings.open_source_licenses),
                                )
                            }
                        }

                        CompositionLocalProvider(LocalScrollbarStyle provides scrollbarStyle()) {
                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(
                                    scrollState = listState,
                                ),
                            )
                        }
                    }

                    AuthorText(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthorText(modifier: Modifier = Modifier) {
    AnnotatedClickableText(
        modifier = modifier
            .padding(Spacing.medium),
        onTextClick = {
            openInBrowser("https://www.marcogomiero.com")
        },
    )
}

@Composable
private fun AboutButtonItem(
    onClick: () -> Unit,
    buttonText: String,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.regular)
            .padding(vertical = Spacing.small),
        onClick = onClick,
    ) {
        Text(buttonText)
    }
}

@Composable
private fun AboutTextItem(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onBackground,
        text = stringResource(MR.strings.about_the_app),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun AnnotatedClickableText(
    modifier: Modifier = Modifier,
    onTextClick: () -> Unit,
) {
    val annotatedText = buildAnnotatedString {
        append(stringResource(MR.strings.author_label))

        pushStringAnnotation(
            tag = "URL",
            annotation = "https://www.marcogomiero.com",
        )
        withStyle(
            style = SpanStyle(
                color = Color.Blue,
                fontWeight = FontWeight.Bold,
            ),
        ) {
            append(" Marco Gomiero")
        }

        pop()
    }

    ClickableText(
        modifier = modifier,
        text = annotatedText,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onBackground,
        ),
        onClick = { offset ->
            annotatedText.getStringAnnotations(
                tag = "URL",
                start = offset,
                end = offset,
            ).firstOrNull()?.let {
                onTextClick()
            }
        },
    )
}
