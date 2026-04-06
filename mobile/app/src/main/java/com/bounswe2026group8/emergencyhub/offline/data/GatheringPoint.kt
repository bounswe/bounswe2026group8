package com.bounswe2026group8.emergencyhub.offline.ui

data class GatheringPoint(
    val name: String,
    val lat: Double,
    val lon: Double,
    val description: String,
    val type: String,
    val region: String? = null
)