package com.songsit.fuellogpro.data

import com.songsit.fuellogpro.data.local.ExpenseDao
import com.songsit.fuellogpro.data.local.ExpenseEntity
import com.songsit.fuellogpro.data.local.PhotoUris
import com.songsit.fuellogpro.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalExpenseRepository(
    private val dao: ExpenseDao,
    private val deletionRecorder: LocalDeletionRecorder,
) {
    fun observe(vehicleId: String): Flow<List<Expense>> =
        dao.observeForVehicle(vehicleId).map { expenses -> expenses.map(ExpenseEntity::toDomain) }

    fun observeCategorySuggestions(vehicleId: String): Flow<List<String>> =
        dao.observeDistinctCategories(vehicleId)

    suspend fun add(
        vehicleId: String,
        date: String,
        time: String,
        category: String,
        description: String,
        amount: Double,
        odometerKm: Double?,
        income: Boolean,
        recurring: Boolean,
        reminderDate: String?,
        photoUri: String? = null,
    ) {
        dao.upsert(
            ExpenseEntity(
                id = UUID.randomUUID().toString(),
                vehicleId = vehicleId,
                date = date,
                time = time,
                category = category.trim(),
                description = description.trim(),
                amount = amount,
                odometerKm = odometerKm,
                income = income,
                recurring = recurring,
                reminderDate = reminderDate?.takeIf(String::isNotBlank),
                createdAt = System.currentTimeMillis(),
                photoUri = photoUri,
            ),
        )
    }

    suspend fun update(
        id: String,
        vehicleId: String,
        date: String,
        time: String,
        category: String,
        description: String,
        amount: Double,
        odometerKm: Double?,
        income: Boolean,
        recurring: Boolean,
        reminderDate: String?,
        photoUri: String? = null,
    ) {
        val existing = dao.getById(id)
        dao.upsert(
            ExpenseEntity(
                id = id,
                vehicleId = vehicleId,
                date = date,
                time = time,
                category = category.trim(),
                description = description.trim(),
                amount = amount,
                odometerKm = odometerKm,
                income = income,
                recurring = recurring,
                reminderDate = reminderDate?.takeIf(String::isNotBlank),
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                photoUri = photoUri ?: existing?.photoUri,
            ),
        )
    }

    suspend fun delete(id: String) = deletionRecorder.delete(
        collectionName = "expenses",
        recordId = id,
        vehicleId = { dao.getById(id)?.vehicleId },
        deleteLocal = { dao.deleteById(id) },
    )
    suspend fun deleteForVehicle(vehicleId: String) = dao.deleteForVehicle(vehicleId)
}

private fun ExpenseEntity.toDomain() = Expense(
    id = id,
    vehicleId = vehicleId,
    date = date,
    time = time,
    category = category,
    description = description,
    amount = amount,
    odometerKm = odometerKm,
    income = income,
    recurring = recurring,
    reminderDate = reminderDate,
    photoUri = photoUri,
    photoUrls = PhotoUris.split(photoUri),
)
