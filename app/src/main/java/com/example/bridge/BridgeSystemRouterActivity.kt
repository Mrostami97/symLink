package com.example.bridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassBridgeCard
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizeDigits
import com.example.ui.i18n.stringsForLanguage
import com.example.ui.theme.ConnectedEmerald
import com.example.ui.theme.ITellTheme
import com.example.ui.theme.MonoTechnicalStyle
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.Sim2Amber

/**
 * Lightweight system handoff sheet invoked directly when the user taps Call (tel:) or SMS (smsto:)
 * from the phone's default Contacts, Phone, or Messages apps and selects iTell Bridge.
 */
class BridgeSystemRouterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val action = intent?.action.orEmpty()
        val dataUri: Uri? = intent?.data
        val scheme = dataUri?.scheme.orEmpty().lowercase()
        val targetNumber = DeviceTelemetryReader.normalizePhoneNumber(dataUri?.schemeSpecificPart.orEmpty())
        val prefilledSmsBody = intent?.getStringExtra("sms_body")
            ?: intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?: ""
        val isSmsMode = scheme == "sms" || scheme == "smsto" || action == Intent.ACTION_SENDTO

        setContent {
            val strings = remember { stringsForLanguage(BridgeSessionHub.languageCode) }
            val layoutDir = if (strings.isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr
            var selectedSim by remember { mutableIntStateOf(BridgeSessionHub.preferredSimSlot) }
            var smsBody by remember { mutableStateOf(prefilledSmsBody) }
            val telemetry = BridgeSessionHub.latestPeerTelemetry
            val sim1Name = telemetry?.simCards?.firstOrNull { it.slotIndex == 1 }?.carrierName ?: "SIM 1"
            val sim2Name = telemetry?.simCards?.firstOrNull { it.slotIndex == 2 }?.carrierName ?: "SIM 2"

            ITellTheme(darkTheme = BridgeSessionHub.darkTheme) {
                CompositionLocalProvider(
                    LocalAppStrings provides strings,
                    LocalLayoutDirection provides layoutDir
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xB8070A10))
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(18.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        GlassBridgeCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = if (isSmsMode) Sim2Amber else ConnectedEmerald,
                            accentStrength = 0.6f,
                            cornerRadius = 24.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = if (isSmsMode) strings.smsHandoffMode else strings.sendDialToHost,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (isSmsMode) Sim2Amber else ConnectedEmerald
                                )
                                Text(
                                    text = targetNumber.ifBlank { strings.dialerPlaceholder },
                                    style = MonoTechnicalStyle,
                                    color = SignalCyan
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedSim == 1,
                                        onClick = { selectedSim = 1 },
                                        label = { Text(sim1Name) },
                                        leadingIcon = { Icon(Icons.Filled.SimCard, contentDescription = null) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = selectedSim == 2,
                                        onClick = { selectedSim = 2 },
                                        label = { Text(sim2Name) },
                                        leadingIcon = { Icon(Icons.Filled.SimCard, contentDescription = null) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (isSmsMode) {
                                    OutlinedTextField(
                                        value = smsBody,
                                        onValueChange = { smsBody = it },
                                        placeholder = { Text(strings.messageInputHint) },
                                        maxLines = 4,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val ok = if (isSmsMode) {
                                                BridgeSessionHub.sendRemoteSmsDraft(
                                                    applicationContext,
                                                    targetNumber,
                                                    smsBody,
                                                    selectedSim
                                                )
                                            } else {
                                                BridgeSessionHub.sendRemoteDial(
                                                    applicationContext,
                                                    targetNumber,
                                                    selectedSim
                                                )
                                            }
                                            Toast.makeText(
                                                this@BridgeSystemRouterActivity,
                                                if (ok) "${strings.statusSent} • SIM $selectedSim".localizeDigits(strings.isPersian)
                                                else strings.connectFirstHint,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            finish()
                                        },
                                        enabled = targetNumber.isNotBlank() && (!isSmsMode || smsBody.isNotBlank()),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSmsMode) Sim2Amber else ConnectedEmerald,
                                            contentColor = Color(0xFF002419)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isSmsMode) Icons.AutoMirrored.Filled.Send else Icons.Filled.Call,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (isSmsMode) strings.send else strings.sendDialToHost,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { finish() },
                                        modifier = Modifier.height(50.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(strings.dismiss)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
