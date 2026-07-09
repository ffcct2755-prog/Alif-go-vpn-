package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class VpnServer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val countryName: String,
    val countryCode: String, // e.g. "US", "SG", "JP"
    val city: String,
    val ipAddress: String,
    val type: String, // "Free", "Premium", "Gaming", "Streaming"
    val latency: Int, // ping in ms
    val loadPercent: Int, // 0-100%
    val isEnabled: Boolean = true,
    val protocol: String = "WireGuard", // WireGuard, OpenVPN, IKEv2
    val maxConnectedDevices: Int = 5, // Maximum concurrent devices allowed to connect
    val connectedDevicesList: String = "" // Semicolon-separated list of active devices/user emails connected
)

@Entity(tableName = "users")
data class UserSession(
    @PrimaryKey val email: String,
    val name: String,
    val isPremium: Boolean = false,
    val premiumExpiryDate: String = "", // e.g., "Pending" or "2026-10-15"
    val premiumExpiryTimestamp: Long = 0L, // timestamp in ms, 0 means no active countdown or infinite
    val coinBalance: Int = 100, // 100 initial free coins
    val referralEarnings: Double = 0.0, // in USDT
    val referralCode: String,
    val invitedBy: String = "",
    val isBanned: Boolean = false,
    val currentPlanName: String = "Free User",
    val dataUsedUploadedInBytes: Long = 0,
    val dataUsedDownloadedInBytes: Long = 0,
    val isGuest: Boolean = false,
    val deviceLimit: Int = 3, // Default limit of 3 devices
    val activeDevicesList: String = "" // Semicolon-separated list of active device IDs (ANDROID_ID)
)

@Entity(tableName = "transactions")
data class USDTTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val walletAddress: String,
    val network: String, // TRC20, BEP20, ERC20
    val amount: Double,
    val txHash: String,
    val screenshotUri: String = "", // optional uri or placeholder
    val status: String = "Pending", // Pending, Approved, Rejected
    val timestamp: Long = System.currentTimeMillis(),
    val planName: String,
    val planDurationDays: Int
)

@Entity(tableName = "support_tickets")
data class SupportTicket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val subject: String,
    val category: String, // Billing, Speed, Server, Other
    val message: String,
    val status: String = "Open", // Open, Answered
    val reply: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "admob_config")
data class AdmobConfig(
    @PrimaryKey val id: Int = 1, // Only 1 configuration row
    val isBannerEnabled: Boolean = true,
    val isInterstitialEnabled: Boolean = true,
    val isRewardedEnabled: Boolean = true,
    val isRewardedInterstitialEnabled: Boolean = true,
    val isNativeEnabled: Boolean = true,
    val isAppOpenEnabled: Boolean = true,
    val rewardCoinsPerAd: Int = 15,
    val adFrequencyMinutes: Int = 2,
    val appId: String = "ca-app-pub-1131981412237081~8138260298",
    val bannerPlacementId: String = "ca-app-pub-1131981412237081/5644757651",
    val nativePlacementId: String = "ca-app-pub-1131981412237081/8083218739",
    val interstitialPlacementId: String = "ca-app-pub-1131981412237081/5320525262",
    val rewardedPlacementId: String = "ca-app-pub-1131981412237081/3495060863"
)

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1, // Only 1 configuration row
    val appName: String = "Alif Go VPN",
    val supportEmail: String = "support@alifvpn.com",
    val telegramLink: String = "https://t.me/alifvpn",
    val websiteLink: String = "https://alifvpn.com",
    val privacyPolicy: String = "https://alifvpn.com/privacy",
    val termsAndConditions: String = "https://alifvpn.com/terms",
    val dailyRewardAmount: Int = 10,
    val referralRewardAmount: Int = 50, // coins
    val referralCommissionPercentage: Double = 10.0, // % commission for top-ups
    val isReferralEnabled: Boolean = true,
    val maintenanceMode: Boolean = false,
    val systemLanguage: String = "en", // "en" or "bn"
    
    // Google Play Billing Configurations
    val isPlayBillingEnabled: Boolean = false,
    val isManualPaymentEnabled: Boolean = true,
    val isCoinRedemptionEnabled: Boolean = true,
    val playLicenseKey: String = "",
    val playWeeklyProductId: String = "alif_vpn_weekly",
    val playMonthlyProductId: String = "alif_vpn_monthly",
    val playYearlyProductId: String = "alif_vpn_yearly",
    val playLifetimeProductId: String = "alif_vpn_lifetime",
    
    // Premium Page Customization
    val premiumPageTitle: String = "Go Premium & Experience Ultimate Speed",
    val premiumPageSubtitle: String = "Access 100+ secure, lightning-fast servers globally without limits",
    val premiumPageHighlightColor: String = "#FFD700",
    val premiumPageFeaturesList: String = "Unlimited Speed & Bandwidth;Ad-free Premium Experience;Access to VIP & Gaming Servers;24/7 Dedicated Live Support",
    val premiumPageBannerUrl: String = "",

    // GitHub Auto-Synchronization Settings
    val gitHubPat: String = "",
    val gitHubOwner: String = "mdshahinislamshamim420-cell",
    val gitHubRepo: String = "alif-go-vpn-josn",
    val gitHubFilePath: String = "index.html", // or index.json based on repo setup
    val isGitHubAutoSyncEnabled: Boolean = false,

    // Web Host / MySQL Custom API Sync Settings
    val customApiUrl: String = "https://alifgo.gt.tc/api/get_vpn_servers.php",
    val customApiKey: String = "",
    val customApiMethod: String = "POST", // "POST" or "PUT"
    val isCustomApiSyncEnabled: Boolean = true
)

@Entity(tableName = "connection_logs")
data class ConnectionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val serverName: String,
    val countryCode: String,
    val connectedAt: Long = System.currentTimeMillis(),
    val durationSeconds: Long,
    val bytesTransferred: Long
)

@Entity(tableName = "subscription_plans")
data class SubscriptionPlan(
    @PrimaryKey val id: String, // "weekly", "monthly", "yearly", etc.
    val name: String,
    val durationDays: Int,
    val priceUsdt: Double,
    val discountPercent: Int = 0,
    val coinsRequired: Int = 500,
    val deviceLimit: Int = 3 // Max devices allowed for this plan
)

@Entity(tableName = "reseller_pins")
data class ResellerPin(
    @PrimaryKey val pinCode: String,
    val planName: String, // e.g. "Weekly Plan"
    val durationDays: Int,
    val deviceLimit: Int,
    val isRedeemed: Boolean = false,
    val redeemedByUserEmail: String = "",
    val redeemedAt: Long = 0L,
    val generatedBy: String = "Admin", // e.g., "Admin" or reseller's email
    val generatedAt: Long = System.currentTimeMillis()
)
