package com.prog7314.IdleLoadsheddingMobile.utils

import java.security.MessageDigest

/**
 * Utility for generating short, user-friendly friend codes from Firebase UIDs.
 * 
 * Firebase UIDs are 28-character random strings which are difficult to share.
 * This utility generates 8-character codes using Base62 encoding for readability.
 * 
 * Example transformations:
 * - Firebase UID: "abc123xyz789def456ghi012" -> Friend Code: "K7M9P2WX"
 * - Short, memorable, and easy to type
 */
object FriendCodeGenerator {
    
    private const val BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private const val CODE_LENGTH = 8
    
    /**
     * Generates a short, user-friendly friend code from a Firebase UID.
     * Uses SHA-256 hash and Base62 encoding for consistency and readability.
     * 
     * @param firebaseUid The full Firebase user ID (28 characters)
     * @return An 8-character alphanumeric friend code (e.g., "K7M9P2WX")
     */
    fun generateFriendCode(firebaseUid: String): String {
        // Hash the UID for consistent transformation
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(firebaseUid.toByteArray())
        
        // Convert hash to Base62 for human readability
        return encodeBase62(hash).take(CODE_LENGTH)
    }
    
    /**
     * Validates if a friend code has the correct format.
     * Friend codes must be exactly 8 alphanumeric characters.
     * 
     * @param code The friend code to validate
     * @return true if the code format is valid
     */
    fun isValidFriendCodeFormat(code: String): Boolean {
        return code.length == CODE_LENGTH && code.all { it in BASE62_CHARS }
    }
    
    /**
     * Formats a friend code for display with a visual separator for readability.
     * Example: "K7M9P2WX" -> "K7M9-P2WX"
     * 
     * @param code The 8-character friend code
     * @return Formatted friend code with separator
     */
    fun formatForDisplay(code: String): String {
        return if (code.length == CODE_LENGTH) {
            "${code.substring(0, 4)}-${code.substring(4)}"
        } else {
            code
        }
    }
    
    /**
     * Removes formatting from a friend code for processing.
     * Example: "K7M9-P2WX" -> "K7M9P2WX"
     * 
     * @param formattedCode The formatted friend code
     * @return Unformatted friend code
     */
    fun removeFormatting(formattedCode: String): String {
        return formattedCode.replace("-", "").uppercase()
    }
    
    /**
     * Encodes byte array to Base62 string.
     * Base62 uses 0-9, A-Z, a-z for human-friendly codes.
     */
    private fun encodeBase62(bytes: ByteArray): String {
        var num = java.math.BigInteger(1, bytes)
        val base = java.math.BigInteger.valueOf(62)
        val result = StringBuilder()
        
        while (num > java.math.BigInteger.ZERO) {
            val remainder = num.mod(base).toInt()
            result.append(BASE62_CHARS[remainder])
            num = num.divide(base)
        }
        
        return result.reverse().toString().ifEmpty { "0" }
    }
}
