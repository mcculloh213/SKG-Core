package ktx.sovereign.core.util

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.*
import java.util.*
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * A Regular Expression pattern to match a valid E-Mail address.
 */
val EMAIL_PATTERN = "[a-zA-Z0-9._-]+@[a-z.]+\\.+[a-z]+"

/**
 * A Regular Expression pattern to match a valid password.
 *
 * *Password Description:*
 *
 *  * `^` &#151; Start of the String
 *  * `(?=.*[0-9])` &#151; Must contain at least one digit
 *  * `(?=.*[a-z])` &#151; Must contain at least one lower-case character
 *  * `(?=.*[A-Z])` &#151; Must contain at least one upper-case character
 *  * `(?=.*[@#$%^&+=])` &#151; Must contain at least one special-character
 *  * `(?=\S+$)` &#151; Must not contain any whitespace
 *  * `.{8,}` &#151; Contains at least 8 characters
 *  * `$` &#151; End of the String
 *
 *
 * @see CONTAINS_DIGIT_PATTERN
 *
 * @see LOWER_CASE_CHARACTER_PATTERN
 *
 * @see UPPER_CASE_CHARACTER_PATTERN
 *
 * @see SPECIAL_CHARACTER_PATTERN
 *
 * @see WHITESPACE_PATTERN
 *
 * @see MIN_SIZE_PATTERN
 */
val PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"

/**
 * A Regular Expression pattern to match at least one digit in a [String].
 */
val CONTAINS_DIGIT_PATTERN = "^(?=.*[0-9]).*$"

/**
 * A Regular Expression pattern to match at least one lower-case character in a [String].
 */
val LOWER_CASE_CHARACTER_PATTERN = "^(?=.*[a-z]).*$"

/**
 * A Regular Expression pattern to match at least one upper-case character in a [String].
 */
val UPPER_CASE_CHARACTER_PATTERN = "^(?=.*[A-Z]).*$"

private val CHARACTER_PATTERN = "^[a-zA-Z]+$"
private val DIGIT_PATTERN = "^[0-9]+$"

/**
 * A Regular Expression pattern to match at least one special character in a [String].
 *
 * *Special Character Constraints:*
 * A special character is defined as:
 *
 *  * @
 *  * #
 *  * $
 *  * %
 *  * ^
 *  * &
 *  * +
 *  * =
 *
 */
val SPECIAL_CHARACTER_PATTERN = "^(?=.*[@#$%^&+=!]).*$"

/**
 * A Regular Expression pattern to match the existence of whitespace in a [String].
 */
val WHITESPACE_PATTERN = "^(?=\\S+$).*$"

/**
 * A *formattable* Regular Expression pattern to match a minimum length
 * requirement for a [String].
 */
val MIN_SIZE_PATTERN = "^.{%s,}$"

/**
 * A *formattable* Regular Expression pattern to match a ranged length
 * requirement for a [String].
 */
val RANGE_SIZE_PATTERN = "^.{%s,%s}%"

/**
 * AES-GCM Algorithm instance with no padding
 */
private val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"

/**
 * The default key-size used to generate a 128-bit key for encryption.
 */
private val KEY_SIZE = 16

/**
 * The default initialization vector (IV) size for Galois/Counter Mode (GCM).
 */
private val INITIALIZATION_VECTOR_SIZE = 12

/**
 * The default GCM Authentication Tag size.
 */
private val AUTHENTICATION_TAG_SIZE = 128

/**
 * String utility method to check if a `String` is either `null`
 * or empty.
 *
 * @param cs The [CharSequence] to validate.
 * @return `true` if the parameter `cs` is `null` or empty;
 * `false` otherwise.
 */
fun isNullOrEmpty(cs: CharSequence?): Boolean {
    return cs == null || cs.length == 0
}

/**
 * String utility method to validate an E-Mail address using the RegEx pattern
 * `[a-zA-Z0-9._-]+@[a-z]+\.+[a-z]+`.
 *
 * @param s The `String` to validate.
 * @return `true` if the parameter `s` is a valid E-Mail address;
 * `false` otherwise;
 *
 * @see EMAIL_PATTERN
 */
fun isValidEmail(s: String?): Boolean {
    return !isNullOrEmpty(s) && s!!.matches(EMAIL_PATTERN.toRegex())
}

/**
 * String utility method to validate a password using the RegEx pattern
 * `^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\S+$).{8,}$`.
 *
 * @param s The `String` to validate.
 * @return `true` if the parameter `s` is a valid password;
 * `false` otherwise;
 *
 * @see PASSWORD_PATTERN
 */
fun isValidPassword(s: String?): Boolean {
    return !isNullOrEmpty(s) && s!!.matches(PASSWORD_PATTERN.toRegex())
}

/**
 *
 * @param s
 * @return
 */
fun includesDigit(s: String?): Boolean {
    return !isNullOrEmpty(s) && s!!.matches(CONTAINS_DIGIT_PATTERN.toRegex())
}

/**
 *
 * @param s
 * @return
 */
fun includesLowerCaseCharacter(s: String?): Boolean {
    return !isNullOrEmpty(s) && s!!.matches(LOWER_CASE_CHARACTER_PATTERN.toRegex())
}

/**
 *
 * @param s
 * @return
 */
fun includesUpperCaseCharacter(s: String?): Boolean {
    return !isNullOrEmpty(s) && s!!.matches(UPPER_CASE_CHARACTER_PATTERN.toRegex())
}

fun includesSpecialCharacter(s: String?): Boolean {
    return !isNullOrEmpty(s) && s!!.matches(SPECIAL_CHARACTER_PATTERN.toRegex())
}

fun containsWhitespace(s: String?): Boolean {
    return isNullOrEmpty(s) || !s!!.matches(WHITESPACE_PATTERN.toRegex())
}

fun meetsLengthRequirement(s: String?): Boolean {
    return !isNullOrEmpty(s) && s!!.matches(String.format(MIN_SIZE_PATTERN, 8).toRegex())
}

fun containsCharacter(s: String?): Boolean {
    return !isNullOrEmpty(s) && s!!.matches(CHARACTER_PATTERN.toRegex())
}

fun containsDigit(s: String?): Boolean {
    return !isNullOrEmpty(s) && s!!.matches(DIGIT_PATTERN.toRegex())
}

/**
 * String utility method to digests a `String` using SHA-1.
 *
 * @param s The `String` to digest.
 * @return The SHA-1 digest of parameter `s`.
 */
fun digest(s: String): String {
    val hash = StringBuilder()

    try {
        val md = MessageDigest.getInstance("SHA1")
        md.reset()
        val buffer = s.toByteArray()
        md.update(buffer)
        val digest = md.digest()

        for (hex in digest)
            hash.append(hexlify(hex))
    } catch (ex: NoSuchAlgorithmException) {
        hash.append(Integer.toHexString(s.hashCode()))
    }

    return hash.toString()
}

/**
 * Encodes a `byte[]` to a [Base64] [String].
 *
 * @param bytes The `byte[]` to encode.
 * @return The `Base64` encoded `String`.
 */
fun encode(bytes: ByteArray): String {
    return Base64.encodeToString(bytes, Base64.DEFAULT)
}

/**
 * Decodes a [Base64] [String] to a `byte[]`.
 *
 * @param payload The [String] to decode.
 * @return The decoded `byte[]`.
 */
fun decode(payload: String): ByteArray {
    return Base64.decode(payload, Base64.DEFAULT)
}

/**
 * Encrypts a [String] using AES-GCM with a randomly generated 128 bit key.
 *
 * @param s The `String` to encrypt.
 * @return The encrypted [SecureString].
 *
 * @throws NoSuchAlgorithmException if `AES/GCM/NoPadding`
 * is in an invalid format, or if no Provider supports a
 * CipherSpi implementation for the specified algorithm.
 * @throws NoSuchPaddingException if `AES/GCM/NoPadding`
 * contains a padding scheme that is not available.
 * @throws InvalidAlgorithmParameterException if the given
 * algorithm parameters are inappropriate for this
 * cipher, or this cipher requires algorithm
 * parameters and `params` is null, or the
 * given algorithm parameters imply a cryptographic
 * strength that would exceed the legal limits (as
 * determined from the configured jurisdiction
 * policy files).
 * @throws InvalidKeyException if the given key is inappropriate for
 * initializing this cipher, or its key-size exceeds the
 * maximum allowable key-size (as determined from the
 * configured jurisdiction policy files).
 * @throws BadPaddingException if this cipher is in decryption mode,
 * and (un)padding has been requested, but the decrypted data is not
 * bounded by the appropriate padding bytes
 * @throws IllegalBlockSizeException if this cipher is a block cipher,
 * no padding has been requested (only in encryption mode), and the total
 * input length of the data processed by this cipher is not a multiple of
 * block size; or if this encryption algorithm is unable to
 * process the input data provided.
 *
 * @see KEY_SIZE
 *
 * @see INITIALIZATION_VECTOR_SIZE
 *
 * @see AUTHENTICATION_TAG_SIZE
 *
 * [
 * Encrypting a file using a {@code CipherInputStream}
](https://stackoverflow.com/questions/41413439/encrypting-and-decrypting-a-file-using-cipherinputstream-and-cipheroutputstream) *
 * [
 * &quot;Security Best Practices: Symmetric Encryption with AES in Java and Android&quot;
](https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9) *
 */
@Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidAlgorithmParameterException::class, InvalidKeyException::class, BadPaddingException::class, IllegalBlockSizeException::class)
fun encrypt(s: String): SecureString {
    val random = SecureRandom()
    val key = ByteArray(KEY_SIZE)
    random.nextBytes(key)

    val iv = ByteArray(INITIALIZATION_VECTOR_SIZE)
    random.nextBytes(iv)

    val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"),
            GCMParameterSpec(AUTHENTICATION_TAG_SIZE, iv))
    val cipherText = cipher.doFinal(s.toByteArray())

    val message = ByteBuffer.allocate(4 + iv.size + cipherText.size)
            .putInt(iv.size).put(iv).put(cipherText)
            .array()

    val encrypted = SecureString(message, encode(message))
    Arrays.fill(key, 0.toByte())
    Arrays.fill(iv, 0.toByte())
    Arrays.fill(message, 0.toByte())

    return encrypted
}

/**
 * Decrypts a [String] using AES-GCM.
 *
 * @param key The [Base64] encoded secret key.
 * @param encrypted The `Base64 String` to decrypt.
 * @return The decrypted `String`.
 *
 * @throws NoSuchAlgorithmException if `AES/GCM/NoPadding`
 * is in an invalid format, or if no Provider supports a
 * CipherSpi implementation for the specified algorithm.
 * @throws NoSuchPaddingException if `AES/GCM/NoPadding`
 * contains a padding scheme that is not available.
 * @throws InvalidAlgorithmParameterException if the given
 * algorithm parameters are inappropriate for this
 * cipher, or this cipher requires algorithm
 * parameters and `params` is null, or the
 * given algorithm parameters imply a cryptographic
 * strength that would exceed the legal limits (as
 * determined from the configured jurisdiction
 * policy files).
 * @throws InvalidKeyException if the given key is inappropriate for
 * initializing this cipher, or its key-size exceeds the
 * maximum allowable key-size (as determined from the
 * configured jurisdiction policy files).
 * @throws BadPaddingException if this cipher is in decryption mode,
 * and (un)padding has been requested, but the decrypted data is not
 * bounded by the appropriate padding bytes
 * @throws IllegalBlockSizeException if this cipher is a block cipher,
 * no padding has been requested (only in encryption mode), and the total
 * input length of the data processed by this cipher is not a multiple of
 * block size; or if this encryption algorithm is unable to
 * process the input data provided.
 */
@Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidAlgorithmParameterException::class, InvalidKeyException::class, BadPaddingException::class, IllegalBlockSizeException::class)
fun decrypt(key: String, encrypted: String): String {
    val buffer = ByteBuffer.wrap(decode(encrypted))
    val ivLength = buffer.int

    require(!(ivLength < 12 || ivLength >= 16)) { String.format("Initialization Vector length is invalid: %s", ivLength) }

    val iv = ByteArray(ivLength)
    buffer.get(iv)

    val cipherText = ByteArray(buffer.remaining())
    buffer.get(cipherText)

    val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(decode(key), "AES"),
            GCMParameterSpec(AUTHENTICATION_TAG_SIZE, iv))

    val decoded = String(cipher.doFinal(cipherText))
    Arrays.fill(iv, 0.toByte())
    Arrays.fill(cipherText, 0.toByte())

    return decoded
}

fun encodeBitmap(bmp: Bitmap): String {
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
    return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
}

fun hashBitmap(bmp: Bitmap): String {
    return try {
        val md = MessageDigest.getInstance("SHA-256")
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
        md.update(out.toByteArray())
        val digest = md.digest()
        Base64.encodeToString(digest, Base64.DEFAULT)
    } catch (e: NoSuchAlgorithmException) {
        "bitmappo"
    }

}

/**
 * Converts a `byte` to its hexadecimal [String] equivalent.
 *
 * @param word The `byte` to convert to a hex `String`
 * @return The hex `String` equivalent of the input parameter, `word`.
 */
private fun hexlify(word: Byte): String {
    return ((word.toInt() and 0xff) + 0x100).toString(16).substring(1)
}

/**
 * Contains an encrypted Key-Message pair.
 */
class SecureString(key: ByteArray, val message: String) {
    val key: String = encode(key)
}
