package com.redn.farm.data.local.security

import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordManager {
    companion object {
        private const val ITERATIONS = 65536
        private const val KEY_LENGTH = 128
        private const val ALGORITHM = "PBKDF2WithHmacSHA1"
        private const val SALT_LENGTH = 16

        fun hashPassword(password: String): String {
            val random = SecureRandom()
            val salt = ByteArray(SALT_LENGTH)
            random.nextBytes(salt)

            val spec: KeySpec = PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
            )

            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val hash = factory.generateSecret(spec).encoded

            // Combine salt and hash
            val combined = ByteArray(salt.size + hash.size)
            System.arraycopy(salt, 0, combined, 0, salt.size)
            System.arraycopy(hash, 0, combined, salt.size, hash.size)

            return Base64.getEncoder().encodeToString(combined)
        }

        fun verifyPassword(password: String, storedHash: String): Boolean {
            try {
                // Decode the stored hash
                val combined = Base64.getDecoder().decode(storedHash)

                // Extract salt and hash
                val salt = ByteArray(SALT_LENGTH)
                val hash = ByteArray(combined.size - SALT_LENGTH)
                System.arraycopy(combined, 0, salt, 0, SALT_LENGTH)
                System.arraycopy(combined, SALT_LENGTH, hash, 0, hash.size)

                // Generate hash for the input password
                val spec: KeySpec = PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH
                )

                val factory = SecretKeyFactory.getInstance(ALGORITHM)
                val testHash = factory.generateSecret(spec).encoded

                // Compare the hashes
                return hash.contentEquals(testHash)
            } catch (e: Exception) {
                return false
            }
        }
    }
} 