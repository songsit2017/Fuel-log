package com.songsit.fuellogpro.domain

import com.songsit.fuellogpro.domain.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseSummaryTest {
    @Test
    fun incomeOffsetsExpensesWithoutChangingExpenseTotal() {
        val result = calculateExpenseSummary(
            listOf(
                expense("service", 2_500.0, income = false),
                expense("refund", 500.0, income = true),
            ),
        )

        assertEquals(2_500.0, result.totalExpense, 0.001)
        assertEquals(500.0, result.totalIncome, 0.001)
        assertEquals(2_000.0, result.netExpense, 0.001)
    }
}

private fun expense(id: String, amount: Double, income: Boolean) = Expense(
    id = id,
    vehicleId = "car",
    date = "2026-07-27",
    category = "อื่น ๆ",
    description = "",
    amount = amount,
    odometerKm = null,
    income = income,
    recurring = false,
    reminderDate = null,
)
