package com.bounswe2026group8.emergencyhub.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.util.LocationCatalog

/**
 * Three-step modal: country → city → district (skipped if the city has none).
 *
 * Each step shows a searchable list. Selecting on the final step invokes
 * [onSelect] with the resolved triple and dismisses the dialog.
 */
class LocationPickerDialog(
    private val context: Context,
    private val initialCountry: String? = null,
    private val initialCity: String? = null,
    private val initialDistrict: String? = null,
    private val onSelect: (country: String, city: String, district: String) -> Unit,
) {

    private enum class Step { COUNTRY, CITY, DISTRICT }

    private var step: Step = Step.COUNTRY
    private var country: LocationCatalog.Country? = null
    private var city: LocationCatalog.City? = null

    private lateinit var dialog: AlertDialog
    private lateinit var titleView: TextView
    private lateinit var backButton: TextView
    private lateinit var searchInput: EditText
    private lateinit var recycler: RecyclerView
    private val adapter = ItemsAdapter { value, hasMore ->
        onItemPicked(value, hasMore)
    }
    private var query: String = ""

    fun show() {
        val root = LayoutInflater.from(context).inflate(R.layout.dialog_location_picker, null)
        titleView = root.findViewById(R.id.locationPickerTitle)
        backButton = root.findViewById(R.id.locationPickerBack)
        searchInput = root.findViewById(R.id.locationPickerSearch)
        recycler = root.findViewById(R.id.locationPickerList)

        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                query = (s?.toString() ?: "").trim()
                refreshList()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        backButton.setOnClickListener { goBack() }

        dialog = AlertDialog.Builder(context)
            .setView(root)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.show()

        // Seed initial selection if provided so the user lands on the same screen they left.
        country = initialCountry?.let { LocationCatalog.findCountry(context, it) }
        city = country?.let { c -> initialCity?.let { LocationCatalog.findCity(c, it) } }
        step = when {
            country != null && city != null && (city?.districts?.isNotEmpty() == true) -> Step.DISTRICT
            country != null -> Step.CITY
            else -> Step.COUNTRY
        }
        refreshList()
    }

    private fun goBack() {
        when (step) {
            Step.DISTRICT -> { step = Step.CITY; city = null; query = ""; searchInput.setText("") }
            Step.CITY -> { step = Step.COUNTRY; country = null; query = ""; searchInput.setText("") }
            Step.COUNTRY -> dialog.dismiss()
        }
        refreshList()
    }

    private fun onItemPicked(value: String, hasMore: Boolean) {
        when (step) {
            Step.COUNTRY -> {
                country = LocationCatalog.findCountry(context, value)
                step = Step.CITY
                query = ""
                searchInput.setText("")
                refreshList()
            }
            Step.CITY -> {
                city = country?.let { LocationCatalog.findCity(it, value) }
                if (hasMore) {
                    step = Step.DISTRICT
                    query = ""
                    searchInput.setText("")
                    refreshList()
                } else {
                    finish("")
                }
            }
            Step.DISTRICT -> finish(value)
        }
    }

    private fun finish(district: String) {
        val countryName = country?.name ?: return
        val cityName = city?.name ?: return
        onSelect(countryName, cityName, district)
        dialog.dismiss()
    }

    private fun refreshList() {
        val q = query.lowercase()
        when (step) {
            Step.COUNTRY -> {
                titleView.text = context.getString(R.string.location_picker_choose_country)
                backButton.visibility = View.GONE
                val items = LocationCatalog.countries(context)
                    .filter { q.isEmpty() || it.name.lowercase().contains(q) }
                    .map { ItemsAdapter.Row(value = it.name, hasMore = it.cities.isNotEmpty()) }
                adapter.submit(items)
            }
            Step.CITY -> {
                val c = country ?: return
                titleView.text = context.getString(R.string.location_picker_choose_city, c.name)
                backButton.visibility = View.VISIBLE
                val items = c.cities
                    .sortedBy { it.name }
                    .filter { q.isEmpty() || it.name.lowercase().contains(q) }
                    .map { ItemsAdapter.Row(value = it.name, hasMore = !it.districts.isNullOrEmpty()) }
                adapter.submit(items)
            }
            Step.DISTRICT -> {
                val cityName = city?.name ?: return
                titleView.text = context.getString(R.string.location_picker_choose_district, cityName)
                backButton.visibility = View.VISIBLE
                val districts = (city?.districts ?: emptyList())
                    .sorted()
                    .filter { q.isEmpty() || it.lowercase().contains(q) }
                    .map { ItemsAdapter.Row(value = it, hasMore = false) }
                adapter.submit(districts)
            }
        }
    }

    private class ItemsAdapter(
        val onClick: (value: String, hasMore: Boolean) -> Unit,
    ) : RecyclerView.Adapter<ItemsAdapter.VH>() {

        data class Row(val value: String, val hasMore: Boolean)

        private val rows = mutableListOf<Row>()

        fun submit(newRows: List<Row>) {
            rows.clear()
            rows.addAll(newRows)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val row = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                val pad = (12 * parent.resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            val name = TextView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f,
                )
                setTextColor(ContextCompat.getColor(parent.context, R.color.text_primary))
                textSize = 15f
            }
            val arrow = TextView(parent.context).apply {
                text = "›"
                setTextColor(ContextCompat.getColor(parent.context, R.color.text_secondary))
                textSize = 18f
            }
            row.addView(name)
            row.addView(arrow)
            return VH(row, name, arrow)
        }

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = rows[position]
            holder.name.text = row.value
            holder.arrow.visibility = if (row.hasMore) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener { onClick(row.value, row.hasMore) }
        }

        class VH(
            itemView: View,
            val name: TextView,
            val arrow: TextView,
        ) : RecyclerView.ViewHolder(itemView)
    }
}
