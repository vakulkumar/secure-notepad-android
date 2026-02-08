package com.securenotes.presentation.ui.screen

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom PIN input screen for authentication.
 * Supports both regular PIN and duress PIN detection.
 */
@Composable
fun PinInputScreen(
    onPinEntered: (String) -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    isPinEnabled: Boolean = true,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    val maxLength = 6

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "Enter PIN",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Enter your PIN to unlock",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // PIN dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(maxLength) { index ->
                PinDot(filled = index < pin.length)
            }
        }

        // Error message
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Keypad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Row 1-2-3
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                PinButton("1") { if (pin.length < maxLength) pin += "1" }
                PinButton("2") { if (pin.length < maxLength) pin += "2" }
                PinButton("3") { if (pin.length < maxLength) pin += "3" }
            }
            // Row 4-5-6
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                PinButton("4") { if (pin.length < maxLength) pin += "4" }
                PinButton("5") { if (pin.length < maxLength) pin += "5" }
                PinButton("6") { if (pin.length < maxLength) pin += "6" }
            }
            // Row 7-8-9
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                PinButton("7") { if (pin.length < maxLength) pin += "7" }
                PinButton("8") { if (pin.length < maxLength) pin += "8" }
                PinButton("9") { if (pin.length < maxLength) pin += "9" }
            }
            // Row biometric-0-backspace
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                // Biometric button (optional)
                if (onBiometricClick != null) {
                    IconButton(
                        onClick = onBiometricClick,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Use Biometric",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(72.dp))
                }
                
                PinButton("0") { if (pin.length < maxLength) pin += "0" }
                
                // Backspace
                IconButton(
                    onClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Submit button
        if (pin.length >= 4) {
            TextButton(
                onClick = {
                    onPinEntered(pin)
                    pin = "" // Clear PIN after submission
                }
            ) {
                Text(
                    text = "Unlock",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PinDot(filled: Boolean) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(
                if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant
            )
    )
}

@Composable
private fun PinButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
