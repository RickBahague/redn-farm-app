package com.redn.farm.data.util

import com.redn.farm.data.repository.CustomerRepository
import com.redn.farm.data.repository.ProductRepository
import com.redn.farm.data.repository.AcquisitionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object DatabasePopulator {
    /**
     * Populates the database with sample customer data
     * @param customerRepository The customer repository instance
     * @param count Number of sample customers to generate (default: 20)
     */
    suspend fun populateCustomers(
        customerRepository: CustomerRepository,
        count: Int = 20
    ) = withContext(Dispatchers.IO) {
        val sampleCustomers = SampleDataGenerator.generateSampleCustomers(count)
        sampleCustomers.forEach { customer ->
            customerRepository.addCustomer(customer)
        }
    }

    /**
     * Populates the database with sample product data
     * @param productRepository The product repository instance
     * @param count Number of sample products to generate (default: 20)
     */
    suspend fun populateProducts(
        productRepository: ProductRepository,
        count: Int = 20
    ) = withContext(Dispatchers.IO) {
        val sampleProducts = SampleDataGenerator.generateSampleProducts(count)
        sampleProducts.forEach { productWithPrice ->
            productRepository.insertProduct(productWithPrice.product)
            productRepository.insertProductPrice(productWithPrice.price)
        }
    }

    /**
     * Populates the database with sample acquisition data
     * @param acquisitionRepository The acquisition repository instance
     * @param productRepository The product repository instance (needed to get product list)
     * @param count Number of sample acquisitions to generate (default: 30)
     */
    suspend fun populateAcquisitions(
        acquisitionRepository: AcquisitionRepository,
        productRepository: ProductRepository,
        count: Int = 30
    ) = withContext(Dispatchers.IO) {
        val products = productRepository.getAllProducts().first()
        if (products.isEmpty()) {
            throw IllegalStateException("No products found. Please generate sample products first.")
        }
        
        val sampleAcquisitions = SampleDataGenerator.generateSampleAcquisitions(products, count)
        sampleAcquisitions.forEach { acquisition ->
            acquisitionRepository.addAcquisition(acquisition)
        }
    }
} 