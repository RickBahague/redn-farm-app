package com.redn.farm.data.local.security

import org.junit.Assert.*
import org.junit.Test

/**
 * Covers BACKLOG TD-08 (weak algorithm) and serves as a regression baseline.
 *
 * Run with: ./gradlew :app:testDebugUnitTest --tests "*.PasswordManagerTest"
 */
class PasswordManagerTest {

    @Test
    fun hashPassword_producesNonEmptyString() {
        val hash = PasswordManager.hashPassword("secret")
        assertTrue("hash should not be blank", hash.isNotBlank())
    }

    @Test
    fun verifyPassword_correctPassword_returnsTrue() {
        val password = "admin123"
        val hash = PasswordManager.hashPassword(password)
        assertTrue(PasswordManager.verifyPassword(password, hash))
    }

    @Test
    fun verifyPassword_wrongPassword_returnsFalse() {
        val hash = PasswordManager.hashPassword("correct")
        assertFalse(PasswordManager.verifyPassword("wrong", hash))
    }

    @Test
    fun hashPassword_sameInputProducesDifferentHashes() {
        // Salt must be random — two hashes of the same password must differ
        val password = "password123"
        val hash1 = PasswordManager.hashPassword(password)
        val hash2 = PasswordManager.hashPassword(password)
        assertNotEquals(
            "Two hashes of the same password should differ (random salt)",
            hash1, hash2
        )
    }

    @Test
    fun verifyPassword_bothHashesVerify_withOriginalPassword() {
        val password = "password123"
        val hash1 = PasswordManager.hashPassword(password)
        val hash2 = PasswordManager.hashPassword(password)
        // Both independently generated hashes must verify with the same password
        assertTrue(PasswordManager.verifyPassword(password, hash1))
        assertTrue(PasswordManager.verifyPassword(password, hash2))
    }

    @Test
    fun verifyPassword_malformedHash_returnsFalse() {
        // Should not throw; must return false gracefully
        assertFalse(PasswordManager.verifyPassword("password", "not-a-valid-hash"))
        assertFalse(PasswordManager.verifyPassword("password", ""))
    }

    @Test
    fun verifyPassword_emptyPassword_doesNotCrash() {
        val hash = PasswordManager.hashPassword("")
        assertTrue(PasswordManager.verifyPassword("", hash))
        assertFalse(PasswordManager.verifyPassword("notempty", hash))
    }

    // -------------------------------------------------------------------------
    // TD-08 documentation test — will need updating when algorithm is upgraded
    // -------------------------------------------------------------------------

    @Test
    fun hashPassword_algorithmIsDocumented() {
        // This test captures the current (weak) algorithm so a future change to
        // PBKDF2WithHmacSHA256 (BACKLOG TD-08) is explicit and intentional.
        // When upgrading: update this constant and add migration logic for existing hashes.
        val expectedAlgorithm = "PBKDF2WithHmacSHA1"
        // Access via reflection to avoid a direct dependency on private constants
        val field = PasswordManager::class.java.getDeclaredField("ALGORITHM")
        field.isAccessible = true
        val actual = field.get(null) as String
        assertEquals(
            "Algorithm changed — ensure existing password hashes are migrated (see BACKLOG TD-08)",
            expectedAlgorithm, actual
        )
    }
}
