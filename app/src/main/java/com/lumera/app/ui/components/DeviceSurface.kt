package com.lumera.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*

/** Device mode, rather than screen width, keeps Shield's remote UI unchanged. */
@Composable
fun isPhoneUi(): Boolean =
    LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK != Configuration.UI_MODE_TYPE_TELEVISION

/** TV Material surfaces handle D-pad clicks only. Add real pointer input on handhelds. */
@Composable
fun DeviceSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    tonalElevation: Dp = 0.dp,
    shape: ClickableSurfaceShape = ClickableSurfaceDefaults.shape(),
    colors: ClickableSurfaceColors = ClickableSurfaceDefaults.colors(),
    scale: ClickableSurfaceScale = ClickableSurfaceDefaults.scale(),
    border: ClickableSurfaceBorder = ClickableSurfaceDefaults.border(),
    glow: ClickableSurfaceGlow = ClickableSurfaceDefaults.glow(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = if (isPhoneUi()) modifier.clickable(enabled = enabled, onClick = onClick) else modifier,
        onLongClick = onLongClick, enabled = enabled, tonalElevation = tonalElevation,
        shape = shape, colors = colors, scale = scale, border = border, glow = glow,
        interactionSource = interactionSource, content = content
    )
}
