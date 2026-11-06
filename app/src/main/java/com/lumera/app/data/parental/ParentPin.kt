package com.lumera.app.data.parental

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

data class PinPrompt(val id: Long, val reason: String)

@Singleton
class ParentPin @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("cinevo_parent_pin", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val verification = Mutex()
    @Volatile private var waiting: CompletableDeferred<Boolean>? = null
    private var requestId = 0L
    private val _prompt = MutableStateFlow<PinPrompt?>(null)
    val prompt: StateFlow<PinPrompt?> = _prompt
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private var failures = 0
    private var blockedUntil = 0L
    val ratingKeyVersion get() = prefs.getInt("rating_key_version", 0)
    val ratingApiKey get() = prefs.getString("rating_api_key", null)?.takeIf { it.isNotBlank() } ?: com.lumera.app.BuildConfig.TMDB_API_KEY
    val configured get() = prefs.contains("hash") && prefs.contains("salt")

    suspend fun authorize(reason: String): Boolean = mutex.withLock {
        _error.value = null
        val pending = CompletableDeferred<Boolean>()
        waiting = pending
        _prompt.value = PinPrompt(++requestId, reason)
        try { pending.await() } finally { waiting = null; _prompt.value = null }
    }
    fun cancel() { waiting?.complete(false) }

    suspend fun submit(pin: String, confirm: String, ratingKey: String = "") = verification.withLock {
        val request = waiting ?: return@withLock
        if (SystemClock.elapsedRealtime() < blockedUntil) {
            _error.value = "Too many attempts. Wait 30 seconds."; return@withLock
        }
        if (pin.length !in 4..8 || pin.any { it !in '0'..'9' }) {
            _error.value = "Use 4–8 digits."; return@withLock
        }
        if (!configured && pin != confirm) { _error.value = "The PINs do not match."; return@withLock }
        val accepted = try { withContext(Dispatchers.Default) {
            if (!configured) {
                val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val hash = derive(pin, salt)
                if (waiting !== request || request.isCompleted) false else prefs.edit()
                    .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP)).commit()
            } else {
                val salt = Base64.decode(prefs.getString("salt", ""), Base64.NO_WRAP)
                MessageDigest.isEqual(derive(pin, salt), Base64.decode(prefs.getString("hash", ""), Base64.NO_WRAP))
            }
        } } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { false }
        if (accepted && waiting === request && !request.isCompleted) {
            if (ratingKey.isNotBlank()) prefs.edit().putString("rating_api_key", ratingKey.trim()).putInt("rating_key_version", ratingKeyVersion + 1).commit()
            failures = 0; request.complete(true)
        }
        else { failures++; if (failures >= 5) { blockedUntil = SystemClock.elapsedRealtime() + 30_000L; failures = 0 }; _error.value = "Incorrect PIN or unable to save. Try again." }
    }
    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }
}
