package com.example.passagenexpress.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme

private val KeyHeight: Dp = 72.dp
// Cantos menos arredondados — visual mais "físico", parece tecla mesmo em vez de pílula.
private val KeyShape = RoundedCornerShape(6.dp)

/**
 * Numeric keypad (0–9 + backspace) shaped for totem touch input.
 *
 * Use when the field accepts digits only (CPF, telefone, data, código).
 */
@Composable
fun TotemNumericKeypad(
    label: String,
    currentValue: String,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
    doneLabel: String,
    closeContentDesc: String,
    deleteContentDesc: String,
    modifier: Modifier = Modifier,
) {
    KeypadFrame(
        label = label,
        currentValue = currentValue,
        onClose = onClose,
        onDone = onDone,
        doneLabel = doneLabel,
        closeContentDesc = closeContentDesc,
        modifier = modifier,
    ) {
        NumericGrid(
            onDigit = onDigit,
            onBackspace = onBackspace,
            deleteContentDesc = deleteContentDesc,
        )
    }
}

/**
 * Alphanumeric keypad (QWERTY PT-BR) with:
 *  - shift toggle for uppercase
 *  - ?123 toggle for numbers/symbols
 *  - accent strip (á é í ó ú ã õ ç) in letters mode
 *  - space + backspace
 *
 * Caller receives single chars via [onChar] and concatenates as needed.
 */
@Composable
fun TotemAlphaKeypad(
    label: String,
    currentValue: String,
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
    doneLabel: String,
    closeContentDesc: String,
    deleteContentDesc: String,
    spaceContentDesc: String,
    shiftContentDesc: String,
    modifier: Modifier = Modifier,
) {
    var shift by remember { mutableStateOf(false) }
    var symbols by remember { mutableStateOf(false) }

    KeypadFrame(
        label = label,
        currentValue = currentValue,
        onClose = onClose,
        onDone = onDone,
        doneLabel = doneLabel,
        closeContentDesc = closeContentDesc,
        // O Concluir vai como tecla própria (à direita da barra de espaço), então a barra
        // full-width do frame é escondida — era ela que os usuários confundiam com o espaço.
        showDoneButton = false,
        modifier = modifier,
    ) {
        if (symbols) {
            SymbolGrid(
                onChar = onChar,
                onBackspace = onBackspace,
                onToggleLetters = { symbols = false },
                onSpace = { onChar(' ') },
                onDone = onDone,
                doneContentDesc = doneLabel,
                spaceContentDesc = spaceContentDesc,
                deleteContentDesc = deleteContentDesc,
            )
        } else {
            LetterGrid(
                shift = shift,
                onChar = { c ->
                    onChar(if (shift) c.uppercaseChar() else c)
                    if (shift) shift = false
                },
                onShift = { shift = !shift },
                onBackspace = onBackspace,
                onToggleSymbols = { symbols = true },
                onSpace = { onChar(' ') },
                onDone = onDone,
                doneContentDesc = doneLabel,
                shiftContentDesc = shiftContentDesc,
                spaceContentDesc = spaceContentDesc,
                deleteContentDesc = deleteContentDesc,
            )
        }
    }
}

@Composable
private fun KeypadFrame(
    label: String,
    currentValue: String,
    onClose: () -> Unit,
    onDone: () -> Unit,
    doneLabel: String,
    closeContentDesc: String,
    modifier: Modifier = Modifier,
    showDoneButton: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Fundo do painel: azul translúcido (mesmo dos chips/pills da marca) — dá contraste pras
    // teclas brancas sem competir com o conteúdo principal da tela.
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TotemPalette.AccentTint
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TotemTheme.dimens.space20),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = TotemPalette.InkMuted,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentValue,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TotemPalette.Ink,
                        )
                        BlinkingCursor()
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = closeContentDesc,
                        tint = TotemPalette.InkMuted,
                    )
                }
            }
            content()
            if (showDoneButton) {
                TotemPrimaryButton(
                    text = doneLabel,
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Cursor vertical piscando, ao lado do texto digitado. */
@Composable
private fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )
    Box(
        modifier = Modifier
            .padding(start = 3.dp)
            .size(width = 2.dp, height = 28.dp)
            .alpha(alpha)
            .background(TotemPalette.Accent, RoundedCornerShape(1.dp)),
    )
}

@Composable
private fun NumericGrid(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    deleteContentDesc: String,
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )
    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8)) {
        rows.forEach { row ->
            KeyRow {
                row.forEach { c -> CharKey(c.toString(), { onDigit(c) }, modifier = Modifier.weight(1f)) }
            }
        }
        KeyRow {
            Spacer(Modifier.weight(1f))
            CharKey("0", { onDigit('0') }, modifier = Modifier.weight(1f))
            BackspaceKey(onBackspace, deleteContentDesc, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LetterGrid(
    shift: Boolean,
    onChar: (Char) -> Unit,
    onShift: () -> Unit,
    onBackspace: () -> Unit,
    onToggleSymbols: () -> Unit,
    onSpace: () -> Unit,
    onDone: () -> Unit,
    doneContentDesc: String,
    shiftContentDesc: String,
    spaceContentDesc: String,
    deleteContentDesc: String,
) {
    val row1 = "qwertyuiop".toList()
    val row2 = "asdfghjklç".toList()
    val row3 = "zxcvbnm".toList()
    val accents = "áéíóúãõ".toList()

    fun display(c: Char) = if (shift) c.uppercaseChar() else c

    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8)) {
        KeyRow {
            row1.forEach { c ->
                CharKey(display(c).toString(), { onChar(c) }, modifier = Modifier.weight(1f))
            }
        }
        KeyRow {
            row2.forEach { c ->
                CharKey(display(c).toString(), { onChar(c) }, modifier = Modifier.weight(1f))
            }
        }
        KeyRow {
            ShiftKey(
                active = shift,
                onClick = onShift,
                contentDescription = shiftContentDesc,
                modifier = Modifier.weight(1.4f),
            )
            row3.forEach { c ->
                CharKey(display(c).toString(), { onChar(c) }, modifier = Modifier.weight(1f))
            }
            BackspaceKey(
                onBackspace = onBackspace,
                contentDescription = deleteContentDesc,
                modifier = Modifier.weight(1.4f),
            )
        }
        // Faixa de acentos precompostos (adaptação ABNT para o totem).
        KeyRow {
            accents.forEach { c ->
                CharKey(display(c).toString(), { onChar(c) }, modifier = Modifier.weight(1f))
            }
        }
        // Linha de ação padrão: ?123 | ESPAÇO (barra larga) | Concluir ✓.
        KeyRow {
            UtilityKey(
                text = "?123",
                onClick = onToggleSymbols,
                modifier = Modifier.weight(1.5f),
            )
            SpaceKey(
                onClick = onSpace,
                contentDescription = spaceContentDesc,
                modifier = Modifier.weight(6f),
            )
            DoneKey(
                onClick = onDone,
                contentDescription = doneContentDesc,
                modifier = Modifier.weight(1.5f),
            )
        }
    }
}

@Composable
private fun SymbolGrid(
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onToggleLetters: () -> Unit,
    onSpace: () -> Unit,
    onDone: () -> Unit,
    doneContentDesc: String,
    spaceContentDesc: String,
    deleteContentDesc: String,
) {
    val row1 = "1234567890".toList()
    val row2 = "@#\$&*-+=/_".toList()
    val row3 = "!?.,;:()'\"".toList()

    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8)) {
        KeyRow {
            row1.forEach { c -> CharKey(c.toString(), { onChar(c) }, modifier = Modifier.weight(1f)) }
        }
        KeyRow {
            row2.forEach { c -> CharKey(c.toString(), { onChar(c) }, modifier = Modifier.weight(1f)) }
        }
        KeyRow {
            row3.forEach { c -> CharKey(c.toString(), { onChar(c) }, modifier = Modifier.weight(1f)) }
        }
        KeyRow {
            UtilityKey(
                text = "ABC",
                onClick = onToggleLetters,
                modifier = Modifier.weight(1.2f),
            )
            SpaceKey(
                onClick = onSpace,
                contentDescription = spaceContentDesc,
                modifier = Modifier.weight(4f),
            )
            BackspaceKey(
                onBackspace = onBackspace,
                contentDescription = deleteContentDesc,
                modifier = Modifier.weight(1.2f),
            )
            DoneKey(
                onClick = onDone,
                contentDescription = doneContentDesc,
                modifier = Modifier.weight(1.2f),
            )
        }
    }
}

@Composable
private fun KeyRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
    ) { content() }
}

/**
 * Wrapper de tecla. Feedback de press fica por conta do ripple interno do Surface clicável —
 * sem `graphicsLayer`/scale (criava artefato de borda fora-da-forma) nem tint manual.
 */
@Composable
private fun KeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    baseColor: Color = TotemPalette.Paper,
    contentColor: Color = TotemPalette.Ink,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(KeyHeight),
        shape = KeyShape,
        color = baseColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun CharKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeyButton(onClick = onClick, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ShiftKey(
    active: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val bg = if (active) TotemPalette.Ink else TotemPalette.Paper
    val fg = if (active) TotemPalette.Paper else TotemPalette.Ink
    KeyButton(
        onClick = onClick,
        modifier = modifier,
        baseColor = bg,
        contentColor = fg,
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = contentDescription,
            tint = fg,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun SpaceKey(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    KeyButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.SpaceBar,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp),
        )
    }
}

/** Tecla de concluir — cor de destaque + ✓, distinta da barra de espaço. */
@Composable
private fun DoneKey(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    KeyButton(
        onClick = onClick,
        modifier = modifier,
        baseColor = TotemPalette.Accent,
        contentColor = TotemPalette.Paper,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = contentDescription,
            tint = TotemPalette.Paper,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun UtilityKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeyButton(onClick = onClick, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BackspaceKey(
    onBackspace: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    KeyButton(onClick = onBackspace, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Preview(name = "Numeric keypad", widthDp = 800, heightDp = 520)
@Composable
private fun TotemNumericKeypadPreview() {
    TotemTheme {
        Box(modifier = Modifier.background(Color.White).padding(16.dp)) {
            TotemNumericKeypad(
                label = "CPF",
                currentValue = "123.456.789",
                onDigit = {},
                onBackspace = {},
                onClose = {},
                onDone = {},
                doneLabel = "Concluído",
                closeContentDesc = "Fechar",
                deleteContentDesc = "Apagar",
            )
        }
    }
}

@Preview(name = "Alphanumeric keypad", widthDp = 800, heightDp = 640)
@Composable
private fun TotemAlphaKeypadPreview() {
    TotemTheme {
        Box(modifier = Modifier.background(Color.White).padding(16.dp)) {
            TotemAlphaKeypad(
                label = "Nome",
                currentValue = "José",
                onChar = {},
                onBackspace = {},
                onClose = {},
                onDone = {},
                doneLabel = "Concluído",
                closeContentDesc = "Fechar",
                deleteContentDesc = "Apagar",
                spaceContentDesc = "Espaço",
                shiftContentDesc = "Maiúsculas",
            )
        }
    }
}
