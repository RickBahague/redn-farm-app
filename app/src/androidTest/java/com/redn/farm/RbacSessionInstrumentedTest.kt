package com.redn.farm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.security.Rbac
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented checks that [SessionManager] role strings line up with [Rbac] write gates
 * (smoke for navigation-denial assumptions on a real device context).
 */
@RunWith(AndroidJUnit4::class)
class RbacSessionInstrumentedTest {

    @Test
    fun sessionPurchasing_canWriteAcquisitions_notOrders() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val sm = SessionManager(ctx)
        sm.endSession()
        sm.createSession("p_test", Rbac.PURCHASING)
        assertTrue(Rbac.canWriteAcquisitions(sm.getRole()))
        assertFalse(Rbac.canWriteOrders(sm.getRole()))
        sm.endSession()
    }

    @Test
    fun sessionFarmer_cannotWriteOrders() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val sm = SessionManager(ctx)
        sm.endSession()
        sm.createSession("f_test", Rbac.FARMER)
        assertFalse(Rbac.canWriteOrders(sm.getRole()))
        assertTrue(Rbac.canWriteFarmOperations(sm.getRole()))
        sm.endSession()
    }
}
