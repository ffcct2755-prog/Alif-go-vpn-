package com.example.ui

import kotlin.random.Random
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Theme Customization Variables
val DeepCosmicDark = Color(0xFF0F172A)
val DeepCosmicSurface = Color(0xFF1E293B)
val RadiantEmerald = Color(0xFF10B981)
val ElectricBlue = Color(0xFF3B82F6)
val GoldenAmber = Color(0xFFF59E0B)
val CrimsonRose = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlifVpnAppScreen(viewModel: AlifVpnViewModel) {
    val servers by viewModel.allServers.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val tickets by viewModel.allTickets.collectAsState()
    val admob by viewModel.admobConfig.collectAsState()
    val config by viewModel.appConfig.collectAsState()
    val plans by viewModel.allPlans.collectAsState()

    val currentUser by viewModel.currentUser.collectAsState()
    val currEmail by viewModel.currentUserEmail.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val userTransactions by viewModel.userTransactions.collectAsState()
    val userTickets by viewModel.userTickets.collectAsState()

    val scope = rememberCoroutineScope()

    // Global settings toggled by App bar
    val isDarkTheme = true
    var langEnglish by remember { mutableStateOf(true) }
    var isAdminMode by remember { mutableStateOf(false) }

    // Navigation and Alerts
    var activeTab by remember { mutableStateOf("connect") }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showServerSelector by remember { mutableStateOf(false) }
    var showProtocolSettings by remember { mutableStateOf(false) }
    var showBillingDialog by remember { mutableStateOf(false) }
    var selectedPlanForPayment by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var billingProgressState by remember { mutableStateOf("idle") } // idle, processing_gpay, processing_gplay, success
    var billingOrderId by remember { mutableStateOf("") }
    var activeNotificationText by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Retrieve device unique ID (ANDROID_ID)
    val context = LocalContext.current
    val deviceId = remember {
        android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
    }
    val authErrorMsg by viewModel.authError.collectAsState()

    // Interstitial and Rewarded Ad status
    var simulatedAdProgress by remember { mutableFloatStateOf(0f) }
    var activeSimulatedAdByCoins by remember { mutableStateOf<Boolean?>(null) } // true: Video Ad, false: Interstitial connect Ad
    var showAdDialog by remember { mutableStateOf(false) }

    val notifications by viewModel.notificationBroadcasts.collectAsState()

    // Authentication fields
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var signUpName by remember { mutableStateOf("") }
    var signUpInviteCode by remember { mutableStateOf("") }
    var isSignUpTab by remember { mutableStateOf(false) }

    // Translations Dict
    val getT = { en: String, bn: String -> if (langEnglish) en else bn }

    // Theme Color Mapping
    val customColorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = ElectricBlue,
            secondary = RadiantEmerald,
            background = DeepCosmicDark,
            surface = DeepCosmicSurface,
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = ElectricBlue,
            secondary = RadiantEmerald,
            background = Color(0xFFF1F5F9),
            surface = Color.White,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A)
        )
    }

    if (currentUser?.isBanned == true) {
        // Banned Notice overlay
        MaterialTheme(colorScheme = customColorScheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Banned",
                        tint = CrimsonRose,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = getT("Access Denied", "প্রবেশাধিকার নিষিদ্ধ"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = getT(
                            "Your account ($currEmail) has been banned by the Administrator due to violating rules of service.",
                            "সেবার নিয়ম অমান্য করার কারণে আপনার অ্যাকাউন্টটি ($currEmail) প্রশাসক দ্বারা সাময়িক অবরুদ্ধ করা হয়েছে।"
                        ),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text(getT("Switch Account", "অ্যাকাউন্ট পরিবর্তন করুন"))
                    }
                }
            }
        }
        return
    }

    MaterialTheme(colorScheme = customColorScheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Logo",
                                tint = RadiantEmerald,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = config?.appName ?: "Alif Go VPN",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (currentUser?.isPremium == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(GoldenAmber, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Language Selector
                        IconButton(onClick = { langEnglish = !langEnglish }) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Change Language",
                                tint = ElectricBlue
                            )
                        }

                        // Admin Mode toggle switch
                        IconButton(onClick = { isAdminMode = !isAdminMode }) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Mode Toggle",
                                tint = if (isAdminMode) RadiantEmerald else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        // Redeem (Rewards) top switch button
                        IconButton(onClick = { activeTab = "rewards"; isAdminMode = false }) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = "Redeem Rewards",
                                tint = if (activeTab == "rewards" && !isAdminMode) GoldenAmber else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Column {
                    // AdMob Banner ad support simulation
                    if (admob?.isBannerEnabled == true) {
                        val activeBannerId = admob?.bannerPlacementId?.ifBlank { "ca-app-pub-1131981412237081/5644757651" } ?: "ca-app-pub-1131981412237081/5644757651"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.DarkGray)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "[AdMob Banner: $activeBannerId] -- " + getT("Sponsored safe connection", "স্পন্সরড নিরাপদ সংযোগ"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Main user-navigation bottom navigation row
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        NavigationBarItem(
                            selected = activeTab == "connect" && !isAdminMode,
                            onClick = { activeTab = "connect"; isAdminMode = false },
                            icon = { Icon(Icons.Default.PowerSettingsNew, "Connect") },
                            label = { Text(getT("Connect", "সংযোগ"), fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = activeTab == "servers" && !isAdminMode,
                            onClick = { activeTab = "servers"; isAdminMode = false },
                            icon = { Icon(Icons.Default.Dns, "Servers") },
                            label = { Text(getT("Servers", "সার্ভার"), fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = activeTab == "account" && !isAdminMode,
                            onClick = { activeTab = "account"; isAdminMode = false },
                            icon = { Icon(Icons.Default.AccountCircle, "Account") },
                            label = { Text(getT("Account", "অ্যাকাউন্ট"), fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = activeTab == "subscriptions" && !isAdminMode,
                            onClick = { activeTab = "subscriptions"; isAdminMode = false },
                            icon = { Icon(Icons.Default.WorkspacePremium, "Premium") },
                            label = { Text(getT("Plans", "প্যাকেজ"), fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = activeTab == "support" && !isAdminMode,
                            onClick = { activeTab = "support"; isAdminMode = false },
                            icon = { Icon(Icons.Default.HeadsetMic, "Support") },
                            label = { Text(getT("Support", "সহায়তা"), fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                // Determine screen displaying content
                AnimatedContent(
                    targetState = if (isAdminMode) "admin" else activeTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "AppScreenContainer"
                ) { target ->
                    when (target) {
                        "connect" -> ConnectDashboardTab(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            connectionState = connectionState,
                            selectedServer = selectedServer,
                            getT = getT,
                            onPickServer = { showServerSelector = true },
                            onSelectProtocol = { showProtocolSettings = true },
                            onTriggerAuth = { showAuthDialog = true },
                            onTriggerInterstitialAdBlock = {
                                simulatedAdProgress = 0f
                                activeSimulatedAdByCoins = false
                                showAdDialog = true
                                scope.launch {
                                    for (i in 1..25) {
                                        delay(100)
                                        simulatedAdProgress = i / 25f
                                    }
                                    showAdDialog = false
                                    viewModel.toggleConnection(deviceId = deviceId)
                                }
                            }
                        )
                        "servers" -> ServerListTab(
                            viewModel = viewModel,
                            servers = servers,
                            selectedServer = selectedServer,
                            currentUser = currentUser,
                            getT = getT,
                            onServerSelected = { activeTab = "connect"; isAdminMode = false }
                        )
                        "rewards" -> RewardsTab(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            getT = getT,
                            onWatchVideoAd = {
                                simulatedAdProgress = 0f
                                activeSimulatedAdByCoins = true
                                showAdDialog = true
                                scope.launch {
                                    for (i in 1..40) {
                                        delay(100)
                                        simulatedAdProgress = i / 40f
                                    }
                                    showAdDialog = false
                                    viewModel.watchAdAndEarn()
                                }
                            }
                        )
                        "account" -> AccountTab(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            getT = getT,
                            onTriggerAuth = { showAuthDialog = true },
                            onGoToRedeem = { activeTab = "rewards" }
                        )
                        "subscriptions" -> SubscriptionPlansTab(
                            viewModel = viewModel,
                            plans = plans,
                            currentUser = currentUser,
                            transactions = userTransactions,
                            getT = getT,
                            onBuyGoogleBilling = { plan ->
                                selectedPlanForPayment = plan
                                showBillingDialog = true
                            }
                        )
                        "support" -> SupportTab(
                            viewModel = viewModel,
                            getT = getT,
                            tickets = userTickets
                        )
                        "admin" -> AdminPanelTab(
                            viewModel = viewModel,
                            servers = servers,
                            users = users,
                            transactions = transactions,
                            tickets = tickets,
                            admob = admob,
                            appConf = config,
                            plans = plans,
                            getT = getT
                        )
                    }
                }
            }
        }

        // --- ALL SYSTEM DIALOG OVERLAYS ---

        // Server selector overlay helper
        if (showServerSelector) {
            ServerSelectorDialog(
                servers = servers,
                selectedServer = selectedServer,
                getT = getT,
                onDismiss = { showServerSelector = false },
                onSelect = {
                    viewModel.selectServer(it)
                    showServerSelector = false
                }
            )
        }

        // Protocol & Security settings dialog helper
        if (showProtocolSettings) {
            ProtocolSettingsDialog(
                viewModel = viewModel,
                getT = getT,
                onDismiss = { showProtocolSettings = false }
            )
        }

        // Device Limit Exceeded Dialog alert
        if (authErrorMsg == "DEVICE_LIMIT_EXCEEDED") {
            AlertDialog(
                onDismissRequest = { viewModel.authError.value = null },
                icon = { Icon(Icons.Default.Devices, contentDescription = "Device Limit", tint = CrimsonRose) },
                title = { Text(getT("Device Limit Exceeded!", "ডিভাইস ব্যবহারের সীমা অতিক্রম হয়েছে!")) },
                text = {
                    Text(
                        getT(
                            "You have reached the maximum allowed device limit for your active Premium plan. Please disconnect another device, contact Alif Go VPN Support, or request your admin to reset devices.",
                            "আপনার প্রিমিয়াম সাবস্ক্রিপশনের সর্বোচ্চ ডিভাইস ব্যবহারের সীমা পূর্ণ হয়েছে। অনুগ্রহ করে অন্য ডিভাইস ডিসকানেক্ট করুন, অথবা আমাদের কাস্টমার সাপোর্টে যোগাযোগ করুন।"
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.authError.value = null },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text(getT("OK", "ঠিক আছে"))
                    }
                }
            )
        }

        // Server Connection Limit Exceeded Dialog alert
        if (authErrorMsg == "SERVER_LIMIT_EXCEEDED") {
            AlertDialog(
                onDismissRequest = { viewModel.authError.value = null },
                icon = { Icon(Icons.Default.Dns, contentDescription = "Server Limit", tint = CrimsonRose) },
                title = { Text(getT("Server Connection Full!", "সার্ভার সংযোগ পূর্ণ হয়েছে!")) },
                text = {
                    Text(
                        getT(
                            "This specific server has reached its maximum concurrent user limit set by the administrator. Please select another high-speed server, try again later, or contact support.",
                            "এই সার্ভারের সর্বোচ্চ সংযোগ সীমা পূর্ণ হয়েছে। অনুগ্রহ করে অন্য কোনো হাই-স্পিড সার্ভার ব্যবহার করুন অথবা কিছু সময় পর চেষ্টা করুন।"
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.authError.value = null },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text(getT("OK", "ঠিক আছে"))
                    }
                }
            )
        }

        // Authentication & Sign up popup screen
        if (showAuthDialog) {
            Dialog(onDismissRequest = { showAuthDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isSignUpTab) getT("Create Alif Account", "আলিফ অ্যাকাউন্ট তৈরি") else getT("Login Securely", "নিরাপদ লগইন"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        if (isSignUpTab) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = signUpName,
                                onValueChange = { signUpName = it },
                                label = { Text("Display Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = signUpInviteCode,
                                onValueChange = { signUpInviteCode = it },
                                label = { Text("Invited By (Referral Code)") },
                                placeholder = { Text("Optional") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (isSignUpTab) {
                                    viewModel.signUp(loginEmail, signUpName.ifEmpty { "User" }, signUpInviteCode, deviceId)
                                } else {
                                    viewModel.login(loginEmail, "Standard User", deviceId)
                                }
                                showAuthDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().testTag("auth_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Text(if (isSignUpTab) getT("Register", "নিবন্ধন") else getT("Login", "লগইন"))
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { isSignUpTab = !isSignUpTab }) {
                            Text(
                                if (isSignUpTab) getT("Already have an account? Login", "অ্যাকাউন্ট আছে? লগইন করুন")
                                else getT("Don't have an account? Sign Up", "নতুন অ্যাকাউন্ট খুলুন")
                            )
                        }

                        TextButton(onClick = {
                            viewModel.loginGuest(deviceId)
                            showAuthDialog = false
                        }) {
                            Text(getT("Skip & Use Default Account", "ডিফল্ট অ্যাকাউন্ট দিয়ে প্রবেশ"), color = RadiantEmerald)
                        }
                    }
                }
            }
        }

        // Google Pay & Google Play Billing secure payment dialog checkout workflow
        val appConf = config ?: AppConfig()
        if (showBillingDialog && selectedPlanForPayment != null) {
            val plan = selectedPlanForPayment!!
            val scope = rememberCoroutineScope()
            val hasPlayBilling = appConf.isPlayBillingEnabled
            val hasManualPayment = appConf.isManualPaymentEnabled
            var paymentTab by remember(hasPlayBilling, hasManualPayment) {
                mutableStateOf(if (hasPlayBilling) "gplay" else "manual")
            } // "gplay" or "manual"
            var manualMethodSelected by remember { mutableStateOf("bKash") } // bKash, Nagad, USDT, TRX
            var senderInput by remember { mutableStateOf("") }
            var txHashInput by remember { mutableStateOf("") }
            val context = androidx.compose.ui.platform.LocalContext.current

            Dialog(onDismissRequest = { 
                if (billingProgressState == "idle" || billingProgressState == "success") {
                    showBillingDialog = false 
                    billingProgressState = "idle"
                }
            }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (billingProgressState == "processing_gpay" || billingProgressState == "processing_gplay") {
                            // Official-looking Google Checkout Progress screen
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(
                                color = if (billingProgressState == "processing_gpay") ElectricBlue else RadiantEmerald,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = if (billingProgressState == "processing_gpay") 
                                    getT("Securing Google Pay payment...", "গুগল পে পেমেন্ট সুরক্ষিত করা হচ্ছে...") 
                                else 
                                    getT("Contacting Google Play Billing...", "গুগল প্লে বিলিং সংযোগ করা হচ্ছে..."),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = getT("Please do not close this modal. Processing credentials securely via Google Services.", "দয়া করে এই উইন্ডোটি বন্ধ করবেন না। গুগল সার্ভিসের মাধ্যমে পেমেন্ট সম্পন্ন হচ্ছে।"),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        } else if (billingProgressState == "success") {
                            // Gorgeous Green Success Screen
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = RadiantEmerald,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = getT("Payment Successful!", "পেমেন্ট সফল হয়েছে!"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = RadiantEmerald
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = getT("Your Premium Account has been activated instantly.", "আপনার প্রিমিয়াম অ্যাকাউন্টটি তাৎক্ষণিকভাবে সক্রিয় করা হয়েছে।"),
                                fontSize = 12.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Invoice Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Package/Plan:", fontSize = 11.sp, color = Color.Gray)
                                        Text(plan.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Order ID:", fontSize = 11.sp, color = Color.Gray)
                                        Text(billingOrderId, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Amount Paid:", fontSize = 11.sp, color = Color.Gray)
                                        Text("$${plan.priceUsdt} USD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RadiantEmerald)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    showBillingDialog = false
                                    billingProgressState = "idle"
                                },
                                modifier = Modifier.fillMaxWidth().testTag("continue_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald)
                            ) {
                                Text(getT("Let's Go / Start VPN", "ভিপিএন শুরু করুন"))
                            }
                        } else {
                            if (!hasPlayBilling && !hasManualPayment) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Disabled",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = getT("Direct purchase is currently disabled by Admin.", "এডমিন দ্বারা সরাসরি ক্রয় করার অপশন সাময়িকভাবে বন্ধ আছে।"),
                                        color = Color.LightGray,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                if (hasPlayBilling && hasManualPayment) {
                                    // Tab selector row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Button(
                                            onClick = { paymentTab = "gplay" },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (paymentTab == "gplay") ElectricBlue else Color.Transparent,
                                                contentColor = if (paymentTab == "gplay") Color.White else Color.LightGray
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            Text(getT("Google Pay/Play", "গুগল পে/প্লে"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { paymentTab = "manual" },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (paymentTab == "manual") RadiantEmerald else Color.Transparent,
                                                contentColor = if (paymentTab == "manual") Color.White else Color.LightGray
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            Text(getT("bKash/Nagad/USDT", "বিকাশ/নগদ/USDT"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (paymentTab == "gplay" && hasPlayBilling) {
                                    // Primary Google Play & Google Pay Billing Screen
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = "Secure Checkout",
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = getT("Google Play Checkout", "গুগল প্লে চেকআউট"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Box(
                                            modifier = Modifier
                                                .background(ElectricBlue.copy(0.15f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("Official SDK", fontSize = 9.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Order Details Box
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            .padding(16.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = plan.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${getT("Duration", "মেয়াদ")}: ${plan.durationDays} " + getT("Days", "দিন"),
                                                fontSize = 12.sp,
                                                color = Color.LightGray
                                            )
                                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.DarkGray)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(getT("Total Bill:", "মোট বিল:"), color = Color.Gray, fontSize = 13.sp)
                                                Text(
                                                    text = "$${plan.priceUsdt} USD",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldenAmber
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Value Propositions
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val perks = listOf(
                                            "⚡" to getT("Unlocks all 50+ Premium high-speed nodes", "সকল ৫০+ প্রিমিয়াম হাই-স্পিড নোড অ্যাক্সেস"),
                                            "🛡️" to getT("100% Ad-Free experience with military encryption", "সম্পূর্ণ বিজ্ঞাপন-মুক্ত ও মিলিটারি গ্রেড এনক্রিপশন"),
                                            "🔄" to getT("Direct Google Play subscription integration", "গুগল প্লে স্টোর দিয়ে সরাসরি সাবস্ক্রিপশন")
                                        )
                                        perks.forEach { (emoji, text) ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(emoji, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text, fontSize = 11.sp, color = Color.LightGray)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Interactive Checkout Action Buttons
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // 1. Google Pay (G-Pay Button)
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    billingProgressState = "processing_gpay"
                                                    delay(1500)
                                                    billingOrderId = "GPA." + Random.nextLong(1000_0000L, 9999_9999L)
                                                    viewModel.payWithGoogleBilling("Google Pay", billingOrderId, plan.priceUsdt, plan)
                                                    billingProgressState = "success"
                                                    viewModel.sendBroadcastNotification(
                                                        "Purchase Complete",
                                                        "Successfully processed $${plan.priceUsdt} USD with Google Pay."
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("gpay_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CreditCard,
                                                    contentDescription = "GPay",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = getT("Buy with Google Pay", "গুগল পে দিয়ে কিনুন"),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }

                                        // 2. Google Play Billing Button
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    billingProgressState = "processing_gplay"
                                                    delay(2000)
                                                    billingOrderId = "GPA." + Random.nextLong(1000_0000L, 9999_9999L)
                                                    viewModel.payWithGoogleBilling("Google Play Sub", billingOrderId, plan.priceUsdt, plan)
                                                    billingProgressState = "success"
                                                    viewModel.sendBroadcastNotification(
                                                        "Subscription Active",
                                                        "Successfully subscribed to ${plan.name} via Google Play Billing."
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("play_billing_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Google Play",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = getT("Google Play Subscription", "গুগল প্লে সাবস্ক্রিপশন"),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = getT("Protected by Google Play Store terms & policies. Unsubscribe anytime.", "গুগল প্লে পলিসি দ্বারা সুরক্ষিত। যেকোনো সময় বাতিলযোগ্য।"),
                                            fontSize = 9.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else if (paymentTab == "manual" && hasManualPayment) {
                                    // Manual payment interface (bKash/Nagad/USDT/TRX)
                                    val bdtAmount = (plan.priceUsdt * 120).toInt()
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = getT("Manual Payment Transfer / ম্যানুয়াল পেমেন্ট", "ম্যানুয়াল পেমেন্ট (বিকাশ / নগদ / USDT)"),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RadiantEmerald
                                        )

                                        // Method selectors
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf("bKash", "Nagad", "USDT", "TRX").forEach { method ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (manualMethodSelected == method) RadiantEmerald else Color.DarkGray,
                                                            shape = RoundedCornerShape(6.dp)
                                                        )
                                                        .background(
                                                            if (manualMethodSelected == method) RadiantEmerald.copy(alpha = 0.15f) else Color.Transparent,
                                                            shape = RoundedCornerShape(6.dp)
                                                        )
                                                        .clickable { manualMethodSelected = method }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = method,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (manualMethodSelected == method) RadiantEmerald else Color.White
                                                    )
                                                }
                                            }
                                        }

                                        // Details Card for selected method
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                when (manualMethodSelected) {
                                                    "bKash" -> {
                                                        Text("মেথড: বিকাশ পার্সোনাল (Send Money)", color = RadiantEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("নাম্বার: +8801755227744", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text("পরিমাণ: $bdtAmount BDT", color = GoldenAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    "Nagad" -> {
                                                        Text("মেথড: নগদ পার্সোনাল (Send Money)", color = RadiantEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("নাম্বার: +8801844991133", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text("পরিমাণ: $bdtAmount BDT", color = GoldenAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    "USDT" -> {
                                                        Text("Network: USDT (TRC-20 Address)", color = RadiantEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Address: TKyD88xG9jA8pZ3kLmQ2wRv9t7uVw2yZ", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text("Amount: ${plan.priceUsdt} USDT", color = GoldenAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    "TRX" -> {
                                                        Text("Network: Tron (TRX Address)", color = RadiantEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Address: TKyD88xG9jA8pZ3kLmQ2wRv9t7uVw2yZ", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        val trxEquivalent = (plan.priceUsdt * 8.5).toInt()
                                                        Text("Amount: $trxEquivalent TRX", color = GoldenAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        // Input fields for submission
                                        OutlinedTextField(
                                            value = senderInput,
                                            onValueChange = { senderInput = it },
                                            label = { Text(getT("Sender Number / Wallet Address", "আপনার প্রেরক নাম্বার / ওয়ালেট"), fontSize = 10.sp) },
                                            placeholder = { Text(if (manualMethodSelected == "bKash" || manualMethodSelected == "Nagad") "017XXXXXXXX" else "TKyD...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.LightGray,
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent
                                            )
                                        )

                                        OutlinedTextField(
                                            value = txHashInput,
                                            onValueChange = { txHashInput = it },
                                            label = { Text(getT("Transaction ID / TxID / Hash", "ট্রানজেকশন আইডি / TxID"), fontSize = 10.sp) },
                                            placeholder = { Text("TrxID / Hash") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.LightGray,
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent
                                            )
                                        )

                                        Button(
                                            onClick = {
                                                if (senderInput.isNotBlank() && txHashInput.isNotBlank()) {
                                                    viewModel.payWithUSDT(
                                                        wallet = senderInput,
                                                        network = manualMethodSelected,
                                                        amount = plan.priceUsdt,
                                                        txHash = txHashInput,
                                                        plan = plan
                                                    )
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "পেমেন্ট রেকর্ড সাবমিট হয়েছে! এডমিন ভেরিফাই করে সচল করে দিবে।",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                    showBillingDialog = false
                                                    senderInput = ""
                                                    txHashInput = ""
                                                } else {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "সবগুলো ঘর পূরণ করুন!",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald),
                                            shape = RoundedCornerShape(8.dp),
                                            enabled = senderInput.isNotBlank() && txHashInput.isNotBlank()
                                        ) {
                                            Text("Submit Payment Proof")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }


        // Animated AdMob overlay simulation (keeps users engaged with real progress bar during reward/connect sequences)
        if (showAdDialog) {
            Dialog(onDismissRequest = {}) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color.Yellow, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(text = "Ad", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (activeSimulatedAdByCoins == true) "AdMob Rewarded Video Ad" else "AdMob Interstitial Ad",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Fake Ad Placeholder with dynamic content
                        Icon(
                            imageVector = if (activeSimulatedAdByCoins == true) Icons.Default.PlayCircleFilled else Icons.Default.CloudSync,
                            contentDescription = "Ad Loading",
                            tint = ElectricBlue,
                            modifier = Modifier.size(70.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (activeSimulatedAdByCoins == true)
                                "Watching sponsor video to earn ${admob?.rewardCoinsPerAd ?: 15} coins..."
                            else "Connecting you via premium routing relay systems...",
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        LinearProgressIndicator(
                            progress = { simulatedAdProgress },
                            color = RadiantEmerald,
                            trackColor = Color.DarkGray,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Time Left: ${Math.round((1.0 - simulatedAdProgress) * (if (activeSimulatedAdByCoins == true) 4.0 else 2.5))}s",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
}

// --- TAB INDIVIDUAL VIEWS ---

@Composable
fun ConnectDashboardTab(
    viewModel: AlifVpnViewModel,
    currentUser: UserSession?,
    connectionState: String,
    selectedServer: VpnServer?,
    getT: (String, String) -> String,
    onPickServer: () -> Unit,
    onSelectProtocol: () -> Unit,
    onTriggerAuth: () -> Unit,
    onTriggerInterstitialAdBlock: () -> Unit
) {
    val context = LocalContext.current
    val deviceId = remember {
        android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
    }

    val downloadSpeed by viewModel.currentDownloadSpeedMbps.collectAsState()
    val uploadSpeed by viewModel.currentUploadSpeedMbps.collectAsState()
    val totalDl by viewModel.totalDataDownloaded.collectAsState()
    val totalUl by viewModel.totalDataUploaded.collectAsState()
    val elapsedSeconds by viewModel.connectedDurationSeconds.collectAsState()

    val protocolSelected by viewModel.selectedProtocol.collectAsState()
    val killSwitchOn by viewModel.killSwitch.collectAsState()
    val dnsLeakOn by viewModel.dnsLeakProtection.collectAsState()
    val ipv6On by viewModel.ipv6LeakProtection.collectAsState()

    val speedTestIsRunning by viewModel.speedTestIsRunning.collectAsState()
    val speedTestDl by viewModel.speedTestDownload.collectAsState()
    val speedTestUl by viewModel.speedTestUpload.collectAsState()
    val speedTestPing by viewModel.speedTestPing.collectAsState()
    val speedTestStage by viewModel.speedTestStage.collectAsState()

    // Breathing Animation for VPN Connection State button
    val transition = rememberInfiniteTransition(label = "breathing")
    val pulseRatio by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Main Connection UI Display
        Box(
            modifier = Modifier
                .size(220.dp)
                .drawBehind {
                    val brushColor = when (connectionState) {
                        "CONNECTED" -> RadiantEmerald
                        "CONNECTING" -> GoldenAmber
                        else -> ElectricBlue
                    }
                    drawCircle(
                        color = brushColor.copy(alpha = 0.08f),
                        radius = (size.minDimension / 2) * (if (connectionState == "CONNECTED") pulseRatio else 1.0f)
                    )
                    drawCircle(
                        color = brushColor.copy(alpha = 0.15f),
                        radius = (size.minDimension / 2.3f)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .border(
                        3.dp,
                        when (connectionState) {
                            "CONNECTED" -> RadiantEmerald
                            "CONNECTING" -> GoldenAmber
                            else -> ElectricBlue.copy(alpha = 0.5f)
                        },
                        CircleShape
                    )
                    .clickable {
                        // Check if connecting. Otherwise trigger Interstitial Ad if free!
                        if (connectionState == "DISCONNECTED") {
                            onTriggerInterstitialAdBlock()
                        } else {
                            viewModel.toggleConnection(false, deviceId)
                        }
                    }
                    .testTag("power_button"),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power VPN",
                    tint = when (connectionState) {
                        "CONNECTED" -> RadiantEmerald
                        "CONNECTING" -> GoldenAmber
                        else -> ElectricBlue
                    },
                    modifier = Modifier.size(50.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (connectionState) {
                        "CONNECTED" -> getT("CONNECTED", "সংযুক্ত")
                        "CONNECTING" -> getT("SHAKING HANDS...", "সংযোগ হচ্ছে...")
                        else -> getT("TAP TO CONNECT", "সংযোগ করুন")
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Server Indicator Widget
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onPickServer() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flag visual placeholder using emojis
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.background,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val codeEmoji = when (selectedServer?.countryCode) {
                        "US" -> "🇺🇸"
                        "SG" -> "🇸🇬"
                        "BD" -> "🇧🇩"
                        "JP" -> "🇯🇵"
                        "GB" -> "🇬🇧"
                        "DE" -> "🇩🇪"
                        "KR" -> "🇰🇷"
                        "IN" -> "🇮🇳"
                        else -> "🌐"
                    }
                    Text(codeEmoji, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedServer?.countryName ?: getT("Smart server", "স্মার্ট সার্ভার"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${selectedServer?.city ?: "Auto Selection"} • IP: ${selectedServer?.ipAddress ?: "0.0.0.0"}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (selectedServer?.type == "Premium") GoldenAmber.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = selectedServer?.type ?: "Free",
                        color = if (selectedServer?.type == "Premium") GoldenAmber else MaterialTheme.colorScheme.onSurface.copy(0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = "Pick")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Connection Speed Statistics
        if (connectionState == "CONNECTED") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(getT("Connected Duration", "সংযোগের দীর্ঘতা"), fontSize = 11.sp, color = Color.Gray)
                            // Formatted Duration
                            val hrs = elapsedSeconds / 3600
                            val mins = (elapsedSeconds % 3600) / 60
                            val secs = elapsedSeconds % 60
                            Text(
                                String.format("%02d:%02d:%02d", hrs, mins, secs),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = RadiantEmerald
                            )
                        }
                        Icon(Icons.Default.Timelapse, contentDescription = "Uptime", tint = RadiantEmerald)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Dl", tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(getT("Downloading", "ডাউনলোড"), fontSize = 11.sp, color = Color.Gray)
                            }
                            Text("$downloadSpeed Mbps", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Total: ${String.format("%.2f", totalDl / (1024.0 * 1024.0))} MB",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = GoldenAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(getT("Uploading", "আপলোড"), fontSize = 11.sp, color = Color.Gray)
                            }
                            Text("$uploadSpeed Mbps", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Total: ${String.format("%.2f", totalUl / (1024.0 * 1024.0))} MB",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Speed Test Interactive widget
        SpeedTestWidget(
            speedTestIsRunning = speedTestIsRunning,
            speedTestDl = speedTestDl,
            speedTestUl = speedTestUl,
            speedTestPing = speedTestPing,
            speedTestStage = speedTestStage,
            getT = getT,
            onRun = { viewModel.runSpeedTest() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Security Toggles, Protocols
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProtocol() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(getT("VPN Protocol Selection", "ভিপিএন প্রোটোকল নির্বাচন"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(getT("Currently using: ", "বর্তমানে ব্যবহৃত: ") + protocolSelected, fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.Settings, contentDescription = "Protocol Config", tint = ElectricBlue)
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.background)
                Spacer(modifier = Modifier.height(14.dp))

                // Security parameter list
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SecurityDotSetting(label = "Kill Switch", isEnabled = killSwitchOn, onToggle = { viewModel.killSwitch.value = !killSwitchOn })
                    SecurityDotSetting(label = "DNS Leak", isEnabled = dnsLeakOn, onToggle = { viewModel.dnsLeakProtection.value = !dnsLeakOn })
                    SecurityDotSetting(label = "IPv6 Leak", isEnabled = ipv6On, onToggle = { viewModel.ipv6LeakProtection.value = !ipv6On })
                }
            }
        }
    }
}

@Composable
fun SecurityDotSetting(label: String, isEnabled: Boolean, onToggle: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onToggle() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (isEnabled) RadiantEmerald else Color.Gray, CircleShape)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = if (isEnabled) "ON" else "OFF", fontSize = 9.sp, color = if (isEnabled) RadiantEmerald else Color.Gray)
    }
}

@Composable
fun SpeedTestWidget(
    speedTestIsRunning: Boolean,
    speedTestDl: Double,
    speedTestUl: Double,
    speedTestPing: Int,
    speedTestStage: String,
    getT: (String, String) -> String,
    onRun: () -> Unit
) {
    // Elegant Canvas Gauge speedometer for speed computations
    val needleAngleAnimate by animateFloatAsState(
        targetValue = when (speedTestStage) {
            "download" -> (speedTestDl / 120.0 * 180.0).toFloat().coerceIn(0f, 180f)
            "upload" -> (speedTestUl / 45.0 * 180.0).toFloat().coerceIn(0f, 180f)
            else -> 0f
        },
        animationSpec = tween(300),
        label = "gauge"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getT("ALIF HYPER SPEED TEST", "আলিফ হাইপার স্পিড টেস্ট"),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricBlue
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Speedometer Canvas layout
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    // Draw dial arc
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Highlight arc representing current progress
                    drawArc(
                        color = ElectricBlue,
                        startAngle = 180f,
                        sweepAngle = needleAngleAnimate,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Dial Indicators
                    val radius = size.width / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Draw speedometer needle pointing details
                    val radians = (180f + needleAngleAnimate) * (PI / 180.0)
                    val needleVal = radius * 0.8
                    val needleEnd = Offset(
                        (center.x + needleVal * cos(radians)).toFloat(),
                        (center.y + needleVal * sin(radians)).toFloat()
                    )
                    drawLine(
                        color = CrimsonRose,
                        start = center,
                        end = needleEnd,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = CrimsonRose, radius = 6.dp.toPx())
                }

                // Inset displaying speeds
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                ) {
                    Text(
                        text = when (speedTestStage) {
                            "download" -> String.format("%.1f", speedTestDl)
                            "upload" -> String.format("%.1f", speedTestUl)
                            "ping" -> "$speedTestPing"
                            else -> "0.0"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (speedTestStage) {
                            "download" -> "MBPS DL"
                            "upload" -> "MBPS UL"
                            "ping" -> "MS PING"
                            else -> getT("READY", "প্রস্তুত")
                        },
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Metric Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ping", fontSize = 10.sp, color = Color.Gray)
                    Text("$speedTestPing ms", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Download", fontSize = 10.sp, color = Color.Gray)
                    Text(String.format("%.1f MBPS", speedTestDl), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ElectricBlue)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Upload", fontSize = 10.sp, color = Color.Gray)
                    Text(String.format("%.1f MBPS", speedTestUl), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RadiantEmerald)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { onRun() },
                modifier = Modifier.fillMaxWidth().testTag("speedtest_button"),
                enabled = !speedTestIsRunning,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text(if (speedTestIsRunning) getT("TESTING LOCAL CHANNELS...", "গতি পরীক্ষা সম্পন্ন হচ্ছে...") else getT("RUN HYPER SPEED TEST", "গতি পরীক্ষা শুরু করুন"))
            }
        }
    }
}

@Composable
fun ServerListTab(
    viewModel: AlifVpnViewModel,
    servers: List<VpnServer>,
    selectedServer: VpnServer?,
    currentUser: UserSession?,
    getT: (String, String) -> String,
    onServerSelected: () -> Unit = {}
) {
    // Dynamic Filter states (Free, Premium, Gaming, Streaming)
    var selectedCategoryFilter by remember { mutableStateOf("Free") }
    
    val context = LocalContext.current
    val deviceId = remember {
        android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
    }

    LaunchedEffect(Unit) {
        viewModel.autoSyncServersFromRemote()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = getT("CHOOSE SECURE RELAY SERVER", "নিরাপদ সংযোগ সার্ভার বাছুন"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ElectricBlue
        )
        Text(
            text = getT("Select optimized routes tuned to reduce lags.", "কম ল্যাগ বিশিষ্ট অপ্টিমাইজড রাউট বাছুন।"),
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Categories chip row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Free", "Premium", "Gaming", "Streaming").forEach { category ->
                val isSelected = selectedCategoryFilter == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryFilter = category },
                    label = { Text(category) },
                    leadingIcon = {
                        val ico = when (category) {
                            "Premium" -> Icons.Default.WorkspacePremium
                            "Gaming" -> Icons.Default.SportsEsports
                            "Streaming" -> Icons.Default.Tv
                            else -> Icons.Default.FilterList
                        }
                        Icon(imageVector = ico, contentDescription = category, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Servers List rendering based on selection category
        val filteredList = servers.filter { it.type == selectedCategoryFilter && it.isEnabled }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Dns, contentDescription = "Empty", tint = Color.LightGray, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(getT("No server active in this category", "এই ক্যাটাগরি তে কোনো সার্ভার সক্রিয় নেই"), color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("servers_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { server ->
                    val isCurrent = selectedServer?.id == server.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                if (isCurrent) 2.dp else 0.dp,
                                ElectricBlue,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (server.type == "Premium" && currentUser?.isPremium == false) {
                                    viewModel.sendBroadcastNotification(
                                        "Premium Server Access Denied",
                                        "Google Play Premium subscription is required to unlock access on premium servers."
                                    )
                                } else {
                                    viewModel.selectServer(server, deviceId)
                                    onServerSelected()
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Flag visual placeholder
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(MaterialTheme.colorScheme.background, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val codeEmoji = when (server.countryCode) {
                                    "US" -> "🇺🇸"
                                    "SG" -> "🇸🇬"
                                    "BD" -> "🇧🇩"
                                    "JP" -> "🇯🇵"
                                    "GB" -> "🇬🇧"
                                    "DE" -> "🇩🇪"
                                    "KR" -> "🇰🇷"
                                    "IN" -> "🇮🇳"
                                    else -> "🌐"
                                }
                                Text(codeEmoji, fontSize = 18.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = server.countryName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = "${server.city} • IP: ${server.ipAddress}", fontSize = 11.sp, color = Color.Gray)
                            }

                            // Load & Ping Metric column
                            Column(horizontalAlignment = Alignment.End) {
                                // Load progress bar
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, contentDescription = "Speed", modifier = Modifier.size(12.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Load: ${server.loadPercent}%", fontSize = 10.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (server.latency < 50) RadiantEmerald.copy(0.2f) else GoldenAmber.copy(0.2f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${server.latency} ms",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (server.latency < 50) RadiantEmerald else GoldenAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RewardsTab(
    viewModel: AlifVpnViewModel,
    currentUser: UserSession?,
    getT: (String, String) -> String,
    onWatchVideoAd: () -> Unit
) {
    val config by viewModel.appConfig.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SnackbarHost(hostState = snackbarHostState)

        Text(
            text = getT("ALIF COIN VAULT", "আলিফ কয়েন ভল্ট"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = RadiantEmerald
        )
        Text(
            text = getT("Redeem coins to earn premium VPN access without spending real money.", "বাস্তব টাকা খরচ না করে প্রিমিয়াম ভিপিএন অ্যাক্সেস পাওয়ার সুবিধা।"),
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Total coin status dashboard card
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = GoldenAmber, modifier = Modifier.size(54.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${currentUser?.coinBalance ?: 0}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = GoldenAmber
                )
                Text(text = getT("Available Coin Balance", "আপনার কয়েন ব্যালেন্স"), fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Check-in claim widget button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.EventAvailable, contentDescription = "Calendar", tint = ElectricBlue, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(getT("Daily Check-In Reward", "দৈনিক লগইন পুরস্কার"), fontWeight = FontWeight.Bold)
                    Text(getT("Claim free coins every single day", "প্রতিদিন ফ্রিতে কয়েন দাবি করুন"), fontSize = 11.sp, color = Color.Gray)
                }
                Button(
                    onClick = {
                        viewModel.claimDailyCheckIn()
                        scope.launch {
                            snackbarHostState.showSnackbar("Claimed daily reward coins successfully!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.testTag("daily_checkin_button")
                ) {
                    Text("+${config?.dailyRewardAmount ?: 10} C")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Watch video ad rewards option
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.OndemandVideo, contentDescription = "Video Ad", tint = RadiantEmerald, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(getT("Watch Sponsored Ads", "স্পন্সরড ভিডিও বিজ্ঞাপন"), fontWeight = FontWeight.Bold)
                    Text(getT("Earn coins on watching short sponsor ad clips", "সংক্ষিপ্ত বিজ্ঞাপন দেখে কয়েন আয় করুন"), fontSize = 11.sp, color = Color.Gray)
                }
                Button(
                    onClick = { onWatchVideoAd() },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald),
                    modifier = Modifier.testTag("watch_ad_button")
                ) {
                    Text("Watch")
                }
            }
        }
    }
}

@Composable
fun AccountTab(
    viewModel: AlifVpnViewModel,
    currentUser: UserSession?,
    getT: (String, String) -> String,
    onTriggerAuth: () -> Unit,
    onGoToRedeem: () -> Unit
) {
    val context = LocalContext.current
    val config by viewModel.appConfig.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Referral affiliate earnings dashboard
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PeopleAlt, contentDescription = "Referral", tint = GoldenAmber, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(getT("Alif Go VPN Affiliate Network", "আলিফ গো ভিপিএন অ্যাফিলিয়েট নেটওয়ার্ক"), fontWeight = FontWeight.Bold)
                        Text(getT("Share your code to earn dynamic rewards", "বন্ধুদের আমন্ত্রণ জানিয়ে আয় করুন"), fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Profile Avatar & Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Avatar icon with modern outline
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(ElectricBlue.copy(alpha = 0.15f), CircleShape)
                        .border(2.dp, ElectricBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = ElectricBlue,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name and email
                Text(
                    text = currentUser?.name ?: getT("Guest User Mode", "গেস্ট ইউজার মোড"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = currentUser?.email ?: "guest@alifvpn.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Premium Status Badge
                val isPremium = currentUser?.isPremium == true
                Box(
                    modifier = Modifier
                        .background(
                            if (isPremium) GoldenAmber.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (isPremium) GoldenAmber else Color.Gray,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPremium) Icons.Default.WorkspacePremium else Icons.Default.StarOutline,
                            contentDescription = "",
                            tint = if (isPremium) GoldenAmber else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPremium) getT("VIP Premium Member", "ভিআইপি প্রিমিয়াম মেম্বার") else getT("Free Tier Active", "ফ্রি মেম্বার"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isPremium) GoldenAmber else Color.Gray
                        )
                    }
                }

                if (isPremium) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${getT("Expiry Date", "মেয়াদ শেষ হবে")}: ${currentUser?.premiumExpiryDate ?: ""}",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    val expiryTimestamp = currentUser?.premiumExpiryTimestamp ?: 0L
                    if (expiryTimestamp > 0L) {
                        SubscriptionCountdownClock(
                            expiryTimestamp = expiryTimestamp,
                            getT = getT,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Wallet & Redeem shortcut Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Wallet",
                        tint = GoldenAmber,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = getT("Alif Coin Wallet", "আলিফ কয়েন ওয়ালেট"),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${currentUser?.coinBalance ?: 0} " + getT("Coins available", "কয়েন রয়েছে"),
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }

                Button(
                    onClick = { onGoToRedeem() },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald)
                ) {
                    Icon(Icons.Default.Stars, contentDescription = "", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(getT("Redeem", "রেডিম"), fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Referral Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PeopleAlt,
                        contentDescription = "Referral",
                        tint = ElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getT("Referral & Earnings", "রেফারেল এবং উপার্জন"),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(getT("My Referral Code", "আমার রেফার কোড"), fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = currentUser?.referralCode ?: "ALIFNULL",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Button(
                        onClick = {
                            val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clipData = android.content.ClipData.newPlainText("Referral Code", currentUser?.referralCode ?: "")
                            clipboardManager.setPrimaryClip(clipData)
                            android.widget.Toast.makeText(context, "Code copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue.copy(alpha = 0.2f), contentColor = ElectricBlue)
                    ) {
                        Text(getT("Copy Code", "কপি কোড"), fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(getT("USD Commission Earnings:", "ডলার কমিশন উপার্জন:") , fontSize = 12.sp)
                    Text("$${currentUser?.referralEarnings ?: 0.0} USD", fontWeight = FontWeight.Bold, color = RadiantEmerald)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Account management buttons (Logout/Switch Account)
        Button(
            onClick = { onTriggerAuth() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRose.copy(alpha = 0.15f), contentColor = CrimsonRose),
            border = BorderStroke(1.dp, CrimsonRose.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Log out")
            Spacer(modifier = Modifier.width(8.dp))
            Text(getT("Switch Account / Logout", "অ্যাকাউন্ট পরিবর্তন করুন / লগআউট"))
        }
    }
}

fun parseColorHex(hex: String, default: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        default
    }
}

@Composable
fun SubscriptionPlansTab(
    viewModel: AlifVpnViewModel,
    plans: List<SubscriptionPlan>,
    currentUser: UserSession?,
    transactions: List<USDTTransaction>,
    getT: (String, String) -> String,
    onBuyGoogleBilling: (SubscriptionPlan) -> Unit
) {
    var promoFilterTypeSelected by remember { mutableStateOf("purch") } // purch: buy plans, tx: history log
    var pinInput by remember { mutableStateOf("") }
    var activationMessage by remember { mutableStateOf("") }
    var isActivationSuccess by remember { mutableStateOf<Boolean?>(null) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
    val appConfigState by viewModel.appConfig.collectAsState()
    val config = appConfigState ?: AppConfig()
    val highlightColor = remember(config.premiumPageHighlightColor) {
        parseColorHex(config.premiumPageHighlightColor, GoldenAmber)
    }
    val features = remember(config.premiumPageFeaturesList) {
        config.premiumPageFeaturesList.split(";").filter { it.isNotBlank() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = getT("ALIF ACTIVE SUBSCRIPTION", "আলিফ সক্রিয় সাবস্ক্রিপশন"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ElectricBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Subscription overview panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (currentUser?.isPremium == true) Icons.Default.WorkspacePremium else Icons.Default.StarOutline,
                        contentDescription = "Subscription Plan Status",
                        tint = if (currentUser?.isPremium == true) GoldenAmber else Color.Gray,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = getT("Current Active Plan", "সক্রিয় প্যাকেজ"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = if (currentUser?.isPremium == true) {
                                currentUser.currentPlanName.ifBlank { getT("VIP Premium Active", "ভিআইপি প্রিমিয়াম সক্রিয়") }
                            } else {
                                getT("Free Tier Active", "ফ্রি মেম্বার")
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (currentUser?.isPremium == true) GoldenAmber else Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (currentUser?.isPremium == true) {
                    Text(
                        text = "${getT("Expiry/Status", "মেয়াদ/স্ট্যাটাস")}: ${currentUser.premiumExpiryDate}",
                        fontSize = 11.sp,
                        color = RadiantEmerald,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    val expiryTimestamp = currentUser.premiumExpiryTimestamp
                    if (expiryTimestamp > 0L) {
                        SubscriptionCountdownClock(
                            expiryTimestamp = expiryTimestamp,
                            getT = getT,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                } else {
                    Text(
                        text = getT(
                            "You are currently on the Free plan. You can connect to all Free servers at no cost! Upgrade below to access ultra-fast Premium VIP servers.",
                            "আপনি বর্তমানে ফ্রি প্ল্যান ব্যবহার করছেন। যেকোনো ফ্রি সার্ভার কোনো খরচ ছাড়াই ব্যবহার করতে পারবেন! আল্ট্রা-ফাস্ট প্রিমিয়াম সার্ভার ব্যবহার করতে নিচে প্যাকেজ কিনুন বা কয়েন এক্সচেঞ্জ করুন।"
                        ),
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter tab controllers
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { promoFilterTypeSelected = "purch" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (promoFilterTypeSelected == "purch") ElectricBlue else MaterialTheme.colorScheme.surface,
                    contentColor = if (promoFilterTypeSelected == "purch") Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text(getT("Premium Plans", "প্রিমিয়াম প্যাকেজ"), fontSize = 12.sp)
            }
            Button(
                onClick = { promoFilterTypeSelected = "tx" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (promoFilterTypeSelected == "tx") ElectricBlue else MaterialTheme.colorScheme.surface,
                    contentColor = if (promoFilterTypeSelected == "tx") Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text(getT("Reseller Option", "রিসেলার অপশন"), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (promoFilterTypeSelected == "purch") {
            // Subscription cards lists
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("plans_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(plans) { plan ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = plan.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(text = "Expires in ${plan.durationDays} days", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Devices,
                                            contentDescription = null,
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = getT("Device Limit: ${plan.deviceLimit} phones", "ডিভাইস সীমা: সর্বোচ্চ ${plan.deviceLimit} টি ফোন"),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(RadiantEmerald.copy(0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                  ) {
                                    Text(
                                        text = "$${plan.priceUsdt} USD",
                                        fontWeight = FontWeight.Bold,
                                        color = RadiantEmerald
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = getT("✨ PREMIUM BENEFITS:", "✨ প্রিমিয়াম সুবিধাসমূহ:"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = highlightColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            features.forEach { feat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "",
                                        tint = highlightColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = feat, fontSize = 11.sp, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                val hasPlayBilling = config.isPlayBillingEnabled
                                val hasCoinRedemption = config.isCoinRedemptionEnabled

                                if (!hasPlayBilling && !hasCoinRedemption) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = getT("Purchasing is temporarily disabled by Admin.", "এডমিন দ্বারা সাময়িকভাবে ক্রয় করার অপশন বন্ধ আছে।"),
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else {
                                    if (hasCoinRedemption) {
                                        // Conversion with Coins button
                                        OutlinedButton(
                                            onClick = { viewModel.redeemCoinsForPremium(plan) },
                                            modifier = if (hasPlayBilling) Modifier.weight(1.5f) else Modifier.fillMaxWidth(),
                                            enabled = (currentUser?.coinBalance ?: 0) >= plan.coinsRequired,
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldenAmber)
                                        ) {
                                            Icon(Icons.Default.MonetizationOn, contentDescription = "", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${plan.coinsRequired} Coins", fontSize = 11.sp)
                                        }
                                    }

                                    if (hasPlayBilling) {
                                        if (hasCoinRedemption) {
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }
                                        // Google Play Checkout button
                                        Button(
                                            onClick = { onBuyGoogleBilling(plan) },
                                            modifier = if (hasCoinRedemption) Modifier.weight(2f) else Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                        ) {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = "", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(getT("Buy / Subscribe", "ক্রয় / সাবস্ক্রাইব"), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Reseller Options and PIN Activation Screen
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Voucher PIN Activation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface),
                    border = BorderStroke(1.dp, ElectricBlue.copy(0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getT("Activate Voucher PIN", "ভাউচার পিন অ্যাক্টিভেট করুন"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = getT(
                                "Enter the premium voucher activation PIN code received from your reseller.",
                                "রিসেলারের কাছ থেকে কেনা প্রিমিয়াম ভাউচার অ্যাক্টিভেশন পিন কোডটি নিচে দিন।"
                            ),
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            placeholder = { Text("e.g. ALIF-WEEKLY-777", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (pinInput.isNotBlank()) {
                                    viewModel.redeemActivationPin(pinInput) { success, msg ->
                                        isActivationSuccess = success
                                        activationMessage = msg
                                        if (success) {
                                            pinInput = ""
                                        }
                                    }
                                } else {
                                    isActivationSuccess = false
                                    activationMessage = "Please enter a PIN code first."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald)
                        ) {
                            Text(getT("ACTIVATE PIN CODE", "পিন কোড অ্যাক্টিভেট করুন"), fontWeight = FontWeight.Bold)
                        }
                        
                        if (activationMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = if (isActivationSuccess == true) RadiantEmerald.copy(0.15f) else CrimsonRose.copy(0.15f),
                                border = BorderStroke(1.dp, if (isActivationSuccess == true) RadiantEmerald else CrimsonRose),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = activationMessage,
                                    color = if (isActivationSuccess == true) RadiantEmerald else CrimsonRose,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Reseller Business Options Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, GoldenAmber.copy(0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BusinessCenter,
                                contentDescription = null,
                                tint = GoldenAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getT("Reseller Panel", "রিসেলার প্যানেল"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldenAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = getT(
                                "Become an official ALIF VPN Reseller! You can buy premium activation PINs in bulk at wholesale discount prices and sell them locally to your clients for cash or mobile money (bKash/Nagad/Recharge).",
                                "আলিফ ভিপিএন-এর অফিসিয়াল রিসেলার হয়ে যান! পাইকারি ডিসকাউন্ট রেটে প্রিমিয়াম পিন কোড কিনুন এবং তা আপনার লোকাল এরিয়ার কাস্টমারদের কাছে নগদ, বিকাশ বা মোবাইল রিচার্জের বিনিময়ে রিটেইল মূল্যে বিক্রি করে ব্যাপক লাভ করুন।"
                            ),
                            fontSize = 11.sp,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri(config.telegramLink)
                                } catch (e: Exception) {
                                    uriHandler.openUri("https://t.me/alifvpn_official")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(getT("Contact Admin to Start Reseller", "রিসেলার হতে অ্যাডমিনের সাথে যোগাযোগ করুন"), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Ledger history listings (Wholesaler Payments / General Logs)
                Text(
                    text = getT("Wholesaler Payments & Receipts", "পাইকারি পেমেন্ট এবং পূর্বের রেকর্ড"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = "None", modifier = Modifier.size(36.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(getT("No transaction records found.", "কোন পেমেন্ট রেকর্ড খুঁজে পাওয়া যায়নি।"), color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    transactions.forEach { tx ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = tx.planName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "Chain: ${tx.network}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                when (tx.status) {
                                                    "Approved" -> RadiantEmerald.copy(0.2f)
                                                    "Rejected" -> CrimsonRose.copy(0.2f)
                                                    else -> GoldenAmber.copy(0.2f)
                                                },
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = tx.status,
                                            fontWeight = FontWeight.Bold,
                                            color = when (tx.status) {
                                                "Approved" -> RadiantEmerald
                                                "Rejected" -> CrimsonRose
                                                else -> GoldenAmber
                                            },
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tx Hash: ${tx.txHash}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(tx.timestamp)),
                                        fontSize = 9.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "$${tx.amount} USD",
                                        fontWeight = FontWeight.Bold,
                                        color = RadiantEmerald,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupportTab(
    viewModel: AlifVpnViewModel,
    getT: (String, String) -> String,
    tickets: List<SupportTicket>
) {
    var supportModeSelected by remember { mutableStateOf("form") } // form: Submit screen, list: History tickets
    
    // Contact Form input states
    var subjectText by remember { mutableStateOf("") }
    var categorySelected by remember { mutableStateOf("Billing") }
    var chatMessageText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = getT("ALIF SUPPORT AGENT HELP", "আলিফ কাস্টমার কেয়ার হেল্প"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ElectricBlue
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Tab sub controls
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { supportModeSelected = "form" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (supportModeSelected == "form") ElectricBlue else MaterialTheme.colorScheme.surface,
                    contentColor = if (supportModeSelected == "form") Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text(getT("Create Ticket", "সহায়তা টিকিট"), fontSize = 12.sp)
            }
            Button(
                onClick = { supportModeSelected = "list" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (supportModeSelected == "list") ElectricBlue else MaterialTheme.colorScheme.surface,
                    contentColor = if (supportModeSelected == "list") Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text(getT("My Tickets", "আমার টিকিট সমূহ"), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (supportModeSelected == "form") {
            // Interactive support ticket generation screen
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Subject/Issue", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(
                    value = subjectText,
                    onValueChange = { subjectText = it },
                    placeholder = { Text("Enter subject") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Billing", "Speed", "Server", "Other").forEach { category ->
                        FilterChip(
                            selected = categorySelected == category,
                            onClick = { categorySelected = category },
                            label = { Text(category) }
                        )
                    }
                }

                Text(text = "Describe your issue", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(
                    value = chatMessageText,
                    onValueChange = { chatMessageText = it },
                    placeholder = { Text("Type details here so our network support can review...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                Button(
                    onClick = {
                        if (subjectText.isNotBlank() && chatMessageText.isNotBlank()) {
                            viewModel.submitTicket(subjectText, categorySelected, chatMessageText)
                            subjectText = ""
                            chatMessageText = ""
                            viewModel.sendBroadcastNotification(
                                "Support Ticket Created",
                                "A ticket categorized in '$categorySelected' has been loaded and queued for Admin's custom reply."
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald),
                    enabled = subjectText.isNotBlank() && chatMessageText.isNotBlank()
                ) {
                    Text(getT("Submit Ticket Request", "সহায়তা টিকিট দাখিল করুন"))
                }
            }
        } else {
            // Display User's submitted ticket history logs
            if (tickets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SupportAgent, contentDescription = "Agent", modifier = Modifier.size(50.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(getT("No tickets active. Create one to get support.", "কোন সমর্থনকারী টিকিট নেই।"), color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tickets) { ticket ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = ticket.subject, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(text = "Category: ${ticket.category}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (ticket.status == "Open") GoldenAmber.copy(0.2f) else RadiantEmerald.copy(0.2f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = ticket.status,
                                            fontWeight = FontWeight.Bold,
                                            color = if (ticket.status == "Open") GoldenAmber else RadiantEmerald,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(text = ticket.message, fontSize = 12.sp)
                                }
                                if (ticket.reply.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(ElectricBlue.copy(0.1f), RoundedCornerShape(8.dp))
                                            .border(1.dp, ElectricBlue.copy(0.2f), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(text = "Admin Support Reply:", fontSize = 11.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = ticket.reply, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SECURE CUSTOM DIRECT DIALOGS ---

@Composable
fun ServerSelectorDialog(
    servers: List<VpnServer>,
    selectedServer: VpnServer?,
    getT: (String, String) -> String,
    onDismiss: () -> Unit,
    onSelect: (VpnServer) -> Unit
) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = getT("Select Fast Server Location", "দ্রুত সার্ভার লোকেশন বাছুন"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(servers) { server ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selectedServer?.id == server.id) MaterialTheme.colorScheme.surfaceVariant
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelect(server) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(server.countryCode, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "${server.countryName} - ${server.city}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = "${server.type} • IP: ${server.ipAddress}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Text(text = "${server.latency}ms", fontSize = 12.sp, color = RadiantEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { onDismiss() }, modifier = Modifier.align(Alignment.End)) {
                    Text(getT("Close", "বন্ধ করুন"))
                }
            }
        }
    }
}

@Composable
fun ProtocolSettingsDialog(
    viewModel: AlifVpnViewModel,
    getT: (String, String) -> String,
    onDismiss: () -> Unit
) {
    val activeProto by viewModel.selectedProtocol.collectAsState()
    var selectedProto by remember { mutableStateOf(activeProto) }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = getT("Select VPN Protocol", "ভিপিএন প্রোটোকল বাছুন"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                listOf("WireGuard", "OpenVPN", "IKEv2").forEach { proto ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProto = proto }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedProto == proto,
                            onClick = { selectedProto = proto }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = proto, fontWeight = FontWeight.Bold)
                            val details = when (proto) {
                                "WireGuard" -> getT("Fastest cryptographically secure connection.", "সবচেয়ে দ্রুত ক্রিপ্টোগ্রাফিকভাবে সুরক্ষিত সংযোগ।")
                                "OpenVPN" -> getT("Industry standard tunnel maximizing firewall bypassing.", "ফায়ারওয়াল বাইপাসিং এ সবচেয়ে উপযোগী ও স্থিতিশীল টানেল।")
                                else -> getT("Lightweight mobile protocol built for swift reconnection.", "মোবাইল ডিভাইসের জন্য দ্রুত পুনঃসংযোগকারী প্রোটোকল।")
                            }
                            Text(text = details, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text(getT("Cancel", "বাতিল"))
                    }
                    Button(
                        onClick = {
                            viewModel.selectedProtocol.value = selectedProto
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text(getT("Save Protocol", "রক্ষা করুন"))
                    }
                }
            }
        }
    }
}

// --- FULL LIVE ADMIN WEB PANEL WORKSPACE SIMULATION ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPanelTab(
    viewModel: AlifVpnViewModel,
    servers: List<VpnServer>,
    users: List<UserSession>,
    transactions: List<USDTTransaction>,
    tickets: List<SupportTicket>,
    admob: AdmobConfig?,
    appConf: AppConfig?,
    plans: List<SubscriptionPlan>,
    getT: (String, String) -> String
) {
    var adminActiveSection by remember { mutableStateOf("dashboard") } // dashboard, users, servers, payments, tickets, configs

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCosmicDark)
    ) {
        // Vertical Left Navigation Column on Admin mode
        Column(
            modifier = Modifier
                .width(90.dp)
                .fillMaxHeight()
                .background(DeepCosmicSurface)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AdminSidebarItem(icon = Icons.Default.Dashboard, label = "Board", isSelected = adminActiveSection == "dashboard") { adminActiveSection = "dashboard" }
            AdminSidebarItem(icon = Icons.Default.Group, label = "Users", isSelected = adminActiveSection == "users") { adminActiveSection = "users" }
            AdminSidebarItem(icon = Icons.Default.Dns, label = "Servers", isSelected = adminActiveSection == "servers") { adminActiveSection = "servers" }
            AdminSidebarItem(icon = Icons.Default.Devices, label = "Srv Limit", isSelected = adminActiveSection == "limits") { adminActiveSection = "limits" }
            AdminSidebarItem(icon = Icons.Default.AttachMoney, label = "TRX Pay", isSelected = adminActiveSection == "payments") { adminActiveSection = "payments" }
            AdminSidebarItem(icon = Icons.Default.Sms, label = "Tickets", isSelected = adminActiveSection == "tickets") { adminActiveSection = "tickets" }
            AdminSidebarItem(icon = Icons.Default.WorkspacePremium, label = "Plans", isSelected = adminActiveSection == "plans") { adminActiveSection = "plans" }
            AdminSidebarItem(icon = Icons.Default.VpnKey, label = "Pins", isSelected = adminActiveSection == "pins") { adminActiveSection = "pins" }
            AdminSidebarItem(icon = Icons.Default.OndemandVideo, label = "AdMob", isSelected = adminActiveSection == "admob") { adminActiveSection = "admob" }
            AdminSidebarItem(icon = Icons.Default.CreditCard, label = "Billing", isSelected = adminActiveSection == "billing") { adminActiveSection = "billing" }
            AdminSidebarItem(icon = Icons.Default.SettingsApplications, label = "Configs", isSelected = adminActiveSection == "configs") { adminActiveSection = "configs" }
        }

        // Live workspaces according to left chosen section
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(12.dp)
        ) {
            when (adminActiveSection) {
                "dashboard" -> AdminDashboardOverview(users, servers, transactions, envEmail = "ffcct2755@gmail.com")
                "users" -> AdminUserManagement(users, viewModel)
                "servers" -> AdminServerManagement(servers, appConf, viewModel)
                "limits" -> AdminServerLimitsManagement(servers, viewModel)
                "payments" -> AdminPaymentVerification(transactions, viewModel)
                "tickets" -> AdminTicketResolution(tickets, viewModel)
                "plans" -> AdminPlansManagement(plans, viewModel)
                "pins" -> AdminResellerPinsManagement(viewModel)
                "admob" -> AdminAdmobManagement(admob, viewModel)
                "billing" -> AdminPlayBillingManagement(appConf, viewModel)
                "configs" -> AdminConfigPanel(admob, appConf, plans, viewModel)
            }
        }
    }
}

@Composable
fun AdminSidebarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (isSelected) ElectricBlue.copy(alpha = 0.15f) else Color.Transparent)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) ElectricBlue else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, color = if (isSelected) Color.White else Color.Gray)
    }
}

@Composable
fun AdminDashboardOverview(
    users: List<UserSession>,
    servers: List<VpnServer>,
    transactions: List<USDTTransaction>,
    envEmail: String
) {
    val totalRevenue = transactions.filter { it.status == "Approved" }.sumOf { it.amount }
    val pendingRevenue = transactions.filter { it.status == "Pending" }.sumOf { it.amount }

    val activeUsers = users.count { !it.isBanned }
    val premiumUsers = users.count { it.isPremium }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "ADMIN CONTROL PANEL",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = RadiantEmerald
        )
        Text("Responsive control panel managing Alif VPN instances recursively.", fontSize = 11.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(6.dp))

        // Total Counts Row Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardOverviewStatCard(label = "Total Users", stat = "${users.size}", activeColor = ElectricBlue, modifier = Modifier.weight(1f))
            DashboardOverviewStatCard(label = "Active VPNs", stat = "${servers.size}", activeColor = RadiantEmerald, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardOverviewStatCard(label = "Active Users (Not Banned)", stat = "$activeUsers", activeColor = ElectricBlue, modifier = Modifier.weight(1f))
            DashboardOverviewStatCard(label = "Premium VIP Users", stat = "$premiumUsers", activeColor = GoldenAmber, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardOverviewStatCard(label = "Total Revenue", stat = "$${totalRevenue}", activeColor = GoldenAmber, modifier = Modifier.weight(1f))
            DashboardOverviewStatCard(label = "Pending USD", stat = "$${pendingRevenue}", activeColor = CrimsonRose, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))
        
        Text("Plan Wise Users (কোন প্ল্যানে কতজন)", fontWeight = FontWeight.Bold, color = Color.White)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                val planCounts = users.groupBy { it.currentPlanName }
                if (planCounts.isEmpty()) {
                    Text("No user data available.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    planCounts.forEach { (planName, userList) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (planName.contains("Free", ignoreCase = true) || planName.contains("Guest", ignoreCase = true)) Color.Gray else GoldenAmber,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = planName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(ElectricBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${userList.size} " + if (userList.size == 1) "User" else "Users",
                                    color = ElectricBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Active Context Info", fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "Admin Active Node: admin.alifvpn.com", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Current developer email: $envEmail", fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "No logs security token is successfully active.", fontSize = 11.sp, color = RadiantEmerald)
            }
        }
    }
}

@Composable
fun DashboardOverviewStatCard(label: String, stat: String, activeColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(activeColor, CircleShape)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(stat, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun AdminUserManagement(users: List<UserSession>, viewModel: AlifVpnViewModel) {
    var searchUserQuery by remember { mutableStateOf("") }

    val filteredList = users.filter {
        it.email.contains(searchUserQuery, ignoreCase = true) || it.name.contains(searchUserQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Manage User Registrations", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = searchUserQuery,
            onValueChange = { searchUserQuery = it },
            placeholder = { Text("Search by email/name...") },
            modifier = Modifier.fillMaxWidth().testTag("user_search"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(user.name, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(user.email, fontSize = 11.sp, color = Color.Gray)
                            }
                            if (user.isPremium) {
                                Box(
                                    modifier = Modifier
                                        .background(GoldenAmber.copy(0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Premium", color = GoldenAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Coins: ${user.coinBalance}", fontSize = 11.sp, color = Color.LightGray)
                            Text("Plan: ${user.currentPlanName}", fontSize = 11.sp, color = RadiantEmerald, fontWeight = FontWeight.Bold)
                            Text("Banned: ${user.isBanned}", fontSize = 11.sp, color = if (user.isBanned) CrimsonRose else Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeDevicesCount = user.activeDevicesList.split(";").filter { it.isNotEmpty() }.size
                            Text("Devices: $activeDevicesCount / ${user.deviceLimit}", fontSize = 11.sp, color = Color.LightGray)

                            // Device Limit Increment / Decrement
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Limit:", fontSize = 10.sp, color = Color.Gray)
                                IconButton(
                                    onClick = { if (user.deviceLimit > 1) viewModel.adminUpdateUserDeviceLimit(user.email, user.deviceLimit - 1) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease Limit", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                }
                                Text("${user.deviceLimit}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                IconButton(
                                    onClick = { viewModel.adminUpdateUserDeviceLimit(user.email, user.deviceLimit + 1) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase Limit", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                }
                            }

                            // Reset device associations
                            TextButton(
                                onClick = { viewModel.adminResetUserDevices(user.email) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Reset Devices", fontSize = 10.sp, color = ElectricBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Ban/Unban Toggle
                            Button(
                                onClick = {
                                    if (user.isBanned) viewModel.adminUnbanUser(user.email)
                                    else viewModel.adminBanUser(user.email)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (user.isBanned) RadiantEmerald else CrimsonRose
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (user.isBanned) "Unban" else "Ban", fontSize = 10.sp, color = Color.White)
                            }

                            // Promote/Upgrade premium toggle
                            Button(
                                onClick = {
                                    if (user.isPremium) viewModel.adminDowngradeUser(user.email)
                                    else viewModel.adminUpgradeUser(user.email, 30) // Grant premium
                                },
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (user.isPremium) Color.Gray else GoldenAmber
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (user.isPremium) "Downgrade" else "Grant Pro", fontSize = 10.sp, color = Color.Black)
                            }

                            // Add Coins gift
                            Button(
                                onClick = { viewModel.adminGiftCoins(user.email, 200) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+200 C", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminServerLimitsManagement(servers: List<VpnServer>, viewModel: AlifVpnViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredServers = remember(servers, searchQuery) {
        servers.filter {
            it.countryName.contains(searchQuery, ignoreCase = true) ||
            it.city.contains(searchQuery, ignoreCase = true) ||
            it.ipAddress.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Header card
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Server Connection Limits (সার্ভার ডিভাইস লিমিট)",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure the maximum allowed concurrent phone connections for each VPN server. Reset active connections instantly to clear user associations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by country, city or IP...", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = ElectricBlue,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedContainerColor = DeepCosmicSurface,
                unfocusedContainerColor = DeepCosmicSurface
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (filteredServers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No servers found matching search query.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredServers) { server ->
                    val activeCount = remember(server.connectedDevicesList) {
                        server.connectedDevicesList.split(";").filter { it.isNotEmpty() }.size
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Country code emoji
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(DeepCosmicDark, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val codeEmoji = when (server.countryCode) {
                                    "US" -> "🇺🇸"
                                    "SG" -> "🇸🇬"
                                    "BD" -> "🇧🇩"
                                    "JP" -> "🇯🇵"
                                    "GB" -> "🇬🇧"
                                    "DE" -> "🇩🇪"
                                    "KR" -> "🇰🇷"
                                    "IN" -> "🇮🇳"
                                    else -> "🌐"
                                }
                                Text(codeEmoji, fontSize = 18.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Server Info column
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${server.countryName} - ${server.city}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "IP: ${server.ipAddress} • Protocol: ${server.protocol}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Active user count status badge
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (activeCount >= server.maxConnectedDevices) CrimsonRose.copy(0.2f) else RadiantEmerald.copy(0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Active Phones: $activeCount / ${server.maxConnectedDevices} connected",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (activeCount >= server.maxConnectedDevices) CrimsonRose else RadiantEmerald
                                    )
                                }
                            }

                            // Controls Row
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Limit Incrementor / Decrementor
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Limit:", fontSize = 11.sp, color = Color.Gray)
                                    IconButton(
                                        onClick = { if (server.maxConnectedDevices > 1) viewModel.adminUpdateServerDeviceLimit(server.id, server.maxConnectedDevices - 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Decrease Max Devices Limit",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "${server.maxConnectedDevices}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = { viewModel.adminUpdateServerDeviceLimit(server.id, server.maxConnectedDevices + 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Increase Max Devices Limit",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Reset Connections Button
                                OutlinedButton(
                                    onClick = { viewModel.adminResetServerDevices(server.id) },
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, CrimsonRose.copy(alpha = 0.6f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRose)
                                ) {
                                    Icon(
                                        Icons.Default.PowerSettingsNew,
                                        contentDescription = "Reset connections",
                                        modifier = Modifier.size(11.dp),
                                        tint = CrimsonRose
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Active", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminServerManagement(servers: List<VpnServer>, appConf: AppConfig?, viewModel: AlifVpnViewModel) {
    var countryNameInput by remember { mutableStateOf("") }
    var cityInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var ipInput by remember { mutableStateOf("") }
    var serverTypeSelect by remember { mutableStateOf("Free") }
    var remoteJsonUrlInput by remember { mutableStateOf(appConf?.customApiUrl?.ifBlank { "https://mdshahinislamshamim420-cell.github.io/alif-go-vpn-josn/" } ?: "https://mdshahinislamshamim420-cell.github.io/alif-go-vpn-josn/") }

    val context = LocalContext.current
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    viewModel.adminImportServersFromZip(bytes)
                    android.widget.Toast.makeText(context, "Processing ZIP. Servers added automatically!", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Failed to read file", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Create New Active Server", fontWeight = FontWeight.Bold, color = RadiantEmerald)
        }
        
        item {
            Card(colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = countryNameInput,
                            onValueChange = { countryNameInput = it },
                            placeholder = { Text("Country Name") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it },
                            placeholder = { Text("Code (US/BD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = cityInput,
                            onValueChange = { cityInput = it },
                            placeholder = { Text("City Location") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            placeholder = { Text("IP Address") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Type:", color = Color.White, fontSize = 12.sp)
                        listOf("Free", "Premium", "Gaming", "Streaming").forEach { cat ->
                            FilterChip(
                                selected = serverTypeSelect == cat,
                                onClick = { serverTypeSelect = cat },
                                label = { Text(cat, fontSize = 10.sp) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (countryNameInput.isNotBlank() && ipInput.isNotBlank()) {
                                viewModel.adminAddServer(
                                    countryNameInput,
                                    codeInput.ifEmpty { "US" },
                                    cityInput.ifEmpty { "New Route" },
                                    ipInput,
                                    serverTypeSelect,
                                    Random.nextInt(15, 95),
                                    Random.nextInt(10, 45),
                                    "WireGuard"
                                )
                                countryNameInput = ""
                                cityInput = ""
                                ipInput = ""
                                codeInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald),
                        enabled = countryNameInput.isNotBlank() && ipInput.isNotBlank()
                    ) {
                        Text("Publish Live Server")
                    }
                }
            }
        }

        item {
            Text("Bulk Import Server ZIP", fontWeight = FontWeight.Bold, color = ElectricBlue)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Upload a server ZIP file containing OpenVPN (.ovpn) or WireGuard (.conf) configs. The app will auto-parse server name, protocol, and configuration IP from each file!",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Naming rule: Code_City_Type.ovpn (e.g., US_Chicago_free.ovpn, SG_Changi_premium.conf)",
                        color = RadiantEmerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { fileLauncher.launch("application/zip") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Choose ZIP", fontSize = 11.sp)
                        }
                        
                        Button(
                            onClick = {
                                viewModel.adminImportSimulatedZip()
                                android.widget.Toast.makeText(context, "Parsed & Imported 5 servers from simulated ZIP!", android.widget.Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald)
                        ) {
                            Icon(Icons.Default.AutoMode, contentDescription = "", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulate ZIP", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Text("Sync Servers from JSON URL", fontWeight = FontWeight.Bold, color = ElectricBlue)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Host a JSON file of your VPN servers on your website or GitHub Pages, and enter the raw URL below to sync automatically!",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = remoteJsonUrlInput,
                        onValueChange = { remoteJsonUrlInput = it },
                        placeholder = { Text("https://yourdomain.com/servers.json") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Button(
                        onClick = {
                            if (remoteJsonUrlInput.isNotBlank()) {
                                if (appConf != null) {
                                    viewModel.adminUpdateGlobalSettings(
                                        appConf.copy(
                                            customApiUrl = remoteJsonUrlInput.trim(),
                                            isCustomApiSyncEnabled = true
                                        )
                                    )
                                }
                                viewModel.adminSyncServersFromJsonUrl(
                                    remoteJsonUrlInput,
                                    onSuccess = {
                                        android.widget.Toast.makeText(context, "Successfully synced servers from JSON!", android.widget.Toast.LENGTH_LONG).show()
                                    },
                                    onError = { err ->
                                        android.widget.Toast.makeText(context, "Error: $err", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald),
                        enabled = remoteJsonUrlInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Servers Now")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Active Server Relay List (${servers.size})", fontWeight = FontWeight.Bold)
        }

        items(servers) { tServer ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepCosmicSurface, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${tServer.countryName} (${tServer.countryCode})", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${tServer.city} • IP: ${tServer.ipAddress} • ${tServer.type} • ${tServer.protocol}", fontSize = 11.sp, color = Color.LightGray)
                }
                IconButton(onClick = { viewModel.adminDeleteServer(tServer.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Server", tint = CrimsonRose)
                }
            }
        }
    }
}

@Composable
fun AdminPaymentVerification(transactions: List<USDTTransaction>, viewModel: AlifVpnViewModel) {
    val pendingList = transactions.filter { it.status == "Pending" }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Play Store Billing Checkout Receipts", fontWeight = FontWeight.Bold, color = GoldenAmber)
        Spacer(modifier = Modifier.height(12.dp))

        if (pendingList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = "None", modifier = Modifier.size(50.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No pending Play Billing orders currently.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pendingList) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("From: ${tx.userEmail}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text("Plan purchase: ${tx.planName}", fontSize = 11.sp, color = Color.LightGray)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(GoldenAmber.copy(0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("$${tx.amount} USD", fontWeight = FontWeight.Bold, color = GoldenAmber, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Checkout: ${tx.network}", fontSize = 11.sp, color = Color.Gray)
                            Text("Order ID / Play Token:", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                tx.txHash,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.adminVerifyPayment(tx.id, false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRose),
                                    modifier = Modifier.weight(1f).testTag("reject_button_${tx.id}"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("REJECT RECEIPT", fontSize = 11.sp, color = Color.White)
                                }
                                Button(
                                    onClick = { viewModel.adminVerifyPayment(tx.id, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald),
                                    modifier = Modifier.weight(1f).testTag("approve_button_${tx.id}"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("APPROVE & UPGRADE", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTicketResolution(tickets: List<SupportTicket>, viewModel: AlifVpnViewModel) {
    val pendingTickets = tickets.filter { it.status == "Open" }
    var activeReplyingText by remember { mutableStateOf("") }
    var replyingTicketId by remember { mutableIntStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Support Ticketing Resolutions", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (pendingTickets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No open support tickets at this moment.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pendingTickets) { ticket ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("User: ${ticket.userEmail}", fontWeight = FontWeight.Bold, color = Color.LightGray, fontSize = 11.sp)
                            Text("Subject: ${ticket.subject}", fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(ticket.message, fontSize = 12.sp, color = Color.LightGray)

                            Spacer(modifier = Modifier.height(12.dp))

                            if (replyingTicketId == ticket.id) {
                                OutlinedTextField(
                                    value = activeReplyingText,
                                    onValueChange = { activeReplyingText = it },
                                    placeholder = { Text("Type reply answer...") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { replyingTicketId = -1 }) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = {
                                            if (activeReplyingText.isNotBlank()) {
                                                viewModel.adminReplySupportTicket(ticket.id, activeReplyingText)
                                                replyingTicketId = -1
                                                activeReplyingText = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald)
                                    ) {
                                        Text("Send Answer")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { replyingTicketId = ticket.id },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Reply To Issue")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminConfigPanel(
    admob: AdmobConfig?,
    appConf: AppConfig?,
    plans: List<SubscriptionPlan>,
    viewModel: AlifVpnViewModel
) {
    var dailyBonusText by remember { mutableStateOf(appConf?.dailyRewardAmount?.toString() ?: "10") }
    var supportEmailText by remember { mutableStateOf(appConf?.supportEmail ?: "ffcct2755@gmail.com") }

    // GitHub Auto-Sync Configurations states
    var gitHubPatState by remember { mutableStateOf(appConf?.gitHubPat ?: "") }
    var gitHubOwnerState by remember { mutableStateOf(appConf?.gitHubOwner ?: "mdshahinislamshamim420-cell") }
    var gitHubRepoState by remember { mutableStateOf(appConf?.gitHubRepo ?: "alif-go-vpn-josn") }
    var gitHubFilePathState by remember { mutableStateOf(appConf?.gitHubFilePath ?: "index.html") }
    var isGitHubAutoSyncEnabledState by remember { mutableStateOf(appConf?.isGitHubAutoSyncEnabled ?: false) }

    // Web Host / MySQL Custom API Configurations states
    var customApiUrlState by remember { mutableStateOf(appConf?.customApiUrl ?: "") }
    var customApiKeyState by remember { mutableStateOf(appConf?.customApiKey ?: "") }
    var customApiMethodState by remember { mutableStateOf(appConf?.customApiMethod ?: "POST") }
    var isCustomApiSyncEnabledState by remember { mutableStateOf(appConf?.isCustomApiSyncEnabled ?: false) }

    // Premium Page Customization states
    var premiumPageTitleState by remember { mutableStateOf(appConf?.premiumPageTitle ?: "Go Premium & Experience Ultimate Speed") }
    var premiumPageSubtitleState by remember { mutableStateOf(appConf?.premiumPageSubtitle ?: "Access 100+ secure, lightning-fast servers globally without limits") }
    var premiumPageHighlightColorState by remember { mutableStateOf(appConf?.premiumPageHighlightColor ?: "#FFD700") }
    var premiumPageFeaturesListState by remember { mutableStateOf(appConf?.premiumPageFeaturesList ?: "Unlimited Speed & Bandwidth;Ad-free Premium Experience;Access to VIP & Gaming Servers;24/7 Dedicated Live Support") }
    var premiumPageBannerUrlState by remember { mutableStateOf(appConf?.premiumPageBannerUrl ?: "") }

    var broadcastTitleText by remember { mutableStateOf("") }
    var broadcastBodyText by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("App configurations and Global Tweak Rules", fontWeight = FontWeight.Bold, color = ElectricBlue)

        // Global Variable Constants
        Card(colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Global Variable & Support Settings", fontWeight = FontWeight.Bold, color = Color.White)
                
                OutlinedTextField(
                    value = dailyBonusText,
                    onValueChange = { dailyBonusText = it },
                    label = { Text("Daily Check-In Coins reward") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = supportEmailText,
                    onValueChange = { supportEmailText = it },
                    label = { Text("Support & Alert Email (ইমেইল যেখানে নোটিফিকেশন যাবে)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val valBonus = dailyBonusText.toIntOrNull() ?: 10
                        if (appConf != null) {
                            viewModel.adminUpdateGlobalSettings(
                                appConf.copy(
                                    dailyRewardAmount = valBonus,
                                    supportEmail = supportEmailText.trim()
                                )
                            )
                            android.widget.Toast.makeText(context, "Settings updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Apply Global Variables & Email Settings")
                }
            }
        }

        // Premium subscription page customizer from admin panel
        Card(colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Customize Premium Subscription Page", fontWeight = FontWeight.Bold, color = Color.White)
                
                OutlinedTextField(
                    value = premiumPageTitleState,
                    onValueChange = { premiumPageTitleState = it },
                    label = { Text("Premium Screen Main Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = premiumPageSubtitleState,
                    onValueChange = { premiumPageSubtitleState = it },
                    label = { Text("Premium Screen Highlight Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = premiumPageHighlightColorState,
                        onValueChange = { premiumPageHighlightColorState = it },
                        label = { Text("Highlight Color (Hex)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = premiumPageBannerUrlState,
                        onValueChange = { premiumPageBannerUrlState = it },
                        label = { Text("Banner Decoration URL (Optional)") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = premiumPageFeaturesListState,
                    onValueChange = { premiumPageFeaturesListState = it },
                    label = { Text("Features List (Semicolon ';' separated)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (appConf != null) {
                            val updated = appConf.copy(
                                premiumPageTitle = premiumPageTitleState,
                                premiumPageSubtitle = premiumPageSubtitleState,
                                premiumPageHighlightColor = premiumPageHighlightColorState,
                                premiumPageFeaturesList = premiumPageFeaturesListState,
                                premiumPageBannerUrl = premiumPageBannerUrlState
                            )
                            viewModel.adminUpdateGlobalSettings(updated)
                            android.widget.Toast.makeText(context, "Premium Page customized successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Premium Customizations")
                }
            }
        }

        // Broadcast notifications widget segment
        Card(colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Send System Notifications and Alerts", fontWeight = FontWeight.Bold, color = Color.White)
                OutlinedTextField(
                    value = broadcastTitleText,
                    onValueChange = { broadcastTitleText = it },
                    label = { Text("Alert Broadcast Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = broadcastBodyText,
                    onValueChange = { broadcastBodyText = it },
                    label = { Text("Alert Message Body") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (broadcastTitleText.isNotBlank()) {
                            viewModel.sendBroadcastNotification(broadcastTitleText, broadcastBodyText)
                            broadcastTitleText = ""
                            broadcastBodyText = ""
                            android.widget.Toast.makeText(context, "Broadcast sent to all sessions!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = broadcastTitleText.isNotBlank()
                ) {
                    Text("Send Broadcast Now")
                }
            }
        }

        // GitHub Auto-Sync segment
        var isPatVisible by remember { mutableStateOf(false) }
        Card(colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("GitHub Pages Auto-Publish (স্বয়ংক্রিয় সার্ভার আপডেট)", fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "আপনার এডমিন প্যানেল থেকে সার্ভার যোগ বা ডিলিট করার সাথে সাথে আপনার গিটহাব পেজে সার্ভার লিস্ট স্বয়ংক্রিয়ভাবে আপডেট হওয়ার জন্য নিচের তথ্যগুলো সেটআপ করুন।",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = gitHubPatState,
                    onValueChange = { gitHubPatState = it },
                    label = { Text("GitHub Personal Access Token (PAT)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (isPatVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPatVisible = !isPatVisible }) {
                            Icon(
                                imageVector = if (isPatVisible) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Toggle PAT Visibility",
                                tint = Color.LightGray
                            )
                        }
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gitHubOwnerState,
                        onValueChange = { gitHubOwnerState = it },
                        label = { Text("GitHub Owner") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = gitHubRepoState,
                        onValueChange = { gitHubRepoState = it },
                        label = { Text("Repo Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = gitHubFilePathState,
                    onValueChange = { gitHubFilePathState = it },
                    label = { Text("JSON File Path (যেমন index.json বা index.html)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Sync on Changes", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("সার্ভার যোগ/ডিলিট করলে গিটহাবে নিজে নিজেই আপডেট হবে", fontSize = 11.sp, color = Color.LightGray)
                    }
                    Switch(
                        checked = isGitHubAutoSyncEnabledState,
                        onCheckedChange = { isGitHubAutoSyncEnabledState = it }
                    )
                }

                Button(
                    onClick = {
                        if (appConf != null) {
                            val updated = appConf.copy(
                                gitHubPat = gitHubPatState.trim(),
                                gitHubOwner = gitHubOwnerState.trim(),
                                gitHubRepo = gitHubRepoState.trim(),
                                gitHubFilePath = gitHubFilePathState.trim(),
                                isGitHubAutoSyncEnabled = isGitHubAutoSyncEnabledState
                            )
                            viewModel.adminUpdateGlobalSettings(updated)
                            
                            // Test & Manual Trigger push to GitHub
                            viewModel.pushServerListToGitHub { success, message ->
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Push to GitHub Pages Now")
                }
            }
        }

        // Web Host / MySQL Custom API Sync segment
        var isApiKeyVisible by remember { mutableStateOf(false) }
        Card(colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Web Host / MySQL Custom API (কাস্টম API সিঙ্ক)", fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "আপনার নিজস্ব হোস্টিং বা MySQL ডেটাবেস সার্ভারে ভিপিএন সার্ভারগুলোর তালিকা সিঙ্ক করতে নিচের সেটিংস কনফিগার করুন।",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = customApiUrlState,
                    onValueChange = { customApiUrlState = it },
                    label = { Text("Custom API URL (যেমন https://example.com/api/servers)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = customApiKeyState,
                    onValueChange = { customApiKeyState = it },
                    label = { Text("Custom API Key / Bearer Token (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (isApiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Toggle API Key Visibility",
                                tint = Color.LightGray
                            )
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HTTP Method:", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = customApiMethodState == "POST",
                                onClick = { customApiMethodState = "POST" }
                            )
                            Text("POST", color = Color.White, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = customApiMethodState == "PUT",
                                onClick = { customApiMethodState = "PUT" }
                            )
                            Text("PUT", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Sync & Fetch", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("কাস্টম API-তে স্বয়ংক্রিয়ভাবে সিঙ্ক হবে এবং অ্যাপের ইউজাররা সরাসরি এই API থেকে সার্ভার ডাউনলোড করবে।", fontSize = 11.sp, color = Color.LightGray)
                    }
                    Switch(
                        checked = isCustomApiSyncEnabledState,
                        onCheckedChange = { isCustomApiSyncEnabledState = it }
                    )
                }

                Button(
                    onClick = {
                        if (appConf != null) {
                            val updated = appConf.copy(
                                customApiUrl = customApiUrlState.trim(),
                                customApiKey = customApiKeyState.trim(),
                                customApiMethod = customApiMethodState,
                                isCustomApiSyncEnabled = isCustomApiSyncEnabledState
                            )
                            viewModel.adminUpdateGlobalSettings(updated)
                            
                            // Test & Manual Trigger push to Custom API
                            viewModel.pushServerListToCustomApi { success, message ->
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Sync Custom API Now")
                }
            }
        }
    }
}

@Composable
fun AdminResellerPinsManagement(
    viewModel: AlifVpnViewModel
) {
    val pins by viewModel.allResellerPins.collectAsState(emptyList())
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    var planName by remember { mutableStateOf("Monthly Premium") }
    var durationDays by remember { mutableStateOf("30") }
    var deviceLimit by remember { mutableStateOf("3") }
    var quantity by remember { mutableStateOf("5") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Reseller Voucher PIN Generator",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Generate New Wholesale PINs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldenAmber
                )

                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it },
                    label = { Text("Plan / Package Name", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationDays,
                        onValueChange = { durationDays = it },
                        label = { Text("Days Duration", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    OutlinedTextField(
                        value = deviceLimit,
                        onValueChange = { deviceLimit = it },
                        label = { Text("Device Limit", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity to Generate", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Button(
                    onClick = {
                        val dDays = durationDays.toIntOrNull() ?: 30
                        val dLimit = deviceLimit.toIntOrNull() ?: 3
                        val qty = quantity.toIntOrNull() ?: 5
                        viewModel.adminGenerateResellerPins(planName, dDays, dLimit, qty)
                        android.widget.Toast.makeText(context, "Successfully generated $qty wholesale voucher PINs!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald)
                ) {
                    Text("GENERATE WHOLESALE PINs", fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "Active Voucher PIN Codes (${pins.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        if (pins.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No reseller PIN codes generated yet.", color = Color.Gray)
            }
        } else {
            pins.forEach { pin ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (pin.isRedeemed) DeepCosmicSurface.copy(alpha = 0.5f) else DeepCosmicSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (pin.isRedeemed) Color.Gray.copy(alpha = 0.4f) else RadiantEmerald.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = pin.pinCode,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp,
                                    color = if (pin.isRedeemed) Color.Gray else Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(pin.pinCode))
                                        android.widget.Toast.makeText(context, "Copied: ${pin.pinCode}", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    viewModel.adminDeletePin(pin.pinCode)
                                    android.widget.Toast.makeText(context, "Deleted PIN ${pin.pinCode}", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = CrimsonRose,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Package: ${pin.planName} (${pin.durationDays} Days)",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                            Text(
                                text = "Limit: ${pin.deviceLimit} Devices",
                                fontSize = 11.sp,
                                color = GoldenAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (pin.isRedeemed) {
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Redeemed by: ${pin.redeemedByUserEmail}",
                                fontSize = 10.sp,
                                color = RadiantEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "At: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(pin.redeemedAt)),
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(RadiantEmerald, RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Unused / Active", fontSize = 10.sp, color = RadiantEmerald)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPlansManagement(
    plans: List<SubscriptionPlan>,
    viewModel: AlifVpnViewModel
) {
    val context = LocalContext.current
    
    var planId by remember { mutableStateOf("") }
    var planName by remember { mutableStateOf("") }
    var planDuration by remember { mutableStateOf("30") }
    var planPrice by remember { mutableStateOf("5.0") }
    var planDiscount by remember { mutableStateOf("0") }
    var planCoins by remember { mutableStateOf("500") }
    var planDeviceLimit by remember { mutableStateOf("3") }
    
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Subscription Plans (প্রিমিয়াম প্ল্যান সমূহ)",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )

        // Add/Edit Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isEditing) "Edit Plan: $planId" else "Add New Subscription Plan",
                    fontWeight = FontWeight.Bold,
                    color = if (isEditing) GoldenAmber else RadiantEmerald,
                    fontSize = 14.sp
                )

                // Plan ID (only editable when creating a new plan)
                OutlinedTextField(
                    value = planId,
                    onValueChange = { if (!isEditing) planId = it },
                    label = { Text("Plan ID (যেমন: weekly, monthly, 3month)") },
                    enabled = !isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Plan Name
                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it },
                    label = { Text("Plan Display Name (প্ল্যানের নাম)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Duration Days
                    OutlinedTextField(
                        value = planDuration,
                        onValueChange = { planDuration = it },
                        label = { Text("Duration (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Price USDT
                    OutlinedTextField(
                        value = planPrice,
                        onValueChange = { planPrice = it },
                        label = { Text("Price (USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Discount Percent
                    OutlinedTextField(
                        value = planDiscount,
                        onValueChange = { planDiscount = it },
                        label = { Text("Discount (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Coins Required
                    OutlinedTextField(
                        value = planCoins,
                        onValueChange = { planCoins = it },
                        label = { Text("Coins Required") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Device Connection Limit Input Field
                OutlinedTextField(
                    value = planDeviceLimit,
                    onValueChange = { planDeviceLimit = it },
                    label = { Text("Device Limit (সর্বোচ্চ কত ফোনে সংযোগ দেয়া যাবে)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (planId.isBlank() || planName.isBlank()) {
                                android.widget.Toast.makeText(context, "ID and Name cannot be empty!", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val duration = planDuration.toIntOrNull() ?: 30
                            val price = planPrice.toDoubleOrNull() ?: 5.0
                            val discount = planDiscount.toIntOrNull() ?: 0
                            val coins = planCoins.toIntOrNull() ?: 500
                            val limit = planDeviceLimit.toIntOrNull() ?: 3

                            val newPlan = SubscriptionPlan(
                                id = planId.trim().lowercase(),
                                name = planName.trim(),
                                durationDays = duration,
                                priceUsdt = price,
                                discountPercent = discount,
                                coinsRequired = coins,
                                deviceLimit = limit
                            )

                            viewModel.adminAddPlan(newPlan)
                            android.widget.Toast.makeText(context, if (isEditing) "Plan updated!" else "Plan added!", android.widget.Toast.LENGTH_SHORT).show()

                            // Clear Form
                            planId = ""
                            planName = ""
                            planDuration = "30"
                            planPrice = "5.0"
                            planDiscount = "0"
                            planCoins = "500"
                            planDeviceLimit = "3"
                            isEditing = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isEditing) GoldenAmber else RadiantEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEditing) "Update Plan" else "Add / Save Plan")
                    }

                    if (isEditing) {
                        Button(
                            onClick = {
                                planId = ""
                                planName = ""
                                planDuration = "30"
                                planPrice = "5.0"
                                planDiscount = "0"
                                planCoins = "500"
                                planDeviceLimit = "3"
                                isEditing = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        Text(
            text = "Active Subscription Plans List (${plans.size} Plans)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.LightGray
        )

        if (plans.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No plans configured. Add one above!", color = Color.Gray)
                }
            }
        } else {
            plans.forEach { plan ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface),
                    border = BorderStroke(1.dp, if (isEditing && planId == plan.id) GoldenAmber.copy(alpha = 0.5f) else Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "Plan Icon",
                                    tint = GoldenAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = plan.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "ID: ${plan.id}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        isEditing = true
                                        planId = plan.id
                                        planName = plan.name
                                        planDuration = plan.durationDays.toString()
                                        planPrice = plan.priceUsdt.toString()
                                        planDiscount = plan.discountPercent.toString()
                                        planCoins = plan.coinsRequired.toString()
                                        planDeviceLimit = plan.deviceLimit.toString()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Plan",
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.adminDeletePlan(plan.id)
                                        android.widget.Toast.makeText(context, "Plan ${plan.name} deleted", android.widget.Toast.LENGTH_SHORT).show()
                                        if (isEditing && planId == plan.id) {
                                            planId = ""
                                            planName = ""
                                            planDuration = "30"
                                            planPrice = "5.0"
                                            planDiscount = "0"
                                            planCoins = "500"
                                            isEditing = false
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Plan",
                                        tint = CrimsonRose,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Price (USD)", fontSize = 10.sp, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = "",
                                        tint = RadiantEmerald,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "$${plan.priceUsdt}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RadiantEmerald
                                    )
                                }
                            }

                            Column {
                                Text("Duration", fontSize = 10.sp, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timelapse,
                                        contentDescription = "",
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${plan.durationDays} Days",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Column {
                                Text("Discount", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    text = "${plan.discountPercent}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (plan.discountPercent > 0) GoldenAmber else Color.White
                                )
                            }

                            Column {
                                Text("Coins Required", fontSize = 10.sp, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = "",
                                        tint = GoldenAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${plan.coinsRequired}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Column {
                                Text("Devices Limit", fontSize = 10.sp, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Devices,
                                        contentDescription = "",
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${plan.deviceLimit}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAdmobManagement(
    admob: AdmobConfig?,
    viewModel: AlifVpnViewModel
) {
    val context = LocalContext.current
    var rewardCoinsText by remember { mutableStateOf(admob?.rewardCoinsPerAd?.toString() ?: "15") }

    // Google AdMob Placement IDs states
    var admobAppIdText by remember { mutableStateOf(admob?.appId ?: "ca-app-pub-1131981412237081~8138260298") }
    var admobBannerIdText by remember { mutableStateOf(admob?.bannerPlacementId ?: "ca-app-pub-1131981412237081/5644757651") }
    var admobInterstitialIdText by remember { mutableStateOf(admob?.interstitialPlacementId ?: "ca-app-pub-1131981412237081/5320525262") }
    var admobRewardedIdText by remember { mutableStateOf(admob?.rewardedPlacementId ?: "ca-app-pub-1131981412237081/3495060863") }
    var admobNativeIdText by remember { mutableStateOf(admob?.nativePlacementId ?: "ca-app-pub-1131981412237081/8083218739") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Google AdMob Configuration (অ্যাডমব কন্ট্রোল প্যানেল)",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )

        // AdMob Reward edit check
        Card(colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("AdMob Coins Configurations", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rewardCoinsText,
                    onValueChange = { rewardCoinsText = it },
                    label = { Text("Reward Coins Per Ad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val num = rewardCoinsText.toIntOrNull() ?: 15
                        if (admob != null) {
                            viewModel.adminUpdateAdConfiguration(admob.copy(rewardCoinsPerAd = num))
                            android.widget.Toast.makeText(context, "AdMob coins updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Apply Coins Reward Settings")
                }
            }
        }

        // Google AdMob Ad ID and Placements Card
        Card(colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Google AdMob Placements Configuration", fontWeight = FontWeight.Bold, color = Color.White)
                
                // Show list of added Ad IDs
                Text("Configured Ad Networks / Placements List:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = RadiantEmerald)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepCosmicDark, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val statusText = { id: String -> if (id.isNotBlank()) "Active ($id)" else "Disabled / Removed" }
                    val colorText = { id: String -> if (id.isNotBlank()) RadiantEmerald else Color.Gray }
                    
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("AdMob App ID:", fontSize = 11.sp, color = Color.White)
                        Text(statusText(admobAppIdText), fontSize = 11.sp, color = colorText(admobAppIdText), fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Banner Ad ID:", fontSize = 11.sp, color = Color.White)
                        Text(statusText(admobBannerIdText), fontSize = 11.sp, color = colorText(admobBannerIdText), fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Interstitial ID:", fontSize = 11.sp, color = Color.White)
                        Text(statusText(admobInterstitialIdText), fontSize = 11.sp, color = colorText(admobInterstitialIdText), fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Rewarded ID:", fontSize = 11.sp, color = Color.White)
                        Text(statusText(admobRewardedIdText), fontSize = 11.sp, color = colorText(admobRewardedIdText), fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Native Ad ID:", fontSize = 11.sp, color = Color.White)
                        Text(statusText(admobNativeIdText), fontSize = 11.sp, color = colorText(admobNativeIdText), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // AdMob Input Fields
                OutlinedTextField(
                    value = admobAppIdText,
                    onValueChange = { admobAppIdText = it },
                    label = { Text("Google AdMob App ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = admobBannerIdText,
                    onValueChange = { admobBannerIdText = it },
                    label = { Text("Banner Placement ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = admobInterstitialIdText,
                    onValueChange = { admobInterstitialIdText = it },
                    label = { Text("Interstitial Placement ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = admobRewardedIdText,
                    onValueChange = { admobRewardedIdText = it },
                    label = { Text("Rewarded Placement ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = admobNativeIdText,
                    onValueChange = { admobNativeIdText = it },
                    label = { Text("Native Placement ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (admob != null) {
                                val updated = admob.copy(
                                    appId = admobAppIdText.trim(),
                                    bannerPlacementId = admobBannerIdText.trim(),
                                    interstitialPlacementId = admobInterstitialIdText.trim(),
                                    rewardedPlacementId = admobRewardedIdText.trim(),
                                    nativePlacementId = admobNativeIdText.trim()
                                )
                                viewModel.adminUpdateAdConfiguration(updated)
                                android.widget.Toast.makeText(context, "Ad IDs applied & updated!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save / Add Ads")
                    }

                    Button(
                        onClick = {
                            admobAppIdText = ""
                            admobBannerIdText = ""
                            admobInterstitialIdText = ""
                            admobRewardedIdText = ""
                            admobNativeIdText = ""
                            if (admob != null) {
                                val updated = admob.copy(
                                    appId = "",
                                    bannerPlacementId = "",
                                    interstitialPlacementId = "",
                                    rewardedPlacementId = "",
                                    nativePlacementId = ""
                                )
                                viewModel.adminUpdateAdConfiguration(updated)
                                android.widget.Toast.makeText(context, "All Ads Removed / Reset!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRose),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Remove / Reset All")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPlayBillingManagement(
    appConf: AppConfig?,
    viewModel: AlifVpnViewModel
) {
    val context = LocalContext.current

    var isPlayBillingEnabledState by remember { mutableStateOf(appConf?.isPlayBillingEnabled ?: false) }
    var isManualPaymentEnabledState by remember { mutableStateOf(appConf?.isManualPaymentEnabled ?: true) }
    var isCoinRedemptionEnabledState by remember { mutableStateOf(appConf?.isCoinRedemptionEnabled ?: true) }
    var playLicenseKeyState by remember { mutableStateOf(appConf?.playLicenseKey ?: "") }
    var playWeeklyProductIdState by remember { mutableStateOf(appConf?.playWeeklyProductId ?: "alif_vpn_weekly") }
    var playMonthlyProductIdState by remember { mutableStateOf(appConf?.playMonthlyProductId ?: "alif_vpn_monthly") }
    var playYearlyProductIdState by remember { mutableStateOf(appConf?.playYearlyProductId ?: "alif_vpn_yearly") }
    var playLifetimeProductIdState by remember { mutableStateOf(appConf?.playLifetimeProductId ?: "alif_vpn_lifetime") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Google Play Store Billing (গুগল প্লে পেমেন্ট কনফিগারেশন)",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )

        // Informational message card to answer user's concern about giving play console credentials
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface),
            border = BorderStroke(1.dp, RadiantEmerald.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Info",
                        tint = RadiantEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "পেমেন্ট নেয়ার জন্য কোনো প্লে কনসোল পাসওয়ার্ড দিতে হবে না!",
                        fontWeight = FontWeight.Bold,
                        color = RadiantEmerald,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "গুগল প্লে স্টোর পেমেন্ট চালু করার জন্য আপনার Google Play Console একাউন্টের পাসওয়ার্ড বা ইমেইল কাউকে দিতে হবে না। শুধুমাত্র নিচের কনফিগারেশনগুলো করুন:\n\n" +
                            "১. আপনার Play Console থেকে 'License Key' কপি করে নিচে দিন।\n" +
                            "২. আপনার ইন-অ্যাপ পারচেস (In-App Purchases / Subscriptions) প্রোডাক্ট আইডিগুলো তৈরি করে নিচের ঘরে বসিয়ে দিন।\n" +
                            "৩. এরপর Google Play Billing সক্রিয় করতে উপরের সুইচটি অন করে দিন।",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    lineHeight = 16.sp
                )
            }
        }

        // Google Play Billing Configuration Panel
        Card(colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Billing System Switch & Base64 Key",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Google Play In-App Billing", fontSize = 12.sp, color = Color.White)
                        Text(
                            text = "এটি চালু করলে ইউজারদের গুগল প্লে পেমেন্ট উইন্ডো দেখাবে।",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isPlayBillingEnabledState,
                        onCheckedChange = { isPlayBillingEnabledState = it }
                    )
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Coin Redemption System", fontSize = 12.sp, color = Color.White)
                        Text(
                            text = "এটি চালু করলে ইউজাররা কয়েন দিয়ে সাবস্ক্রাইব করতে পারবে। বন্ধ থাকলে কয়েন দিয়ে সাবস্ক্রাইব করার অপশন স্ক্রিন থেকে লুকিয়ে যাবে।",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isCoinRedemptionEnabledState,
                        onCheckedChange = { isCoinRedemptionEnabledState = it }
                    )
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Manual Payments (bKash/Nagad/USDT/TRX)", fontSize = 12.sp, color = Color.White)
                        Text(
                            text = "এটি চালু করলে ইউজাররা ম্যানুয়াল ট্রানজেকশন প্রুফ (বিকাশ, নগদ, ইউএসডিটি, টিআরএক্স) জমা দিয়ে পেমেন্ট করতে পারবে।",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isManualPaymentEnabledState,
                        onCheckedChange = { isManualPaymentEnabledState = it }
                    )
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                OutlinedTextField(
                    value = playLicenseKeyState,
                    onValueChange = { playLicenseKeyState = it },
                    label = { Text("Google Play License Public Key (Base64 Key)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQ...") }
                )
            }
        }

        // Product IDs
        Card(colors = CardDefaults.cardColors(containerColor = DeepCosmicSurface)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Subscription Product IDs (গুগল প্লে কনসোলের সাথে মিলিয়ে দিন)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = playWeeklyProductIdState,
                    onValueChange = { playWeeklyProductIdState = it },
                    label = { Text("Weekly Subscription Product ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = playMonthlyProductIdState,
                    onValueChange = { playMonthlyProductIdState = it },
                    label = { Text("Monthly Subscription Product ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = playYearlyProductIdState,
                    onValueChange = { playYearlyProductIdState = it },
                    label = { Text("Yearly Subscription Product ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = playLifetimeProductIdState,
                    onValueChange = { playLifetimeProductIdState = it },
                    label = { Text("Lifetime / One-time Purchase Product ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Apply Button
        Button(
            onClick = {
                if (appConf != null) {
                    val updated = appConf.copy(
                        isPlayBillingEnabled = isPlayBillingEnabledState,
                        isManualPaymentEnabled = isManualPaymentEnabledState,
                        isCoinRedemptionEnabled = isCoinRedemptionEnabledState,
                        playLicenseKey = playLicenseKeyState.trim(),
                        playWeeklyProductId = playWeeklyProductIdState.trim(),
                        playMonthlyProductId = playMonthlyProductIdState.trim(),
                        playYearlyProductId = playYearlyProductIdState.trim(),
                        playLifetimeProductId = playLifetimeProductIdState.trim()
                    )
                    viewModel.adminUpdateGlobalSettings(updated)
                    android.widget.Toast.makeText(context, "Google Play Billing configurations updated!", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Play Billing Configuration", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SubscriptionCountdownClock(
    expiryTimestamp: Long,
    getT: (String, String) -> String,
    modifier: Modifier = Modifier
) {
    var days by remember(expiryTimestamp) { mutableStateOf(0L) }
    var hours by remember(expiryTimestamp) { mutableStateOf(0L) }
    var minutes by remember(expiryTimestamp) { mutableStateOf(0L) }
    var seconds by remember(expiryTimestamp) { mutableStateOf(0L) }
    var isExpired by remember(expiryTimestamp) { mutableStateOf(false) }

    if (expiryTimestamp == Long.MAX_VALUE) {
        Box(
            modifier = modifier
                .padding(vertical = 8.dp)
                .background(Color(0xFF0F0F0F), shape = RoundedCornerShape(10.dp))
                .border(width = 2.dp, color = GoldenAmber, shape = RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "L I F E T I M E",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldenAmber
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = getT("Unlimited Access Activated", "আজীবন মেয়াদের প্যাকেজ সচল"),
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    LaunchedEffect(expiryTimestamp) {
        while (true) {
            val now = System.currentTimeMillis()
            val diff = expiryTimestamp - now
            if (diff <= 0) {
                isExpired = true
                break
            } else {
                days = diff / (24 * 60 * 60 * 1000)
                hours = (diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
                minutes = (diff % (60 * 60 * 1000)) / (60 * 1000)
                seconds = (diff % (60 * 1000)) / 1000
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    if (isExpired) {
        Box(
            modifier = modifier
                .padding(vertical = 8.dp)
                .background(Color(0xFF2C0F0F), shape = RoundedCornerShape(10.dp))
                .border(width = 2.dp, color = Color.Red, shape = RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getT("Subscription Expired", "প্যাকেজের মেয়াদ শেষ"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
        }
    } else {
        Column(
            modifier = modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getT("REMAINING TIME", "মেয়াদ বাকি"),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.LightGray,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(Color(0xFF0F0F0F), shape = RoundedCornerShape(12.dp))
                    .border(width = 2.5.dp, color = GoldenAmber, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                ClockSegment(value = days, label = getT("Days", "দিন"))
                ClockDivider()
                ClockSegment(value = hours, label = getT("Hours", "ঘণ্টা"))
                ClockDivider()
                ClockSegment(value = minutes, label = getT("Mins", "মিনিট"))
                ClockDivider()
                ClockSegment(value = seconds, label = getT("Secs", "সেকেন্ড"))
            }
        }
    }
}

@Composable
fun ClockSegment(value: Long, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Color(0xFF1F1F1F), shape = RoundedCornerShape(6.dp))
                .border(width = 1.dp, color = Color(0xFF333333), shape = RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = String.format("%02d", value),
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GoldenAmber
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
    }
}

@Composable
fun ClockDivider() {
    Text(
        text = ":",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = GoldenAmber,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}



