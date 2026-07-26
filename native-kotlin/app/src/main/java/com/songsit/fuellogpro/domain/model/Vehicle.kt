package com.songsit.fuellogpro.domain.model

data class Vehicle(
    val id: String,
    val name: String,
    val registration: String = "",
    val fuelType: String = "",
)
