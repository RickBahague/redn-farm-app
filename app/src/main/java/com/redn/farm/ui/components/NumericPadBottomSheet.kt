package com.redn.farm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumericPadBottomSheet(
    visible: Boolean,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    decimalEnabled: Boolean,
    maxDecimalPlaces: Int,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    // IMPORTANT: This must be able to appear above `AlertDialog` flows.
    // `ModalBottomSheet` is hosted in the activity window, but `AlertDialog` is its own dialog window.
    // When NumericPad is triggered from inside an AlertDialog, a bottom sheet can end up behind the dialog.
    // Rendering as a Dialog keeps it on top consistently.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                tonalElevation = 2.dp,
            ) {
                NumericPadContent(
                    title = title,
                    value = value,
                    onValueChange = onValueChange,
                    decimalEnabled = decimalEnabled,
                    maxDecimalPlaces = maxDecimalPlaces,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

private fun appendDigit(
    current: String,
    digit: Char,
    maxDecimalPlaces: Int
): String {
    val d = digit
    if (d !in '0'..'9') return current

    if (current.isEmpty()) {
        return if (d == '0') "0" else d.toString()
    }

    // If current is just "0" and it has no decimal part, replace it.
    if (current == "0") return d.toString()

    val dotIndex = current.indexOf('.')
    return if (dotIndex >= 0) {
        val decimals = current.length - dotIndex - 1
        if (decimals >= maxDecimalPlaces) current else current + d
    } else {
        current + d
    }
}

private fun appendDecimal(
    current: String,
    decimalEnabled: Boolean
): String {
    if (!decimalEnabled) return current
    if (current.contains('.')) return current
    return if (current.isEmpty()) "0." else current + "."
}

private fun backspace(current: String): String {
    if (current.isEmpty()) return ""
    if (current.length == 1) return ""
    return current.dropLast(1)
}

@Composable
private fun NumericPadContent(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    decimalEnabled: Boolean,
    maxDecimalPlaces: Int,
    onDismiss: () -> Unit,
) {
    val displayValue = if (value.isEmpty()) "0" else value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(),
        ) {
            Text(
                text = displayValue,
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val rows = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
                listOf('0', if (decimalEnabled) '.' else null, null),
            )

            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { key ->
                        val weight = 1f
                        if (key == null) {
                            Spacer(modifier = Modifier.weight(weight))
                        } else {
                            NumericKeyButton(
                                text = key.toString(),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val next = if (key == '.') {
                                        appendDecimal(
                                            current = value,
                                            decimalEnabled = decimalEnabled
                                        )
                                    } else {
                                        appendDigit(
                                            current = value,
                                            digit = key,
                                            maxDecimalPlaces = maxDecimalPlaces
                                        )
                                    }
                                    onValueChange(next)
                                }
                            )
                        }
                    }
                }

                if (rowIndex == 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = { onValueChange(backspace(value)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Filled.Backspace,
                                contentDescription = "Backspace",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        OutlinedButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun NumericKeyButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

