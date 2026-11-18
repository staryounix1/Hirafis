package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import com.mapbox.common.MapboxOptions
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.HirafiDatabase
import com.example.data.local.UserPreferencesDataStore
import com.example.data.location.RealTimeLocationService
import com.example.data.model.MoroccanLocations
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseAdminRepository
import com.example.data.remote.SupabaseMarketplaceRepository
import com.example.data.remote.SupabaseSessionManager
import com.example.data.remote.SupabaseStorageService
import com.example.data.remote.SupabaseUserProfileRepository
import com.example.data.repository.HirafiRepository
import com.example.notifications.HirafiNotificationHelper
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.HirafiDrawerContent
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ClientHomeScreen
import com.example.ui.screens.CraftsmanDashboardScreen
import com.example.ui.screens.CraftsmanVerificationScreen
import com.example.ui.screens.OrdersHistoryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RoleRegistrationScreen
import com.example.ui.screens.SafetyScreen
import com.example.ui.theme.HirafiTheme
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.viewmodel.HirafiViewModel
import com.example.ui.viewmodel.RealtimeBiddingViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mapboxToken = BuildConfig.MAPBOX_ACCESS_TOKEN.trim()
        if (mapboxToken.isNotBlank() && !mapboxToken.contains("YOUR_MAPBOX_ACCESS_TOKEN")) {
            MapboxOptions.accessToken = mapboxToken
        }

        HirafiNotificationHelper.ensureChannels(applicationContext)

        val db = HirafiDatabase.getInstance(applicationContext)
        val prefs = UserPreferencesDataStore(applicationContext)
        val supabaseSessionManager = SupabaseSessionManager(prefs)
        val supabaseUserRepo = SupabaseUserProfileRepository(supabaseSessionManager)
        val supabaseMarketplaceRepo = SupabaseMarketplaceRepository(supabaseSessionManager)
        val supabaseStorageService = SupabaseStorageService(applicationContext, supabaseSessionManager)
        val supabaseAdminRepo = SupabaseAdminRepository(supabaseSessionManager, supabaseStorageService)
        val repository = HirafiRepository(
            dao = db.hirafiDao(),
            prefs = prefs,
            supabaseSessionManager = supabaseSessionManager,
            supabaseUserRepo = supabaseUserRepo,
            supabaseMarketplaceRepo = supabaseMarketplaceRepo,
            supabaseAdminRepo = supabaseAdminRepo,
            supabaseStorageService = supabaseStorageService
        )

        setContent {
            HirafiTheme {
                HirafiAppRoot(repository = repository)
            }
        }
    }
}

@Composable
fun HirafiAppRoot(repository: HirafiRepository) {
    val context = LocalContext.current
    val locationService = remember(context) { RealTimeLocationService(context.applicationContext) }
    val viewModel: HirafiViewModel = viewModel(
        factory = HirafiViewModel.factory(repository, locationService, context.applicationContext)
    )
    val biddingViewModel: RealtimeBiddingViewModel = viewModel(
        factory = RealtimeBiddingViewModel.factory(repository)
    )

    val session by viewModel.userSession.collectAsStateWithLifecycle()
    val supabaseAuth by viewModel.supabaseAuthState.collectAsStateWithLifecycle()
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val biddingEvent by biddingViewModel.statusEvent.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    var showCityDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.restoreSupabaseSession()
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeSnackbar()
    }

    LaunchedEffect(biddingEvent) {
        val msg = biddingEvent ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        biddingViewModel.consumeStatusEvent()
    }

    val isCloudSessionReady = supabaseAuth != null &&
        session.isRoleLocked &&
        session.userId == supabaseAuth?.uid

    var askedNotificationPermission by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(isCloudSessionReady) {
        if (isCloudSessionReady &&
            !askedNotificationPermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !HirafiNotificationHelper.hasNotificationPermission(context)
        ) {
            askedNotificationPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isCloudSessionReady && drawerState.isOpen,
        drawerContent = {
            HirafiDrawerContent(
                session = session,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onCityClick = {
                    scope.launch { drawerState.close() }
                    showCityDialog = true
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    if (currentRoute != "home") {
                        navController.popBackStack("home", inclusive = false)
                    }
                    viewModel.stopCloudListenersBeforeSignOut()
                    val credManager = CredentialManager.create(context)
                    GoogleAuthHelper.signOut(credManager, scope) {
                        viewModel.logoutAccount()
                    }
                }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { _ ->
            if (!isCloudSessionReady) {
                RoleRegistrationScreen(
                    initialSession = session,
                    isCloudSaving = authUiState.isLoading,
                    currentSupabaseUid = supabaseAuth?.uid,
                    currentSupabaseEmail = supabaseAuth?.email,
                    currentSupabaseDisplayName = supabaseAuth?.displayName,
                    onEmailSignUp = { email, password, fullName, city, role, specialtyId ->
                        viewModel.signUpSupabaseEmail(
                            email = email,
                            password = password,
                            fullName = fullName,
                            city = city,
                            preferredRole = role,
                            preferredSpecialtyId = specialtyId
                        )
                    },
                    onEmailSignIn = { email, password ->
                        viewModel.signInSupabaseEmail(email, password)
                    },
                    onGoogleAuthSuccess = { account, preferredDisplayName, city, role, specialtyId ->
                        viewModel.completeGoogleSignIn(
                            idToken = account.idToken,
                            displayName = account.displayName,
                            preferredDisplayName = preferredDisplayName,
                            email = account.email,
                            city = city,
                            preferredRole = role,
                            preferredSpecialtyId = specialtyId
                        )
                    },
                    onSwitchAccount = {
                        viewModel.stopCloudListenersBeforeSignOut()
                        val credManager = CredentialManager.create(context)
                        GoogleAuthHelper.signOut(credManager, scope) {
                            viewModel.logoutAccount()
                        }
                    },
                    onShowError = { message -> viewModel.showMessage(message) }
                )
            } else {
                key(supabaseAuth!!.uid) {
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("home") {
                            when (session.role) {
                                UserRole.ADMIN -> {
                                    AdminDashboardScreen(
                                        viewModel = viewModel,
                                        onOpenDrawer = { scope.launch { drawerState.open() } }
                                    )
                                }
                                UserRole.CLIENT -> {
                                    ClientHomeScreen(
                                        viewModel = viewModel,
                                        biddingViewModel = biddingViewModel,
                                        onOpenDrawer = { scope.launch { drawerState.open() } },
                                        onOpenChat = { reqId -> navController.navigate("chat/$reqId") },
                                        onOpenSafety = { navController.navigate("safety") }
                                    )
                                }
                                UserRole.CRAFTSMAN -> {
                                    CraftsmanDashboardScreen(
                                        viewModel = viewModel,
                                        biddingViewModel = biddingViewModel,
                                        onOpenDrawer = { scope.launch { drawerState.open() } },
                                        onOpenChat = { reqId -> navController.navigate("chat/$reqId") }
                                    )
                                }
                            }
                        }

                        composable("orders") {
                            OrdersHistoryScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onOpenChat = { reqId -> navController.navigate("chat/$reqId") }
                            )
                        }

                        composable("profile") {
                            ProfileScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("safety") {
                            SafetyScreen(onBack = { navController.popBackStack() })
                        }

                        composable("verification") {
                            CraftsmanVerificationScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "chat/{requestId}",
                            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
                        ) { entry ->
                            val requestId = entry.arguments?.getString("requestId") ?: return@composable
                            ChatScreen(
                                requestId = requestId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCityDialog) {
        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            title = { Text("اختر مدينتك", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MoroccanLocations.cities.forEach { cityName ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCity(cityName)
                                    showCityDialog = false
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(Icons.Rounded.LocationCity, contentDescription = null, tint = VerifiedEmerald)
                                Spacer(Modifier.width(8.dp))
                                Text(cityName, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityDialog = false }) { Text("إغلاق") }
            }
        )
    }
}
