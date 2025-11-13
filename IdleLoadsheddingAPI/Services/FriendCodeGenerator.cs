using System.Security.Cryptography;
using System.Text;

namespace IdleLoadsheddingAPI.Services;

/// <summary>
/// Utility for generating short, user-friendly friend codes from Firebase UIDs.
/// This matches the implementation on the Android client side for consistency.
/// </summary>
public static class FriendCodeGenerator
{
    private const string Base62Chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private const int CodeLength = 8;

    /// <summary>
    /// Generates a short, user-friendly friend code from a Firebase UID.
    /// Uses SHA-256 hash and Base62 encoding for consistency and readability.
    /// </summary>
    /// <param name="firebaseUid">The full Firebase user ID (28 characters)</param>
    /// <returns>An 8-character alphanumeric friend code (e.g., "K7M9P2WX")</returns>
    public static string GenerateFriendCode(string firebaseUid)
    {
        // Hash the UID for consistent transformation
        var hash = SHA256.HashData(Encoding.UTF8.GetBytes(firebaseUid));
        
        // Convert hash to Base62 for human readability
        var encoded = EncodeBase62(hash);
        
        // Take the first 8 characters
        return encoded.Length >= CodeLength 
            ? encoded[..CodeLength] 
            : encoded.PadRight(CodeLength, '0');
    }

    /// <summary>
    /// Validates if a friend code has the correct format.
    /// Friend codes must be exactly 8 alphanumeric characters.
    /// </summary>
    /// <param name="code">The friend code to validate</param>
    /// <returns>true if the code format is valid</returns>
    public static bool IsValidFriendCodeFormat(string code)
    {
        if (string.IsNullOrWhiteSpace(code) || code.Length != CodeLength)
        {
            return false;
        }

        return code.All(c => Base62Chars.Contains(c));
    }

    /// <summary>
    /// Formats a friend code for display with a visual separator for readability.
    /// Example: "K7M9P2WX" -> "K7M9-P2WX"
    /// </summary>
    /// <param name="code">The 8-character friend code</param>
    /// <returns>Formatted friend code with separator</returns>
    public static string FormatForDisplay(string code)
    {
        if (code.Length == CodeLength)
        {
            return $"{code[..4]}-{code[4..]}";
        }
        return code;
    }

    /// <summary>
    /// Removes formatting from a friend code for processing.
    /// Example: "K7M9-P2WX" -> "K7M9P2WX"
    /// </summary>
    /// <param name="formattedCode">The formatted friend code</param>
    /// <returns>Unformatted friend code</returns>
    public static string RemoveFormatting(string formattedCode)
    {
        return formattedCode.Replace("-", "").ToUpperInvariant();
    }

    /// <summary>
    /// Encodes byte array to Base62 string.
    /// Base62 uses 0-9, A-Z, a-z for human-friendly codes.
    /// </summary>
    private static string EncodeBase62(byte[] bytes)
    {
        var num = new System.Numerics.BigInteger(bytes.Concat(new byte[] { 0 }).ToArray());
        var base62 = new System.Numerics.BigInteger(62);
        var result = new StringBuilder();

        while (num > System.Numerics.BigInteger.Zero)
        {
            var remainder = (int)(num % base62);
            result.Insert(0, Base62Chars[remainder]);
            num /= base62;
        }

        return result.Length > 0 ? result.ToString() : "0";
    }
}
