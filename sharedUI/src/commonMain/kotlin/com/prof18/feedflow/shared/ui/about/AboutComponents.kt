package com.prof18.feedflow.shared.ui.about

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.utils.Websites.MG_WEBSITE
import com.prof18.feedflow.shared.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun AuthorText(nameClicked: () -> Unit, modifier: Modifier = Modifier) {
    AnnotatedClickableText(
        modifier = modifier
            .padding(Spacing.medium),
        onTextClick = nameClicked,
    )
}

@Composable
fun AboutButtonItem(
    onClick: () -> Unit,
    buttonText: String,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.regular)
            .padding(vertical = Spacing.small),
        onClick = onClick,
    ) {
        Text(buttonText)
    }
}

@Composable
fun AboutTextItem(
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
            annotation = MG_WEBSITE,
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
