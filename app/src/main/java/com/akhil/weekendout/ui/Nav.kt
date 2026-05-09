package com.akhil.weekendout.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.akhil.weekendout.ui.detail.DetailScreen
import com.akhil.weekendout.ui.planner.PlannerScreen
import com.akhil.weekendout.ui.results.ResultsScreen
import com.akhil.weekendout.ui.results.ResultsState
import com.akhil.weekendout.ui.saved.SavedScreen

object Routes {
    const val Planner = "planner"
    const val Results = "results"
    const val Saved = "saved"
    const val Detail = "detail/{placeId}"
    fun detail(placeId: String) = "detail/$placeId"
}

@Composable
fun WeekendOutNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.Planner) {
        composable(Routes.Planner) {
            PlannerScreen(
                onSubmitted = { nav.navigate(Routes.Results) },
                onOpenSaved = { nav.navigate(Routes.Saved) }
            )
        }
        composable(Routes.Results) {
            ResultsScreen(
                onOpenDetail = { nav.navigate(Routes.detail(it)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.Saved) {
            SavedScreen(
                onOpenDetail = { nav.navigate(Routes.detail(it)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.Detail) { entry ->
            val placeId = entry.arguments?.getString("placeId") ?: return@composable
            DetailScreen(placeId = placeId, onBack = { nav.popBackStack() })
        }
    }
}
