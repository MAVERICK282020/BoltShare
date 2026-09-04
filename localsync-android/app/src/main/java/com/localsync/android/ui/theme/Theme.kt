package com.localsync.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = SurfaceDark,
    primaryContainer = ElevatedDark,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    background = BackgroundBase,
    onBackground = TextPrimary,
    outline = CardBorder
)

@Composable
fun LocalSyncTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
