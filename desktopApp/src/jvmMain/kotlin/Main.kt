import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.prof18.feedflow.DatabaseHelper
import com.prof18.feedflow.FeedSource
import com.prof18.feedflow.ParsedFeedSource
import com.prof18.feedflow.getPlatform
import com.prof18.feedflow.initKoin
import com.prof18.feedflow.initKoinDesktop
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Button(onClick = {
            text = getPlatform().name
        }) {
            Text(text)
        }
    }
}

private val koin = initKoinDesktop().koin
fun main() = application {

    Window(onCloseRequest = ::exitApplication) {
        val dbHelper = koin.get<DatabaseHelper>()

        App()
    }
}
