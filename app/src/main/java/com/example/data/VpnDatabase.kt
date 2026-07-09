package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        VpnServer::class,
        UserSession::class,
        USDTTransaction::class,
        SupportTicket::class,
        AdmobConfig::class,
        AppConfig::class,
        ConnectionLog::class,
        SubscriptionPlan::class
    ],
    version = 8,
    exportSchema = false
)
abstract class VpnDatabase : RoomDatabase() {

    abstract fun serverDao(): VpnServerDao
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): USDTTransactionDao
    abstract fun ticketDao(): SupportTicketDao
    abstract fun admobDao(): AdmobConfigDao
    abstract fun configDao(): AppConfigDao
    abstract fun logDao(): ConnectionLogDao
    abstract fun planDao(): SubscriptionPlanDao

    companion object {
        @Volatile
        private var INSTANCE: VpnDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): VpnDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VpnDatabase::class.java,
                    "alif_vpn_database"
                )
                .addCallback(VpnDatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class VpnDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: VpnDatabase) {
            // Seed VPN Servers
            val serverDao = db.serverDao()
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

            // Seed App Configurations
            val configDao = db.configDao()
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
                    isPlayBillingEnabled = false,
                    isManualPaymentEnabled = true,
                    isCoinRedemptionEnabled = true
                )
            )

            // Seed Admob Default Configurations
            val admobDao = db.admobDao()
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

            // Seed Subscription Plans
            val planDao = db.planDao()
            val plans = listOf(
                SubscriptionPlan("weekly", "Weekly Plan", 7, 2.0, 0, 150),
                SubscriptionPlan("monthly", "Monthly Premium", 30, 5.0, 15, 500),
                SubscriptionPlan("3month", "3 Months Super saving", 90, 12.0, 20, 1200),
                SubscriptionPlan("6month", "6 Months Ultimate", 180, 20.0, 25, 2000),
                SubscriptionPlan("yearly", "Yearly Unlimited", 365, 30.0, 40, 3500),
                SubscriptionPlan("lifetime", "Lifetime Freedom", 9999, 50.0, 50, 6000)
            )
            for (plan in plans) {
                planDao.insertPlan(plan)
            }

            // Seed Support FAQs / seed user as default demo
            val userDao = db.userDao()
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
            userDao.insertUser(
                UserSession(
                    email = "ffcct2755@gmail.com", // user's context-email
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
    }
}
