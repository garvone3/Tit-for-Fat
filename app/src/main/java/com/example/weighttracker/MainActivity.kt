// app/src/main/java/com/weighttracker/MainActivity.kt
package com.weighttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.weighttracker.ui.screens.DashboardScreen
import com.weighttracker.ui.screens.GraphScreen
import com.weighttracker.ui.screens.OnboardingScreen
import com.weighttracker.ui.theme.WeightTrackerTheme
import com.weighttracker.viewmodel.WeightViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeightTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    WeightTrackerApp()
                }
            }
        }
    }
}

@Composable
fun WeightTrackerApp() {
    // Single shared ViewModel drives the onboarding gate
    val vm: WeightViewModel = viewModel()
    val onboardingDone by vm.onboardingDone.collectAsStateWithLifecycle()

    val navController = rememberNavController()

    // Start destination depends on whether onboarding has been completed
    val startDest = if (onboardingDone) "dashboard" else "onboarding"

    NavHost(
        navController    = navController,
        startDestination = startDest
    ) {
        composable("onboarding") {
            OnboardingScreen(
                vm         = vm,
                onComplete = {
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(navController = navController, vm = vm)
        }
        composable("graph") {
            GraphScreen(navController = navController, vm = vm)
        }
    }
}