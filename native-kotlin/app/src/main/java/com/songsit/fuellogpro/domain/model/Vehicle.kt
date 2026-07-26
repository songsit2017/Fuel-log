package com.songsit.fuellogpro.domain.model

data class Vehicle(
    val id: String,
    val name: String,
    val ownerUid: String,
    val memberUids: Set<String> = emptySet(),
)

