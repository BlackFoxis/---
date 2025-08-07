package com.blackfoxis.telros.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.blackfoxis.telros.presentation.screens.dictionary.PasswordsScreen
import com.blackfoxis.telros.presentation.screens.generate.GeneratePasswordScreen
import com.blackfoxis.telros.presentation.screens.splash.SplashScreen

//Навигация
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Generate : Screen("generate")
    object Dictionary : Screen("dictionary")
}


@Composable
fun MainNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Generate.route) {
            GeneratePasswordScreen(navController)
        }
        composable(Screen.Dictionary.route) {
            PasswordsScreen(navController)
        }
    }
}