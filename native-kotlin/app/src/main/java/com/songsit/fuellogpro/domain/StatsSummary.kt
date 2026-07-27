package com.songsit.fuellogpro.domain

import com.songsit.fuellogpro.domain.model.Expense
import com.songsit.fuellogpro.domain.model.FuelEntry
import java.time.LocalDate

/**
 * Item D (Stats screen): small period-comparison helpers built on top of the same raw
 * FuelEntry/Expense lists NativeAppState already exposes. The overall average km/L figure is
 * NOT recomputed here — the Stats screen reads NativeAppState.summary.averageKmPerLiter, which
 * is calculateFuelSummary()'s output (see FuelSummary.kt), directly.
 */
private fun yearOf(date: String): Int? = runCatching { LocalDate.parse(date).year }.getOrNull()
private fun yearMonthOf(date: String): Pair<Int, Int>? =
    runCatching { LocalDate.parse(date) }.getOrNull()?.let { it.year to it.monthValue }

fun fuelCountByYear(entries: List<FuelEntry>, year: Int): Int = entries.count { yearOf(it.date) == year }

fun fuelCountByMonth(entries: List<FuelEntry>, year: Int, month: Int): Int =
    entries.count { yearMonthOf(it.date) == year to month }

fun fuelLitersByYear(entries: List<FuelEntry>, year: Int): Double =
    entries.filter { yearOf(it.date) == year }.sumOf { it.liters }

fun fuelLitersByMonth(entries: List<FuelEntry>, year: Int, month: Int): Double =
    entries.filter { yearMonthOf(it.date) == year to month }.sumOf { it.liters }

fun expenseTotalByYear(expenses: List<Expense>, year: Int): Double =
    expenses.filterNot(Expense::income).filter { yearOf(it.date) == year }.sumOf { it.amount }

fun expenseTotalByMonth(expenses: List<Expense>, year: Int, month: Int): Double =
    expenses.filterNot(Expense::income).filter { yearMonthOf(it.date) == year to month }.sumOf { it.amount }

/**
 * Distance driven in a period, derived from odometer deltas between consecutive full-tank
 * fill-ups — the same interval definition calculateFuelSummary()/calculatePerEntryKmPerLiter()
 * use — attributed to the later fill-up's date.
 */
private fun distanceIntervals(entries: List<FuelEntry>): List<Pair<String, Double>> {
    val ordered = entries.sortedWith(compareBy<FuelEntry>({ it.date }, { it.time }, { it.odometerKm })).filter { it.fullTank }
    val result = mutableListOf<Pair<String, Double>>()
    for (index in 1 until ordered.size) {
        val previous = ordered[index - 1]
        val current = ordered[index]
        val intervalDistance = current.odometerKm - previous.odometerKm
        if (intervalDistance > 0) result += current.date to intervalDistance
    }
    return result
}

fun distanceByYear(entries: List<FuelEntry>, year: Int): Double =
    distanceIntervals(entries).filter { (date, _) -> yearOf(date) == year }.sumOf { it.second }

fun distanceByMonth(entries: List<FuelEntry>, year: Int, month: Int): Double =
    distanceIntervals(entries).filter { (date, _) -> yearMonthOf(date) == year to month }.sumOf { it.second }
