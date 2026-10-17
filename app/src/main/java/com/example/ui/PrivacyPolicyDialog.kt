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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.theme.PnbCardStroke
import com.example.ui.theme.PnbPrimary
import com.example.ui.theme.PnbPrimaryDark
import com.example.ui.theme.PnbSecondary
import com.example.ui.theme.PnbSuccess
import com.example.ui.theme.PnbTertiary

@Composable
fun PrivacyPolicyDialog(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("privacy_policy_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = PnbTertiary.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Privacy Policy",
                            tint = PnbPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = AppStrings.get("privacy_policy_title", language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Google Play Developer Policy Compliant",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp)
            ) {
                // Section 1: Non-Affiliation Disclaimer Banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Disclaimer",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Important Non-Affiliation Disclaimer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = AppStrings.get("disclaimer_text", language),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                PolicySectionItem(
                    icon = Icons.Default.Security,
                    iconTint = PnbSuccess,
                    title = "1. Zero Remote Data Collection",
                    content = "We do NOT operate any remote server, database, or backend API that stores your credentials, transactions, or banking data. All user interactions occur directly between your device and the official bank portal via standard SSL encrypted connections."
                )

                PolicySectionItem(
                    icon = Icons.Default.Lock,
                    iconTint = PnbPrimary,
                    title = "2. On-Device Keystore Encryption (AES-256)",
                    content = "If you choose to save your Merchant User ID and Password for the 1-Tap Auto-fill feature, they are encrypted locally on your physical hardware using the Android Keystore Master Key (AES-256-GCM). Your credentials never leave your device."
                )

                PolicySectionItem(
                    icon = Icons.Default.AccountBalance,
                    iconTint = PnbSecondary,
                    title = "3. Webview Utility Browser Functionality",
                    content = "This application acts as a custom web wrapper and productivity launcher for merchants accessing the publicly accessible PNB RTSP web portal. The app does not intercept, modify, or proxy your financial transactions."
                )

                PolicySectionItem(
                    icon = Icons.Default.Policy,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "4. Third-Party Advertising (Google AdMob)",
                    content = "This app displays contextual advertising through Google AdMob SDK. Google AdMob may collect standard non-identifying telemetry (such as device type, coarse location, and advertising ID) in accordance with Google's Privacy Policy."
                )

                PolicySectionItem(
                    icon = Icons.Default.CheckCircle,
                    iconTint = PnbSuccess,
                    title = "5. User Control & Instant Data Deletion",
                    content = "You maintain complete ownership of your data. You can delete your saved login credentials at any time by tapping 'Manage Saved Login' > 'Clear' or by clearing the app data in Android system settings."
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Developer Contact & Policy Inquiries:\nFor questions or support, please contact the developer via the official Google Play store listing.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Effective Date: August 2026 • Version 1.0",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PnbPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(AppStrings.get("agree_close", language))
            }
        }
    )
}

@Composable
private fun PolicySectionItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    content: String
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            lineHeight = 16.sp
        )
    }
}
