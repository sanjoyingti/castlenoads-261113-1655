package com.aibuilt.castlenoads.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aibuilt.castlenoads.ui.components.CastleBottomBar
import com.aibuilt.castlenoads.ui.screens.AccountScreen
import com.aibuilt.castlenoads.ui.screens.CastScreen
import com.aibuilt.castlenoads.ui.screens.DetailScreen
import com.aibuilt.castlenoads.ui.screens.DownloadsScreen
import com.aibuilt.castlenoads.ui.screens.HomeScreen
import com.aibuilt.castlenoads.ui.screens.OnboardingScreen
import com.aibuilt.castlenoads.ui.screens.PlayerScreen
import com.aibuilt.castlenoads.ui.screens.PremiumScreen
import com.aibuilt.castlenoads.ui.screens.SearchScreen
import com.aibuilt.castlenoads.ui.screens.SplashScreen
import com.aibuilt.castlenoads.ui.screens.WatchlistScreen
import com.aibuilt.castlenoads.ui.theme.CastleTheme

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SEARCH = "search"
    const val WATCHLIST = "watchlist"
    const val DOWNLOADS = "downloads"
    const val ACCOUNT = "account"
    const val PREMIUM = "premium"
    const val CAST = "cast"
    const val DETAIL = "detail/{titleId}"
    const val PLAYER = "player/{titleId}?episodeId={episodeId}&trailer={trailer}"

    fun detail(titleId: String) = "detail/$titleId"

    fun player(titleId: String, episodeId: String?, trailer: Boolean): String {
        val ep = episodeId ?: ""
        return "player/$titleId?episodeId=$ep&trailer=$trailer"
    }
}

private val bottomRoutes = setOf(
    Routes.HOME,
    Routes.SEARCH,
    Routes.WATCHLIST,
    Routes.DOWNLOADS,
    Routes.ACCOUNT
)

@Composable
fun CastleRoot() {
    val vm: CastleViewModel = viewModel()
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CastleTheme(darkTheme = settings.darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val nav = rememberNavController()
            val backStack by nav.currentBackStackEntryAsState()
            val route = backStack?.destination?.route

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (route != null && bottomRoutes.contains(route)) {
                        CastleBottomBar(current = route) { target ->
                            nav.navigate(target) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = nav,
                    startDestination = Routes.SPLASH,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Routes.SPLASH) {
                        SplashScreen(vm) { onboarded ->
                            val next = if (onboarded) Routes.HOME else Routes.ONBOARDING
                            nav.navigate(next) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    }

                    composable(Routes.ONBOARDING) {
                        OnboardingScreen {
                            vm.finishOnboarding()
                            nav.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    }

                    composable(Routes.HOME) {
                        HomeScreen(
                            vm = vm,
                            onOpenTitle = { title -> nav.navigate(Routes.detail(title.id)) },
                            onPlay = { title ->
                                nav.navigate(Routes.player(title.id, title.firstEpisode()?.id, false))
                            },
                            onResume = { title, episodeId ->
                                nav.navigate(Routes.player(title.id, episodeId, false))
                            },
                            onSearch = { nav.navigate(Routes.SEARCH) },
                            onCast = { nav.navigate(Routes.CAST) }
                        )
                    }

                    composable(Routes.SEARCH) {
                        SearchScreen(vm) { title -> nav.navigate(Routes.detail(title.id)) }
                    }

                    composable(Routes.WATCHLIST) {
                        WatchlistScreen(
                            vm = vm,
                            onOpenTitle = { title -> nav.navigate(Routes.detail(title.id)) },
                            onSearch = { nav.navigate(Routes.SEARCH) }
                        )
                    }

                    composable(Routes.DOWNLOADS) {
                        DownloadsScreen(
                            vm = vm,
                            onOpenTitle = { titleId, episodeId ->
                                nav.navigate(Routes.player(titleId, episodeId, false))
                            }
                        )
                    }

                    composable(Routes.ACCOUNT) {
                        AccountScreen(
                            vm = vm,
                            onOpenPremium = { nav.navigate(Routes.PREMIUM) },
                            onCast = { nav.navigate(Routes.CAST) }
                        )
                    }

                    composable(Routes.PREMIUM) {
                        PremiumScreen(onBack = { nav.popBackStack() })
                    }

                    composable(Routes.CAST) {
                        CastScreen(vm = vm, onBack = { nav.popBackStack() })
                    }

                    composable(
                        route = Routes.DETAIL,
                        arguments = listOf(navArgument("titleId") { type = NavType.StringType })
                    ) { entry ->
                        val titleId = entry.arguments?.getString("titleId").orEmpty()
                        DetailScreen(
                            vm = vm,
                            titleId = titleId,
                            onBack = { nav.popBackStack() },
                            onPlay = { title, episodeId, trailer ->
                                nav.navigate(Routes.player(title.id, episodeId, trailer))
                            },
                            onOpenTitle = { title -> nav.navigate(Routes.detail(title.id)) }
                        )
                    }

                    composable(
                        route = Routes.PLAYER,
                        arguments = listOf(
                            navArgument("titleId") { type = NavType.StringType },
                            navArgument("episodeId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("trailer") {
                                type = NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { entry ->
                        val titleId = entry.arguments?.getString("titleId").orEmpty()
                        val episodeId = entry.arguments?.getString("episodeId")?.takeIf { it.isNotBlank() }
                        val trailer = entry.arguments?.getBoolean("trailer") ?: false
                        val destinationId = entry.destination.id
                        PlayerScreen(
                            vm = vm,
                            titleId = titleId,
                            episodeId = episodeId,
                            trailer = trailer,
                            onBack = { nav.popBackStack() },
                            onCast = { nav.navigate(Routes.CAST) },
                            onPlayEpisode = { id, nextEpisodeId, isTrailer ->
                                nav.navigate(Routes.player(id, nextEpisodeId, isTrailer)) {
                                    popUpTo(destinationId) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
