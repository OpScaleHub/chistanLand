package com.github.opscalehub.chistanland

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.opscalehub.chistanland.ui.LearningViewModel
import com.github.opscalehub.chistanland.ui.screens.HomeScreen
import com.github.opscalehub.chistanland.ui.screens.IslandMapScreen
import com.github.opscalehub.chistanland.ui.screens.LearningSessionScreen
import com.github.opscalehub.chistanland.ui.screens.ParentDashboardScreen
import com.github.opscalehub.chistanland.ui.theme.ChistanLandTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChistanLandTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            val viewModel: LearningViewModel = viewModel()
                            ChistanApp(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChistanApp(viewModel: LearningViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.9f) },
        exitTransition = { fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 1.1f) },
        popEnterTransition = { fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 1.1f) },
        popExitTransition = { fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 0.9f) }
    ) {
        composable("home") {
            HomeScreen(
                onSelectCategory = { category ->
                    viewModel.selectCategory(category)
                    navController.navigate("map")
                },
                onOpenParentPanel = {
                    navController.navigate("parent")
                }
            )
        }
        composable("map") {
            IslandMapScreen(
                viewModel = viewModel,
                onStartItem = { item ->
                    viewModel.startLearning(item)
                    navController.navigate("learning")
                },
                onStartReview = { allowedItems ->
                    viewModel.startReviewSession(allowedItems) {
                        navController.navigate("learning")
                    }
                },
                onOpenParentPanel = {
                    navController.navigate("parent")
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("learning") {
            LearningSessionScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("parent") {
            ParentDashboardScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
