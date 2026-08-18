package com.songsit.fuellogpro.ui.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.songsit.fuellogpro.R

/**
 * One-time concept screen from the mockup — shown once, before a vehicle exists / before the
 * user has signed in. Both actions call straight into the same callbacks FuelLogApp already
 * wires to GoogleAuthRepository (onGoogleSignIn) and to entering the app (onSkip); no new
 * auth/data logic here.
 */
@Composable
fun ProOnboardingScreen(
    averageKmPerLiterLabel: String?,
    onGoogleSignIn: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Soft decorative glows — radial gradients fading to transparent, not a hard-edged shape.
        // Modifier.blur needs API 31+ (minSdk here is 26), so the "soft" look comes from the
        // gradient's own fade instead of a blur pass — works on every supported device.
        Box(
            Modifier
                .size(260.dp)
                .offset(x = (-70).dp, y = (-50).dp)
                .background(
                    Brush.radialGradient(listOf(primary.copy(alpha = 0.24f), Color.Transparent)),
                    shape = CircleShape,
                ),
        )
        Box(
            Modifier
                .size(240.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 70.dp)
                .background(
                    Brush.radialGradient(listOf(secondary.copy(alpha = 0.22f), Color.Transparent)),
                    shape = CircleShape,
                ),
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 26.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                }
                Text("FuelLog", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        stringResource(R.string.pro_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Glow ring behind the hero circle, same fade trick as the background blobs.
                    Box(
                        Modifier
                            .size(196.dp)
                            .background(
                                Brush.radialGradient(listOf(primary.copy(alpha = 0.30f), Color.Transparent)),
                                shape = CircleShape,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(primary, secondary)))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            averageKmPerLiterLabel ?: "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.onboarding_headline),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    OnboardingFeature(
                        Icons.Filled.LocalGasStation,
                        stringResource(R.string.onboarding_feature_log_fuel),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        tint = primary,
                    )
                    OnboardingFeature(
                        Icons.Filled.WaterDrop,
                        stringResource(R.string.onboarding_feature_track_expenses),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        tint = secondary,
                    )
                    OnboardingFeature(
                        Icons.Filled.Build,
                        stringResource(R.string.onboarding_feature_maintenance_alerts),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        tint = primary,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onGoogleSignIn),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.onboarding_sign_in_google),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                }
                Text(
                    stringResource(R.string.onboarding_skip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onSkip).padding(bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun OnboardingFeature(icon: ImageVector, label: String, containerColor: Color, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(containerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
