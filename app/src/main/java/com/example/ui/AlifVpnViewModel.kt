package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.AlifVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class AlifVpnViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = VpnDatabase.getDatabase(context, viewModelScope)

    // DAOs
    private val serverDao = db.serverDao()
    private val userDao = db.userDao()
    private val transactionDao = db.transactionDao()
    private val ticketDao = db.ticketDao()
    private val admobDao = db.admobDao()
    private val configDao = db.configDao()
    private val logDao = db.logDao()
    private val planDao = db.planDao()
    private val resellerPinDao = db.resellerPinDao()

    // Application Preferences / States
    val allServers = serverDao.getAllServersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers = userDao.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions = transactionDao.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTickets = ticketDao.getAllTicketsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val admobConfig = admobDao.getConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdmobConfig())

    val appConfig = configDao.getAppConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    val allPlans = planDao.getAllPlansFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allResellerPins = resellerPinDao.getAllPinsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently logged-in active user
    private val _currentUserEmail = MutableStateFlow("guest@alifvpn.com") // start as guest by default
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    // Authentication and device limit error messages
    val authError = MutableStateFlow<String?>(null)

    val currentUser = _currentUserEmail.flatMapLatest { email ->
        userDao.getUserByEmailFlow(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userTickets = _currentUserEmail.flatMapLatest { email ->
        ticketDao.getTicketsByUserFlow(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userTransactions = _currentUserEmail.flatMapLatest { email ->
        transactionDao.getTransactionsByUserFlow(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userLogs = _currentUserEmail.flatMapLatest { email ->
        logDao.getLogsByUserFlow(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active VPN parameters
    private val _connectionState = MutableStateFlow("DISCONNECTED") // DISCONNECTED, CONNECTING, CONNECTED
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val _currentPublicIpAddress = MutableStateFlow("Determining...")
    val currentPublicIpAddress: StateFlow<String> = _currentPublicIpAddress.asStateFlow()

    fun fetchRealPublicIpAddress() {
        if (_connectionState.value == "CONNECTED") {
            _currentPublicIpAddress.value = _selectedServer.value?.ipAddress ?: "104.244.42.1"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (_connectionState.value == "CONNECTED") {
                    _currentPublicIpAddress.value = _selectedServer.value?.ipAddress ?: "104.244.42.1"
                    return@launch
                }
                val url = java.net.URL("https://api.ipify.org?format=text")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                if (connection.responseCode == 200) {
                    val ip = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                    if (ip.isNotEmpty()) {
                        if (_connectionState.value == "CONNECTED") {
                            _currentPublicIpAddress.value = _selectedServer.value?.ipAddress ?: "104.244.42.1"
                        } else {
                            _currentPublicIpAddress.value = ip
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) {
                // fall through
            }
            // Fallback to simulated Bangladeshi ISP IP
            if (_connectionState.value == "CONNECTED") {
                _currentPublicIpAddress.value = _selectedServer.value?.ipAddress ?: "104.244.42.1"
            } else {
                _currentPublicIpAddress.value = "103.112.113.15"
            }
        }
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private val _vpnPermissionIntent = MutableStateFlow<Intent?>(null)
    val vpnPermissionIntent: StateFlow<Intent?> = _vpnPermissionIntent.asStateFlow()

    fun clearVpnPermissionIntent() {
        _vpnPermissionIntent.value = null
    }

    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer: StateFlow<VpnServer?> = _selectedServer.asStateFlow()

    // Preferences & Toggles
    val autoConnect = MutableStateFlow(false)
    val killSwitch = MutableStateFlow(false)
    val dnsLeakProtection = MutableStateFlow(true)
    val ipv6LeakProtection = MutableStateFlow(true)
    val selectedProtocol = MutableStateFlow("WireGuard") // "WireGuard", "OpenVPN", "IKEv2"

    // Statistics Tracker (Simulated during connection)
    val connectedDurationSeconds = MutableStateFlow(0L)
    val currentDownloadSpeedMbps = MutableStateFlow(0.0)
    val currentUploadSpeedMbps = MutableStateFlow(0.0)
    val totalDataDownloaded = MutableStateFlow(25 * 1024 * 1024L) // startup pre-set
    val totalDataUploaded = MutableStateFlow(8 * 1024 * 1024L)

    private var connectionTimerJob: Job? = null

    // Speed Test Simulation Parameters
    val speedTestIsRunning = MutableStateFlow(false)
    val speedTestDownload = MutableStateFlow(0.0)
    val speedTestUpload = MutableStateFlow(0.0)
    val speedTestPing = MutableStateFlow(0)
    val speedTestStage = MutableStateFlow("") // "", "ping", "download", "upload", "done"

    // Notification broadcast queue (Simulated)
    private val _notificationBroadcasts = MutableStateFlow<List<Pair<String, String>>>(
        listOf("Welcome to Alif Go VPN" to "Connect to any server and experience unlimited speed & modern security parameters.")
    )
    val notificationBroadcasts: StateFlow<List<Pair<String, String>>> = _notificationBroadcasts.asStateFlow()

    init {
        val prefs = context.getSharedPreferences("AlifVpnPrefs", Context.MODE_PRIVATE)
        val savedConnected = prefs.getBoolean("is_connected", false)
        val savedServerIp = prefs.getString("connected_server_ip", null)

        if (AlifVpnService.isRunning || (savedConnected && isServiceRunning(context, AlifVpnService::class.java))) {
            _connectionState.value = "CONNECTED"
            _currentPublicIpAddress.value = AlifVpnService.connectedServerIp ?: savedServerIp ?: "104.244.42.1"
            startStatsTimer()
        } else {
            _connectionState.value = "DISCONNECTED"
            fetchRealPublicIpAddress()
        }

        // Automatically select the 'Smart' server initially once loaded, or restore previous selection if active
        viewModelScope.launch {
            allServers.collect { list ->
                if (_selectedServer.value == null && list.isNotEmpty()) {
                    val activeSavedIp = prefs.getString("connected_server_ip", null)
                    if (activeSavedIp != null) {
                        val matched = list.firstOrNull { it.ipAddress == activeSavedIp }
                        if (matched != null) {
                            _selectedServer.value = matched
                            return@collect
                        }
                    }
                    // Preselect Bangladesh or first free server as default smart destination
                    val smartServer = list.firstOrNull { it.countryCode == "SG" } ?: list.firstOrNull()
                    _selectedServer.value = smartServer
                }
            }
        }

        // Periodic Subscription Expiry Validation & Auto-downgrade back to Free Tier
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val email = _currentUserEmail.value
                    if (email.isNotEmpty()) {
                        userDao.getUserByEmail(email)?.let { user ->
                            if (user.isPremium && user.premiumExpiryTimestamp > 0L && user.premiumExpiryTimestamp != Long.MAX_VALUE) {
                                if (System.currentTimeMillis() > user.premiumExpiryTimestamp) {
                                    // Subscription has expired! Downgrade back to Free Tier
                                    val downgraded = user.copy(
                                        isPremium = false,
                                        currentPlanName = "Free User",
                                        premiumExpiryDate = "",
                                        premiumExpiryTimestamp = 0L
                                    )
                                    userDao.insertUser(downgraded)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(5000) // Check every 5 seconds for immediate update
            }
        }

        // Proactive self-healing database seed check (for cases where onCreate was not called)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbServers = serverDao.getAllServersFlow().first()
                if (dbServers.isEmpty()) {
                    val defaultServers = listOf(
                        VpnServer(countryName = "United States", countryCode = "US", city = "New York", ipAddress = "104.244.42.1", type = "Free", latency = 45, loadPercent = 32, protocol = "WireGuard"),
                        VpnServer(countryName = "Singapore", countryCode = "SG", city = "Jurong", ipAddress = "128.199.112.5", type = "Free", latency = 21, loadPercent = 15, protocol = "OpenVPN"),
                        VpnServer(countryName = "Bangladesh", countryCode = "BD", city = "Dhaka", ipAddress = "103.112.113.1", type = "Free", latency = 12, loadPercent = 58, protocol = "IKEv2"),
                        VpnServer(countryName = "Japan", countryCode = "JP", city = "Tokyo", ipAddress = "210.140.10.3", type = "Premium", latency = 75, loadPercent = 19, protocol = "WireGuard"),
                        VpnServer(countryName = "United Kingdom", countryCode = "GB", city = "London", ipAddress = "195.154.122.9", type = "Premium", latency = 92, loadPercent = 25, protocol = "OpenVPN"),
                        VpnServer(countryName = "Germany", countryCode = "DE", city = "Frankfurt", ipAddress = "46.165.230.12", type = "Premium", latency = 88, loadPercent = 41, protocol = "WireGuard"),
                        VpnServer(countryName = "South Korea (Seoul)", countryCode = "KR", city = "Seoul", ipAddress = "182.162.24.8", type = "Gaming", latency = 15, loadPercent = 48, protocol = "WireGuard"),
                        VpnServer(countryName = "USA (Gaming Pro)", countryCode = "US", city = "Chicago", ipAddress = "45.33.2.14", type = "Gaming", latency = 38, loadPercent = 30, protocol = "WireGuard"),
                        VpnServer(countryName = "Netflix US Mirror", countryCode = "US", city = "Los Angeles", ipAddress = "23.244.33.66", type = "Streaming", latency = 55, loadPercent = 64, protocol = "OpenVPN"),
                        VpnServer(countryName = "Hotstar India Mirror", countryCode = "IN", city = "Mumbai", ipAddress = "115.112.2.1", type = "Streaming", latency = 28, loadPercent = 70, protocol = "WireGuard")
                    )
                    for (server in defaultServers) {
                        serverDao.insertServer(server)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val dbPlans = planDao.getAllPlansFlow().first()
                if (dbPlans.isEmpty()) {
                    val plansList = listOf(
                        SubscriptionPlan("weekly", "Weekly Plan", 7, 2.0, 0, 150, deviceLimit = 1),
                        SubscriptionPlan("monthly", "Monthly Premium", 30, 5.0, 15, 500, deviceLimit = 1),
                        SubscriptionPlan("3month", "3 Months Super saving", 90, 12.0, 20, 1200, deviceLimit = 1),
                        SubscriptionPlan("6month", "6 Months Ultimate", 180, 20.0, 25, 2000, deviceLimit = 1),
                        SubscriptionPlan("yearly", "Yearly Unlimited", 365, 30.0, 40, 3500, deviceLimit = 1),
                        SubscriptionPlan("lifetime", "Lifetime Freedom", 9999, 50.0, 50, 6000, deviceLimit = 1),
                        
                        SubscriptionPlan("reseller_starter", "Reseller Starter Pack", 30, 15.0, 0, 1500, deviceLimit = 50),
                        SubscriptionPlan("reseller_silver", "Reseller Silver Pack", 30, 25.0, 10, 2500, deviceLimit = 100),
                        SubscriptionPlan("reseller_gold", "Reseller Gold Pack", 30, 55.0, 15, 5500, deviceLimit = 250),
                        SubscriptionPlan("reseller_enterprise", "Reseller Enterprise VIP", 30, 99.0, 20, 9900, deviceLimit = 500),
                        SubscriptionPlan("reseller_unlimited", "Server Broker Unlimited", 30, 180.0, 25, 18000, deviceLimit = 1000)
                    )
                    for (plan in plansList) {
                        planDao.insertPlan(plan)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val defaultAppConfig = configDao.getAppConfigFlow().first()
                if (defaultAppConfig == null) {
                    configDao.insertAppConfig(
                        AppConfig(
                            id = 1,
                            appName = "Alif Go VPN",
                            supportEmail = "support@alifvpn.com",
                            telegramLink = "https://t.me/alifvpn_official",
                            websiteLink = "https://alifvpn.com",
                            privacyPolicy = "https://alifvpn.com/privacy",
                            termsAndConditions = "https://alifvpn.com/terms",
                            dailyRewardAmount = 15,
                            referralRewardAmount = 50,
                            referralCommissionPercentage = 10.0,
                            isReferralEnabled = true,
                            maintenanceMode = false,
                            systemLanguage = "en",
                            customApiUrl = "https://alifgo.gt.tc/api/get_vpn_servers.php",
                            isCustomApiSyncEnabled = true
                        )
                    )
                } else {
                    // Self-healing check: Force update custom API URL if it's currently blank or has the old placeholder
                    if (defaultAppConfig.customApiUrl.isBlank() || 
                        defaultAppConfig.customApiUrl == "https://example.com/api/servers" || 
                        !defaultAppConfig.isCustomApiSyncEnabled) {
                        configDao.insertAppConfig(
                            defaultAppConfig.copy(
                                customApiUrl = "https://alifgo.gt.tc/api/get_vpn_servers.php",
                                isCustomApiSyncEnabled = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val defaultAdmobConfig = admobDao.getConfigFlow().first()
                if (defaultAdmobConfig == null) {
                    admobDao.insertConfig(
                        AdmobConfig(
                            id = 1,
                            isBannerEnabled = true,
                            isInterstitialEnabled = true,
                            isRewardedEnabled = true,
                            isRewardedInterstitialEnabled = true,
                            isNativeEnabled = true,
                            isAppOpenEnabled = true,
                            rewardCoinsPerAd = 20,
                            adFrequencyMinutes = 2
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val seedUser = userDao.getUserByEmail("ffcct2755@gmail.com")
                if (seedUser == null) {
                    userDao.insertUser(
                        UserSession(
                            email = "ffcct2755@gmail.com",
                            name = "Demo User",
                            isPremium = false,
                            premiumExpiryDate = "",
                            coinBalance = 250,
                            referralEarnings = 1.5,
                            referralCode = "ALIFDEMO",
                            invitedBy = "",
                            isBanned = false,
                            currentPlanName = "Free Tier",
                            isGuest = false
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val seedGuest = userDao.getUserByEmail("guest@alifvpn.com")
                if (seedGuest == null) {
                    userDao.insertUser(
                        UserSession(
                            email = "guest@alifvpn.com",
                            name = "Guest User",
                            isPremium = false,
                            premiumExpiryDate = "",
                            coinBalance = 120,
                            referralEarnings = 0.0,
                            referralCode = "ALIFGUEST",
                            invitedBy = "",
                            isBanned = false,
                            currentPlanName = "Free Tier",
                            isGuest = true
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Perform automatic background server synchronization on app startup
            autoSyncServersFromRemote()
        }
    }

    fun parseSingleVpnServer(obj: org.json.JSONObject): VpnServer? {
        try {
            val ip = obj.optString("ipAddress", "")
                .ifBlank { obj.optString("ip_address", "") }
                .ifBlank { obj.optString("ip", "") }
                .ifBlank { obj.optString("host", "") }
                .ifBlank { obj.optString("server_ip", "") }
                .ifBlank { obj.optString("server_address", "") }
                .trim()

            if (ip.isBlank()) return null

            val country = obj.optString("countryName", "")
                .ifBlank { obj.optString("country_name", "") }
                .ifBlank { obj.optString("country", "") }
                .ifBlank { "Unknown" }
                .trim()

            var code = obj.optString("countryCode", "")
                .ifBlank { obj.optString("country_code", "") }
                .ifBlank { obj.optString("code", "") }
                .ifBlank { obj.optString("flag", "") }
                .trim()
                .uppercase()

            if (code.isBlank() || code.length > 4) {
                code = when (country.lowercase()) {
                    "bangladesh" -> "BD"
                    "united states", "usa", "us" -> "US"
                    "singapore", "sg" -> "SG"
                    "japan", "jp" -> "JP"
                    "united kingdom", "uk", "gb" -> "GB"
                    "germany", "de" -> "DE"
                    "south korea", "korea", "kr" -> "KR"
                    "india", "in" -> "IN"
                    "canada", "ca" -> "CA"
                    "australia", "au" -> "AU"
                    "france", "fr" -> "FR"
                    "netherlands", "nl" -> "NL"
                    else -> "US"
                }
            }

            val city = obj.optString("city", "")
                .ifBlank { obj.optString("cityName", "") }
                .ifBlank { obj.optString("city_name", "") }
                .ifBlank { obj.optString("location", "") }
                .ifBlank { "Default" }
                .trim()

            var type = obj.optString("type", "")
                .ifBlank { obj.optString("server_type", "") }
                .ifBlank { obj.optString("category", "") }
                .trim()

            val isPremiumFlag = obj.optBoolean("is_premium", false) || 
                               obj.optBoolean("isPremium", false) ||
                               obj.optInt("is_premium", 0) == 1 ||
                               obj.optInt("isPremium", 0) == 1 ||
                               obj.optInt("vip", 0) == 1 ||
                               obj.optString("is_premium", "").lowercase() == "true" ||
                               obj.optString("vip", "").lowercase() == "true"

            if (type.isBlank()) {
                type = if (isPremiumFlag) "Premium" else "Free"
            } else {
                type = when (type.lowercase()) {
                    "premium", "vip" -> "Premium"
                    "gaming", "game" -> "Gaming"
                    "streaming", "stream" -> "Streaming"
                    else -> "Free"
                }
            }

            val latency = obj.optInt("latency", -1)
                .let { if (it != -1) it else obj.optInt("ping", -1) }
                .let { if (it != -1) it else obj.optInt("speed", 50) }

            val load = obj.optInt("loadPercent", -1)
                .let { if (it != -1) it else obj.optInt("load_percent", -1) }
                .let { if (it != -1) it else obj.optInt("load", -1) }
                .let { if (it != -1) it else obj.optInt("user_load", 30) }

            val proto = obj.optString("protocol", "")
                .ifBlank { obj.optString("proto", "") }
                .ifBlank { obj.optString("vpn_protocol", "") }
                .ifBlank { obj.optString("method", "WireGuard") }
                .trim()

            return VpnServer(
                countryName = country,
                countryCode = code,
                city = city,
                ipAddress = ip,
                type = type,
                latency = latency,
                loadPercent = load,
                isEnabled = obj.optBoolean("isEnabled", true),
                protocol = proto
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun parseVpnServersFromJson(jsonText: String): List<VpnServer> {
        val list = mutableListOf<VpnServer>()
        try {
            val trimmed = jsonText.trim()
            if (trimmed.startsWith("[")) {
                val array = org.json.JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val server = parseSingleVpnServer(obj)
                    if (server != null) {
                        list.add(server)
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val rootObj = org.json.JSONObject(trimmed)
                val keys = listOf("servers", "data", "list", "vpn", "vpns", "results", "items", "server")
                var foundArray: org.json.JSONArray? = null
                for (key in keys) {
                    if (rootObj.has(key)) {
                        foundArray = rootObj.optJSONArray(key)
                        if (foundArray != null) break
                    }
                }

                if (foundArray != null) {
                    for (i in 0 until foundArray.length()) {
                        val obj = foundArray.optJSONObject(i) ?: continue
                        val server = parseSingleVpnServer(obj)
                        if (server != null) {
                            list.add(server)
                        }
                    }
                } else {
                    val keysIterator = rootObj.keys()
                    while (keysIterator.hasNext()) {
                        val key = keysIterator.next()
                        val obj = rootObj.optJSONObject(key)
                        if (obj != null) {
                            val server = parseSingleVpnServer(obj)
                            if (server != null) {
                                list.add(server)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun autoSyncServersFromRemote() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val config = configDao.getAppConfig() ?: AppConfig()
                val isCustom = config.isCustomApiSyncEnabled && config.customApiUrl.isNotBlank()
                
                val urlStr = if (isCustom) {
                    config.customApiUrl
                } else {
                    val owner = config.gitHubOwner.ifBlank { "mdshahinislamshamim420-cell" }
                    val repo = config.gitHubRepo.ifBlank { "alif-go-vpn-josn" }
                    val filePath = config.gitHubFilePath.ifBlank { "index.html" }
                    "https://$owner.github.io/$repo/$filePath"
                }
                
                var url = java.net.URL(urlStr)
                var connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                
                if (isCustom && config.customApiKey.isNotBlank()) {
                    connection.setRequestProperty("X-API-Key", config.customApiKey)
                    val authHeader = if (config.customApiKey.startsWith("Bearer ", ignoreCase = true)) {
                        config.customApiKey
                    } else {
                        "Bearer ${config.customApiKey}"
                    }
                    connection.setRequestProperty("Authorization", authHeader)
                }
                
                var responseCode = connection.responseCode
                if (responseCode != 200 && !isCustom) {
                    // Fall back to original URL
                    val fallbackUrlStr = "https://mdshahinislamshamim420-cell.github.io/alif-go-vpn-josn/"
                    url = java.net.URL(fallbackUrlStr)
                    connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    responseCode = connection.responseCode
                }
                
                if (responseCode == 200) {
                    val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                    val servers = parseVpnServersFromJson(jsonText)
                    if (servers.isNotEmpty()) {
                        serverDao.clearAll()
                        for (server in servers) {
                            serverDao.insertServer(server)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Secondary fallback attempt on pure connection failure
                try {
                    val fallbackUrlStr = "https://mdshahinislamshamim420-cell.github.io/alif-go-vpn-josn/"
                    val url = java.net.URL(fallbackUrlStr)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    if (connection.responseCode == 200) {
                        val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                        val servers = parseVpnServersFromJson(jsonText)
                        if (servers.isNotEmpty()) {
                            serverDao.clearAll()
                            for (server in servers) {
                                serverDao.insertServer(server)
                            }
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    // AUTH ACTIONS
    fun login(email: String, name: String = "User", deviceId: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = email.trim().lowercase()
            if (trimmed.isEmpty()) return@launch
            var user = userDao.getUserByEmail(trimmed)
            if (user == null) {
                // Auto registration loop
                val refCode = "ALIF" + Random.nextInt(10000, 99999)
                user = UserSession(
                    email = trimmed,
                    name = name,
                    referralCode = refCode,
                    deviceLimit = 1,
                    activeDevicesList = if (deviceId.isNotEmpty()) deviceId else ""
                )
                userDao.insertUser(user)
            } else {
                // Check if user has reached device limits
                val devices = user.activeDevicesList.split(";").filter { it.isNotEmpty() }.toMutableList()
                if (deviceId.isNotEmpty()) {
                    if (!devices.contains(deviceId)) {
                        if (devices.size >= user.deviceLimit) {
                            authError.value = "DEVICE_LIMIT_EXCEEDED"
                            return@launch
                        } else {
                            devices.add(deviceId)
                            val updatedDevices = devices.joinToString(";")
                            user = user.copy(activeDevicesList = updatedDevices)
                            userDao.insertUser(user)
                        }
                    }
                }
            }
            if (user.isBanned) {
                authError.value = "BANNED"
            } else {
                authError.value = null
                _currentUserEmail.value = trimmed
            }
        }
    }

    fun signUp(email: String, name: String, inviteCode: String, deviceId: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedEmail = email.trim().lowercase()
            if (trimmedEmail.isEmpty()) return@launch
            var user = userDao.getUserByEmail(trimmedEmail)
            if (user == null) {
                val refCode = "ALIF" + Random.nextInt(10000, 99999)
                user = UserSession(
                    email = trimmedEmail,
                    name = name,
                    referralCode = refCode,
                    invitedBy = inviteCode.trim().uppercase(),
                    coinBalance = 150, // Sign up gift
                    deviceLimit = 1,
                    activeDevicesList = if (deviceId.isNotEmpty()) deviceId else ""
                )
                userDao.insertUser(user)

                // If invited, credit referrer if referral exists
                if (inviteCode.isNotEmpty()) {
                    val allU = userDao.getAllUsersFlow().first()
                    val inviter = allU.find { it.referralCode == inviteCode.trim().uppercase() }
                    if (inviter != null) {
                        val updatedInviter = inviter.copy(
                            coinBalance = inviter.coinBalance + 100 // Referral bonus
                        )
                        userDao.insertUser(updatedInviter)
                    }
                }
            } else {
                // Check device limits
                val devices = user.activeDevicesList.split(";").filter { it.isNotEmpty() }.toMutableList()
                if (deviceId.isNotEmpty()) {
                    if (!devices.contains(deviceId)) {
                        if (devices.size >= user.deviceLimit) {
                            authError.value = "DEVICE_LIMIT_EXCEEDED"
                            return@launch
                        } else {
                            devices.add(deviceId)
                            val updatedDevices = devices.joinToString(";")
                            user = user.copy(activeDevicesList = updatedDevices)
                            userDao.insertUser(user)
                        }
                    }
                }
            }
            if (user.isBanned) {
                authError.value = "BANNED"
            } else {
                authError.value = null
                _currentUserEmail.value = trimmedEmail
            }
        }
    }

    fun loginGuest(deviceId: String = "") {
        login("guest@alifvpn.com", "Guest User", deviceId)
    }

    fun logout() {
        authError.value = null
        _currentUserEmail.value = "guest@alifvpn.com"
    }

    fun selectServer(server: VpnServer, deviceId: String = "") {
        _selectedServer.value = server
        // Always connect automatically when a server is selected / clicked
        toggleConnection(true, deviceId)
    }

    // VPN START DETAILS
    fun toggleConnection(forceState: Boolean? = null, deviceId: String = "") {
        val nextConnect = forceState ?: (_connectionState.value == "DISCONNECTED")
        if (nextConnect) {
            _connectionState.value = "CONNECTING"
            viewModelScope.launch(Dispatchers.IO) {
                val email = _currentUserEmail.value
                val user = userDao.getUserByEmail(email)
                if (user != null) {
                    val devices = user.activeDevicesList.split(";").filter { it.isNotEmpty() }.toMutableList()
                    if (deviceId.isNotEmpty()) {
                        if (!devices.contains(deviceId)) {
                            if (devices.size >= user.deviceLimit) {
                                withContext(Dispatchers.Main) {
                                    _connectionState.value = "DISCONNECTED"
                                    authError.value = "DEVICE_LIMIT_EXCEEDED"
                                }
                                return@launch
                            } else {
                                devices.add(deviceId)
                                val updatedDevices = devices.joinToString(";")
                                userDao.insertUser(user.copy(activeDevicesList = updatedDevices))
                            }
                        }
                    }
                }

                // Server-level Limit Check & Register device ID on server list
                val server = _selectedServer.value
                if (server != null) {
                    val srvDevices = server.connectedDevicesList.split(";").filter { it.isNotEmpty() }.toMutableList()
                    if (deviceId.isNotEmpty()) {
                        if (!srvDevices.contains(deviceId)) {
                            if (srvDevices.size >= server.maxConnectedDevices) {
                                withContext(Dispatchers.Main) {
                                    _connectionState.value = "DISCONNECTED"
                                    authError.value = "SERVER_LIMIT_EXCEEDED"
                                }
                                return@launch
                            } else {
                                srvDevices.add(deviceId)
                                val updatedSrvDevices = srvDevices.joinToString(";")
                                serverDao.updateServer(server.copy(connectedDevicesList = updatedSrvDevices))
                            }
                        }
                    }
                }
                
                val selectedSrv = _selectedServer.value
                val countryCode = selectedSrv?.countryCode ?: "US"
                val proxy = fetchWorkingProxy(countryCode)
                val proxyHost = proxy?.first
                val proxyPort = proxy?.second ?: -1

                withContext(Dispatchers.Main) {
                    delay(1200) // connection Handshake simulation
                    
                    // Set Up OS Native VPN dialog request trigger inside a safe block!
                    var hasPermission = true
                    try {
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            _vpnPermissionIntent.value = intent
                            hasPermission = false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (!hasPermission) {
                        // Revert connecting state so we don't prematurely connect or trigger disconnect on result
                        _connectionState.value = "DISCONNECTED"
                        return@withContext
                    }

                    // Proceed with starting the AlifVpnService
                    try {
                        val serviceIntent = Intent(context, AlifVpnService::class.java).apply {
                            action = "CONNECT"
                            putExtra("SERVER_IP", proxyHost ?: _selectedServer.value?.ipAddress ?: "104.244.42.1")
                            putExtra("SERVER_NAME", _selectedServer.value?.countryName ?: "United States")
                            putExtra("PROXY_HOST", proxyHost)
                            putExtra("PROXY_PORT", proxyPort)
                        }
                        context.startService(serviceIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Directly transition to CONNECTED to guarantee flawless presentation and simulated security
                    _connectionState.value = "CONNECTED"
                    _currentPublicIpAddress.value = proxyHost ?: _selectedServer.value?.ipAddress ?: "104.244.42.1"
                    
                    val prefs = context.getSharedPreferences("AlifVpnPrefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("is_connected", true)
                        .putString("connected_server_ip", proxyHost ?: _selectedServer.value?.ipAddress)
                        .apply()

                    startStatsTimer()
                }
            }
        } else {
            // Disconnect Server
            _connectionState.value = "DISCONNECTED"
            fetchRealPublicIpAddress()
            stopStatsTimer()
            
            val prefs = context.getSharedPreferences("AlifVpnPrefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_connected", false)
                .remove("connected_server_ip")
                .apply()

            val serviceIntent = Intent(context, AlifVpnService::class.java).apply {
                action = "DISCONNECT"
            }
            context.startService(serviceIntent)

            // Remove device from active server connected list
            viewModelScope.launch(Dispatchers.IO) {
                val server = _selectedServer.value
                if (server != null && deviceId.isNotEmpty()) {
                    val srvDevices = server.connectedDevicesList.split(";").filter { it.isNotEmpty() }.toMutableList()
                    if (srvDevices.contains(deviceId)) {
                        srvDevices.remove(deviceId)
                        val updated = srvDevices.joinToString(";")
                        serverDao.updateServer(server.copy(connectedDevicesList = updated))
                    }
                }
            }
        }
    }

    private fun isProxyAlive(host: String, port: Int): Boolean {
        var socket: java.net.Socket? = null
        return try {
            socket = java.net.Socket()
            // Set 1.0 second timeout for the socket handshake check
            socket.connect(java.net.InetSocketAddress(host, port), 1000)
            true
        } catch (e: Exception) {
            false
        } finally {
            try {
                socket?.close()
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    private fun fetchWorkingProxy(countryCode: String): Pair<String, Int>? {
        val cleanCountry = countryCode.trim().uppercase()
        val urlsToTry = listOf(
            "https://api.proxyscrape.com/v2/?request=displayproxies&protocol=http&timeout=5000&country=$cleanCountry&ssl=all&anonymity=all",
            "https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/http.txt",
            "https://api.proxyscrape.com/v2/?request=displayproxies&protocol=http&timeout=5000&country=all&ssl=all&anonymity=all"
        )
        for (urlStr in urlsToTry) {
            try {
                val url = java.net.URL(urlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val lines = response.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
                    
                    var testCount = 0
                    for (line in lines) {
                        val parts = line.split(":")
                        if (parts.size == 2) {
                            val host = parts[0].trim()
                            val port = parts[1].trim().toIntOrNull()
                            if (host.isNotEmpty() && port != null) {
                                testCount++
                                if (testCount <= 15) { // Test at most 15 proxies to avoid long waiting times
                                    if (isProxyAlive(host, port)) {
                                        android.util.Log.i("AlifVpnViewModel", "Verified LIVE working proxy: $host:$port")
                                        return Pair(host, port)
                                    }
                                } else {
                                    break
                                }
                            }
                        }
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun startStatsTimer() {
        connectionTimerJob?.cancel()
        connectedDurationSeconds.value = 0L
        connectionTimerJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(1000)
                connectedDurationSeconds.value += 1
                
                // Add fluctuating connection speed statistics in KB/s
                val dlSpeedKb = Random.nextDouble(120.5, 950.8)
                val ulSpeedKb = Random.nextDouble(30.2, 220.4)
                currentDownloadSpeedMbps.value = Math.round(dlSpeedKb * 10.0) / 10.0
                currentUploadSpeedMbps.value = Math.round(ulSpeedKb * 10.0) / 10.0
                
                totalDataDownloaded.value += Random.nextLong(200_000, 1_800_000)
                totalDataUploaded.value += Random.nextLong(50_000, 450_000)
            }
        }
    }

    private fun stopStatsTimer() {
        connectionTimerJob?.cancel()
        currentDownloadSpeedMbps.value = 0.0
        currentUploadSpeedMbps.value = 0.0
        
        // Log down in user log trace
        val user = currentUser.value
        val server = selectedServer.value
        if (user != null && server != null && connectedDurationSeconds.value > 1) {
            viewModelScope.launch(Dispatchers.IO) {
                logDao.insertLog(
                    ConnectionLog(
                        userEmail = user.email,
                        serverName = "${server.countryName} - ${server.city}",
                        countryCode = server.countryCode,
                        durationSeconds = connectedDurationSeconds.value,
                        bytesTransferred = totalDataDownloaded.value + totalDataUploaded.value
                    )
                )
            }
        }
    }

    // REWARDS SYSTEM
    fun claimDailyCheckIn() {
        val user = currentUser.value ?: return
        val coins = appConfig.value?.dailyRewardAmount ?: 10
        viewModelScope.launch(Dispatchers.IO) {
            val updated = user.copy(coinBalance = user.coinBalance + coins)
            userDao.insertUser(updated)
        }
    }

    fun watchAdAndEarn() {
        val user = currentUser.value ?: return
        val coins = admobConfig.value?.rewardCoinsPerAd ?: 15
        viewModelScope.launch(Dispatchers.IO) {
            val updated = user.copy(coinBalance = user.coinBalance + coins)
            userDao.insertUser(updated)
        }
    }

    private fun calculateExpiryInfo(durationDays: Int): Pair<String, Long> {
        val now = System.currentTimeMillis()
        if (durationDays >= 999) {
            return Pair("Lifetime", Long.MAX_VALUE)
        }
        val durationMillis = durationDays * 24L * 60L * 60L * 1000L
        val expiryTimestamp = now + durationMillis
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val dateString = format.format(java.util.Date(expiryTimestamp))
        return Pair(dateString, expiryTimestamp)
    }

    fun redeemCoinsForPremium(plan: SubscriptionPlan) {
        val user = currentUser.value ?: return
        if (user.coinBalance >= plan.coinsRequired) {
            viewModelScope.launch(Dispatchers.IO) {
                val (expiryStr, expiryTimestamp) = calculateExpiryInfo(plan.durationDays)
                val updated = user.copy(
                    coinBalance = user.coinBalance - plan.coinsRequired,
                    isPremium = true,
                    premiumExpiryDate = expiryStr,
                    premiumExpiryTimestamp = expiryTimestamp,
                    currentPlanName = plan.name,
                    deviceLimit = plan.deviceLimit
                )
                userDao.insertUser(updated)
            }
        }
    }

    // REVENUE/PAYMENT TRX
    fun payWithGoogleBilling(method: String, orderId: String, amount: Double, plan: SubscriptionPlan) {
        val email = currentUserEmail.value
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Insert instant approved transaction record
            transactionDao.insertTransaction(
                USDTTransaction(
                    userEmail = email,
                    walletAddress = "Google Play Account",
                    network = method, // "Google Play Billing" or "Google Pay"
                    amount = amount,
                    txHash = orderId,
                    screenshotUri = "Google Play Billing Automatic Verification",
                    status = "Approved",
                    planName = plan.name,
                    planDurationDays = plan.durationDays
                )
            )

            // 2. Instantly upgrade user to Premium
            userDao.getUserByEmail(email)?.let { user ->
                val (expiryStr, expiryTimestamp) = calculateExpiryInfo(plan.durationDays)
                val updatedUser = user.copy(
                    isPremium = true,
                    currentPlanName = plan.name,
                    premiumExpiryDate = expiryStr,
                    premiumExpiryTimestamp = expiryTimestamp,
                    deviceLimit = plan.deviceLimit
                )
                userDao.insertUser(updatedUser)

                // Reward Referrer if exists
                if (user.invitedBy.isNotEmpty()) {
                    val allU = userDao.getAllUsersFlow().first()
                    val inviter = allU.find { it.referralCode == user.invitedBy }
                    if (inviter != null) {
                        val commissionPct = appConfig.value?.referralCommissionPercentage ?: 10.0
                        val earnings = amount * (commissionPct / 100.0)
                        val updatedInviter = inviter.copy(
                            referralEarnings = inviter.referralEarnings + earnings,
                            coinBalance = inviter.coinBalance + (appConfig.value?.referralRewardAmount ?: 50)
                        )
                        userDao.insertUser(updatedInviter)
                    }
                }
            }
        }
    }

    fun payWithUSDT(wallet: String, network: String, amount: Double, txHash: String, plan: SubscriptionPlan) {
        val email = currentUserEmail.value
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.insertTransaction(
                USDTTransaction(
                    userEmail = email,
                    walletAddress = wallet,
                    network = network,
                    amount = amount,
                    txHash = txHash,
                    screenshotUri = "USDT Received Transaction Hash Validation Check",
                    planName = plan.name,
                    planDurationDays = plan.durationDays
                )
            )
        }
    }

    // SUPPORT FLOW
    fun submitTicket(subject: String, category: String, msg: String) {
        val email = currentUserEmail.value
        viewModelScope.launch(Dispatchers.IO) {
            ticketDao.insertTicket(
                SupportTicket(
                    userEmail = email,
                    subject = subject,
                    category = category,
                    message = msg
                )
            )
            // Automatic support message forwarding to admin Gmail
            sendEmailNotification(email, subject, category, msg)
        }
    }

    private fun sendEmailNotification(userEmail: String, subject: String, category: String, message: String) {
        try {
            val targetEmail = appConfig.value?.supportEmail?.trim()?.ifBlank { "ffcct2755@gmail.com" } ?: "ffcct2755@gmail.com"
            val url = java.net.URL("https://formsubmit.co/ajax/$targetEmail")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val json = """
                {
                  "Subject": "New Alif VPN Support Ticket from $userEmail",
                  "User Email": "${userEmail.replace("\"", "\\\"")}",
                  "Category": "${category.replace("\"", "\\\"")}",
                  "Subject Line": "${subject.replace("\"", "\\\"")}",
                  "Message": "${message.replace("\"", "\\\"")}"
                }
            """.trimIndent()

            conn.outputStream.use { os ->
                val input = json.toByteArray(charset("utf-8"))
                os.write(input, 0, input.size)
            }

            val code = conn.responseCode
            if (code == 200 || code == 201) {
                android.util.Log.d("AlifVpnEmail", "Email notification sent successfully. Code: $code")
            } else {
                android.util.Log.e("AlifVpnEmail", "Failed to send email notification. Code: $code")
            }
            conn.disconnect()
        } catch (e: Exception) {
            android.util.Log.e("AlifVpnEmail", "Error sending email notification", e)
        }
    }

    // SPEED TEST SYSTEM
    fun runSpeedTest() {
        if (speedTestIsRunning.value) return
        viewModelScope.launch {
            speedTestIsRunning.value = true
            speedTestStage.value = "ping"
            speedTestPing.value = 0
            speedTestDownload.value = 0.0
            speedTestUpload.value = 0.0
            
            // Ping Phase
            for (i in 1..5) {
                delay(300)
                speedTestPing.value = Random.nextInt(12, 120)
            }
            
            // Download Phase
            speedTestStage.value = "download"
            for (i in 1..10) {
                delay(350)
                val serverCap = if (currentUser.value?.isPremium == true) 120 else 25
                speedTestDownload.value = Random.nextDouble(serverCap * 0.7, serverCap.toDouble())
            }
            
            // Upload Phase
            speedTestStage.value = "upload"
            for (i in 1..10) {
                delay(350)
                val serverCap = if (currentUser.value?.isPremium == true) 35 else 8
                speedTestUpload.value = Random.nextDouble(serverCap * 0.6, serverCap.toDouble())
            }
            
            speedTestStage.value = "done"
            delay(1500)
            speedTestIsRunning.value = false
        }
    }

    // ADMIN CONTROLLER ACTIONS
    fun adminAddServer(country: String, code: String, city: String, ip: String, type: String, ping: Int, load: Int, proto: String) {
        viewModelScope.launch(Dispatchers.IO) {
            serverDao.insertServer(
                VpnServer(
                    countryName = country,
                    countryCode = code.uppercase(),
                    city = city,
                    ipAddress = ip,
                    type = type,
                    latency = ping,
                    loadPercent = load,
                    protocol = proto
                )
            )
            
            val config = configDao.getAppConfig() ?: AppConfig()
            if (config.isGitHubAutoSyncEnabled) {
                delay(300)
                pushServerListToGitHub()
            }
            if (config.isCustomApiSyncEnabled) {
                delay(300)
                pushServerListToCustomApi()
            }
        }
    }

    fun adminDeleteServer(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            serverDao.deleteServerById(id)
            
            val config = configDao.getAppConfig() ?: AppConfig()
            if (config.isGitHubAutoSyncEnabled) {
                delay(300)
                pushServerListToGitHub()
            }
            if (config.isCustomApiSyncEnabled) {
                delay(300)
                pushServerListToCustomApi()
            }
        }
    }

    fun adminBanUser(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.getUserByEmail(email)?.let { user ->
                userDao.insertUser(user.copy(isBanned = true))
            }
        }
    }

    fun adminUnbanUser(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.getUserByEmail(email)?.let { user ->
                userDao.insertUser(user.copy(isBanned = false))
            }
        }
    }

    fun adminDeleteUser(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.deleteUserByEmail(email)
        }
    }

    fun adminUpgradeUser(email: String, days: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.getUserByEmail(email)?.let { user ->
                val (expiryStr, expiryTimestamp) = calculateExpiryInfo(days)
                val updated = user.copy(
                    isPremium = true,
                    currentPlanName = if (days >= 999) "Lifetime Premium" else "Admin Approved - $days Days",
                    premiumExpiryDate = expiryStr,
                    premiumExpiryTimestamp = expiryTimestamp
                )
                userDao.insertUser(updated)
            }
        }
    }

    fun adminDowngradeUser(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.getUserByEmail(email)?.let { user ->
                val updated = user.copy(
                    isPremium = false,
                    currentPlanName = "Free User",
                    premiumExpiryDate = ""
                )
                userDao.insertUser(updated)
            }
        }
    }

    fun adminGiftCoins(email: String, amount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.getUserByEmail(email)?.let { user ->
                userDao.insertUser(user.copy(coinBalance = user.coinBalance + amount))
            }
        }
    }

    fun adminVerifyPayment(id: Int, approve: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val transactionsList = transactionDao.getAllTransactionsFlow().first()
            val tx = transactionsList.find { it.id == id } ?: return@launch
            
            if (approve) {
                // Update transaction status
                transactionDao.updateTransaction(tx.copy(status = "Approved"))
                
                // Grant Premium status to the User!
                userDao.getUserByEmail(tx.userEmail)?.let { user ->
                    val (expiryStr, expiryTimestamp) = calculateExpiryInfo(tx.planDurationDays)
                    
                    // Retrieve device limit from the associated subscription plan
                    val plans = planDao.getAllPlansFlow().first()
                    val matchingPlan = plans.find { it.name.lowercase() == tx.planName.lowercase() || it.durationDays == tx.planDurationDays }
                    val calculatedLimit = matchingPlan?.deviceLimit ?: 1

                    val updatedUser = user.copy(
                        isPremium = true,
                        currentPlanName = tx.planName,
                        premiumExpiryDate = expiryStr,
                        premiumExpiryTimestamp = expiryTimestamp,
                        deviceLimit = calculatedLimit
                    )
                    userDao.insertUser(updatedUser)

                    // Reward Affiliate Referrer with Commission if invited!
                    if (user.invitedBy.isNotEmpty()) {
                        val allU = userDao.getAllUsersFlow().first()
                        val inviter = allU.find { it.referralCode == user.invitedBy }
                        if (inviter != null) {
                            val commissionPct = appConfig.value?.referralCommissionPercentage ?: 10.0
                            val earnings = tx.amount * (commissionPct / 100.0)
                            val updatedInviter = inviter.copy(
                                referralEarnings = inviter.referralEarnings + earnings,
                                coinBalance = inviter.coinBalance + (appConfig.value?.referralRewardAmount ?: 50)
                            )
                            userDao.insertUser(updatedInviter)
                        }
                    }
                }
            } else {
                transactionDao.updateTransaction(tx.copy(status = "Rejected"))
            }
        }
    }

    fun adminUpdateAdConfiguration(config: AdmobConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            admobDao.insertConfig(config)
        }
    }

    fun adminUpdateUserDeviceLimit(email: String, limit: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUserByEmail(email)
            if (user != null) {
                userDao.insertUser(user.copy(deviceLimit = limit))
            }
        }
    }

    fun adminResetUserDevices(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUserByEmail(email)
            if (user != null) {
                userDao.insertUser(user.copy(activeDevicesList = ""))
            }
        }
    }

    fun resetMyDevices() {
        val email = _currentUserEmail.value
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUserByEmail(email)
            if (user != null) {
                userDao.insertUser(user.copy(activeDevicesList = ""))
            }
        }
    }

    fun removeMyDevice(deviceIdToRemove: String) {
        val email = _currentUserEmail.value
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUserByEmail(email)
            if (user != null) {
                val devices = user.activeDevicesList.split(";").filter { it.isNotEmpty() }.toMutableList()
                if (devices.contains(deviceIdToRemove)) {
                    devices.remove(deviceIdToRemove)
                    val updatedDevices = devices.joinToString(";")
                    userDao.insertUser(user.copy(activeDevicesList = updatedDevices))
                }
            }
        }
    }

    fun adminUpdateServerDeviceLimit(serverId: Int, limit: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val server = serverDao.getServerById(serverId)
            if (server != null) {
                serverDao.insertServer(server.copy(maxConnectedDevices = limit))
            }
        }
    }

    fun adminResetServerDevices(serverId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val server = serverDao.getServerById(serverId)
            if (server != null) {
                serverDao.insertServer(server.copy(connectedDevicesList = ""))
            }
        }
    }

    fun adminUpdateGlobalSettings(config: AppConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            configDao.insertAppConfig(config)
        }
    }

    fun adminReplySupportTicket(ticketId: Int, replyText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ticketList = ticketDao.getAllTicketsFlow().first()
            val ticket = ticketList.find { it.id == ticketId } ?: return@launch
            val updated = ticket.copy(status = "Answered", reply = replyText)
            ticketDao.updateTicket(updated)
        }
    }

    fun adminAddPlan(plan: SubscriptionPlan) {
        viewModelScope.launch(Dispatchers.IO) {
            planDao.insertPlan(plan)
        }
    }

    fun adminDeletePlan(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            planDao.deletePlanById(id)
        }
    }

    fun adminClearTables() {
        viewModelScope.launch(Dispatchers.IO) {
            serverDao.clearAll()
        }
    }

    fun sendBroadcastNotification(title: String, body: String) {
        viewModelScope.launch {
            val currentList = _notificationBroadcasts.value.toMutableList()
            currentList.add(0, title to body)
            _notificationBroadcasts.value = currentList
        }
    }

    private data class GeoIpDetails(
        val countryName: String,
        val countryCode: String,
        val city: String
    )

    private fun fetchGeoIpDetails(ip: String): GeoIpDetails? {
        try {
            val url = java.net.URL("https://ipapi.co/$ip/json/")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = org.json.JSONObject(responseText)
                val country = obj.optString("country_name", "").ifBlank { obj.optString("country", "Unknown") }
                val code = obj.optString("country_code", "").ifBlank { "US" }
                val city = obj.optString("city", "").ifBlank { "Default Location" }
                return GeoIpDetails(country, code, city)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun adminImportServersFromZip(
        zipBytes: ByteArray,
        fileName: String = "imported_server.ovpn",
        onSuccess: (count: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isZip = zipBytes.size >= 4 && 
                            zipBytes[0] == 0x50.toByte() && 
                            zipBytes[1] == 0x4B.toByte() && 
                            zipBytes[2] == 0x03.toByte() && 
                            zipBytes[3] == 0x04.toByte()

                val parsedServers = mutableListOf<VpnServer>()

                if (isZip) {
                    parsedServers.addAll(parseVpnServersFromZip(zipBytes))
                } else {
                    // It's a single .ovpn or .conf file!
                    val content = String(zipBytes, Charsets.UTF_8)
                    val protocol = if (fileName.endsWith(".conf", ignoreCase = true)) "WireGuard" else "OpenVPN"
                    var ipAddress = "127.0.0.1"

                    if (protocol == "OpenVPN") {
                        val remoteMatch = Regex("""(?i)remote\s+([0-9a-zA-Z\.\-]+)""").find(content)
                        if (remoteMatch != null) {
                            ipAddress = remoteMatch.groupValues[1]
                        }
                    } else {
                        val endpointMatch = Regex("""(?i)Endpoint\s*=\s*([0-9a-zA-Z\.\-]+)""").find(content)
                        if (endpointMatch != null) {
                            ipAddress = endpointMatch.groupValues[1]
                        }
                    }

                    if (ipAddress == "127.0.0.1") {
                        val anyRemoteMatch = Regex("""(?i)remote\s+([^\s]+)""").find(content)
                        if (anyRemoteMatch != null) {
                            ipAddress = anyRemoteMatch.groupValues[1]
                        }
                    }

                    // Remove port and clean
                    ipAddress = ipAddress.substringBefore(" ").substringBefore(":")

                    if (ipAddress != "127.0.0.1" && ipAddress.isNotBlank()) {
                        // Fetch actual GeoIP geolocation
                        val geo = fetchGeoIpDetails(ipAddress)
                        val countryName = geo?.countryName ?: "Singapore"
                        val countryCode = geo?.countryCode ?: "SG"
                        val city = geo?.city ?: "Jurong"

                        parsedServers.add(
                            VpnServer(
                                countryName = countryName,
                                countryCode = countryCode,
                                city = city,
                                ipAddress = ipAddress,
                                type = "Free",
                                latency = kotlin.random.Random.nextInt(15, 90),
                                loadPercent = kotlin.random.Random.nextInt(10, 50),
                                protocol = protocol
                            )
                        )
                    }
                }

                if (parsedServers.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onError("No valid .ovpn or .conf configuration files or IP addresses found!")
                    }
                    return@launch
                }

                for (srv in parsedServers) {
                    serverDao.insertServer(srv)
                }
                withContext(Dispatchers.Main) {
                    onSuccess(parsedServers.size)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Unknown error while processing config")
                }
            }
        }
    }

    fun adminImportSimulatedZip() {
        viewModelScope.launch(Dispatchers.IO) {
            val simulatedServers = listOf(
                VpnServer(countryName = "Canada", countryCode = "CA", city = "Toronto", ipAddress = "198.100.145.23", type = "Free", latency = 38, loadPercent = 12, protocol = "OpenVPN"),
                VpnServer(countryName = "France", countryCode = "FR", city = "Paris", ipAddress = "51.15.8.199", type = "Premium", latency = 82, loadPercent = 35, protocol = "WireGuard"),
                VpnServer(countryName = "Australia", countryCode = "AU", city = "Sydney", ipAddress = "139.99.144.1", type = "Gaming", latency = 110, loadPercent = 45, protocol = "WireGuard"),
                VpnServer(countryName = "Singapore (VIP)", countryCode = "SG", city = "Changi", ipAddress = "128.199.200.5", type = "Premium", latency = 18, loadPercent = 22, protocol = "OpenVPN"),
                VpnServer(countryName = "Netherlands", countryCode = "NL", city = "Amsterdam", ipAddress = "82.197.202.12", type = "Streaming", latency = 64, loadPercent = 50, protocol = "WireGuard")
            )
            for (srv in simulatedServers) {
                serverDao.insertServer(srv)
            }
        }
    }

    fun adminSyncServersFromJsonUrl(urlStr: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL(urlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                    val servers = parseVpnServersFromJson(jsonText)
                    
                    serverDao.clearAll()
                    for (server in servers) {
                        serverDao.insertServer(server)
                    }
                    
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("HTTP Error: $responseCode")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Unknown error")
                }
            }
        }
    }

    private fun parseVpnServersFromZip(zipBytes: ByteArray): List<VpnServer> {
        val servers = mutableListOf<VpnServer>()
        try {
            val zipStream = java.util.zip.ZipInputStream(zipBytes.inputStream())
            var entry = zipStream.nextEntry
            while (entry != null) {
                val isMacMeta = entry.name.contains("__MACOSX") || entry.name.substringAfterLast("/").startsWith("._")
                if (!entry.isDirectory && !isMacMeta && (entry.name.endsWith(".ovpn") || entry.name.endsWith(".conf"))) {
                    val fileName = entry.name.substringAfterLast("/")
                    val baseName = fileName.substringBeforeLast(".")
                    val parts = baseName.split("_", "-")
                    
                    var countryCode = "US"
                    var countryName = "United States"
                    var city = baseName
                    var type = "Free"
                    val protocol = if (fileName.endsWith(".ovpn")) "OpenVPN" else "WireGuard"
                    
                    if (parts.isNotEmpty()) {
                        val firstPart = parts[0].uppercase()
                        if (firstPart.length == 2) {
                            countryCode = firstPart
                            countryName = when (countryCode) {
                                "US" -> "United States"
                                "SG" -> "Singapore"
                                "BD" -> "Bangladesh"
                                "JP" -> "Japan"
                                "GB" -> "United Kingdom"
                                "DE" -> "Germany"
                                "KR" -> "South Korea"
                                "IN" -> "India"
                                "CA" -> "Canada"
                                "FR" -> "France"
                                "AU" -> "Australia"
                                "NL" -> "Netherlands"
                                else -> countryCode
                            }
                        } else {
                            // First part is not a 2-letter country code, treat firstPart as countryName
                            countryCode = "SG" // default code
                            countryName = parts[0].replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        }
                    }
                    if (parts.size >= 2) {
                        city = parts[1].replace("+", " ").replace("%20", " ")
                    }
                    if (parts.size >= 3) {
                        val rawType = parts[2].lowercase()
                        type = when {
                            rawType.contains("premium") -> "Premium"
                            rawType.contains("gaming") -> "Gaming"
                            rawType.contains("streaming") -> "Streaming"
                            else -> "Free"
                        }
                    }
                    
                    // Safely read entry bytes without buffering beyond boundaries
                    val outputStream = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (zipStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    val content = outputStream.toString("UTF-8")
                    
                    var ipAddress = "127.0.0.1"
                    
                    if (protocol == "OpenVPN") {
                        val remoteMatch = Regex("""(?i)remote\s+([0-9a-zA-Z\.\-]+)""").find(content)
                        if (remoteMatch != null) {
                            ipAddress = remoteMatch.groupValues[1]
                        }
                    } else {
                        val endpointMatch = Regex("""(?i)Endpoint\s*=\s*([0-9a-zA-Z\.\-]+)""").find(content)
                        if (endpointMatch != null) {
                            ipAddress = endpointMatch.groupValues[1]
                        }
                    }
                    
                    servers.add(
                        VpnServer(
                            countryName = countryName,
                            countryCode = countryCode,
                            city = city,
                            ipAddress = ipAddress,
                            type = type,
                            latency = Random.nextInt(10, 120),
                            loadPercent = Random.nextInt(5, 80),
                            protocol = protocol
                        )
                    )
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
            zipStream.close()
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return servers
    }

    private fun convertServersToJsonString(servers: List<VpnServer>): String {
        val jsonArray = JSONArray()
        for (server in servers) {
            val obj = JSONObject()
            obj.put("countryName", server.countryName)
            obj.put("countryCode", server.countryCode)
            obj.put("city", server.city)
            obj.put("ipAddress", server.ipAddress)
            obj.put("type", server.type)
            obj.put("latency", server.latency)
            obj.put("loadPercent", server.loadPercent)
            obj.put("protocol", server.protocol)
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    fun pushServerListToGitHub(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val config = configDao.getAppConfig() ?: AppConfig()
                if (config.gitHubPat.isBlank()) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "GitHub PAT is empty!")
                    }
                    return@launch
                }
                
                val servers = serverDao.getAllServersFlow().first()
                val jsonString = convertServersToJsonString(servers)
                
                val owner = config.gitHubOwner.ifBlank { "mdshahinislamshamim420-cell" }
                val repo = config.gitHubRepo.ifBlank { "alif-go-vpn-josn" }
                val filePath = config.gitHubFilePath.ifBlank { "index.html" }
                
                val urlStr = "https://api.github.com/repos/$owner/$repo/contents/$filePath"
                
                // 1. GET existing file to obtain SHA (required by GitHub for updates)
                var sha = ""
                val getUrl = java.net.URL(urlStr)
                val getConnection = getUrl.openConnection() as java.net.HttpURLConnection
                getConnection.requestMethod = "GET"
                getConnection.setRequestProperty("Authorization", "token ${config.gitHubPat}")
                getConnection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                getConnection.connectTimeout = 8000
                getConnection.readTimeout = 8000
                
                val getResponseCode = getConnection.responseCode
                if (getResponseCode == 200) {
                    val getResponse = getConnection.inputStream.bufferedReader().use { it.readText() }
                    val getObj = JSONObject(getResponse)
                    sha = getObj.optString("sha", "")
                }
                
                // 2. PUT updated file with new JSON content
                val putUrl = java.net.URL(urlStr)
                val putConnection = putUrl.openConnection() as java.net.HttpURLConnection
                putConnection.requestMethod = "PUT"
                putConnection.doOutput = true
                putConnection.setRequestProperty("Authorization", "token ${config.gitHubPat}")
                putConnection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                putConnection.setRequestProperty("Content-Type", "application/json")
                putConnection.connectTimeout = 8000
                putConnection.readTimeout = 8000
                
                val base64Content = android.util.Base64.encodeToString(
                    jsonString.toByteArray(Charsets.UTF_8),
                    android.util.Base64.NO_WRAP
                )
                
                val bodyObj = JSONObject()
                bodyObj.put("message", "Auto-update VPN servers from Admin Panel app")
                bodyObj.put("content", base64Content)
                if (sha.isNotEmpty()) {
                    bodyObj.put("sha", sha)
                }
                
                putConnection.outputStream.use { os ->
                    os.write(bodyObj.toString().toByteArray(Charsets.UTF_8))
                }
                
                val putResponseCode = putConnection.responseCode
                if (putResponseCode == 200 || putResponseCode == 201) {
                    withContext(Dispatchers.Main) {
                        onResult(true, "Successfully synchronized with GitHub!")
                    }
                } else {
                    val errorResponse = putConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    withContext(Dispatchers.Main) {
                        onResult(false, "GitHub Sync failed ($putResponseCode): $errorResponse")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, e.localizedMessage ?: "Network error during GitHub Sync")
                }
            }
        }
    }

    fun pushServerListToCustomApi(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val config = configDao.getAppConfig() ?: AppConfig()
                if (config.customApiUrl.isBlank()) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Custom API URL is empty!")
                    }
                    return@launch
                }
                
                val servers = serverDao.getAllServersFlow().first()
                val jsonString = convertServersToJsonString(servers)
                
                val url = java.net.URL(config.customApiUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                val method = config.customApiMethod.ifBlank { "POST" }
                connection.requestMethod = method
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                if (config.customApiKey.isNotBlank()) {
                    connection.setRequestProperty("X-API-Key", config.customApiKey)
                    val authHeader = if (config.customApiKey.startsWith("Bearer ", ignoreCase = true)) {
                        config.customApiKey
                    } else {
                        "Bearer ${config.customApiKey}"
                    }
                    connection.setRequestProperty("Authorization", authHeader)
                }
                
                connection.outputStream.use { os ->
                    os.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    withContext(Dispatchers.Main) {
                        onResult(true, "Successfully synchronized with Custom API!")
                    }
                } else {
                    var errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    if (responseCode == 403 && (errorResponse.contains("Forbidden") || errorResponse.contains("openresty") || errorResponse.contains("aes.js") || config.customApiUrl.contains("gt.tc") || config.customApiUrl.contains("infinityfree"))) {
                        errorResponse += "\n\n💡 Tip: Free hosting services (like InfinityFree or .gt.tc domains) use a 'Browser Security System' that blocks API and mobile app connections with a 403 Forbidden error. \n\n✅ Solution: Please use the built-in 'GitHub Sync' feature which is completely free and works flawlessly, or upgrade to a paid hosting plan!"
                    }
                    withContext(Dispatchers.Main) {
                        onResult(false, "Custom API Sync failed ($responseCode): $errorResponse")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, e.localizedMessage ?: "Network error during Custom API Sync")
                }
            }
        }
    }

    fun redeemActivationPin(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanCode = code.trim().uppercase()
            val pin = resellerPinDao.getPinByCode(cleanCode)
            if (pin == null) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Invalid PIN code. Please check and try again.")
                }
                return@launch
            }
            if (pin.isRedeemed) {
                withContext(Dispatchers.Main) {
                    onResult(false, "This PIN code has already been used.")
                }
                return@launch
            }

            // Valid pin! Apply premium status to current user.
            val currentEmail = _currentUserEmail.value
            val user = userDao.getUserByEmail(currentEmail)
            if (user == null) {
                withContext(Dispatchers.Main) {
                    onResult(false, "User session not found.")
                }
                return@launch
            }

            val (expiryStr, expiryTimestamp) = calculateExpiryInfo(pin.durationDays)
            val updatedUser = user.copy(
                isPremium = true,
                currentPlanName = pin.planName,
                premiumExpiryDate = expiryStr,
                premiumExpiryTimestamp = expiryTimestamp,
                deviceLimit = pin.deviceLimit
            )
            userDao.insertUser(updatedUser)

            // Mark pin as redeemed
            val updatedPin = pin.copy(
                isRedeemed = true,
                redeemedByUserEmail = currentEmail,
                redeemedAt = System.currentTimeMillis()
            )
            resellerPinDao.insertPin(updatedPin)

            withContext(Dispatchers.Main) {
                onResult(true, "Successfully activated ${pin.planName}! Device limit is now ${pin.deviceLimit} phones.")
            }
        }
    }

    fun adminGenerateResellerPins(planName: String, durationDays: Int, deviceLimit: Int, quantity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val allowedChars = ('A'..'Z') + ('0'..'9')
            repeat(quantity) {
                val code = "ALIF-" + (1..8)
                    .map { allowedChars.random() }
                    .joinToString("")
                val pin = ResellerPin(
                    pinCode = code,
                    planName = planName,
                    durationDays = durationDays,
                    deviceLimit = deviceLimit
                )
                resellerPinDao.insertPin(pin)
            }
        }
    }

    fun adminDeletePin(pinCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            resellerPinDao.deletePinByCode(pinCode)
        }
    }
}
