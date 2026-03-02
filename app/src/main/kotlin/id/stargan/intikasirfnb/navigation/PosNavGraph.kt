package id.stargan.intikasirfnb.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.stargan.intikasirfnb.feature.identity.navigation.identityNavGraph
import id.stargan.intikasirfnb.ui.landing.LandingScreen

object PosRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val OUTLET_PICKER = "outlet_picker"
    const val LANDING = "landing"
}

@Composable
fun PosNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = PosRoutes.SPLASH
    ) {
        identityNavGraph(
            navController = navController,
            onLoginSuccess = {
                navController.navigate(PosRoutes.OUTLET_PICKER) {
                    popUpTo(PosRoutes.LOGIN) { inclusive = true }
                }
            },
            onOutletSelected = {
                navController.navigate(PosRoutes.LANDING) {
                    popUpTo(PosRoutes.OUTLET_PICKER) { inclusive = true }
                }
            },
            onOnboardingComplete = {
                navController.navigate(PosRoutes.LANDING) {
                    popUpTo(PosRoutes.ONBOARDING) { inclusive = true }
                }
            }
        )

        composable(PosRoutes.LANDING) {
            LandingScreen(
                onLogout = {
                    navController.navigate(PosRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
