package com.prometheuscoach.mobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.prometheuscoach.mobile.ui.components.BottomNavItem
import com.prometheuscoach.mobile.ui.components.CoachBottomBar
import com.prometheuscoach.mobile.ui.components.QuickAction
import com.prometheuscoach.mobile.ui.screens.auth.AuthScreen
import com.prometheuscoach.mobile.ui.screens.auth.AuthViewModel
import com.prometheuscoach.mobile.ui.screens.auth.EmailVerifiedScreen
import com.prometheuscoach.mobile.ui.screens.calendar.CalendarScreen
import com.prometheuscoach.mobile.ui.screens.community.CommunityScreen
import com.prometheuscoach.mobile.ui.screens.community.CreatePostScreen
import com.prometheuscoach.mobile.ui.screens.community.PostDetailScreen
import com.prometheuscoach.mobile.ui.screens.chat.ChatScreen
import com.prometheuscoach.mobile.ui.screens.chat.ConversationsScreen
import com.prometheuscoach.mobile.ui.screens.clients.ClientDetailScreen
import com.prometheuscoach.mobile.ui.screens.clients.ClientNutritionScreen
import com.prometheuscoach.mobile.ui.screens.clients.ClientProgressScreen
import com.prometheuscoach.mobile.ui.screens.clients.ClientsScreen
import com.prometheuscoach.mobile.ui.screens.dashboard.DashboardScreen
import com.prometheuscoach.mobile.ui.screens.workouts.WorkoutDetailScreen
import com.prometheuscoach.mobile.ui.screens.workouts.WorkoutsScreen
import com.prometheuscoach.mobile.ui.screens.library.LibraryScreen
import com.prometheuscoach.mobile.ui.screens.library.ExerciseDetailScreen
import com.prometheuscoach.mobile.ui.screens.programs.CreateProgramScreen
import com.prometheuscoach.mobile.ui.screens.programs.ProgramDetailScreen
import com.prometheuscoach.mobile.ui.screens.settings.SettingsScreen

@Composable
fun PrometheusNavHost(
    deepLinkUri: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    val startDestination: NavRoute = if (authState.isAuthenticated) {
        NavRoute.Community
    } else {
        NavRoute.Auth
    }

    // Handle deep links for auth callback
    LaunchedEffect(deepLinkUri) {
        deepLinkUri?.let { uri ->
            when {
                uri.contains("auth/callback") || uri.contains("prometheuscoach://auth") -> {
                    // Handle Supabase auth callback
                    authViewModel.handleAuthCallback(uri)
                    navController.navigate(NavRoute.EmailVerified) {
                        popUpTo(NavRoute.Auth) { inclusive = false }
                    }
                    onDeepLinkHandled()
                }
            }
        }
    }

    // Track current route for bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Routes where bottom bar should be shown
    val mainRoutes = listOf(
        "com.prometheuscoach.mobile.ui.navigation.NavRoute.Community",
        "com.prometheuscoach.mobile.ui.navigation.NavRoute.Clients/{openAddSheet}",
        "com.prometheuscoach.mobile.ui.navigation.NavRoute.Calendar",
        "com.prometheuscoach.mobile.ui.navigation.NavRoute.Library"
    )
    val showBottomBar = authState.isAuthenticated && mainRoutes.any { currentRoute?.contains(it.substringBefore("/")) == true }

    // Map route to bottom nav item
    fun getBottomNavRoute(route: String?): String? {
        return when {
            route?.contains("Community") == true -> BottomNavItem.Home.route
            route?.contains("Clients") == true -> BottomNavItem.Clients.route
            route?.contains("Calendar") == true -> BottomNavItem.Calendar.route
            route?.contains("Library") == true -> BottomNavItem.Library.route
            else -> null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = if (showBottomBar) Modifier.padding(bottom = 80.dp) else Modifier
        ) {
        composable<NavRoute.Auth> {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoute.Community) {
                        popUpTo(NavRoute.Auth) { inclusive = true }
                    }
                }
            )
        }

        composable<NavRoute.EmailVerified> {
            EmailVerifiedScreen(
                onContinueToLogin = {
                    navController.navigate(NavRoute.Auth) {
                        popUpTo(NavRoute.EmailVerified) { inclusive = true }
                    }
                }
            )
        }

        composable<NavRoute.Dashboard> {
            DashboardScreen(
                onNavigateToClients = { navController.navigate(NavRoute.Clients()) },
                onNavigateToCalendar = { navController.navigate(NavRoute.Calendar) },
                onNavigateToSettings = { navController.navigate(NavRoute.Settings) },
                onNavigateToClientDetail = { clientId ->
                    navController.navigate(NavRoute.ClientDetail(clientId))
                },
                onNavigateToWorkouts = { navController.navigate(NavRoute.Workouts) },
                onNavigateToLibrary = { navController.navigate(NavRoute.Library) },
                onNavigateToConversations = { navController.navigate(NavRoute.Conversations) },
                onNavigateToAddClient = { navController.navigate(NavRoute.Clients(openAddSheet = true)) }
            )
        }

        composable<NavRoute.Community> {
            CommunityScreen(
                onNavigateToCreatePost = { navController.navigate(NavRoute.CreatePost) },
                onNavigateToPostDetail = { postId ->
                    navController.navigate(NavRoute.PostDetail(postId))
                },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(NavRoute.UserProfile(userId))
                },
                onNavigateToSettings = { navController.navigate(NavRoute.Settings) }
            )
        }

        composable<NavRoute.CreatePost> {
            CreatePostScreen(
                onNavigateBack = { navController.popBackStack() },
                onPostCreated = {
                    navController.popBackStack()
                }
            )
        }

        composable<NavRoute.PostDetail> { backStackEntry ->
            val route: NavRoute.PostDetail = backStackEntry.toRoute()
            PostDetailScreen(
                postId = route.postId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(NavRoute.UserProfile(userId))
                }
            )
        }

        composable<NavRoute.Clients> { backStackEntry ->
            val route: NavRoute.Clients = backStackEntry.toRoute()
            ClientsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToClientDetail = { clientId ->
                    navController.navigate(NavRoute.ClientDetail(clientId))
                },
                openAddClientSheet = route.openAddSheet
            )
        }

        composable<NavRoute.ClientDetail> { backStackEntry ->
            val route: NavRoute.ClientDetail = backStackEntry.toRoute()
            ClientDetailScreen(
                clientId = route.clientId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { conversationId ->
                    navController.navigate(NavRoute.Chat(conversationId))
                },
                onNavigateToWorkouts = { navController.navigate(NavRoute.Workouts) },
                onNavigateToProgress = {
                    navController.navigate(NavRoute.ClientProgress(route.clientId))
                },
                onNavigateToNutrition = { clientId, clientName ->
                    navController.navigate(NavRoute.ClientNutrition(clientId, clientName))
                }
            )
        }

        composable<NavRoute.ClientProgress> { backStackEntry ->
            val route: NavRoute.ClientProgress = backStackEntry.toRoute()
            ClientProgressScreen(
                clientId = route.clientId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NavRoute.ClientNutrition> { backStackEntry ->
            val route: NavRoute.ClientNutrition = backStackEntry.toRoute()
            ClientNutritionScreen(
                clientId = route.clientId,
                clientName = route.clientName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NavRoute.Calendar> {
            CalendarScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NavRoute.Settings> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(NavRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<NavRoute.Conversations> {
            ConversationsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { conversationId ->
                    navController.navigate(NavRoute.Chat(conversationId))
                }
            )
        }

        composable<NavRoute.Chat> { backStackEntry ->
            val route: NavRoute.Chat = backStackEntry.toRoute()
            ChatScreen(
                conversationId = route.conversationId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NavRoute.Library> {
            LibraryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWorkoutDetail = { workoutId ->
                    navController.navigate(NavRoute.WorkoutDetail(workoutId))
                },
                onNavigateToExerciseDetail = { exerciseId ->
                    navController.navigate(NavRoute.ExerciseDetail(exerciseId))
                },
                onNavigateToCreateProgram = {
                    navController.navigate(NavRoute.CreateProgram)
                }
            )
        }

        composable<NavRoute.Workouts> {
            WorkoutsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWorkoutDetail = { workoutId ->
                    navController.navigate(NavRoute.WorkoutDetail(workoutId))
                }
            )
        }

        composable<NavRoute.ExerciseDetail> { backStackEntry ->
            val route: NavRoute.ExerciseDetail = backStackEntry.toRoute()
            ExerciseDetailScreen(
                exerciseId = route.exerciseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NavRoute.WorkoutDetail> { backStackEntry ->
            val route: NavRoute.WorkoutDetail = backStackEntry.toRoute()
            WorkoutDetailScreen(
                workoutId = route.workoutId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NavRoute.CreateProgram> {
            CreateProgramScreen(
                onNavigateBack = { navController.popBackStack() },
                onProgramCreated = { programId ->
                    // Navigate to program detail after creation
                    navController.navigate(NavRoute.ProgramDetail(programId)) {
                        popUpTo(NavRoute.CreateProgram) { inclusive = true }
                    }
                }
            )
        }

        composable<NavRoute.ProgramDetail> { backStackEntry ->
            val route: NavRoute.ProgramDetail = backStackEntry.toRoute()
            ProgramDetailScreen(
                programId = route.programId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWorkoutDetail = { workoutId ->
                    navController.navigate(NavRoute.WorkoutDetail(workoutId))
                }
            )
        }
    }

        // Bottom Bar with FAB
        if (showBottomBar) {
            CoachBottomBar(
                currentRoute = getBottomNavRoute(currentRoute),
                onNavigate = { route ->
                    when (route) {
                        BottomNavItem.Home.route -> {
                            navController.navigate(NavRoute.Community) {
                                popUpTo(NavRoute.Community) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        BottomNavItem.Clients.route -> {
                            navController.navigate(NavRoute.Clients()) {
                                launchSingleTop = true
                            }
                        }
                        BottomNavItem.Calendar.route -> {
                            navController.navigate(NavRoute.Calendar) {
                                launchSingleTop = true
                            }
                        }
                        BottomNavItem.Library.route -> {
                            navController.navigate(NavRoute.Library) {
                                launchSingleTop = true
                            }
                        }
                    }
                },
                onQuickAction = { action ->
                    when (action) {
                        QuickAction.NEW_POST -> {
                            navController.navigate(NavRoute.CreatePost)
                        }
                        QuickAction.ADD_CLIENT -> {
                            navController.navigate(NavRoute.Clients(openAddSheet = true))
                        }
                        QuickAction.NEW_WORKOUT -> {
                            navController.navigate(NavRoute.Workouts)
                        }
                        QuickAction.NEW_PROGRAM -> {
                            navController.navigate(NavRoute.CreateProgram)
                        }
                        QuickAction.SCHEDULE_EVENT -> {
                            navController.navigate(NavRoute.Calendar)
                        }
                        QuickAction.SEND_MESSAGE -> {
                            navController.navigate(NavRoute.Conversations)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } // End of Box
}
