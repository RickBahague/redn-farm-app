package com.redn.farm.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for `Rbac` write helpers vs `docs/user_roles.md` matrix. */
class RbacWritePermissionsTest {

    @Test
    fun admin_canWriteEverythingExceptImplicitReadOnlyProductView() {
        val r = Rbac.ADMIN
        assertTrue(Rbac.canMutateProducts(r))
        assertTrue(Rbac.canWriteCustomers(r))
        assertTrue(Rbac.canWriteOrders(r))
        assertTrue(Rbac.canWriteAcquisitions(r))
        assertTrue(Rbac.canWriteRemittances(r))
        assertTrue(Rbac.canWriteEmployees(r))
        assertTrue(Rbac.canWriteFarmOperations(r))
        assertTrue(Rbac.canExport(r))
        assertTrue(Rbac.canManageSettingsAndPricing(r))
        assertTrue(Rbac.canManageUsers(r))
    }

    @Test
    fun storeAssistant_ordersCustomersRemittance_productsMutate_noExport() {
        val r = Rbac.STORE_ASSISTANT
        assertTrue(Rbac.canMutateProducts(r))
        assertTrue(Rbac.canWriteCustomers(r))
        assertTrue(Rbac.canWriteOrders(r))
        assertTrue(Rbac.canWriteRemittances(r))
        assertFalse(Rbac.canWriteAcquisitions(r))
        assertFalse(Rbac.canWriteEmployees(r))
        assertFalse(Rbac.canWriteFarmOperations(r))
        assertFalse(Rbac.canExport(r))
        assertFalse(Rbac.canManageSettingsAndPricing(r))
        assertFalse(Rbac.canManageUsers(r))
    }

    @Test
    fun purchasing_acquisitions_productsMutate_noOrders() {
        val r = Rbac.PURCHASING
        assertTrue(Rbac.canMutateProducts(r))
        assertTrue(Rbac.canWriteAcquisitions(r))
        assertFalse(Rbac.canWriteCustomers(r))
        assertFalse(Rbac.canWriteOrders(r))
        assertFalse(Rbac.canWriteRemittances(r))
        assertFalse(Rbac.canWriteFarmOperations(r))
    }

    @Test
    fun farmer_farmOpsOnly_productsReadOnly() {
        val r = Rbac.FARMER
        assertFalse(Rbac.canMutateProducts(r))
        assertTrue(Rbac.canWriteFarmOperations(r))
        assertFalse(Rbac.canWriteOrders(r))
        assertFalse(Rbac.canWriteAcquisitions(r))
    }

    @Test
    fun user_readOnlyProducts() {
        val r = Rbac.USER
        assertFalse(Rbac.canMutateProducts(r))
        assertFalse(Rbac.canWriteOrders(r))
        assertFalse(Rbac.canWriteFarmOperations(r))
    }

    @Test
    fun aliases_normalizeForChecks() {
        assertTrue(Rbac.canWriteOrders("store"))
        assertTrue(Rbac.canWriteAcquisitions("PURCHASING_ASSISTANT"))
    }
}
