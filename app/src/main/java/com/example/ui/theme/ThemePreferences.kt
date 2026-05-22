package com.example.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class AppBackgroundTheme(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val accentColor: Color,
    val surfaceColor: Color
) {
    CLASSIC_DARK(
        id = "classic_dark",
        displayName = "Tối Cổ Điển",
        primaryColor = Color(0xFF3F51B5),
        accentColor = Color(0xFF7585E9),
        surfaceColor = Color(0x331E1E2C)
    ),
    WARM_SUNSET(
        id = "warm_sunset",
        displayName = "Hoàng Hôn Ấm",
        primaryColor = Color(0xFFFF5722),
        accentColor = Color(0xFFFF9800),
        surfaceColor = Color(0x333A181C)
    ),
    AURORA_NIGHT(
        id = "aurora_night",
        displayName = "Cực Quang Đêm",
        primaryColor = Color(0xFF00D2FF),
        accentColor = Color(0xFF00E676),
        surfaceColor = Color(0x330C192E)
    ),
    FOREST_SERENITY(
        id = "forest_serenity",
        displayName = "Rừng Nghiêng",
        primaryColor = Color(0xFF81C784),
        accentColor = Color(0xFF26A69A),
        surfaceColor = Color(0x33112E24)
    ),
    CYBERPUNK(
        id = "cyberpunk",
        displayName = "Năng Lượng Neon",
        primaryColor = Color(0xFFFF007F),
        accentColor = Color(0xFF00F3FF),
        surfaceColor = Color(0x33250133)
    );

    fun getBrush(): Brush {
        return when (this) {
            CLASSIC_DARK -> Brush.verticalGradient(
                colors = listOf(Color(0xFF121420), Color(0xFF1B1E36))
            )
            WARM_SUNSET -> Brush.verticalGradient(
                colors = listOf(Color(0xFF2D142C), Color(0xFF5E1B24), Color(0xFF802013))
            )
            AURORA_NIGHT -> Brush.verticalGradient(
                colors = listOf(Color(0xFF070F1E), Color(0xFF0B2144), Color(0xFF104A66))
            )
            FOREST_SERENITY -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0C1713), Color(0xFF1A382A), Color(0xFF1C4D35))
            )
            CYBERPUNK -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0C0012), Color(0xFF20012B), Color(0xFF060D2D))
            )
        }
    }
}

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE)

    fun getBackgroundTheme(): AppBackgroundTheme {
        val id = prefs.getString("bg_theme_id", AppBackgroundTheme.AURORA_NIGHT.id)
        return AppBackgroundTheme.values().firstOrNull { it.id == id } ?: AppBackgroundTheme.AURORA_NIGHT
    }

    fun setBackgroundTheme(theme: AppBackgroundTheme) {
        prefs.edit().putString("bg_theme_id", theme.id).apply()
    }

    fun getSelectedCountryCode(): String {
        return prefs.getString("selected_country_code", "VN") ?: "VN"
    }

    fun setSelectedCountryCode(code: String) {
        prefs.edit().putString("selected_country_code", code).apply()
    }
}
