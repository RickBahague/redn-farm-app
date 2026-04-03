package com.redn.farm.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.abs

/**
 * Read-only amount field that opens [NumericPadBottomSheet] (dialpad icon + tap field).
 * Use for string-backed form state (e.g. preset spoilage text).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumericPadOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    padTitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    decimalEnabled: Boolean = true,
    maxDecimalPlaces: Int = 4,
    prefix: @Composable (() -> Unit)? = null,
) {
    var padOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) {
        if (pressed && enabled) {
            padOpen = true
            focusManager.clearFocus()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        label = label,
        modifier = modifier.fillMaxWidth(),
        supportingText = supportingText,
        isError = isError,
        singleLine = singleLine,
        prefix = prefix,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        interactionSource = interaction,
        trailingIcon = {
            IconButton(
                onClick = {
                    if (enabled) {
                        padOpen = true
                        focusManager.clearFocus()
                    }
                },
                enabled = enabled,
            ) {
                Icon(Icons.Filled.Dialpad, contentDescription = "Open numeric pad")
            }
        },
    )

    NumericPadBottomSheet(
        visible = padOpen,
        title = padTitle,
        value = value,
        onValueChange = onValueChange,
        decimalEnabled = decimalEnabled,
        maxDecimalPlaces = maxDecimalPlaces,
        onDismiss = { padOpen = false },
    )
}

/**
 * Same as [NumericPadOutlinedTextField] but binds a [Double] (e.g. hauling fee amount).
 * Keeps local text so incomplete input like `12.` is not overwritten by [LaunchedEffect] sync.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumericPadOutlinedTextFieldForDouble(
    value: Double,
    emptyWhenZero: Boolean,
    onValueChange: (Double) -> Unit,
    label: @Composable () -> Unit,
    padTitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    decimalEnabled: Boolean = true,
    maxDecimalPlaces: Int = 4,
    prefix: @Composable (() -> Unit)? = null,
    rememberKey: Any? = null,
) {
    fun format(d: Double) = when {
        emptyWhenZero && d == 0.0 -> ""
        else -> d.toString()
    }

    var text by remember(rememberKey) { mutableStateOf(format(value)) }

    LaunchedEffect(value) {
        val t = text.toDoubleOrNull()
        if (text.isNotEmpty() && t == null) return@LaunchedEffect
        if (t != null && abs(t - value) < 1e-9) return@LaunchedEffect
        if (text.isEmpty() && value == 0.0 && emptyWhenZero) return@LaunchedEffect
        text = format(value)
    }

    NumericPadOutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new
            when {
                new.isEmpty() -> onValueChange(0.0)
                else -> new.toDoubleOrNull()?.let { onValueChange(it) }
            }
        },
        label = label,
        padTitle = padTitle,
        modifier = modifier,
        enabled = enabled,
        supportingText = supportingText,
        isError = isError,
        singleLine = singleLine,
        decimalEnabled = decimalEnabled,
        maxDecimalPlaces = maxDecimalPlaces,
        prefix = prefix,
    )
}

/**
 * Optional [Double] (null = empty). Keeps incomplete decimals in local text while editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumericPadOutlinedTextFieldForNullableDouble(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    label: @Composable () -> Unit,
    padTitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    decimalEnabled: Boolean = true,
    maxDecimalPlaces: Int = 4,
    prefix: @Composable (() -> Unit)? = null,
    rememberKey: Any? = null,
) {
    fun format(d: Double?) = d?.toString() ?: ""

    var text by remember(rememberKey) { mutableStateOf(format(value)) }

    LaunchedEffect(value) {
        val t = text.toDoubleOrNull()
        if (text.isNotEmpty() && t == null) return@LaunchedEffect
        if (value == null && text.isEmpty()) return@LaunchedEffect
        if (value != null && t != null && abs(t - value) < 1e-9) return@LaunchedEffect
        text = format(value)
    }

    NumericPadOutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new
            onValueChange(new.toDoubleOrNull())
        },
        label = label,
        padTitle = padTitle,
        modifier = modifier,
        enabled = enabled,
        supportingText = supportingText,
        isError = isError,
        singleLine = singleLine,
        decimalEnabled = decimalEnabled,
        maxDecimalPlaces = maxDecimalPlaces,
        prefix = prefix,
    )
}
