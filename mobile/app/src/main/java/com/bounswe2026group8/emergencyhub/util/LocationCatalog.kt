package com.bounswe2026group8.emergencyhub.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStreamReader

/**
 * Hardcoded country/city/district catalog loaded from assets/locations.json.
 *
 * Lazily loads on first access and caches on the Application context.
 */
object LocationCatalog {

    data class City(
        val name: String,
        val districts: List<String>? = null,
    )

    data class Country(
        val code: String,
        val name: String,
        val cities: List<City>,
    )

    data class Root(
        @SerializedName("countries") val countries: List<Country>,
    )

    @Volatile
    private var cached: List<Country>? = null

    fun countries(context: Context): List<Country> {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val loaded = context.applicationContext.assets.open("locations.json").use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                    Gson().fromJson(reader, Root::class.java).countries
                }
            }
            val sorted = loaded.sortedBy { it.name }
            cached = sorted
            return sorted
        }
    }

    fun findCountry(context: Context, name: String): Country? =
        countries(context).firstOrNull { it.name == name }

    fun findCity(country: Country, name: String): City? =
        country.cities.firstOrNull { it.name == name }
}
