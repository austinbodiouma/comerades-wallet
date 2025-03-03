package com.example.commeradeswallet.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.commeradeswallet.ui.theme.CommeradesWalletTheme

@Preview(
    name = "Light Mode",
    showBackground = true
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
annotation class ThemePreviews

@Composable
fun PreviewWrapper(
    content: @Composable () -> Unit
) {
    CommeradesWalletTheme {
        content()
    }
} 