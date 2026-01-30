package com.example.chistanland

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chistanland.ui.LearningViewModel
import com.example.chistanland.ui.screens.IslandMapScreen
import com.example.chistanland.ui.screens.LearningSessionScreen
import com.example.chistanland.ui.theme.ChistanLandTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChistanLandTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: LearningViewModel = viewModel()
                    ChistanApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun ChistanApp(viewModel: LearningViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "map") {
        composable("map") {
            IslandMapScreen(
                viewModel = viewModel,
                onStartItem = { item ->
                    viewModel.startLearning(item)
                    navController.navigate("learning")
                }
            )
        }
        composable("learning") {
            LearningSessionScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
