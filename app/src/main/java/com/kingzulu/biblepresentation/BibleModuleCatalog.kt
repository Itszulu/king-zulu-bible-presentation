package com.kingzulu.biblepresentation

/**
 * Desired King Zulu translations and their offline eligibility.
 * IMPORTANT: a translation is not downloadable merely because an API can display it.
 * `offlineApproved` must only become true when the applicable licence explicitly
 * permits King Zulu to retain the full text locally as an installable module.
 */
data class BibleModuleCatalogEntry(
    val abbreviation: String,
    val name: String,
    val provider: String,
    val offlineApproved: Boolean,
    val note: String
)

object BibleModuleCatalog {
    val desired = listOf(
        BibleModuleCatalogEntry("KJV", "King James Version", "Bundled", true, "Installed with King Zulu"),
        BibleModuleCatalogEntry("NIV", "New International Version", "API.Bible / Biblica", false, "API access may be cached under licence; full offline-module permission must be confirmed"),
        BibleModuleCatalogEntry("NKJV", "New King James Version", "API.Bible / Thomas Nelson", false, "Available to qualifying non-commercial API.Bible apps; full offline-module permission must be confirmed"),
        BibleModuleCatalogEntry("NLT", "New Living Translation", "API.Bible / Tyndale", false, "API/caching rights do not automatically grant permanent full-Bible offline redistribution"),
        BibleModuleCatalogEntry("AMP", "Amplified Bible", "API.Bible", false, "Full offline-module permission must be confirmed"),
        BibleModuleCatalogEntry("AMPC", "Amplified Bible, Classic Edition", "Rights holder / authorised provider", false, "Provider and offline rights still require verification"),
        BibleModuleCatalogEntry("ESV", "English Standard Version", "Crossway", false, "Use Crossway's authorised route; offline-module rights require separate confirmation")
    )

    fun downloadable(): List<BibleModuleCatalogEntry> = desired.filter { it.offlineApproved }
    fun entry(abbreviation: String) = desired.firstOrNull { it.abbreviation.equals(abbreviation, true) }
}
