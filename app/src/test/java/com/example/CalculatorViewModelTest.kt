package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {

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

    // ── Initial state ──

    @Test
    fun `initial expression is 0`() {
        assertEquals("0", vm.expression.value)
    }

    @Test
    fun `initial history is empty`() {
        assertTrue(vm.history.value.isEmpty())
    }

    @Test
    fun `initial historyExpanded is false`() {
        assertFalse(vm.historyExpanded.value)
    }

    @Test
    fun `initial scientificExpanded is false`() {
        assertFalse(vm.scientificExpanded.value)
    }

    @Test
    fun `initial showCopyFeedback is false`() {
        assertFalse(vm.showCopyFeedback.value)
    }

    // ── onDigit ──

    @Test
    fun `onDigit replaces initial 0`() {
        vm.onDigit("5")
        assertEquals("5", vm.expression.value)
    }

    @Test
    fun `onDigit replaces Error`() {
        // Force error state
        vm.onSquareRoot() // sqrt of 0 is 0, need negative
        vm.onDigit("4")
        vm.onToggleSign() // -4
        vm.onSquareRoot() // Error
        assertEquals("Error", vm.expression.value)

        vm.onDigit("7")
        assertEquals("7", vm.expression.value)
    }

    @Test
    fun `onDigit appends to existing number`() {
        vm.onDigit("1")
        vm.onDigit("2")
        vm.onDigit("3")
        assertEquals("123", vm.expression.value)
    }

    @Test
    fun `onDigit replaces standalone 0 token after operator`() {
        vm.onDigit("5")
        vm.onOperator("+")
        vm.onDigit("0")
        // expression is "5 + 0"
        vm.onDigit("3")
        assertEquals("5 + 3", vm.expression.value)
    }

    @Test
    fun `onDigit replaces negative zero token`() {
        vm.onDigit("5")
        vm.onOperator("+")
        vm.onToggleSign() // appends -
        vm.onDigit("0")
        // expression is "5 + -0"
        vm.onDigit("7")
        assertEquals("5 + -7", vm.expression.value)
    }

    @Test
    fun `onDigit collapses history panel`() {
        vm.toggleHistory()
        assertTrue(vm.historyExpanded.value)
        vm.onDigit("1")
        assertFalse(vm.historyExpanded.value)
    }

    // ── onOperator ──

    @Test
    fun `onOperator appends operator with spaces`() {
        vm.onDigit("5")
        vm.onOperator("+")
        assertEquals("5 + ", vm.expression.value)
    }

    @Test
    fun `onOperator replaces trailing operator`() {
        vm.onDigit("5")
        vm.onOperator("+")
        vm.onOperator("×")
        assertEquals("5 × ", vm.expression.value)
    }

    @Test
    fun `onOperator from Error state resets to 0 op`() {
        // Force error
        vm.onDigit("4")
        vm.onToggleSign()
        vm.onSquareRoot()
        assertEquals("Error", vm.expression.value)
        vm.onOperator("+")
        assertEquals("0 + ", vm.expression.value)
    }

    @Test
    fun `onOperator from empty expression resets to 0 op`() {
        // The expression starts at "0", but let's test the empty branch
        // by exploiting the code path: clear sets to "0", not empty.
        // We test the normal path from "0":
        vm.onOperator("-")
        assertEquals("0 - ", vm.expression.value)
    }

    @Test
    fun `onOperator collapses history panel`() {
        vm.toggleHistory()
        assertTrue(vm.historyExpanded.value)
        vm.onOperator("+")
        assertFalse(vm.historyExpanded.value)
    }

    // ── onDecimal ──

    @Test
    fun `onDecimal appends dot to current number`() {
        vm.onDigit("5")
        vm.onDecimal()
        assertEquals("5.", vm.expression.value)
    }

    @Test
    fun `onDecimal does not add second dot to same number`() {
        vm.onDigit("5")
        vm.onDecimal()
        vm.onDecimal()
        assertEquals("5.", vm.expression.value)
    }

    @Test
    fun `onDecimal after operator adds 0 dot`() {
        vm.onDigit("3")
        vm.onOperator("+")
        vm.onDecimal()
        assertEquals("3 + 0.", vm.expression.value)
    }

    @Test
    fun `onDecimal collapses history panel`() {
        vm.toggleHistory()
        assertTrue(vm.historyExpanded.value)
        vm.onDecimal()
        assertFalse(vm.historyExpanded.value)
    }

    // ── onToggleSign ──

    @Test
    fun `onToggleSign negates positive number`() {
        vm.onDigit("5")
        vm.onToggleSign()
        assertEquals("-5", vm.expression.value)
    }

    @Test
    fun `onToggleSign makes negative number positive`() {
        vm.onDigit("5")
        vm.onToggleSign()
        vm.onToggleSign()
        assertEquals("5", vm.expression.value)
    }

    @Test
    fun `onToggleSign after operator appends minus`() {
        vm.onDigit("3")
        vm.onOperator("+")
        vm.onToggleSign()
        assertEquals("3 + -", vm.expression.value)
    }

    @Test
    fun `onToggleSign does nothing on Error`() {
        vm.onDigit("4")
        vm.onToggleSign()
        vm.onSquareRoot() // sqrt(-4) = Error
        assertEquals("Error", vm.expression.value)
        vm.onToggleSign()
        assertEquals("Error", vm.expression.value)
    }

    @Test
    fun `onToggleSign collapses history panel`() {
        vm.toggleHistory()
        vm.onDigit("1")
        vm.toggleHistory()
        assertTrue(vm.historyExpanded.value)
        vm.onToggleSign()
        assertFalse(vm.historyExpanded.value)
    }

    // ── onPercentage ──

    @Test
    fun `onPercentage divides last token by 100`() {
        vm.onDigit("5")
        vm.onDigit("0")
        vm.onPercentage()
        assertEquals("0.5", vm.expression.value)
    }

    @Test
    fun `onPercentage on multi-token expression only affects last token`() {
        vm.onDigit("1")
        vm.onDigit("0")
        vm.onDigit("0")
        vm.onOperator("+")
        vm.onDigit("5")
        vm.onDigit("0")
        vm.onPercentage()
        assertEquals("100 + 0.5", vm.expression.value)
    }

    @Test
    fun `onPercentage does nothing on empty or trailing operator`() {
        vm.onDigit("5")
        vm.onOperator("+")
        val before = vm.expression.value
        vm.onPercentage()
        assertEquals(before, vm.expression.value)
    }

    @Test
    fun `onPercentage does nothing on Error`() {
        vm.onDigit("4")
        vm.onToggleSign()
        vm.onSquareRoot()
        assertEquals("Error", vm.expression.value)
        vm.onPercentage()
        assertEquals("Error", vm.expression.value)
    }

    // ── onSquareRoot ──

    @Test
    fun `onSquareRoot computes square root of positive number`() {
        vm.onDigit("9")
        vm.onSquareRoot()
        assertEquals("3", vm.expression.value)
    }

    @Test
    fun `onSquareRoot of 0 returns 0`() {
        vm.onSquareRoot()
        assertEquals("0", vm.expression.value)
    }

    @Test
    fun `onSquareRoot of negative number returns Error`() {
        vm.onDigit("4")
        vm.onToggleSign()
        vm.onSquareRoot()
        assertEquals("Error", vm.expression.value)
    }

    @Test
    fun `onSquareRoot of non-perfect square`() {
        vm.onDigit("2")
        vm.onSquareRoot()
        assertEquals("1.41421356", vm.expression.value)
    }

    @Test
    fun `onSquareRoot does nothing when trailing operator`() {
        vm.onDigit("9")
        vm.onOperator("+")
        val before = vm.expression.value
        vm.onSquareRoot()
        assertEquals(before, vm.expression.value)
    }

    // ── onSquare ──

    @Test
    fun `onSquare squares the last token`() {
        vm.onDigit("5")
        vm.onSquare()
        assertEquals("25", vm.expression.value)
    }

    @Test
    fun `onSquare of negative number`() {
        vm.onDigit("3")
        vm.onToggleSign()
        vm.onSquare()
        assertEquals("9", vm.expression.value)
    }

    @Test
    fun `onSquare of 0 returns 0`() {
        vm.onSquare()
        assertEquals("0", vm.expression.value)
    }

    @Test
    fun `onSquare does nothing when trailing operator`() {
        vm.onDigit("5")
        vm.onOperator("+")
        val before = vm.expression.value
        vm.onSquare()
        assertEquals(before, vm.expression.value)
    }

    @Test
    fun `onSquare does nothing on Error`() {
        vm.onDigit("4")
        vm.onToggleSign()
        vm.onSquareRoot()
        assertEquals("Error", vm.expression.value)
        vm.onSquare()
        assertEquals("Error", vm.expression.value)
    }

    // ── onPi ──

    @Test
    fun `onPi replaces initial 0`() {
        vm.onPi()
        assertEquals("3.14159265", vm.expression.value)
    }

    @Test
    fun `onPi replaces Error`() {
        vm.onDigit("4")
        vm.onToggleSign()
        vm.onSquareRoot()
        vm.onPi()
        assertEquals("3.14159265", vm.expression.value)
    }

    @Test
    fun `onPi after operator appends pi value`() {
        vm.onDigit("2")
        vm.onOperator("×")
        vm.onPi()
        assertEquals("2 × 3.14159265", vm.expression.value)
    }

    @Test
    fun `onPi replaces standalone 0 token after operator`() {
        vm.onDigit("5")
        vm.onOperator("+")
        vm.onDigit("0")
        vm.onPi()
        assertEquals("5 + 3.14159265", vm.expression.value)
    }

    @Test
    fun `onPi appends to existing non-zero number`() {
        vm.onDigit("2")
        vm.onPi()
        assertEquals("23.14159265", vm.expression.value)
    }

    // ── onBackspace ──

    @Test
    fun `onBackspace removes last character`() {
        vm.onDigit("1")
        vm.onDigit("2")
        vm.onDigit("3")
        vm.onBackspace()
        assertEquals("12", vm.expression.value)
    }

    @Test
    fun `onBackspace on single digit resets to 0`() {
        vm.onDigit("5")
        vm.onBackspace()
        assertEquals("0", vm.expression.value)
    }

    @Test
    fun `onBackspace on 0 stays 0`() {
        vm.onBackspace()
        assertEquals("0", vm.expression.value)
    }

    @Test
    fun `onBackspace on Error resets to 0`() {
        vm.onDigit("4")
        vm.onToggleSign()
        vm.onSquareRoot()
        assertEquals("Error", vm.expression.value)
        vm.onBackspace()
        assertEquals("0", vm.expression.value)
    }

    @Test
    fun `onBackspace removes trailing operator`() {
        vm.onDigit("5")
        vm.onOperator("+")
        vm.onBackspace()
        assertEquals("5", vm.expression.value)
    }

    // ── onClear ──

    @Test
    fun `onClear resets expression to 0`() {
        vm.onDigit("1")
        vm.onDigit("2")
        vm.onDigit("3")
        vm.onClear()
        assertEquals("0", vm.expression.value)
    }

    @Test
    fun `onClear collapses history panel`() {
        vm.toggleHistory()
        assertTrue(vm.historyExpanded.value)
        vm.onClear()
        assertFalse(vm.historyExpanded.value)
    }

    // ── toggleHistory / toggleScientific ──

    @Test
    fun `toggleHistory toggles historyExpanded`() {
        assertFalse(vm.historyExpanded.value)
        vm.toggleHistory()
        assertTrue(vm.historyExpanded.value)
        vm.toggleHistory()
        assertFalse(vm.historyExpanded.value)
    }

    @Test
    fun `toggleScientific toggles scientificExpanded`() {
        assertFalse(vm.scientificExpanded.value)
        vm.toggleScientific()
        assertTrue(vm.scientificExpanded.value)
        vm.toggleScientific()
        assertFalse(vm.scientificExpanded.value)
    }

    // ── History management ──

    @Test
    fun `onCalculate adds to history when expression contains operator`() {
        vm.onDigit("3")
        vm.onOperator("+")
        vm.onDigit("4")
        vm.onCalculate()
        assertEquals(1, vm.history.value.size)
        assertEquals("3 + 4", vm.history.value[0].equation)
        assertEquals("7", vm.history.value[0].result)
    }

    @Test
    fun `onCalculate does not add to history for single number`() {
        vm.onDigit("5")
        vm.onCalculate()
        assertTrue(vm.history.value.isEmpty())
    }

    @Test
    fun `history items are prepended (newest first)`() {
        vm.onDigit("1")
        vm.onOperator("+")
        vm.onDigit("2")
        vm.onCalculate()

        vm.onOperator("+")
        vm.onDigit("5")
        vm.onCalculate()

        assertEquals(2, vm.history.value.size)
        assertEquals("3 + 5", vm.history.value[0].equation)
        assertEquals("1 + 2", vm.history.value[1].equation)
    }

    @Test
    fun `selectHistoryItem sets expression to result and collapses history`() {
        vm.onDigit("3")
        vm.onOperator("+")
        vm.onDigit("4")
        vm.onCalculate()

        vm.toggleHistory()
        assertTrue(vm.historyExpanded.value)

        val item = vm.history.value[0]
        vm.selectHistoryItem(item)
        assertEquals("7", vm.expression.value)
        assertFalse(vm.historyExpanded.value)
    }

    @Test
    fun `clearHistory empties the history list`() {
        vm.onDigit("1")
        vm.onOperator("+")
        vm.onDigit("2")
        vm.onCalculate()
        assertFalse(vm.history.value.isEmpty())

        vm.clearHistory()
        assertTrue(vm.history.value.isEmpty())
    }

    // ── Copy feedback ──

    @Test
    fun `triggerCopyFeedback sets showCopyFeedback true`() {
        vm.triggerCopyFeedback()
        assertTrue(vm.showCopyFeedback.value)
    }

    @Test
    fun `dismissCopyFeedback sets showCopyFeedback false`() {
        vm.triggerCopyFeedback()
        vm.dismissCopyFeedback()
        assertFalse(vm.showCopyFeedback.value)
    }

    // ── onCalculate ──

    @Test
    fun `onCalculate simple addition`() {
        vm.onDigit("3")
        vm.onOperator("+")
        vm.onDigit("4")
        vm.onCalculate()
        assertEquals("7", vm.expression.value)
    }

    @Test
    fun `onCalculate simple subtraction`() {
        vm.onDigit("9")
        vm.onOperator("-")
        vm.onDigit("4")
        vm.onCalculate()
        assertEquals("5", vm.expression.value)
    }

    @Test
    fun `onCalculate simple multiplication`() {
        vm.onDigit("6")
        vm.onOperator("×")
        vm.onDigit("7")
        vm.onCalculate()
        assertEquals("42", vm.expression.value)
    }

    @Test
    fun `onCalculate simple division`() {
        vm.onDigit("8")
        vm.onOperator("÷")
        vm.onDigit("4")
        vm.onCalculate()
        assertEquals("2", vm.expression.value)
    }

    @Test
    fun `onCalculate division by zero returns Error`() {
        vm.onDigit("5")
        vm.onOperator("÷")
        vm.onDigit("0")
        vm.onCalculate()
        assertEquals("Error", vm.expression.value)
    }

    @Test
    fun `onCalculate respects operator precedence multiply before add`() {
        // 2 + 3 × 4 = 14
        vm.onDigit("2")
        vm.onOperator("+")
        vm.onDigit("3")
        vm.onOperator("×")
        vm.onDigit("4")
        vm.onCalculate()
        assertEquals("14", vm.expression.value)
    }

    @Test
    fun `onCalculate respects operator precedence divide before subtract`() {
        // 10 - 6 ÷ 2 = 7
        vm.onDigit("1")
        vm.onDigit("0")
        vm.onOperator("-")
        vm.onDigit("6")
        vm.onOperator("÷")
        vm.onDigit("2")
        vm.onCalculate()
        assertEquals("7", vm.expression.value)
    }

    @Test
    fun `onCalculate power operation`() {
        // 2 ^ 3 = 8
        vm.onDigit("2")
        vm.onOperator("^")
        vm.onDigit("3")
        vm.onCalculate()
        assertEquals("8", vm.expression.value)
    }

    @Test
    fun `onCalculate power before multiplication`() {
        // 2 × 3 ^ 2 = 18
        vm.onDigit("2")
        vm.onOperator("×")
        vm.onDigit("3")
        vm.onOperator("^")
        vm.onDigit("2")
        vm.onCalculate()
        assertEquals("18", vm.expression.value)
    }

    @Test
    fun `onCalculate strips trailing operator`() {
        vm.onDigit("5")
        vm.onOperator("+")
        vm.onCalculate()
        assertEquals("5", vm.expression.value)
    }

    @Test
    fun `onCalculate does nothing on Error`() {
        vm.onDigit("4")
        vm.onToggleSign()
        vm.onSquareRoot()
        assertEquals("Error", vm.expression.value)
        vm.onCalculate()
        assertEquals("Error", vm.expression.value)
    }

    @Test
    fun `onCalculate chained operations`() {
        // 1 + 2 + 3 = 6
        vm.onDigit("1")
        vm.onOperator("+")
        vm.onDigit("2")
        vm.onOperator("+")
        vm.onDigit("3")
        vm.onCalculate()
        assertEquals("6", vm.expression.value)
    }

    @Test
    fun `onCalculate decimal result`() {
        // 1 ÷ 3 = 0.33333333
        vm.onDigit("1")
        vm.onOperator("÷")
        vm.onDigit("3")
        vm.onCalculate()
        assertEquals("0.33333333", vm.expression.value)
    }

    @Test
    fun `onCalculate large integer uses scientific notation`() {
        // Test with a value that exceeds 1e12
        vm.onDigit("9")
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
        // Result is 9999999 * 9999999 = 99999980000001 > 1e12
        assertTrue(vm.expression.value.contains("E"))
    }

    @Test
    fun `onCalculate negative result`() {
        // 3 - 7 = -4
        vm.onDigit("3")
        vm.onOperator("-")
        vm.onDigit("7")
        vm.onCalculate()
        assertEquals("-4", vm.expression.value)
    }

    // ── realTimePreview ──

    @Test
    fun `realTimePreview returns empty for single number`() {
        vm.onDigit("5")
        assertEquals("", vm.realTimePreview)
    }

    @Test
    fun `realTimePreview returns empty for Error`() {
        vm.onDigit("4")
        vm.onToggleSign()
        vm.onSquareRoot()
        assertEquals("", vm.realTimePreview)
    }

    @Test
    fun `realTimePreview shows preview for expression with operator and second operand`() {
        vm.onDigit("3")
        vm.onOperator("+")
        vm.onDigit("4")
        assertEquals("= 7", vm.realTimePreview)
    }

    @Test
    fun `realTimePreview strips trailing operators`() {
        vm.onDigit("5")
        vm.onOperator("+")
        // Only operator, no second operand: cleanTokens will be ["5"] → size <= 1 → ""
        assertEquals("", vm.realTimePreview)
    }

    @Test
    fun `realTimePreview handles complex expressions`() {
        // 2 + 3 × 4 preview should be = 14
        vm.onDigit("2")
        vm.onOperator("+")
        vm.onDigit("3")
        vm.onOperator("×")
        vm.onDigit("4")
        assertEquals("= 14", vm.realTimePreview)
    }

    // ── Edge cases and integration scenarios ──

    @Test
    fun `full calculation workflow digit operator digit equals`() {
        vm.onDigit("1")
        vm.onDigit("2")
        vm.onOperator("+")
        vm.onDigit("3")
        vm.onDigit("4")
        vm.onCalculate()
        assertEquals("46", vm.expression.value)
        assertEquals(1, vm.history.value.size)
    }

    @Test
    fun `decimal number calculation`() {
        vm.onDigit("1")
        vm.onDecimal()
        vm.onDigit("5")
        vm.onOperator("+")
        vm.onDigit("2")
        vm.onDecimal()
        vm.onDigit("5")
        vm.onCalculate()
        assertEquals("4", vm.expression.value)
    }

    @Test
    fun `percentage then calculate`() {
        // 200 + 50% (= 0.5) → 200 + 0.5 = 200.5
        vm.onDigit("2")
        vm.onDigit("0")
        vm.onDigit("0")
        vm.onOperator("+")
        vm.onDigit("5")
        vm.onDigit("0")
        vm.onPercentage()
        vm.onCalculate()
        assertEquals("200.5", vm.expression.value)
    }

    @Test
    fun `square root then calculate`() {
        // √9 + 1 = 4
        vm.onDigit("9")
        vm.onSquareRoot()
        vm.onOperator("+")
        vm.onDigit("1")
        vm.onCalculate()
        assertEquals("4", vm.expression.value)
    }

    @Test
    fun `clear after calculation resets to 0`() {
        vm.onDigit("5")
        vm.onOperator("+")
        vm.onDigit("3")
        vm.onCalculate()
        assertEquals("8", vm.expression.value)
        vm.onClear()
        assertEquals("0", vm.expression.value)
    }

    @Test
    fun `backspace during input removes last character`() {
        vm.onDigit("1")
        vm.onDigit("2")
        vm.onDigit("3")
        vm.onBackspace()
        vm.onDigit("4")
        assertEquals("124", vm.expression.value)
    }

    @Test
    fun `multiple operators in sequence only keeps last`() {
        vm.onDigit("5")
        vm.onOperator("+")
        vm.onOperator("-")
        vm.onOperator("×")
        assertEquals("5 × ", vm.expression.value)
    }

    @Test
    fun `pi then multiply`() {
        vm.onPi()
        vm.onOperator("×")
        vm.onDigit("2")
        vm.onCalculate()
        assertEquals("6.2831853", vm.expression.value)
    }

    @Test
    fun `square then add`() {
        // 5² + 1 = 26
        vm.onDigit("5")
        vm.onSquare()
        vm.onOperator("+")
        vm.onDigit("1")
        vm.onCalculate()
        assertEquals("26", vm.expression.value)
    }

    @Test
    fun `onCalculate does not add duplicate history when result matches expression`() {
        vm.onDigit("5")
        vm.onCalculate()
        assertTrue(vm.history.value.isEmpty())
    }

    @Test
    fun `mixed operations with precedence`() {
        // 2 + 3 × 4 - 1 = 13
        vm.onDigit("2")
        vm.onOperator("+")
        vm.onDigit("3")
        vm.onOperator("×")
        vm.onDigit("4")
        vm.onOperator("-")
        vm.onDigit("1")
        vm.onCalculate()
        assertEquals("13", vm.expression.value)
    }

    @Test
    fun `negative number in calculation`() {
        // -3 + 5 = 2
        vm.onDigit("3")
        vm.onToggleSign()
        vm.onOperator("+")
        vm.onDigit("5")
        vm.onCalculate()
        assertEquals("2", vm.expression.value)
    }

    @Test
    fun `multiple trailing operators stripped before calculate`() {
        vm.onDigit("5")
        vm.onOperator("+")
        vm.onOperator("-")
        // Expression is "5 - " with trailing operator
        vm.onCalculate()
        assertEquals("5", vm.expression.value)
    }
}
