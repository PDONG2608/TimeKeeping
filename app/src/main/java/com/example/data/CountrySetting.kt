package com.example.data

import com.example.R

data class CountrySetting(
    val code: String, // e.g. "VN", "US", etc.
    val stringResId: Int, // string Resource ID for localizable display name
    val timezoneId: String // e.g., "Asia/Ho_Chi_Minh"
) {
    companion object {
        val ALL_COUNTRIES = listOf(
            CountrySetting("VN", R.string.country_vietnam, "Asia/Ho_Chi_Minh"),
            CountrySetting("US", R.string.country_usa, "America/New_York"),
            CountrySetting("JP", R.string.country_japan, "Asia/Tokyo"),
            CountrySetting("KR", R.string.country_korea, "Asia/Seoul"),
            CountrySetting("SG", R.string.country_singapore, "Asia/Singapore"),
            CountrySetting("UK", R.string.country_uk, "Europe/London"),
            CountrySetting("DE", R.string.country_germany, "Europe/Berlin"),
            CountrySetting("FR", R.string.country_france, "Europe/Paris"),
            CountrySetting("AU", R.string.country_australia, "Australia/Sydney")
        )

        fun getByCode(code: String): CountrySetting {
            return ALL_COUNTRIES.firstOrNull { it.code.uppercase() == code.uppercase() } ?: ALL_COUNTRIES[0]
        }
    }
}
