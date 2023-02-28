package com.prof18.feedflow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push

@Composable
fun MainContent() {
    val navigation = remember { StackNavigation<Screen>() }

    ChildStack(
        source = navigation,
        initialStack = { listOf(Screen.Home) },
        handleBackButton = true,
        animation = stackAnimation(fade() + scale()),
    ) { screen ->
        when (screen) {
            is Screen.Home -> ListContent(onItemClick = { navigation.push(Screen.Settings) })
            is Screen.Settings -> DetailsContent(text = "screen.text", onBack = navigation::pop)
        }
    }
}

@Composable
fun ListContent(onItemClick: (String) -> Unit) {
    val items = remember { List(100) { "Item $it" } }

    LazyColumn {
        items(items) { item ->
            Text(
                text = item,
                modifier = Modifier
                    .clickable { onItemClick(item) }
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}


@Composable
fun DetailsContent(text: String, onBack: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = text)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) {
            Text(text = "Back")
        }
    }
}
