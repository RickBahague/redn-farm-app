package com.redn.farm.utils

import java.text.NumberFormat
import java.util.*

object CurrencyFormatter {
    private val formatter = NumberFormat.getCurrencyInstance(Locale("en", "PH")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        currency = Currency.getInstance("PHP")
    }

    fun format(amount: Double): String {
        return formatter.format(amount)
    }
} 