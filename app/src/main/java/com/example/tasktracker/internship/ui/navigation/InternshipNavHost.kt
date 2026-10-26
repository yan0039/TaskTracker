package com.example.tasktracker.internship.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tasktracker.data.preference.ThemeMode
import com.example.tasktracker.internship.ui.form.ApplicationFormScreen
import com.example.tasktracker.internship.ui.list.ApplicationListScreen
import com.example.tasktracker.ui.theme.TaskTrackerTheme

private const val APPLICATION_LIST = "applications"
private const val APPLICATION_FORM = "application_form?recordId={recordId}"

/** Two destinations share one form; only the record ID crosses the navigation boundary. */
@Composable
fun InternshipNavHost() {
    val navController = rememberNavController()
    TaskTrackerTheme(themeMode = ThemeMode.SYSTEM) {
        NavHost(navController = navController, startDestination = APPLICATION_LIST) {
            composable(APPLICATION_LIST) {
                ApplicationListScreen(
                    onAdd = { navController.navigate("application_form?recordId=-1") },
                    onEdit = { id -> navController.navigate("application_form?recordId=$id") },
                )
            }
            composable(
                route = APPLICATION_FORM,
                arguments = listOf(navArgument("recordId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }),
            ) {
                ApplicationFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
        }
    }
}
