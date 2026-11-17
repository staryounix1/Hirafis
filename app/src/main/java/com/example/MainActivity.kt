package com.example

import android.os.Bundle
import com.mapbox.common.MapboxOptions
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.credentials.CredentialManager
import com.example.data.local.HirafiDatabase
import com.example.data.local.UserPreferencesDataStore
import com.example.data.location.RealTimeLocationService
import com.example.data.model.MoroccanLocations
import com.example.data.model.UserRole
import com.example.data.remote.FirestoreMarketplaceRepository
import com.example.data.remote.FirestoreUserProfileRepository
import com.example.data.repository.HirafiRepository
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.HirafiDrawerContent
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

        val db = HirafiDatabase.getInstance(applicationContext)
        val prefs = UserPreferencesDataStore(applicationContext)
        val firestoreUserRepo = FirestoreUserProfileRepository(applicationContext)
        val firestoreMarketplaceRepo = FirestoreMarketplaceRepository(applicationContext)
        val repository = HirafiRepository(db.hirafiDao(), prefs, firestoreUserRepo, firestoreMarketplaceRepo)

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
        factory = HirafiViewModel.factory(repository, locationService)
    )
    val biddingViewModel: RealtimeBiddingViewModel = viewModel(
        factory = RealtimeBiddingViewModel.factory(repository)
    )

    val session by viewModel.userSession.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val biddingEvent by biddingViewModel.statusEvent.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    var showCityDialog by remember { mutableStateOf(false) }

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = session.isRoleLocked && drawerState.isOpen,
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
            if (!session.isRoleLocked) {
                RoleRegistrationScreen(
                    initialSession = session,
                    onRegisterWithEmail = { fullName, email, password, city, role, specialtyId ->
                        viewModel.registerFirstTimeAccount(
                            fullName = fullName,
                            email = email,
                            password = password,
                            city = city,
                            role = role,
                            specialtyId = specialtyId
                        )
                    },
                    onGoogleAuthSuccess = { account, city, role, specialtyId ->
                        viewModel.completeGoogleSignIn(
                            uid = account.uid,
                            displayName = account.displayName,
                            email = account.email,
                            city = city,
                            preferredRole = role,
                            preferredSpecialtyId = specialtyId
                        )
                    },
                    onShowError = { message -> viewModel.showMessage(message) }
                )
            } else {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("home") {
                        if (session.role == UserRole.CLIENT) {
                            ClientHomeScreen(
                                viewModel = viewModel,
                                biddingViewModel = biddingViewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenChat = { reqId -> navController.navigate("chat/$reqId") },
                                onOpenSafety = { navController.navigate("safety") }
                            )
                        } else {
                            CraftsmanDashboardScreen(
                                viewModel = viewModel,
                                biddingViewModel = biddingViewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenChat = { reqId -> navController.navigate("chat/$reqId") }
                            )
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
