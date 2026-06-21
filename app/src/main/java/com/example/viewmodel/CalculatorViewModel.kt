package com.example.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HistoryItem(
    val equation: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

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

    private val _realTimePreview = MutableStateFlow("")
    val realTimePreview: StateFlow<String> = _realTimePreview.asStateFlow()

    companion object {
        private const val MAX_HISTORY_SIZE = 100
        private val PI_STRING = Math.PI.toString()
    }

    init {
        _expression.value = "0"
    }

    private fun updatePreview() {
        val current = _expression.value.trim()
        if (current.isEmpty() || !current.contains(" ") || current == "Error") {
            _realTimePreview.value = ""
            return
        }
        val tokens = current.split(" ")
        val cleanTokens = tokens.toMutableList()
        while (cleanTokens.isNotEmpty() && cleanTokens.last() in listOf("+", "-", "\u00D7", "\u00F7", "^")) {
            cleanTokens.removeAt(cleanTokens.size - 1)
        }
        if (cleanTokens.size <= 1) {
            _realTimePreview.value = ""
            return
        }
        _realTimePreview.value = try {
            val value = evaluateTokens(cleanTokens)
            val formatted = formatResult(value)
            if (formatted == "Error") "" else "= $formatted"
        } catch (e: Exception) {
            ""
        }
    }

    fun onDigit(digit: String) {
        _historyExpanded.value = false
        val current = _expression.value
        if (current == "0" || current == "Error") {
            _expression.value = digit
            updatePreview()
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
        updatePreview()
    }

    fun onOperator(op: String) {
        _historyExpanded.value = false
        val current = _expression.value
        if (current == "Error" || current.isEmpty()) {
            _expression.value = "0 $op "
            updatePreview()
            return
        }
        if (current.endsWith(" ")) {
            val trimmed = current.trimEnd()
            val lastSpaceIdx = trimmed.lastIndexOf(' ')
            val base = if (lastSpaceIdx >= 0) trimmed.substring(0, lastSpaceIdx) else trimmed
            _expression.value = "$base $op "
        } else {
            _expression.value = "$current $op "
        }
        updatePreview()
    }

    fun onDecimal() {
        _historyExpanded.value = false
        val current = _expression.value
        if (current.isEmpty() || current.endsWith(" ")) {
            _expression.value = current + "0."
            updatePreview()
            return
        }
        val lastToken = current.split(" ").lastOrNull() ?: ""
        if (!lastToken.contains(".")) {
            _expression.value = current + "."
        }
        updatePreview()
    }

    fun onToggleSign() {
        _historyExpanded.value = false
        val current = _expression.value
        if (current.isEmpty() || current == "Error") return
        if (current.endsWith(" ")) {
            _expression.value = current + "-0"
            updatePreview()
            return
        }
        val tokens = current.split(" ").toMutableList()
        val lastToken = tokens.lastOrNull() ?: return
        if (lastToken == "-" || lastToken.isEmpty()) return
        if (lastToken.startsWith("-")) {
            tokens[tokens.lastIndex] = lastToken.substring(1)
        } else {
            tokens[tokens.lastIndex] = "-$lastToken"
        }
        _expression.value = tokens.joinToString(" ")
        updatePreview()
    }

    fun onPercentage() {
        _historyExpanded.value = false
        val current = _expression.value
        if (current.isEmpty() || current.endsWith(" ") || current == "Error") return
        val tokens = current.split(" ").toMutableList()
        val lastToken = tokens.lastOrNull() ?: ""
        val num = lastToken.toDoubleOrNull() ?: return
        val result = num / 100.0
        tokens[tokens.lastIndex] = formatResult(result)
        _expression.value = tokens.joinToString(" ")
        updatePreview()
    }

    fun onSquareRoot() {
        _historyExpanded.value = false
        val current = _expression.value
        if (current.isEmpty() || current.endsWith(" ") || current == "Error") return
        val tokens = current.split(" ").toMutableList()
        val lastToken = tokens.lastOrNull() ?: ""
        val num = lastToken.toDoubleOrNull() ?: return
        if (num < 0) {
            _expression.value = "Error"
            updatePreview()
            return
        }
        val result = Math.sqrt(num)
        tokens[tokens.lastIndex] = formatResult(result)
        _expression.value = tokens.joinToString(" ")
        updatePreview()
    }

    fun onSquare() {
        _historyExpanded.value = false
        val current = _expression.value
        if (current.isEmpty() || current.endsWith(" ") || current == "Error") return
        val tokens = current.split(" ").toMutableList()
        val lastToken = tokens.lastOrNull() ?: ""
        val num = lastToken.toDoubleOrNull() ?: return
        val result = num * num
        tokens[tokens.lastIndex] = formatResult(result)
        _expression.value = tokens.joinToString(" ")
        updatePreview()
    }

    fun onPi() {
        _historyExpanded.value = false
        val current = _expression.value
        if (current == "0" || current == "Error" || current.isEmpty()) {
            _expression.value = PI_STRING
            updatePreview()
            return
        }
        if (current.endsWith(" ")) {
            _expression.value = current + PI_STRING
        } else {
            val tokens = current.split(" ")
            val lastToken = tokens.lastOrNull() ?: ""
            if (lastToken == "0") {
                val newList = tokens.toMutableList()
                newList[newList.lastIndex] = PI_STRING
                _expression.value = newList.joinToString(" ")
            } else {
                // Implicit multiplication instead of concatenation
                _expression.value = "$current \u00D7 $PI_STRING"
            }
        }
        updatePreview()
    }

    fun onBackspace() {
        _historyExpanded.value = false
        val current = _expression.value
        if (current.isEmpty() || current == "0" || current == "Error") {
            _expression.value = "0"
            updatePreview()
            return
        }
        if (current.endsWith(" ")) {
            val trimmed = current.trimEnd()
            val lastSpaceIdx = trimmed.lastIndexOf(' ')
            _expression.value = if (lastSpaceIdx >= 0) trimmed.substring(0, lastSpaceIdx + 1) else trimmed.dropLast(1).ifEmpty { "0" }
        } else {
            val remain = current.dropLast(1)
            _expression.value = if (remain.isEmpty() || remain == "-") "0" else remain
        }
        updatePreview()
    }

    fun onClear() {
        _expression.value = "0"
        _historyExpanded.value = false
        updatePreview()
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
        updatePreview()
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

    fun onCalculate() {
        val current = _expression.value
        if (current == "Error" || current.isEmpty()) return
        val tokens = current.split(" ")
        try {
            val cleanTokens = tokens.toMutableList()
            while (cleanTokens.isNotEmpty() && cleanTokens.last() in listOf("+", "-", "\u00D7", "\u00F7", "^")) {
                cleanTokens.removeAt(cleanTokens.size - 1)
            }
            if (cleanTokens.isEmpty()) return

            val value = evaluateTokens(cleanTokens)
            val formatted = formatResult(value)

            val trimmedExpr = current.trim()
            if (trimmedExpr != formatted && trimmedExpr.contains(" ")) {
                val historyItem = HistoryItem(trimmedExpr, formatted)
                val newHistory = listOf(historyItem) + _history.value
                _history.value = if (newHistory.size > MAX_HISTORY_SIZE) {
                    newHistory.take(MAX_HISTORY_SIZE)
                } else {
                    newHistory
                }
            }

            _expression.value = formatted
            _realTimePreview.value = ""
        } catch (e: ArithmeticException) {
            _expression.value = "Error"
            _realTimePreview.value = ""
        } catch (e: Exception) {
            _expression.value = "Error"
            _realTimePreview.value = ""
        }
    }

    private fun evaluateTokens(tokens: List<String>): Double {
        val cleanTokens = tokens.toMutableList()

        // 1. Evaluate Power "^" (right-to-left associativity)
        var i = cleanTokens.size - 1
        while (i >= 0) {
            if (cleanTokens[i] == "^") {
                if (i > 0 && i < cleanTokens.size - 1) {
                    val base = cleanTokens[i - 1].toDoubleOrNull()
                        ?: throw IllegalArgumentException("Invalid operand: ${cleanTokens[i - 1]}")
                    val exponent = cleanTokens[i + 1].toDoubleOrNull()
                        ?: throw IllegalArgumentException("Invalid operand: ${cleanTokens[i + 1]}")
                    val r = Math.pow(base, exponent)
                    cleanTokens[i - 1] = r.toString()
                    cleanTokens.removeAt(i)
                    cleanTokens.removeAt(i)
                } else {
                    cleanTokens.removeAt(i)
                }
            }
            i--
        }

        // 2. Evaluate Multiplication and Division (left-to-right)
        i = 0
        while (i < cleanTokens.size) {
            if (cleanTokens[i] == "\u00D7" || cleanTokens[i] == "\u00F7") {
                if (i > 0 && i < cleanTokens.size - 1) {
                    val left = cleanTokens[i - 1].toDoubleOrNull()
                        ?: throw IllegalArgumentException("Invalid operand: ${cleanTokens[i - 1]}")
                    val right = cleanTokens[i + 1].toDoubleOrNull()
                        ?: throw IllegalArgumentException("Invalid operand: ${cleanTokens[i + 1]}")
                    val r = if (cleanTokens[i] == "\u00D7") {
                        left * right
                    } else {
                        if (right == 0.0) throw ArithmeticException("Division by zero")
                        left / right
                    }
                    cleanTokens[i - 1] = r.toString()
                    cleanTokens.removeAt(i)
                    cleanTokens.removeAt(i)
                } else {
                    cleanTokens.removeAt(i)
                }
            } else {
                i++
            }
        }

        // 3. Evaluate Addition and Subtraction (left-to-right)
        i = 0
        while (i < cleanTokens.size) {
            if (cleanTokens[i] == "+" || cleanTokens[i] == "-") {
                if (i > 0 && i < cleanTokens.size - 1) {
                    val left = cleanTokens[i - 1].toDoubleOrNull()
                        ?: throw IllegalArgumentException("Invalid operand: ${cleanTokens[i - 1]}")
                    val right = cleanTokens[i + 1].toDoubleOrNull()
                        ?: throw IllegalArgumentException("Invalid operand: ${cleanTokens[i + 1]}")
                    val r = if (cleanTokens[i] == "+") {
                        left + right
                    } else {
                        left - right
                    }
                    cleanTokens[i - 1] = r.toString()
                    cleanTokens.removeAt(i)
                    cleanTokens.removeAt(i)
                } else if (i == 0 && cleanTokens[i] == "-") {
                    if (cleanTokens.size > 1) {
                        val next = cleanTokens[i + 1].toDoubleOrNull()
                            ?: throw IllegalArgumentException("Invalid operand: ${cleanTokens[i + 1]}")
                        cleanTokens[i] = (-next).toString()
                        cleanTokens.removeAt(i + 1)
                    } else {
                        cleanTokens.removeAt(i)
                    }
                } else {
                    cleanTokens.removeAt(i)
                }
            } else {
                i++
            }
        }

        return cleanTokens.firstOrNull()?.toDoubleOrNull()
            ?: throw IllegalArgumentException("Failed to evaluate expression")
    }

    private fun formatResult(value: Double): String {
        if (value.isInfinite()) return "Error"
        if (value.isNaN()) return "Error"

        if (value % 1 == 0.0 && Math.abs(value) < 1e15) {
            if (Math.abs(value) >= 1e12) {
                return String.format("%.6E", value)
            }
            return value.toLong().toString()
        }

        if (Math.abs(value) >= 1e12 || (Math.abs(value) < 1e-6 && value != 0.0)) {
            return String.format("%.6E", value)
        }

        val formatted = String.format("%.8f", value)
        val cleaned = formatted.replace(Regex("0+$"), "").replace(Regex("\\.$"), "")
        return cleaned
    }
}
