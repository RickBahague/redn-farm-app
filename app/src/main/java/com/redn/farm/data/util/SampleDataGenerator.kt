package com.redn.farm.data.util

import com.redn.farm.data.model.Customer
import com.redn.farm.data.model.CustomerType
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.ProductPrice
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.AcquisitionLocation
import java.time.LocalDateTime
import kotlin.random.Random

object SampleDataGenerator {
    private val firstNames = listOf(
        "John", "Jane", "Michael", "Sarah", "David", "Emma", "James", "Maria",
        "Robert", "Lisa", "William", "Anna", "Richard", "Emily", "Thomas", "Sofia"
    )

    private val lastNames = listOf(
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller",
        "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson"
    )

    private val cities = listOf(
        "Manila", "Quezon City", "Davao", "Cebu", "Makati", "Pasig",
        "Taguig", "Pasay", "Mandaluyong", "San Juan"
    )

    private val provinces = listOf(
        "Metro Manila", "Cebu", "Davao", "Rizal", "Cavite", "Laguna",
        "Pampanga", "Bulacan", "Batangas", "Nueva Ecija"
    )

    private val streetNames = listOf(
        "Main St.", "Oak Avenue", "Maple Road", "Cedar Lane", "Pine Street",
        "Rizal Avenue", "Mabini Street", "Quezon Boulevard", "Bonifacio Drive"
    )

    private val productNames = listOf(
        "Tomatoes", "Carrots", "Potatoes", "Onions", "Garlic",
        "Lettuce", "Cabbage", "Bell Peppers", "Eggplant", "String Beans",
        "Sweet Corn", "Cucumber", "Squash", "Bitter Gourd", "Okra",
        "Spinach", "Kangkong", "Pechay", "Mustard Leaves", "Radish"
    )

    private val productDescriptions = listOf(
        "Fresh and locally grown",
        "Organically farmed",
        "Premium quality",
        "Freshly harvested",
        "Best quality produce",
        "Farm-fresh vegetables",
        "Naturally grown",
        "Pesticide-free",
        "Hand-picked fresh",
        "Sustainably farmed"
    )

    private val acquisitionLocations = listOf(
        AcquisitionLocation.FARM,
        AcquisitionLocation.MARKET,
        AcquisitionLocation.SUPPLIER
    )

    data class ProductWithPrice(
        val product: Product,
        val price: ProductPrice
    )

    fun generateSampleCustomers(count: Int = 20): List<Customer> {
        return (1..count).map { generateCustomer() }
    }

    fun generateSampleProducts(count: Int = 20): List<ProductWithPrice> {
        val usedNames = mutableSetOf<String>()
        return (1..count).map { generateProductWithPrice(usedNames) }
    }

    private fun generateProductWithPrice(usedNames: MutableSet<String>): ProductWithPrice {
        var productName = productNames.random()
        // Ensure unique product names
        while (usedNames.contains(productName)) {
            productName = productNames.random()
        }
        usedNames.add(productName)

        val description = productDescriptions.random()
        val isPerKg = Random.nextBoolean()
        val unitType = if (isPerKg) "kg" else "piece"
        val productId = generateProductId(productName)
        
        val product = Product(
            product_id = productId,
            product_name = productName,
            product_description = description,
            unit_type = unitType,
            is_active = true
        )

        val price = ProductPrice(
            product_id = productId,
            per_kg_price = if (isPerKg) generatePrice() else null,
            per_piece_price = if (!isPerKg) generatePrice() else null,
            date_created = LocalDateTime.now()
        )

        return ProductWithPrice(product, price)
    }

    private fun generatePrice(): Double {
        // Generate a random price between 20 and 200 with 2 decimal places
        return String.format("%.2f", Random.nextDouble(20.0, 200.0)).toDouble()
    }

    private fun generateProductId(productName: String): String {
        // Generate a product ID based on the first 3 letters of the product name and a random number
        val prefix = productName.take(3).uppercase()
        val number = String.format("%03d", Random.nextInt(1, 1000))
        return "$prefix$number"
    }

    fun generateCustomer(): Customer {
        val firstName = firstNames.random()
        val lastName = lastNames.random()
        val streetNumber = Random.nextInt(1, 999)
        val street = streetNames.random()
        val city = cities.random()
        val province = provinces.random()
        val postalCode = String.format("%04d", Random.nextInt(1000, 9999))
        
        return Customer(
            firstname = firstName,
            lastname = lastName,
            contact = generatePhoneNumber(),
            customer_type = CustomerType.values().random(),
            address = "$streetNumber $street",
            city = city,
            province = province,
            postal_code = postalCode,
            date_created = LocalDateTime.now(),
            date_updated = LocalDateTime.now()
        )
    }

    private fun generatePhoneNumber(): String {
        // Generate Philippine mobile number format (e.g., 09XX-XXX-XXXX)
        val prefix = "09"
        val secondPart = Random.nextInt(10, 99)
        val thirdPart = Random.nextInt(100, 999)
        val lastPart = Random.nextInt(1000, 9999)
        return "$prefix$secondPart$thirdPart$lastPart"
    }

    fun generateSampleAcquisitions(products: List<Product>, count: Int = 30): List<Acquisition> {
        return (1..count).map { 
            generateAcquisition(products.random())
        }
    }

    private fun generateAcquisition(product: Product): Acquisition {
        val isPerKg = product.unit_type == "kg"
        val quantity = if (isPerKg) {
            // Generate quantity between 1 and 100 kg with 2 decimal places
            String.format("%.2f", Random.nextDouble(1.0, 100.0)).toDouble()
        } else {
            // Generate whole number quantity between 1 and 50 pieces
            Random.nextInt(1, 50).toDouble()
        }
        
        val pricePerUnit = generatePrice()
        val totalAmount = quantity * pricePerUnit

        return Acquisition(
            product_id = product.product_id,
            product_name = product.product_name,
            quantity = quantity,
            price_per_unit = pricePerUnit,
            total_amount = totalAmount,
            is_per_kg = isPerKg,
            date_acquired = System.currentTimeMillis(),
            location = acquisitionLocations.random()
        )
    }
} 