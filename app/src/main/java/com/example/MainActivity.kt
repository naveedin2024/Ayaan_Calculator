package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

private const val TAG = "CalculatorViewModel"

// Color Palette for Cosmic Obsidian Theme
val CosmicOnyx = Color(0xFF090A0E)
val CosmicSlate = Color(0xFF131620)
val CosmicSteel = Color(0xFF232A3B)
val LightAccentCyan = Color(0xFF00FFF0)
val DarkAccentCyan = Color(0xFF00C8FF)
val HighNeonOrange = Color(0xFFFF7B39)
val CosmicCoral = Color(0xFFFF5252)
val TextGray = Color(0xFF8E9AAA)
val HighWhite = Color(0xFFFFFFFF)

// History List Item Mode
data class HistoryItem(val equation: String, val result: String, val timestamp: Long = System.currentTimeMillis())

class CalculatorViewModel : ViewModel() {
    private val _expression = MutableStateFlow("0")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _historyExpanded = MutableStateFlow(false)
    val historyExpanded: StateFlow<Boolean> = _historyExpanded.asStateFlow()

    private val _scientificExpanded = MutableStateFlow(false)
    val scientificExpanded: StateFlow<Boolean> = _scientificExpanded.asStateFlow()

    private val _showCopyFeedback = MutableStateFlow(false)
    val showCopyFeedback: StateFlow<Boolean> = _showCopyFeedback.asStateFlow()

    val previewResult: StateFlow<String> = MutableStateFlow("")

    init {
        // Automatically compute previews
        _expression.value = "0"
    }

    fun onDigit(digit: String) {
        _historyExpanded.value = false
        val current = _expression.value
        if (current == "0" || current == "Error") {
            _expression.value = digit
            return
        }
        val tokens = current.split(" ")
        val lastToken = tokens.lastOrNull() ?: ""
        if (lastToken == "0") {
            val newList = tokens.toMutableList()
            newList[newList.lastIndex] = digit
            _expression.value = newList.joinToString(" ")
        } else if (lastToken == "-0") {
            val newList = tokens.toMutableList()
            newList[newList.lastIndex] = "-$digit"
            _expression.value = newList.joinToString(" ")
        } else {
            _expression.value = current + digit
        }
    }

    fun onOperator(op: String) {
        _historyExpanded.value = false
        val current = _expression.value
        if (current == "Error" || current.isEmpty()) {
            _expression.value = "0 $op "
            return
        }
        if (current.endsWith(" ")) {
            // Replace trailing space operator
            _expression.value = current.trimEnd().dropLast(1).trimEnd() + " $op "
        } else {
            _expression.value = current + " $op "
        }
    }

    fun onDecimal() {
        _historyExpanded.value = false
        val current = _expression.value
        if (current.isEmpty() || current.endsWith(" ")) {
            _expression.value = current + "0."
            return
        }
        val lastToken = current.split(" ").lastOrNull() ?: ""
        if (!lastToken.contains(".")) {
            _expression.value = current + "."
        }
    }

    fun onToggleSign() {
        _historyExpanded.value = false
        val current = _expression.value
        if (current.isEmpty() || current == "Error") return
        if (current.endsWith(" ")) {
            _expression.value = current + "-"
            return
        }
        val tokens = current.split(" ").toMutableList()
        val lastToken = tokens.lastOrNull() ?: ""
        if (lastToken.startsWith("-")) {
            tokens[tokens.lastIndex] = lastToken.substring(1)
        } else {
            tokens[tokens.lastIndex] = "-$lastToken"
        }
        _expression.value = tokens.joinToString(" ")
    }

    private fun transformLastToken(callerName: String, transform: (Double) -> Double?) {
        _historyExpanded.value = false
        val current = _expression.value
        if (current.isEmpty() || current.endsWith(" ") || current == "Error") return
        val tokens = current.split(" ").toMutableList()
        val lastToken = tokens.lastOrNull() ?: ""
        val num = lastToken.toDoubleOrNull()
        if (num == null) {
            Log.w(TAG, "$callerName: unable to parse '$lastToken' as a number")
            _expression.value = "Error"
            return
        }
        val result = transform(num)
        if (result == null) {
            _expression.value = "Error"
            return
        }
        tokens[tokens.lastIndex] = formatResult(result)
        _expression.value = tokens.joinToString(" ")
    }

    fun onPercentage() = transformLastToken("onPercentage") { it / 100.0 }

    fun onSquareRoot() = transformLastToken("onSquareRoot") { num ->
        if (num < 0) {
            Log.w(TAG, "onSquareRoot: cannot take square root of negative number $num")
            null
        } else Math.sqrt(num)
    }

    fun onSquare() = transformLastToken("onSquare") { it * it }

    fun onPi() {
        _historyExpanded.value = false
        val current = _expression.value
        if (current == "0" || current == "Error" || current.isEmpty()) {
            _expression.value = "3.14159265"
            return
        }
        if (current.endsWith(" ")) {
            _expression.value = current + "3.14159265"
        } else {
            val tokens = current.split(" ")
            val lastToken = tokens.lastOrNull() ?: ""
            if (lastToken == "0") {
                val newList = tokens.toMutableList()
                newList[newList.lastIndex] = "3.14159265"
                _expression.value = newList.joinToString(" ")
            } else {
                _expression.value = current + "3.14159265"
            }
        }
    }

    fun onBackspace() {
        _historyExpanded.value = false
        val current = _expression.value
        if (current.isEmpty() || current == "0" || current == "Error") {
            _expression.value = "0"
            return
        }
        
        if (current.endsWith(" ")) {
            _expression.value = current.trimEnd().dropLast(1).trimEnd()
        } else {
            val remain = current.dropLast(1)
            _expression.value = if (remain.isEmpty()) "0" else remain
        }
    }

    fun onClear() {
        _expression.value = "0"
        _historyExpanded.value = false
    }

    fun toggleHistory() {
        _historyExpanded.value = !_historyExpanded.value
    }

    fun toggleScientific() {
        _scientificExpanded.value = !_scientificExpanded.value
    }

    fun selectHistoryItem(item: HistoryItem) {
        _expression.value = item.result
        _historyExpanded.value = false
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    fun triggerCopyFeedback() {
        _showCopyFeedback.value = true
    }

    fun dismissCopyFeedback() {
        _showCopyFeedback.value = false
    }

    private fun cleanTrailingOperators(tokens: List<String>): MutableList<String> {
        val clean = tokens.toMutableList()
        while (clean.isNotEmpty() && clean.last() in listOf("+", "-", "×", "÷", "^")) {
            clean.removeAt(clean.size - 1)
        }
        return clean
    }

    fun onCalculate() {
        val current = _expression.value
        if (current == "Error" || current.isEmpty()) return
        try {
            val cleanTokens = cleanTrailingOperators(current.split(" "))
            if (cleanTokens.isEmpty()) return
            
            val value = evaluateTokens(cleanTokens)
            val formatted = formatResult(value)
            
            val trimmedExpr = current.trim()
            if (trimmedExpr != formatted && trimmedExpr.contains(" ")) {
                val historyItem = HistoryItem(trimmedExpr, formatted)
                _history.value = listOf(historyItem) + _history.value
            }
            
            _expression.value = formatted
        } catch (e: ArithmeticException) {
            Log.w(TAG, "onCalculate: arithmetic error evaluating '$current'", e)
            _expression.value = "Error"
        } catch (e: NumberFormatException) {
            Log.w(TAG, "onCalculate: malformed number in expression '$current'", e)
            _expression.value = "Error"
        } catch (e: Exception) {
            Log.e(TAG, "onCalculate: unexpected error evaluating '$current'", e)
            _expression.value = "Error"
        }
    }

    val realTimePreview: String
        get() {
            val current = _expression.value.trim()
            if (current.isEmpty() || !current.contains(" ") || current == "Error") return ""
            val cleanTokens = cleanTrailingOperators(current.split(" "))
            if (cleanTokens.size <= 1) return ""
            return try {
                val value = evaluateTokens(cleanTokens)
                "= " + formatResult(value)
            } catch (e: ArithmeticException) {
                Log.d(TAG, "realTimePreview: arithmetic error for '$current': ${e.message}")
                "= Error"
            } catch (e: NumberFormatException) {
                Log.d(TAG, "realTimePreview: malformed number in '$current': ${e.message}")
                "= Error"
            } catch (e: Exception) {
                Log.w(TAG, "realTimePreview: unexpected error for '$current'", e)
                ""
            }
        }

    private fun parseOperand(token: String): Double {
        return token.toDoubleOrNull()
            ?: throw NumberFormatException("Invalid number: '$token'")
    }

    private fun evaluateBinaryPass(
        tokens: MutableList<String>,
        operators: Set<String>,
        compute: (String, Double, Double) -> Double
    ) {
        var i = 0
        while (i < tokens.size) {
            if (tokens[i] in operators) {
                if (i > 0 && i < tokens.size - 1) {
                    val left = parseOperand(tokens[i - 1])
                    val right = parseOperand(tokens[i + 1])
                    tokens[i - 1] = compute(tokens[i], left, right).toString()
                    tokens.removeAt(i)
                    tokens.removeAt(i)
                } else if (i == 0 && tokens[i] == "-") {
                    if (tokens.size > 1) {
                        val next = parseOperand(tokens[i + 1])
                        tokens[i] = (-next).toString()
                        tokens.removeAt(i + 1)
                    } else {
                        throw ArithmeticException("Dangling '-' operator with no operand")
                    }
                } else {
                    throw ArithmeticException("Operator '${tokens[i]}' at invalid position $i")
                }
            } else {
                i++
            }
        }
    }

    private fun evaluateTokens(tokens: List<String>): Double {
        val t = tokens.toMutableList()

        evaluateBinaryPass(t, setOf("^")) { _, l, r -> Math.pow(l, r) }

        evaluateBinaryPass(t, setOf("×", "÷")) { op, l, r ->
            if (op == "×") l * r
            else {
                if (r == 0.0) throw ArithmeticException("Division by zero")
                l / r
            }
        }

        evaluateBinaryPass(t, setOf("+", "-")) { op, l, r ->
            if (op == "+") l + r else l - r
        }

        val result = t.firstOrNull()
            ?: throw ArithmeticException("Expression evaluated to empty result")
        return parseOperand(result)
    }

    private fun formatResult(value: Double): String {
        if (value.isInfinite()) return "Error"
        if (value.isNaN()) return "Error"
        
        // Integer check
        if (value % 1 == 0.0) {
            if (value >= 1e12 || value <= -1e12) {
                return String.format("%.6E", value)
            }
            return value.toLong().toString()
        }
        
        // Size limits
        if (Math.abs(value) >= 1e12 || (Math.abs(value) < 1e-6 && value != 0.0)) {
            return String.format("%.6E", value)
        }
        
        val formatted = String.format("%.8f", value)
        val cleaned = formatted.replace(Regex("0+$"), "").replace(Regex("\\.$"), "")
        return cleaned
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorTheme {
                CalculatorScreen()
            }
        }
    }
}

@Composable
fun CalculatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = LightAccentCyan,
            onPrimary = CosmicOnyx,
            secondary = HighNeonOrange,
            background = CosmicOnyx,
            surface = CosmicSlate
        ),
        content = content
    )
}

@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel = viewModel()) {
    val expression by viewModel.expression.collectAsState()
    val history by viewModel.history.collectAsState()
    val isHistoryExpanded by viewModel.historyExpanded.collectAsState()
    val isScientificExpanded by viewModel.scientificExpanded.collectAsState()
    val showCopyFeedback by viewModel.showCopyFeedback.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(showCopyFeedback) {
        if (showCopyFeedback) {
            delay(1800)
            viewModel.dismissCopyFeedback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CosmicOnyx, CosmicSlate)
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Operation Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(LightAccentCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        //text = "COSMIC AYAAN CALCULATOR",
                        text = "AYAAN CALCULATOR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HighWhite,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row {
                    // Scientific toggles
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleScientific()
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isScientificExpanded) CosmicSteel else Color.Transparent)
                            .testTag("toggle_scientific")
                    ) {
                        Text(
                            text = "f(x)",
                            fontWeight = FontWeight.Bold,
                            color = if (isScientificExpanded) LightAccentCyan else TextGray,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // History toggles
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleHistory()
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isHistoryExpanded) CosmicSteel else Color.Transparent)
                            .testTag("toggle_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History Log",
                            tint = if (isHistoryExpanded) LightAccentCyan else TextGray
                        )
                    }
                }
            }

            // Display Box (interactive with copy support & gestures)
            var dragAccumulator by remember { mutableStateOf(0f) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f, fill = true)
                    .padding(vertical = 12.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onDragEnd = {
                                if (dragAccumulator < -40f) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onBackspace()
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulator += dragAmount
                            }
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (expression != "0" && expression != "Error") {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                clipboardManager.setText(AnnotatedString(expression))
                                viewModel.triggerCopyFeedback()
                            }
                        }
                    ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0x3300FFF0)),
                colors = CardDefaults.cardColors(containerColor = Color(0x15131620))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.End
                    ) {
                        // Formula Equation
                        Text(
                            text = expression,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = if (expression.length > 12) 28.sp else 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighWhite,
                                textAlign = TextAlign.End,
                                fontFamily = FontFamily.SansSerif,
                                lineHeight = 44.sp
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("display_equation")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Realtime preview calculation
                        val preview = viewModel.realTimePreview
                        AnimatedVisibility(
                            visible = preview.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Text(
                                text = preview,
                                fontSize = 20.sp,
                                color = LightAccentCyan.copy(alpha = 0.75f),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("display_preview")
                            )
                        }
                    }

                    // Swipe hint
                    Text(
                        text = "Swipe to delete • Tap to copy",
                        fontSize = 10.sp,
                        color = TextGray.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }

            // Keyboard Grid Area with layered overlay panel for history drawer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.60f, fill = true)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Scientific row sliding
                    AnimatedVisibility(
                        visible = isScientificExpanded,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically { -it } + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val scientificKeys = listOf(
                                "√" to { viewModel.onSquareRoot() },
                                "x²" to { viewModel.onSquare() },
                                "^" to { viewModel.onOperator("^") },
                                "π" to { viewModel.onPi() }
                            )
                            val scientificTags = listOf("btn_sqrt", "btn_square", "btn_power", "btn_pi")
                            scientificKeys.forEachIndexed { idx, (label, action) ->
                                ScientificButton(text = label, isSelected = false, tag = scientificTags[idx]) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    action()
                                }
                            }
                        }
                    }

                    // Normal keys layout
                    val keyRows = listOf(
                        listOf("AC", "±", "%", "÷"),
                        listOf("7", "8", "9", "×"),
                        listOf("4", "5", "6", "-"),
                        listOf("1", "2", "3", "+"),
                        listOf("0", ".", "⌫", "=")
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (row in keyRows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (key in row) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        CalculatorKey(
                                            symbol = key,
                                            onClick = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                when (key) {
                                                    "AC" -> viewModel.onClear()
                                                    "±" -> viewModel.onToggleSign()
                                                    "%" -> viewModel.onPercentage()
                                                    "÷" -> viewModel.onOperator("÷")
                                                    "×" -> viewModel.onOperator("×")
                                                    "-" -> viewModel.onOperator("-")
                                                    "+" -> viewModel.onOperator("+")
                                                    "." -> viewModel.onDecimal()
                                                    "⌫" -> viewModel.onBackspace()
                                                    "=" -> viewModel.onCalculate()
                                                    else -> viewModel.onDigit(key)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // History Drawer sliding down as overlay taking over Keyboard space
        AnimatedVisibility(
            visible = isHistoryExpanded,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSlate),
                border = BorderStroke(1.dp, CosmicSteel)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Calculation Logs",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightAccentCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        if (history.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.clearHistory()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = CosmicCoral)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear All Logs",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Logs", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "No Logs",
                                    tint = TextGray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Equations will show up here",
                                    color = TextGray.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(history) { log ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x1A616161))
                                        .clickable {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.selectHistoryItem(log)
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = log.equation,
                                        fontSize = 13.sp,
                                        color = TextGray,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "= ${log.result}",
                                        fontSize = 16.sp,
                                        color = LightAccentCyan,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clip board Copy Feedback HUD
        AnimatedVisibility(
            visible = showCopyFeedback,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(containerColor = LightAccentCyan),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Result copied to clipboard",
                        color = CosmicOnyx,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun PressScaleBox(
    modifier: Modifier = Modifier,
    pressedScale: Float = 0.88f,
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    label: String = "press_scale",
    testTag: String? = null,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness),
        label = label
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun ScientificButton(
    text: String,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    PressScaleBox(
        modifier = Modifier
            .height(44.dp)
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) LightAccentCyan else CosmicSteel),
        pressedScale = 0.88f,
        label = "scientific_scale",
        testTag = tag,
        onClick = onClick
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) CosmicOnyx else LightAccentCyan
        )
    }
}

@Composable
fun CalculatorKey(
    symbol: String,
    onClick: () -> Unit
) {
    val isOperator = symbol in listOf("÷", "×", "-", "+", "=")
    val isFunction = symbol in listOf("AC", "±", "%", "⌫")

    val containerColor = when {
        isOperator -> HighNeonOrange
        isFunction -> Color(0x33232A3B)
        else -> CosmicSteel.copy(alpha = 0.45f)
    }

    val contentColor = when {
        isOperator -> HighWhite
        isFunction -> LightAccentCyan
        else -> HighWhite
    }

    PressScaleBox(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .padding(vertical = 12.dp),
        pressedScale = 0.85f,
        dampingRatio = Spring.DampingRatioMediumBouncy,
        label = "key_scale",
        testTag = "btn_$symbol",
        onClick = onClick
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = if (isFunction && symbol.length > 1) 18.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        )
    }
}
