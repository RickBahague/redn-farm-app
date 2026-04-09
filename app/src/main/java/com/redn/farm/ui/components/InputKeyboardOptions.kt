package com.redn.farm.ui.components

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

/**
 * Operational text entry without IME grammar/suggestions/autocomplete on typical keyboards.
 * Delegates to [dataEntryNoImeSuggestionsKeyboardOptions] — [KeyboardType.Ascii] alone is not
 * reliable on Gboard/Samsung-style IMEs (see **BUG-IME-01** / **BUG-IME-02**).
 */
fun alphaNumericKeyboardOptions(imeAction: ImeAction): KeyboardOptions =
    dataEntryNoImeSuggestionsKeyboardOptions(imeAction)

/**
 * Password **input class** without masking: suppresses spell/grammar/autocomplete on many IMEs.
 * Do not combine with [androidx.compose.ui.text.input.PasswordVisualTransformation] unless the
 * field should actually hide characters.
 */
fun dataEntryNoImeSuggestionsKeyboardOptions(imeAction: ImeAction): KeyboardOptions =
    KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
    )

/**
 * Login username — same keyboard class as [dataEntryNoImeSuggestionsKeyboardOptions].
 */
fun loginUsernameKeyboardOptions(): KeyboardOptions =
    dataEntryNoImeSuggestionsKeyboardOptions(ImeAction.Next)

