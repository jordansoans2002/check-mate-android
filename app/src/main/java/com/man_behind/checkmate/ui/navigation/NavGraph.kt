package com.man_behind.checkmate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.man_behind.checkmate.ui.screens.all_checklists.AllChecklistsScreen
import com.man_behind.checkmate.ui.screens.fill_checklist.FillChecklistScreen

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun NavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AllChecklists,
        modifier = modifier,
    ) {
        composable<AllChecklists> {
            AllChecklistsScreen(
                onChecklistClick = { checklistId ->
                    navController.navigate(
                        FillChecklist(checklistId)
                    )
                }
            )
        }

        composable<FillChecklist> {
            FillChecklistScreen()
        }
    }
}