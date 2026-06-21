package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CosmicSteel
import com.example.ui.theme.HighNeonOrange
import com.example.ui.theme.HighWhite
import com.example.ui.theme.LightAccentCyan

@Composable
fun CalculatorKey(
    symbol: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "key_scale"
    )

    val isOperator = symbol in listOf("\u00F7", "\u00D7", "-", "+", "=")
    val isFunction = symbol in listOf("AC", "\u00B1", "%", "\u232B")

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

    Box(
        modifier = Modifier
            .scale(scale)
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp)
            .testTag("btn_$symbol"),
        contentAlignment = Alignment.Center
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
