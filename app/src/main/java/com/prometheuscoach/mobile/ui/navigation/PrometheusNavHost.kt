package com.prometheuscoach.mobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.ui.components.BetaFeedbackSheet
import com.prometheuscoach.mobile.ui.screens.settings.SettingsViewModel
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.prometheuscoach.mobile.ui.components.BottomNavItem
import com.prometheuscoach.mobile.ui.components.CoachBottomBar
import com.prometheuscoach.mobile.ui.screens.auth.AuthScreen
import com.prometheuscoach.mobile.ui.screens.auth.AuthViewModel
import com.prometheuscoach.mobile.ui.screens.auth.EmailVerifiedScreen
import com.prometheuscoach.mobile.ui.screens.subscription.SubscriptionGateScreen
import com.prometheuscoach.mobile.ui.screens.subscription.SubscriptionViewModel
import com.prometheuscoach.mobile.ui.screens.calendar.CalendarScreen
import com.prometheuscoach.mobile.ui.screens.community.ChallengesScreen
import com.prometheuscoach.mobile.ui.screens.community.CommunityScreen
import com.prometheuscoach.mobile.ui.screens.community.CreatePostScreen
import com.prometheuscoach.mobile.ui.screens.community.PostDetailScreen
import com.prometheuscoach.mobile.ui.screens.chat.ChatScreen
import com.prometheuscoach.mobile.ui.screens.chat.ConversationsScreen
import com.prometheuscoach.mobile.ui.screens.clients.ClientDetailScreen
import com.prometheuscoach.mobile.ui.screens.clients.ClientNutritionScreen
import com.prometheuscoach.mobile.ui.screens.clients.ClientProgressScreen
import com.prometheuscoach.mobile.ui.screens.clients.ClientsScreen
import com.prometheuscoach.mobile.ui.screens.dashboard.CoachDashboardScreen
import com.prometheuscoach.mobile.ui.screens.dashboard.DashboardScreen
import com.prometheuscoach.mobile.ui.screens.workouts.TemplateDetailScreen
import com.prometheuscoach.mobile.ui.screens.workouts.WorkoutDetailScreen
import com.prometheuscoach.mobile.ui.screens.workouts.WorkoutsScreen
import com.prometheuscoach.mobile.ui.screens.library.LibraryScreen
import com.prometheuscoach.mobile.ui.screens.library.ExerciseDetailScreen
import com.prometheuscoach.mobile.ui.screens.programs.CreateProgramScreen
import com.prometheuscoach.mobile.ui.screens.programs.ProgramDetailScreen
import com.prometheuscoach.mobile.ui.screens.settings.SettingsScreen
import com.prometheuscoach.mobile.ui.screens.account.CoachAccountScreen
import com.prometheuscoach.mobile.ui.screens.coach.CoachSetCardScreen
import com.prometheuscoach.mobile.ui.screens.coach.EditCoachProfileScreen
import com.prometheuscoach.mobile.ui.screens.vbt.VBTDashboardScreen
import com.prometheuscoach.mobile.ui.screens.formanalysis.ClientFormAnalysisScreen
import com.prometheuscoach.mobile.ui.screens.ai.AIAssistantScreen
import com.prometheuscoach.mobile.ui.screens.gamification.GamificationDashboardScreen
import com.prometheuscoach.mobile.ui.screens.gamification.ClientGamificationScreen
import com.prometheuscoach.mobile.data.model.AIContextType

@Composable
fun PrometheusNavHost(
    deepLinkUri: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    // After login, first check subscription before going to dashboard
    val startDestination: NavRoute = if (authState.isAuthenticated) {
        NavRoute.SubscriptionGate
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
        "com.prometheuscoach.mobile.ui.navigation.NavRoute.CoachDashboard",
        "com.prometheuscoach.mobile.ui.navigation.NavRoute.Conversations",
        "com.prometheuscoach.mobile.ui.navigation.NavRoute.Clients/{openAddSheet}",
        "com.prometheuscoach.mobile.ui.navigation.NavRoute.Workouts",
        "com.prometheuscoach.mobile.ui.navigation.NavRoute.Calendar"
    )
    val showBottomBar = authState.isAuthenticated && mainRoutes.any { currentRoute?.contains(it.substringBefore("/")) == true }

    // Feedback FAB state
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var isSubmittingFeedback by remember { mutableStateOf(false) }
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()

    // Handle feedback success
    LaunchedEffect(settingsState.feedbackSubmitSuccess) {
        if (settingsState.feedbackSubmitSuccess) {
            showFeedbackSheet = false
            isSubmittingFeedback = false
            settingsViewModel.clearFeedbackSuccess()
        }
    }

    // Get current screen name for feedback context
    val currentScreenName = remember(currentRoute) {
        when {
            currentRoute?.contains("CoachDashboard") == true -> "Dashboard"
            currentRoute?.contains("Clients") == true -> "Clients"
            currentRoute?.contains("ClientDetail") == true -> "Client Detail"
            currentRoute?.contains("Calendar") == true -> "Calendar"
            currentRoute?.contains("Library") == true -> "Library"
            currentRoute?.contains("Workouts") == true -> "Workouts"
            currentRoute?.contains("Community") == true -> "Community"
            currentRoute?.contains("Chat") == true -> "Chat"
            currentRoute?.contains("Settings") == true -> "Settings"
            currentRoute?.contains("Account") == true -> "Account"
            else -> "App"
        }
    }

    // Map route to bottom nav item
    fun getBottomNavRoute(route: String?): String? {
        return when {
            route?.contains("CoachDashboard") == true -> BottomNavItem.Home.route
            route?.contains("Conversations") == true -> BottomNavItem.Messages.route
            route?.contains("Clients") == true -> BottomNavItem.Clients.route
            route?.contains("Workouts") == true -> BottomNavItem.Workouts.route
            route?.contains("Calendar") == true -> BottomNavItem.Calendar.route
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
                    // Navigate to subscription gate to check subscription status
                    navController.navigate(NavRoute.SubscriptionGate) {
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

        // Subscription Gate - checks if coach has active subscription
        composable<NavRoute.SubscriptionGate> {
            val subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
            val subscriptionState by subscriptionViewModel.state.collectAsState()

            // If subscription is active, navigate to dashboard
            LaunchedEffect(subscriptionState) {
                if (!subscriptionState.isLoading && subscriptionState.subscriptionInfo.canAccessApp) {
                    navController.navigate(NavRoute.CoachDashboard) {
                        popUpTo(NavRoute.SubscriptionGate) { inclusive = true }
                    }
                }
            }

            // Show gate screen while loading or if no subscription
            if (subscriptionState.isLoading || !subscriptionState.subscriptionInfo.canAccessApp) {
                SubscriptionGateScreen(
                    viewModel = subscriptionViewModel,
                    onRetry = {
                        // Check again - the screen already handles this
                    }
                )
            }
        }

        composable<NavRoute.Dashboard> {
            DashboardScreen(
                onNavigateToClients = { navController.navigate(NavRoute.Clients()) },
                onNavigateToCalendar = { navController.navigate(NavRoute.Calendar) },
                onNavigateToSettings = { navController.navigate(NavRoute.Account) },
                onNavigateToClientDetail = { clientId ->
                    navController.navigate(NavRoute.ClientDetail(clientId))
                },
                onNavigateToWorkouts = { navController.navigate(NavRoute.Workouts) },
                onNavigateToLibrary = { navController.navigate(NavRoute.Library) },
                onNavigateToConversations = { navController.navigate(NavRoute.Conversations) },
                onNavigateToAddClient = { navController.navigate(NavRoute.Clients(openAddSheet = true)) }
            )
        }

        composable<NavRoute.CoachDashboard> {
            CoachDashboardScreen(
                onNavigateToClientDetail = { clientId ->
                    navController.navigate(NavRoute.ClientDetail(clientId))
                },
                onNavigateToChat = { clientId ->
                    // Navigate to chat with this client
                    navController.navigate(NavRoute.Chat(clientId))
                },
                onNavigateToClients = { navController.navigate(NavRoute.Clients()) },
                onNavigateToAddClient = { navController.navigate(NavRoute.Clients(openAddSheet = true)) },
                onNavigateToSettings = { navController.navigate(NavRoute.Account) },
                onNavigateToPostDetail = { postId ->
                    navController.navigate(NavRoute.PostDetail(postId))
                },
                onNavigateToCreatePost = { navController.navigate(NavRoute.CreatePost) },
                onNavigateToChallenges = { navController.navigate(NavRoute.Challenges) },
                onNavigateToGamification = { navController.navigate(NavRoute.GamificationDashboard) }
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
                onNavigateToSettings = { navController.navigate(NavRoute.Account) }
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

        // UserProfile - redirects to CoachSetCard (since all users are coaches in this app)
        composable<NavRoute.UserProfile> { backStackEntry ->
            val route: NavRoute.UserProfile = backStackEntry.toRoute()
            CoachSetCardScreen(
                coachId = route.userId,
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateToWorkoutDetail = { workoutId ->
                    navController.navigate(NavRoute.WorkoutDetail(workoutId))
                },
                onNavigateToProgress = {
                    navController.navigate(NavRoute.ClientProgress(route.clientId))
                },
                onNavigateToNutrition = { clientId, clientName ->
                    navController.navigate(NavRoute.ClientNutrition(clientId, clientName))
                },
                onNavigateToVBT = { clientId, clientName ->
                    navController.navigate(NavRoute.ClientVBT(clientId, clientName))
                },
                onNavigateToFormAnalysis = { clientId, clientName ->
                    navController.navigate(NavRoute.ClientFormAnalysis(clientId, clientName))
                },
                onNavigateToAI = { clientId, clientName ->
                    navController.navigate(NavRoute.AIAssistantWithContext(
                        contextType = "client_analysis",
                        contextId = clientId,
                        contextName = clientName
                    ))
                },
                onNavigateToGamification = { clientId, clientName ->
                    navController.navigate(NavRoute.ClientGamification(clientId, clientName))
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

        composable<NavRoute.ClientVBT> { backStackEntry ->
            val route: NavRoute.ClientVBT = backStackEntry.toRoute()
            VBTDashboardScreen(
                clientId = route.clientId,
                clientName = route.clientName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NavRoute.ClientFormAnalysis> { backStackEntry ->
            val route: NavRoute.ClientFormAnalysis = backStackEntry.toRoute()
            ClientFormAnalysisScreen(
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

        composable<NavRoute.Account> {
            val currentUserId = authViewModel.getCurrentUserId()
            CoachAccountScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(NavRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onEditPublicProfile = {
                    navController.navigate(NavRoute.EditCoachProfile)
                },
                onPreviewSetCard = {
                    currentUserId?.let { id ->
                        navController.navigate(NavRoute.CoachSetCard(id))
                    }
                }
            )
        }

        composable<NavRoute.EditCoachProfile> {
            val currentUserId = authViewModel.getCurrentUserId()
            EditCoachProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onPreviewSetCard = {
                    currentUserId?.let { id ->
                        navController.navigate(NavRoute.CoachSetCard(id))
                    }
                }
            )
        }

        composable<NavRoute.CoachSetCard> { backStackEntry ->
            val route: NavRoute.CoachSetCard = backStackEntry.toRoute()
            CoachSetCardScreen(
                coachId = route.coachId,
                onNavigateBack = { navController.popBackStack() }
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
                },
                onNavigateToTemplateDetail = { templateId ->
                    navController.navigate(NavRoute.TemplateDetail(templateId))
                }
            )
        }

        composable<NavRoute.TemplateDetail> { backStackEntry ->
            val route: NavRoute.TemplateDetail = backStackEntry.toRoute()
            TemplateDetailScreen(
                templateId = route.templateId,
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

        composable<NavRoute.AIAssistant> {
            AIAssistantScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NavRoute.AIAssistantWithContext> { backStackEntry ->
            val route: NavRoute.AIAssistantWithContext = backStackEntry.toRoute()
            val contextType = try {
                AIContextType.valueOf(route.contextType.uppercase())
            } catch (e: Exception) {
                AIContextType.GENERAL
            }
            AIAssistantScreen(
                contextType = contextType,
                contextId = route.contextId,
                contextName = route.contextName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NavRoute.GamificationDashboard> {
            GamificationDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToClientGamification = { clientId, clientName ->
                    navController.navigate(NavRoute.ClientGamification(clientId, clientName))
                }
            )
        }

        composable<NavRoute.ClientGamification> { backStackEntry ->
            val route: NavRoute.ClientGamification = backStackEntry.toRoute()
            ClientGamificationScreen(
                clientId = route.clientId,
                clientName = route.clientName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NavRoute.Challenges> {
            ChallengesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChallengeDetail = { challengeId ->
                    navController.navigate(NavRoute.ChallengeDetail(challengeId))
                }
            )
        }

        composable<NavRoute.ChallengeDetail> { backStackEntry ->
            val route: NavRoute.ChallengeDetail = backStackEntry.toRoute()
            // For now, navigate back to challenges screen with the challenge pre-selected
            ChallengesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChallengeDetail = { /* Already on detail */ }
            )
        }
    }

        // Bottom Navigation Bar
        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                CoachBottomBar(
                    currentRoute = getBottomNavRoute(currentRoute),
                    onNavigate = { route ->
                        when (route) {
                            BottomNavItem.Home.route -> {
                                navController.navigate(NavRoute.CoachDashboard) {
                                    popUpTo(NavRoute.CoachDashboard) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            BottomNavItem.Messages.route -> {
                                navController.navigate(NavRoute.Conversations) {
                                    launchSingleTop = true
                                }
                            }
                            BottomNavItem.Clients.route -> {
                                navController.navigate(NavRoute.Clients()) {
                                    launchSingleTop = true
                                }
                            }
                            BottomNavItem.Workouts.route -> {
                                navController.navigate(NavRoute.Workouts) {
                                    launchSingleTop = true
                                }
                            }
                            BottomNavItem.Calendar.route -> {
                                navController.navigate(NavRoute.Calendar) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                )
            }
        }

        // Global Feedback FAB - 170dp from bottom, right side
        if (authState.isAuthenticated) {
            FloatingActionButton(
                onClick = { showFeedbackSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 170.dp)
                    .size(48.dp),
                shape = CircleShape,
                containerColor = PrometheusOrange.copy(alpha = 0.9f),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Feedback,
                    contentDescription = "Give Feedback",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Feedback Sheet
        if (showFeedbackSheet) {
            BetaFeedbackSheet(
                screenName = currentScreenName,
                isSubmitting = settingsState.isSubmittingFeedback,
                onSubmit = { feedbackType, message ->
                    isSubmittingFeedback = true
                    settingsViewModel.submitFeedback(feedbackType, message, currentScreenName)
                },
                onDismiss = { showFeedbackSheet = false }
            )
        }
    } // End of Box
}
