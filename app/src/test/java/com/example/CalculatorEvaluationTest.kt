package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests focused on expression evaluation (evaluateTokens) and result formatting
 * (formatResult) exercised through the CalculatorViewModel public API.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorEvaluationTest {

    private lateinit var vm: CalculatorViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        vm = CalculatorViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Helper to build an expression and calculate, returning the result string.
    private fun evaluate(vararg actions: () -> Unit): String {
        actions.forEach { it() }
        vm.onCalculate()
        return vm.expression.value
    }

    // ── Operator Precedence ──

    @Test
    fun `power evaluated before multiplication`() {
        // 2 × 3 ^ 2 = 2 × 9 = 18
        val result = evaluate(
            { vm.onDigit("2") },
            { vm.onOperator("×") },
            { vm.onDigit("3") },
            { vm.onOperator("^") },
            { vm.onDigit("2") }
        )
        assertEquals("18", result)
    }

    @Test
    fun `power evaluated before addition`() {
        // 1 + 2 ^ 3 = 1 + 8 = 9
        val result = evaluate(
            { vm.onDigit("1") },
            { vm.onOperator("+") },
            { vm.onDigit("2") },
            { vm.onOperator("^") },
            { vm.onDigit("3") }
        )
        assertEquals("9", result)
    }

    @Test
    fun `multiplication evaluated before addition`() {
        // 5 + 3 × 2 = 5 + 6 = 11
        val result = evaluate(
            { vm.onDigit("5") },
            { vm.onOperator("+") },
            { vm.onDigit("3") },
            { vm.onOperator("×") },
            { vm.onDigit("2") }
        )
        assertEquals("11", result)
    }

    @Test
    fun `division evaluated before subtraction`() {
        // 20 - 8 ÷ 4 = 20 - 2 = 18
        val result = evaluate(
            { vm.onDigit("2") },
            { vm.onDigit("0") },
            { vm.onOperator("-") },
            { vm.onDigit("8") },
            { vm.onOperator("÷") },
            { vm.onDigit("4") }
        )
        assertEquals("18", result)
    }

    @Test
    fun `left-to-right evaluation for same precedence add and subtract`() {
        // 10 - 3 + 2 = 9
        val result = evaluate(
            { vm.onDigit("1") },
            { vm.onDigit("0") },
            { vm.onOperator("-") },
            { vm.onDigit("3") },
            { vm.onOperator("+") },
            { vm.onDigit("2") }
        )
        assertEquals("9", result)
    }

    @Test
    fun `left-to-right evaluation for same precedence multiply and divide`() {
        // 12 ÷ 3 × 2 = 8
        val result = evaluate(
            { vm.onDigit("1") },
            { vm.onDigit("2") },
            { vm.onOperator("÷") },
            { vm.onDigit("3") },
            { vm.onOperator("×") },
            { vm.onDigit("2") }
        )
        assertEquals("8", result)
    }

    // ── Division edge cases ──

    @Test
    fun `division by zero produces Error`() {
        val result = evaluate(
            { vm.onDigit("1") },
            { vm.onOperator("÷") },
            { vm.onDigit("0") }
        )
        assertEquals("Error", result)
    }

    @Test
    fun `division resulting in repeating decimal`() {
        // 1 ÷ 7 = 0.14285714...
        val result = evaluate(
            { vm.onDigit("1") },
            { vm.onOperator("÷") },
            { vm.onDigit("7") }
        )
        assertEquals("0.14285714", result)
    }

    @Test
    fun `division yielding exact integer`() {
        // 15 ÷ 5 = 3
        val result = evaluate(
            { vm.onDigit("1") },
            { vm.onDigit("5") },
            { vm.onOperator("÷") },
            { vm.onDigit("5") }
        )
        assertEquals("3", result)
    }

    // ── Power edge cases ──

    @Test
    fun `power of zero`() {
        // 5 ^ 0 = 1
        val result = evaluate(
            { vm.onDigit("5") },
            { vm.onOperator("^") },
            { vm.onDigit("0") }
        )
        assertEquals("1", result)
    }

    @Test
    fun `zero to the power of something`() {
        // 0 ^ 5 = 0
        val result = evaluate(
            { vm.onOperator("^") },
            { vm.onDigit("5") }
        )
        assertEquals("0", result)
    }

    @Test
    fun `one to any power`() {
        // 1 ^ 9 = 1
        val result = evaluate(
            { vm.onDigit("1") },
            { vm.onOperator("^") },
            { vm.onDigit("9") }
        )
        assertEquals("1", result)
    }

    // ── Format result: integer vs decimal ──

    @Test
    fun `integer result formatted without decimal point`() {
        // 4 + 6 = 10
        val result = evaluate(
            { vm.onDigit("4") },
            { vm.onOperator("+") },
            { vm.onDigit("6") }
        )
        assertEquals("10", result)
    }

    @Test
    fun `decimal result trailing zeros stripped`() {
        // 1 ÷ 4 = 0.25 (not 0.25000000)
        val result = evaluate(
            { vm.onDigit("1") },
            { vm.onOperator("÷") },
            { vm.onDigit("4") }
        )
        assertEquals("0.25", result)
    }

    @Test
    fun `result with many decimal places truncated to 8`() {
        // 1 ÷ 3 = 0.33333333 (8 decimal places)
        val result = evaluate(
            { vm.onDigit("1") },
            { vm.onOperator("÷") },
            { vm.onDigit("3") }
        )
        assertEquals("0.33333333", result)
    }

    // ── Format result: scientific notation ──

    @Test
    fun `very large integer uses scientific notation`() {
        // Build a large number via repeated multiplication
        // 999999 × 9999999 > 1e12
        vm.onDigit("9")
        vm.onDigit("9")
        vm.onDigit("9")
        vm.onDigit("9")
        vm.onDigit("9")
        vm.onDigit("9")
        vm.onOperator("×")
        vm.onDigit("9")
        vm.onDigit("9")
        vm.onDigit("9")
        vm.onDigit("9")
        vm.onDigit("9")
        vm.onDigit("9")
        vm.onDigit("9")
        vm.onCalculate()
        assertTrue(vm.expression.value.contains("E"))
    }

    // ── Complex multi-operation expressions ──

    @Test
    fun `complex expression with all four basic operators`() {
        // 2 + 3 × 4 - 6 ÷ 2 = 2 + 12 - 3 = 11
        val result = evaluate(
            { vm.onDigit("2") },
            { vm.onOperator("+") },
            { vm.onDigit("3") },
            { vm.onOperator("×") },
            { vm.onDigit("4") },
            { vm.onOperator("-") },
            { vm.onDigit("6") },
            { vm.onOperator("÷") },
            { vm.onDigit("2") }
        )
        assertEquals("11", result)
    }

    @Test
    fun `expression with power multiply and add`() {
        // 1 + 2 ^ 3 × 2 = 1 + 8 × 2 = 1 + 16 = 17
        val result = evaluate(
            { vm.onDigit("1") },
            { vm.onOperator("+") },
            { vm.onDigit("2") },
            { vm.onOperator("^") },
            { vm.onDigit("3") },
            { vm.onOperator("×") },
            { vm.onDigit("2") }
        )
        assertEquals("17", result)
    }

    @Test
    fun `subtraction yielding negative result`() {
        // 5 - 10 = -5
        val result = evaluate(
            { vm.onDigit("5") },
            { vm.onOperator("-") },
            { vm.onDigit("1") },
            { vm.onDigit("0") }
        )
        assertEquals("-5", result)
    }

    @Test
    fun `multiply by zero`() {
        // 999 × 0 = 0
        val result = evaluate(
            { vm.onDigit("9") },
            { vm.onDigit("9") },
            { vm.onDigit("9") },
            { vm.onOperator("×") },
            { vm.onDigit("0") }
        )
        assertEquals("0", result)
    }

    @Test
    fun `add zero does not change value`() {
        // 42 + 0 = 42
        val result = evaluate(
            { vm.onDigit("4") },
            { vm.onDigit("2") },
            { vm.onOperator("+") },
            { vm.onDigit("0") }
        )
        assertEquals("42", result)
    }

    // ── Trailing operator cleanup ──

    @Test
    fun `multiple trailing operators all stripped before calculate`() {
        // Build "5 + " then replace with "- " then calculate → should just be 5
        vm.onDigit("5")
        vm.onOperator("+")
        vm.onOperator("-")
        vm.onCalculate()
        assertEquals("5", vm.expression.value)
    }

    // ── realTimePreview with operator precedence ──

    @Test
    fun `realTimePreview respects operator precedence`() {
        // 1 + 2 × 3 → preview should be = 7
        vm.onDigit("1")
        vm.onOperator("+")
        vm.onDigit("2")
        vm.onOperator("×")
        vm.onDigit("3")
        assertEquals("= 7", vm.realTimePreview)
    }

    @Test
    fun `realTimePreview returns empty for single token after operator strip`() {
        // "5 + " → after stripping trailing operator, tokens = ["5"] → size <= 1 → ""
        vm.onDigit("5")
        vm.onOperator("+")
        assertEquals("", vm.realTimePreview)
    }

    @Test
    fun `realTimePreview handles division by zero gracefully`() {
        // 5 ÷ 0 → evaluateTokens throws ArithmeticException → preview = ""
        vm.onDigit("5")
        vm.onOperator("÷")
        vm.onDigit("0")
        assertEquals("", vm.realTimePreview)
    }

    // ── Decimal number operations ──

    @Test
    fun `add two decimal numbers`() {
        // 0.1 + 0.2
        vm.onDigit("0")
        vm.onDecimal()
        vm.onDigit("1")
        vm.onOperator("+")
        vm.onDigit("0")
        vm.onDecimal()
        vm.onDigit("2")
        vm.onCalculate()
        assertEquals("0.3", vm.expression.value)
    }

    @Test
    fun `multiply decimal numbers`() {
        // 2.5 × 4 = 10
        vm.onDigit("2")
        vm.onDecimal()
        vm.onDigit("5")
        vm.onOperator("×")
        vm.onDigit("4")
        vm.onCalculate()
        assertEquals("10", vm.expression.value)
    }

    // ── Negative number operations ──

    @Test
    fun `negative times negative is positive`() {
        // -3 × -4 = 12
        vm.onDigit("3")
        vm.onToggleSign()
        vm.onOperator("×")
        vm.onDigit("4")
        vm.onToggleSign()
        vm.onCalculate()
        assertEquals("12", vm.expression.value)
    }

    @Test
    fun `negative divided by positive`() {
        // -10 ÷ 2 = -5
        vm.onDigit("1")
        vm.onDigit("0")
        vm.onToggleSign()
        vm.onOperator("÷")
        vm.onDigit("2")
        vm.onCalculate()
        assertEquals("-5", vm.expression.value)
    }

    // ── Scientific functions then evaluate ──

    @Test
    fun `sqrt then multiply`() {
        // √16 × 3 = 12
        vm.onDigit("1")
        vm.onDigit("6")
        vm.onSquareRoot()
        vm.onOperator("×")
        vm.onDigit("3")
        vm.onCalculate()
        assertEquals("12", vm.expression.value)
    }

    @Test
    fun `square then subtract`() {
        // 4² - 6 = 10
        vm.onDigit("4")
        vm.onSquare()
        vm.onOperator("-")
        vm.onDigit("6")
        vm.onCalculate()
        assertEquals("10", vm.expression.value)
    }

    @Test
    fun `percentage in complex expression`() {
        // 50 × 200% → 50 × 2 = 100
        vm.onDigit("5")
        vm.onDigit("0")
        vm.onOperator("×")
        vm.onDigit("2")
        vm.onDigit("0")
        vm.onDigit("0")
        vm.onPercentage()
        vm.onCalculate()
        assertEquals("100", vm.expression.value)
    }
}
