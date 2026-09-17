package com.kingzulu.biblepresentation

/**
 * Commercial entitlement boundary for King Zulu.
 *
 * Presentation features must ask this layer for entitlement instead of checking
 * purchase flags directly. This lets a future server/Play Billing implementation
 * be added without rewriting presentation, Bible, Cast, Songs or media code.
 *
 * SECURITY: never store private signing keys or payment secrets in the APK.
 */
enum class KingZuluPlan { FREE, PRO_LIFETIME, CHURCH, SUBSCRIPTION }

enum class KingZuluFeature {
    BIBLE_PRESENTATION,
    AI_LISTEN,
    SONGS,
    VIDEO_BACKGROUNDS,
    CUSTOM_BRANDING,
    GOOGLE_CAST,
    MULTI_DEVICE
}

data class LicenseEntitlement(
    val plan: KingZuluPlan = KingZuluPlan.FREE,
    val accountId: String? = null,
    val entitlementId: String? = null,
    val activeDeviceId: String? = null,
    val maxActiveDevices: Int = 1,
    val offlineValidUntilEpochMs: Long? = null,
    val serverSignature: String? = null
) {
    fun has(feature: KingZuluFeature): Boolean = when (plan) {
        KingZuluPlan.FREE -> feature == KingZuluFeature.BIBLE_PRESENTATION
        KingZuluPlan.PRO_LIFETIME -> feature != KingZuluFeature.MULTI_DEVICE
        KingZuluPlan.CHURCH, KingZuluPlan.SUBSCRIPTION -> true
    }
}

interface LicenseService {
    suspend fun restore(accountToken: String): Result<LicenseEntitlement>
    suspend fun activate(accountToken: String, deviceId: String): Result<LicenseEntitlement>
    suspend fun transfer(accountToken: String, newDeviceId: String): Result<LicenseEntitlement>
    suspend fun refresh(current: LicenseEntitlement): Result<LicenseEntitlement>
    fun verifyOffline(entitlement: LicenseEntitlement, nowEpochMs: Long): Boolean
}

/**
 * Current builds remain fully usable while commercial licensing is not launched.
 * Replace this provider with a server-backed implementation at launch time.
 */
object Entitlements {
    @Volatile private var current = LicenseEntitlement(KingZuluPlan.CHURCH)
    fun current(): LicenseEntitlement = current
    fun install(entitlement: LicenseEntitlement) { current = entitlement }
    fun canUse(feature: KingZuluFeature): Boolean = current.has(feature)
}
