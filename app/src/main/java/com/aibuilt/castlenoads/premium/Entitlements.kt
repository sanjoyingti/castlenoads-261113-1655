package com.aibuilt.castlenoads.premium

/**
 * The subscription / billing system of the shipped app was cut out. In its place this object always
 * reports an active entitlement, so every internal `isPremium` / `isSubscribed` check passes.
 *
 * This is the source-level equivalent of patching the smali `isPremium()` / `isSubscribed()`
 * accessors to `const/4 v0, 0x1; return v0`.
 */
object Entitlements {

    /** Always true: the plan is reported as active inside the app. */
    val isPremium: Boolean = true

    /** Alias used by older checks in the code base. */
    val isSubscribed: Boolean get() = true

    val planName: String = "Castle Premium"

    val planPriceLabel: String = "Active · no billing"

    const val MAX_PROFILES = 5

    /** Everything that used to sit behind the paywall. */
    val unlockedFeatures: List<String> = listOf(
        "Every film and series, no locks",
        "Ultra HD 4K streaming",
        "Offline downloads",
        "Up to $MAX_PROFILES profiles",
        "Subtitles and audio track picking",
        "Chromecast to your TV",
        "Zero ads anywhere"
    )

    fun maxProfiles(): Int = MAX_PROFILES

    fun isTitleUnlocked(premiumTitle: Boolean): Boolean = isPremium || !premiumTitle

    fun isUltraHdUnlocked(): Boolean = isPremium

    fun isDownloadsUnlocked(): Boolean = isPremium
}
