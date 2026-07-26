package com.songsit.fuellogpro.domain

import com.songsit.fuellogpro.domain.model.Expense

data class ExpenseSummary(
    val totalExpense: Double,
    val totalIncome: Double,
    val netExpense: Double,
)

fun calculateExpenseSummary(items: List<Expense>): ExpenseSummary {
    val expense = items.filterNot(Expense::income).sumOf(Expense::amount)
    val income = items.filter(Expense::income).sumOf(Expense::amount)
    return ExpenseSummary(
        totalExpense = expense,
        totalIncome = income,
        netExpense = expense - income,
    )
}
