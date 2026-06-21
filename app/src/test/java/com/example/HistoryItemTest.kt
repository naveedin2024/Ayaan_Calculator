package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryItemTest {

    @Test
    fun `HistoryItem stores equation and result`() {
        val item = HistoryItem(equation = "3 + 4", result = "7")
        assertEquals("3 + 4", item.equation)
        assertEquals("7", item.result)
    }

    @Test
    fun `HistoryItem timestamp defaults to current time`() {
        val before = System.currentTimeMillis()
        val item = HistoryItem(equation = "1 + 1", result = "2")
        val after = System.currentTimeMillis()
        assertTrue(item.timestamp in before..after)
    }

    @Test
    fun `HistoryItem custom timestamp`() {
        val item = HistoryItem(equation = "5 × 2", result = "10", timestamp = 12345L)
        assertEquals(12345L, item.timestamp)
    }

    @Test
    fun `HistoryItem equality based on all fields`() {
        val item1 = HistoryItem(equation = "3 + 4", result = "7", timestamp = 100L)
        val item2 = HistoryItem(equation = "3 + 4", result = "7", timestamp = 100L)
        assertEquals(item1, item2)
    }

    @Test
    fun `HistoryItem inequality with different equation`() {
        val item1 = HistoryItem(equation = "3 + 4", result = "7", timestamp = 100L)
        val item2 = HistoryItem(equation = "5 + 2", result = "7", timestamp = 100L)
        assertNotEquals(item1, item2)
    }

    @Test
    fun `HistoryItem inequality with different result`() {
        val item1 = HistoryItem(equation = "3 + 4", result = "7", timestamp = 100L)
        val item2 = HistoryItem(equation = "3 + 4", result = "8", timestamp = 100L)
        assertNotEquals(item1, item2)
    }

    @Test
    fun `HistoryItem copy creates modified copy`() {
        val item = HistoryItem(equation = "3 + 4", result = "7", timestamp = 100L)
        val copy = item.copy(result = "8")
        assertEquals("8", copy.result)
        assertEquals("3 + 4", copy.equation)
        assertEquals(100L, copy.timestamp)
    }
}
