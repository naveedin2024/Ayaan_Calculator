package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CalculatorKey
import com.example.ui.components.ScientificButton
import com.example.ui.theme.*
import com.example.viewmodel.CalculatorViewModel
import kotlinx.coroutines.delay

@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel = viewModel()) {
    val expression by viewModel.expression.collectAsState()
    val history by viewModel.history.collectAsState()
    val isHistoryExpanded by viewModel.historyExpanded.collectAsState()
    val isScientificExpanded by viewModel.scientificExpanded.collectAsState()
    val showCopyFeedback by viewModel.showCopyFeedback.collectAsState()
    val preview by viewModel.realTimePreview.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

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
            TopRibbon(
                isScientificExpanded = isScientificExpanded,
                isHistoryExpanded = isHistoryExpanded,
                onToggleScientific = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleScientific()
                },
                onToggleHistory = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleHistory()
                }
            )

            // Display Box
            DisplayCard(
                expression = expression,
                preview = preview,
                onSwipeLeft = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.onBackspace()
                },
                onTapCopy = {
                    if (expression != "0" && expression != "Error") {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(AnnotatedString(expression))
                        viewModel.triggerCopyFeedback()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f, fill = true)
                    .padding(vertical = 12.dp)
            )

            // Keyboard Grid Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.60f, fill = true)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Scientific row
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
                            ScientificButton(text = "\u221A", isSelected = false, tag = "btn_sqrt") {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.onSquareRoot()
                            }
                            ScientificButton(text = "x\u00B2", isSelected = false, tag = "btn_square") {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.onSquare()
                            }
                            ScientificButton(text = "^", isSelected = false, tag = "btn_power") {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.onOperator("^")
                            }
                            ScientificButton(text = "\u03C0", isSelected = false, tag = "btn_pi") {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.onPi()
                            }
                        }
                    }

                    // Normal keys layout
                    KeypadGrid(
                        hapticFeedback = hapticFeedback,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // History Drawer
        HistoryDrawer(
            isExpanded = isHistoryExpanded,
            history = history,
            onSelectItem = { item ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.selectHistoryItem(item)
            },
            onClearHistory = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.clearHistory()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
        )

        // Copy Feedback HUD
        CopyFeedbackHud(
            visible = showCopyFeedback,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        )
    }
}

@Composable
private fun TopRibbon(
    isScientificExpanded: Boolean,
    isHistoryExpanded: Boolean,
    onToggleScientific: () -> Unit,
    onToggleHistory: () -> Unit
) {
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
                text = "AYAAN CALCULATOR",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HighWhite,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row {
            IconButton(
                onClick = onToggleScientific,
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

            IconButton(
                onClick = onToggleHistory,
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
}

@Composable
private fun DisplayCard(
    expression: String,
    preview: String,
    onSwipeLeft: () -> Unit,
    onTapCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccumulator by remember { mutableStateOf(0f) }
    Card(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDragEnd = {
                        if (dragAccumulator < -40f) {
                            onSwipeLeft()
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
                onClick = onTapCopy
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

            Text(
                text = "Swipe to delete \u2022 Tap to copy",
                fontSize = 10.sp,
                color = TextGray.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun KeypadGrid(
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val keyRows = listOf(
        listOf("AC", "\u00B1", "%", "\u00F7"),
        listOf("7", "8", "9", "\u00D7"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "\u232B", "=")
    )

    Column(
        modifier = modifier,
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
                                    "\u00B1" -> viewModel.onToggleSign()
                                    "%" -> viewModel.onPercentage()
                                    "\u00F7" -> viewModel.onOperator("\u00F7")
                                    "\u00D7" -> viewModel.onOperator("\u00D7")
                                    "-" -> viewModel.onOperator("-")
                                    "+" -> viewModel.onOperator("+")
                                    "." -> viewModel.onDecimal()
                                    "\u232B" -> viewModel.onBackspace()
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

@Composable
private fun HistoryDrawer(
    isExpanded: Boolean,
    history: List<com.example.viewmodel.HistoryItem>,
    onSelectItem: (com.example.viewmodel.HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
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
                            onClick = onClearHistory,
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
                                    .clickable { onSelectItem(log) }
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
}

@Composable
private fun CopyFeedbackHud(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
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
