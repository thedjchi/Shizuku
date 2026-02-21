package moe.shizuku.manager.utils

import java.security.SecureRandom

private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
private const val DIGITS = "0123456789"
private const val CHARS = UPPERCASE + LOWERCASE + DIGITS

private const val DEFAULT_TOKEN_LENGTH = 24

private val random = SecureRandom()

object Token {

    @JvmStatic
    fun generateToken(): String = generateToken(DEFAULT_TOKEN_LENGTH)

    @JvmStatic
    fun generateToken(length: Int): String =
        (1..length)
            .map { CHARS[random.nextInt(CHARS.length)] }
            .joinToString("")
            
}
