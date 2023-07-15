package com.prof18.feedflow

import FeedFlowTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.prof18.feedflow.addfeed.AddFeedScreen
import com.prof18.feedflow.feedsourcelist.FeedSourceListScreen
import com.prof18.feedflow.home.HomeScreen
import com.prof18.feedflow.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {

            val systemUiController = rememberSystemUiController()
            val darkTheme = isSystemInDarkTheme()

            // Update the dark content of the system bars to match the theme
            DisposableEffect(systemUiController, darkTheme) {
                systemUiController.systemBarsDarkContentEnabled = !darkTheme
                onDispose {}
            }

            FeedFlowTheme {

                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = Screen.Home.name) {
                        composable(Screen.Home.name) {
                            HomeScreen(
                                onSettingsButtonClicked = {
                                    navController.navigate(Screen.Settings.name)
                                }
                            )
                        }

                        composable(Screen.Settings.name) {
                            SettingsScreen(
                                onFeedListClick = {
                                    navController.navigate(Screen.FeedList.name)
                                },
                                 navigateBack = {
                                     navController.popBackStack()
                                 },
                            )
                        }

                        composable(Screen.AddFeed.name) {
                            AddFeedScreen(
                                navigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.FeedList.name) {
                            FeedSourceListScreen(
                                onAddFeedClick = {
                                    navController.navigate(Screen.AddFeed.name)
                                },
                                navigateBack = {
                                    navController.popBackStack()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
