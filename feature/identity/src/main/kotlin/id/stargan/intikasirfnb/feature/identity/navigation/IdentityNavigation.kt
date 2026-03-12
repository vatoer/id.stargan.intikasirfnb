package id.stargan.intikasirfnb.feature.identity.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import id.stargan.intikasirfnb.feature.identity.ui.login.LoginScreen
import id.stargan.intikasirfnb.feature.identity.ui.onboarding.OnboardingScreen
import id.stargan.intikasirfnb.feature.identity.ui.outlet.OutletPickerScreen
import id.stargan.intikasirfnb.feature.identity.ui.splash.SplashScreen

fun NavGraphBuilder.identityNavGraph(
    navController: NavController,
    onLoginSuccess: () -> Unit,
    onOutletSelected: () -> Unit,
    onOnboardingComplete: () -> Unit,
    onNeedActivation: () -> Unit,
    onDebugSeed: (() -> Unit)? = null
) {
    composable("splash") {
        SplashScreen(
            onNeedActivation = onNeedActivation,
            onNeedOnboarding = {
                if (onDebugSeed != null) {
                    onDebugSeed()
                } else {
                    navController.navigate("onboarding") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            },
            onHasData = {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        )
    }

    composable("onboarding") {
        OnboardingScreen(onOnboardingComplete = onOnboardingComplete)
    }

    composable("login") {
        LoginScreen(onLoginSuccess = onLoginSuccess)
    }

    composable("outlet_picker") {
        OutletPickerScreen(onOutletSelected = onOutletSelected)
    }
}
