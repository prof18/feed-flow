package com.prof18.feedflow.desktop.addfeed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.desktop.utils.generateUniqueKey
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

internal data class AddFeedFullScreen(
    private val onFeedAdded: () -> Unit,
) : Screen {

    override val key: String = generateUniqueKey()

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        AddFeedScreenContent(
            onFeedAdded = onFeedAdded,
            topAppBar = {
                TopAppBar(
                    title = {
                        Text(LocalFeedFlowStrings.current.addFeed)
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                navigator.pop()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
        )
    }
}
