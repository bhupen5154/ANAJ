package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.theme.PnbCardStroke
import com.example.ui.theme.PnbGoldLight
import com.example.ui.theme.PnbPrimary
import com.example.ui.theme.PnbPrimaryDark
import com.example.ui.theme.PnbSecondary
import com.example.ui.theme.PnbSuccess
import com.example.ui.theme.PnbTertiary
import com.example.update.AppUpdateState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateDialog(
    updateState: AppUpdateState,
    language: AppLanguage,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val isForced = updateState.isForced

    AlertDialog(
        onDismissRequest = {
            if (!isForced) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForced,
            dismissOnClickOutside = !isForced
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mandatory_update_dialog"),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Rocket / System Update Icon with Gradient Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PnbPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(72.dp)
                    ) {}

                    Surface(
                        shape = CircleShape,
                        color = PnbPrimary,
                        modifier = Modifier.size(54.dp),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // Mandatory Badge
                if (isForced) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFD32F2F).copy(alpha = 0.12f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = AppStrings.get("update_mandatory_badge", language),
                                color = Color(0xFFD32F2F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Main Title
                Text(
                    text = AppStrings.get("update_dialog_title", language),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Description
                Text(
                    text = AppStrings.get("update_dialog_desc", language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Version Comparison Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = AppStrings.get("update_current_version", language),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.5.sp
                            )
                            Text(
                                text = "v${updateState.currentVersionName} (${updateState.currentVersionCode})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = PnbPrimary,
                            modifier = Modifier.size(18.dp)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = AppStrings.get("update_latest_version", language),
                                style = MaterialTheme.typography.labelSmall,
                                color = PnbSuccess,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "v${updateState.latestVersionName} (${updateState.latestVersionCode})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = PnbSuccess
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // What's New Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PnbPrimary.copy(alpha = 0.06f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PnbPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.get("update_whats_new", language),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PnbPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (updateState.releaseNotes.isNotBlank()) {
                                updateState.releaseNotes
                            } else {
                                AppStrings.get("update_whats_new_desc", language)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onUpdateClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_update_now_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PnbPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Upgrade,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = PnbGoldLight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("update_now_btn", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                }

                if (!isForced) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("dialog_update_later_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = AppStrings.get("update_later_btn", language),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        dismissButton = null
    )
}
