package dev.dheirav.thirsttrap.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.OutlinedTextField as M3OutlinedTextField

/**
 * The one affordance core/ui never wrapped.
 *
 * Buttons, cards, chips, icon buttons and the rest all shadow their Material
 * name here so a screen converts by changing an import rather than by editing
 * call sites, and so a decision about how they look is made once. Text fields
 * were left out, and at 34 call sites across eleven screens they are the
 * largest single affordance in the app. That is the same shape of gap spacing
 * had before Space.kt: not drift away from a decision, but no place for the
 * decision to live.
 *
 * It showed. The edit form had five empty full-width boxes taking about a third
 * of the screen, each drawn at exactly the weight of the two that had content
 * in them, and there was nowhere to change that once.
 *
 * ## Empty fields are drawn quieter
 *
 * An empty optional field is the form offering something, and a filled one is
 * the form holding an answer. Those are not equally important and they were
 * drawn identically. The unfocused border fades while the field is empty and
 * returns to full weight as soon as there is anything in it. Focus is
 * untouched: a field you are typing in is the loudest thing on the page whether
 * or not it has content yet.
 *
 * The amount of fade is measured, not chosen by eye. `outlineVariant` was the
 * obvious answer and is wrong: it is the tone for a divider between filled
 * surfaces, and on this theme it renders an empty field's border at 1.7:1
 * against the page, where WCAG asks 3:1 for the boundary of a control you can
 * interact with. UI-SPEC section 8 says accessibility is not optional, so a
 * border nobody can find does not qualify as quieter. Fading `outline` itself
 * keeps the hue and lets the step be sized: at 0.7 it lands at 4.24:1 in dark
 * and 3.46:1 in light, and light is the binding case because its outline is
 * darker than its background rather than lighter. 0.6 passes dark at 3.42:1 and
 * fails light at 2.80:1, which is why this is not a rounder number.
 *
 * Everything else is Material's, deliberately. The floating label that moves
 * into the border when a field fills is behaviour every Android user already
 * reads as one control in two states, and departing from it would make this app
 * less familiar rather than more.
 */
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    colors: TextFieldColors = quietWhenEmpty(value),
) = M3OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    enabled = enabled,
    readOnly = readOnly,
    textStyle = textStyle,
    label = label,
    placeholder = placeholder,
    prefix = prefix,
    suffix = suffix,
    supportingText = supportingText,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    isError = isError,
    visualTransformation = visualTransformation,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    singleLine = singleLine,
    minLines = minLines,
    maxLines = maxLines,
    shape = MaterialTheme.shapes.extraSmall,
    colors = colors,
)

/**
 * The border one step down the outline ladder while the field holds nothing.
 *
 * Separate and public so a screen that genuinely wants Material's even weight,
 * or its own colours, can say so at the call site rather than reaching past the
 * wrapper for the raw component.
 */
@Composable
fun quietWhenEmpty(value: String): TextFieldColors = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(
        alpha = if (value.isEmpty()) EMPTY_BORDER_ALPHA else 1f,
    ),
)

/** See [OutlinedTextField]: the lowest fade that holds 3:1 in both schemes. */
private const val EMPTY_BORDER_ALPHA = 0.7f
