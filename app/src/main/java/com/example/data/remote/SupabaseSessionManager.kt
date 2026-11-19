package com.example.data.remote

import android.util.Log
import com.example.data.local.UserPreferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import retrofit2.Response

data class SupabaseAuthState(
    val uid: String,
    val email: String,
    val displayName: String = "",
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtMillis: Long = 0L
)

class SupabaseSessionManager(
    private val prefs: UserPreferencesDataStore
) {
    private val refreshMutex = Mutex()
    private val _authState = MutableStateFlow<SupabaseAuthState?>(null)
    val authState: StateFlow<SupabaseAuthState?> = _authState.asStateFlow()

    val currentUidOrNull: String?
        get() = _authState.value?.uid?.takeIf { it.isNotBlank() && it != "local_user" }

    val currentEmailOrNull: String?
        get() = _authState.value?.email?.takeIf { it.isNotBlank() }

    val isAuthenticated: Boolean
        get() = currentUidOrNull != null && !_authState.value?.accessToken.isNullOrBlank()

    suspend fun restoreSavedSession(): SupabaseAuthState? {
        val saved = prefs.userSessionFlow.first()
        val token = saved.supabaseAccessToken?.takeIf { it.isNotBlank() } ?: return null
        val uid = saved.userId.takeIf { it.isNotBlank() && it != "local_user" } ?: return null
        val email = (saved.supabaseEmail ?: saved.email).trim().lowercase()
        val state = SupabaseAuthState(
            uid = uid,
            email = email,
            displayName = saved.fullName,
            accessToken = token,
            refreshToken = saved.supabaseRefreshToken,
            expiresAtMillis = saved.supabaseExpiresAtMillis
        )
        _authState.value = state
        return refreshSessionIfNeeded(force = false) ?: state
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<SupabaseAuthState> {
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("يرجى إضافة SUPABASE_URL و SUPABASE_ANON_KEY في لوحة Secrets أولاً"))
        return try {
            val response = api.signInWithIdToken(
                apiKey = SupabaseConfig.anonKey,
                body = SupabaseIdTokenAuthRequest(provider = "google", idToken = idToken)
            )
            handleAuthResponse(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Google ID token exchange failed", e)
            Result.failure(wrapAuthException(e))
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<SupabaseAuthState> {
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("يرجى إضافة SUPABASE_URL و SUPABASE_ANON_KEY في لوحة Secrets أولاً"))
        val cleanEmail = email.trim().lowercase()
        return try {
            val response = api.signInWithPassword(
                apiKey = SupabaseConfig.anonKey,
                body = SupabaseAuthRequest(email = cleanEmail, password = password)
            )
            handleAuthResponse(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Email sign in failed", e)
            Result.failure(wrapAuthException(e))
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<SupabaseAuthState> {
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("يرجى إضافة SUPABASE_URL و SUPABASE_ANON_KEY في لوحة Secrets أولاً"))
        val cleanEmail = email.trim().lowercase()
        return try {
            val response = api.signUp(
                apiKey = SupabaseConfig.anonKey,
                body = SupabaseAuthRequest(email = cleanEmail, password = password)
            )
            if (!response.isSuccessful) {
                return Result.failure(IllegalStateException(parseErrorMessage(response)))
            }
            val body = response.body()
                ?: return Result.failure(IllegalStateException("استجابة فارغة من Supabase"))
            val accessToken = body.accessToken
            val uid = body.user?.id
            if (accessToken.isNullOrBlank() || uid.isNullOrBlank()) {
                return Result.failure(
                    IllegalStateException(
                        "تم إنشاء الحساب! يرجى تأكيد رابط التفعيل المرسل لبريدك ثم تسجيل الدخول."
                    )
                )
            }
            persistAuthState(body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Email sign up failed", e)
            Result.failure(wrapAuthException(e))
        }
    }

    suspend fun getValidBearerToken(): String? {
        val current = _authState.value ?: restoreSavedSession() ?: return null
        val active = refreshSessionIfNeeded(force = false) ?: current
        return "Bearer ${active.accessToken}"
    }

    suspend fun refreshSessionIfNeeded(force: Boolean = false): SupabaseAuthState? {
        return refreshMutex.withLock {
            val current = _authState.value ?: return@withLock null
            val now = System.currentTimeMillis()
            val needsRefresh = force || (current.expiresAtMillis in 1L..now + 60_000L)
            if (!needsRefresh) return@withLock current
            val refreshToken = current.refreshToken?.takeIf { it.isNotBlank() } ?: return@withLock current
            val api = SupabaseClientProvider.getApiServiceOrNull() ?: return@withLock current
            val res = runCatching {
                api.refreshSession(
                    apiKey = SupabaseConfig.anonKey,
                    body = SupabaseRefreshTokenRequest(refreshToken)
                )
            }.getOrNull() ?: return@withLock current

            if (res.isSuccessful) {
                val body = res.body()
                if (body != null && !body.accessToken.isNullOrBlank()) {
                    return@withLock persistAuthState(body).getOrNull() ?: current
                }
            } else if (res.code() == 400 || res.code() == 401) {
                Log.w(TAG, "Refresh token rejected (${res.code()}), clearing session")
            }
            current
        }
    }

    suspend fun signOut() {
        val current = _authState.value
        val api = SupabaseClientProvider.getApiServiceOrNull()
        if (api != null && current != null) {
            runCatching {
                api.signOut(SupabaseConfig.anonKey, "Bearer ${current.accessToken}")
            }
        }
        _authState.value = null
        prefs.clearSupabaseAuth()
    }

    private suspend fun handleAuthResponse(response: Response<SupabaseAuthResponse>): Result<SupabaseAuthState> {
        if (!response.isSuccessful) {
            return Result.failure(IllegalStateException(parseErrorMessage(response)))
        }
        val body = response.body()
            ?: return Result.failure(IllegalStateException("استجابة فارغة من خادم Supabase"))
        return persistAuthState(body)
    }

    private suspend fun persistAuthState(body: SupabaseAuthResponse): Result<SupabaseAuthState> {
        val token = body.accessToken?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("لم يتم استلام رمز الجلسة من Supabase"))
        val uid = body.user?.id?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("تعذر قراءة معرف المستخدم من Supabase"))
        val email = body.user.email.orEmpty().trim().lowercase()
        val metaName = (body.user.userMetadata?.get("full_name") as? String)
            ?: (body.user.userMetadata?.get("name") as? String)
            ?: ""
        val expiresInSec = (body.expiresIn ?: 3600L).coerceAtLeast(300L)
        val expiresAtMillis = System.currentTimeMillis() + (expiresInSec * 1000L)

        prefs.saveSupabaseAuth(
            userId = uid,
            email = email,
            accessToken = token,
            refreshToken = body.refreshToken,
            expiresAtMillis = expiresAtMillis
        )
        val state = SupabaseAuthState(
            uid = uid,
            email = email,
            displayName = metaName,
            accessToken = token,
            refreshToken = body.refreshToken,
            expiresAtMillis = expiresAtMillis
        )
        _authState.value = state
        return Result.success(state)
    }

    private fun parseErrorMessage(response: Response<*>): String {
        val raw = runCatching { response.errorBody()?.string() }.getOrNull().orEmpty()
        val parsed = runCatching {
            val json = JSONObject(raw)
            json.optString("error_description")
                .ifBlank { json.optString("msg") }
                .ifBlank { json.optString("message") }
        }.getOrNull().orEmpty()

        val lowered = parsed.lowercase()
        return when {
            lowered.contains("invalid login credentials") -> "البريد الإلكتروني أو كلمة المرور غير صحيحة"
            lowered.contains("user already registered") -> "هذا البريد الإلكتروني مسجل مسبقاً، اختر تسجيل الدخول"
            lowered.contains("email not confirmed") -> "يرجى تأكيد بريدك الإلكتروني أولاً قبل تسجيل الدخول"
            lowered.contains("rate limit") || lowered.contains("429") -> "تم تجاوز الحد المسموح من محاولات التسجيل أو إرسال البريد مؤقتاً (429). يرجى الانتظار قليلًا أو تسجيل الدخول مباشرة إذا كان حسابك مسجلاً."
            lowered.contains("provider is not enabled") -> "تسجيل الدخول عبر غوغل غير مفعّل بعد في إعدادات Supabase Authentication"
            lowered.contains("password should be at least") -> "يجب أن تتكون كلمة المرور من 6 أحرف على الأقل"
            parsed.isNotBlank() -> "$parsed (${response.code()})"
            else -> "تعذر إتمام المصادقة (${response.code()})"
        }
    }

    private fun wrapAuthException(e: Exception): Throwable {
        val msg = e.localizedMessage.orEmpty()
        return if (msg.contains("Unable to resolve host") || msg.contains("No address associated") || e is java.net.UnknownHostException) {
            IllegalStateException("تعذر الوصول إلى الخادم. يرجى التأكد من صحة رابط المشروع (SUPABASE_URL) في لوحة Secrets وتوفر اتصال الإنترنت.")
        } else {
            IllegalStateException(msg.ifBlank { "تعذر الاتصال بالخادم" })
        }
    }

    companion object {
        private const val TAG = "SupabaseSession"
    }
}
