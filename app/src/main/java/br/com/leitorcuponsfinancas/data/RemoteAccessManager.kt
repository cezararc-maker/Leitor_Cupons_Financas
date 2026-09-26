package br.com.leitorcuponsfinancas.data

import android.content.Context
import android.os.Build
import br.com.leitorcuponsfinancas.BuildConfig
import br.com.leitorcuponsfinancas.domain.RemoteAccessPolicy
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class RemoteAccessDecision {
    DEVELOPMENT_BYPASS,
    LOADING,
    SIGNED_OUT,
    ACTIVE_ONLINE,
    ACTIVE_OFFLINE_GRACE,
    ACCOUNT_PENDING,
    SUSPENDED,
    BLOCKED,
    DEVICE_PENDING,
    DEVICE_REVOKED,
    ONLINE_REQUIRED,
    CONFIGURATION_ERROR,
    ERROR,
}

data class RemoteAccessState(
    val decision: RemoteAccessDecision,
    val email: String? = null,
    val message: String? = null,
    val offlineAccessUntilMillis: Long? = null,
) {
    val canUseApp: Boolean
        get() = decision == RemoteAccessDecision.DEVELOPMENT_BYPASS ||
            decision == RemoteAccessDecision.ACTIVE_ONLINE ||
            decision == RemoteAccessDecision.ACTIVE_OFFLINE_GRACE
}

class RemoteAccessManager private constructor(
    private val context: Context,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val installationStore = InstallationIdentityStore.getInstance(context)
    private val sessionStore = RemoteAccessSessionStore(context)

    private val _state = MutableStateFlow(
        if (BuildConfig.REMOTE_ACCESS_REQUIRED) {
            RemoteAccessState(RemoteAccessDecision.LOADING)
        } else {
            RemoteAccessState(RemoteAccessDecision.DEVELOPMENT_BYPASS)
        },
    )
    val state: StateFlow<RemoteAccessState> = _state.asStateFlow()

    @Volatile
    private var started = false

    @Volatile
    private var auth: FirebaseAuth? = null

    @Volatile
    private var firestore: FirebaseFirestore? = null

    fun start() {
        if (started) return
        started = true

        if (!BuildConfig.REMOTE_ACCESS_REQUIRED) {
            _state.value = RemoteAccessState(RemoteAccessDecision.DEVELOPMENT_BYPASS)
            return
        }

        refresh()
    }

    fun refresh() {
        if (!BuildConfig.REMOTE_ACCESS_REQUIRED) {
            _state.value = RemoteAccessState(RemoteAccessDecision.DEVELOPMENT_BYPASS)
            return
        }

        scope.launch {
            validateAccess()
        }
    }

    fun signIn(
        email: String,
        password: String,
    ) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            _state.value = RemoteAccessState(
                decision = RemoteAccessDecision.SIGNED_OUT,
                email = normalizedEmail.ifBlank { null },
                message = "Informe e-mail e senha.",
            )
            return
        }

        scope.launch {
            _state.value = RemoteAccessState(
                decision = RemoteAccessDecision.LOADING,
                email = normalizedEmail,
                message = "Validando acesso...",
            )

            val clients = ensureFirebaseClients()
            if (clients == null) return@launch

            try {
                clients.first
                    .signInWithEmailAndPassword(normalizedEmail, password)
                    .awaitResult()
                validateAccess()
            } catch (_: Exception) {
                _state.value = RemoteAccessState(
                    decision = RemoteAccessDecision.SIGNED_OUT,
                    email = normalizedEmail,
                    message = "Não foi possível entrar. Confira e-mail, senha e conexão com a internet.",
                )
            }
        }
    }

    fun signOut() {
        sessionStore.clear()
        auth?.signOut()
        _state.value = RemoteAccessState(RemoteAccessDecision.SIGNED_OUT)
    }

    private suspend fun validateAccess() {
        _state.value = RemoteAccessState(
            decision = RemoteAccessDecision.LOADING,
            email = auth?.currentUser?.email,
            message = "Verificando autorização...",
        )

        val clients = ensureFirebaseClients() ?: return
        val currentUser = clients.first.currentUser

        if (currentUser == null) {
            _state.value = RemoteAccessState(RemoteAccessDecision.SIGNED_OUT)
            return
        }

        val uid = currentUser.uid
        val email = currentUser.email
        val installationId = installationStore.getOrCreateInstallationId()

        try {
            val userDocument = clients.second
                .collection(USERS_COLLECTION)
                .document(uid)
                .get(Source.SERVER)
                .awaitResult()

            if (!userDocument.exists()) {
                sessionStore.clear()
                _state.value = RemoteAccessState(
                    decision = RemoteAccessDecision.ACCOUNT_PENDING,
                    email = email,
                    message = "Sua conta existe, mas ainda não foi liberada para usar o aplicativo.",
                )
                return
            }

            when (
                userDocument.getString(FIELD_STATUS)
                    ?.trim()
                    ?.uppercase(Locale.ROOT)
            ) {
                STATUS_ACTIVE -> Unit

                STATUS_SUSPENDED -> {
                    sessionStore.clear()
                    _state.value = RemoteAccessState(
                        decision = RemoteAccessDecision.SUSPENDED,
                        email = email,
                        message = "Seu acesso está suspenso.",
                    )
                    return
                }

                STATUS_BLOCKED -> {
                    sessionStore.clear()
                    _state.value = RemoteAccessState(
                        decision = RemoteAccessDecision.BLOCKED,
                        email = email,
                        message = "Seu acesso está bloqueado.",
                    )
                    return
                }

                else -> {
                    sessionStore.clear()
                    _state.value = RemoteAccessState(
                        decision = RemoteAccessDecision.ACCOUNT_PENDING,
                        email = email,
                        message = "Sua conta ainda não está ativa.",
                    )
                    return
                }
            }

            val maxDevices = userDocument.getLong(FIELD_MAX_DEVICES)
                ?.coerceAtLeast(1L)
                ?: 1L

            val deviceReference = clients.second
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection(DEVICES_COLLECTION)
                .document(installationId)

            val deviceDocument = deviceReference
                .get(Source.SERVER)
                .awaitResult()

            if (!deviceDocument.exists()) {
                deviceReference.set(
                    mapOf(
                        FIELD_INSTALLATION_ID to installationId,
                        FIELD_DEVICE_STATUS to DEVICE_STATUS_PENDING,
                        FIELD_ACTIVE to false,
                        FIELD_MODEL to deviceLabel(),
                        FIELD_APP_VERSION to appVersion(),
                        FIELD_FIRST_SEEN_AT to FieldValue.serverTimestamp(),
                        FIELD_LAST_SEEN_AT to FieldValue.serverTimestamp(),
                    ),
                ).awaitResult()

                sessionStore.clear()
                _state.value = RemoteAccessState(
                    decision = RemoteAccessDecision.DEVICE_PENDING,
                    email = email,
                    message = "Este celular foi registrado e aguarda autorização. Limite atual: $maxDevices aparelho(s).",
                )
                return
            }

            val deviceStatus = deviceDocument
                .getString(FIELD_DEVICE_STATUS)
                ?.trim()
                ?.uppercase(Locale.ROOT)
            val deviceActive = deviceDocument.getBoolean(FIELD_ACTIVE) == true

            if (!deviceActive || deviceStatus != DEVICE_STATUS_ACTIVE) {
                sessionStore.clear()
                val pending = deviceStatus == DEVICE_STATUS_PENDING
                _state.value = RemoteAccessState(
                    decision = if (pending) {
                        RemoteAccessDecision.DEVICE_PENDING
                    } else {
                        RemoteAccessDecision.DEVICE_REVOKED
                    },
                    email = email,
                    message = if (pending) {
                        "Este celular aguarda autorização."
                    } else {
                        "Este celular não está autorizado para esta conta."
                    },
                )
                return
            }

            val verifiedAt = System.currentTimeMillis()
            sessionStore.markVerified(
                uid = uid,
                installationId = installationId,
                verifiedAtMillis = verifiedAt,
            )

            runCatching {
                deviceReference.update(
                    mapOf(
                        FIELD_LAST_SEEN_AT to FieldValue.serverTimestamp(),
                        FIELD_MODEL to deviceLabel(),
                        FIELD_APP_VERSION to appVersion(),
                    ),
                ).awaitResult()
            }

            _state.value = RemoteAccessState(
                decision = RemoteAccessDecision.ACTIVE_ONLINE,
                email = email,
            )
        } catch (_: Exception) {
            val lastVerifiedAt = sessionStore.lastVerifiedAt(
                uid = uid,
                installationId = installationId,
            )

            if (RemoteAccessPolicy.isOfflineGraceValid(lastVerifiedAt)) {
                _state.value = RemoteAccessState(
                    decision = RemoteAccessDecision.ACTIVE_OFFLINE_GRACE,
                    email = email,
                    message = "Acesso offline temporário usando a última validação segura.",
                    offlineAccessUntilMillis = lastVerifiedAt?.plus(
                        RemoteAccessPolicy.OFFLINE_GRACE_MILLIS,
                    ),
                )
            } else {
                _state.value = RemoteAccessState(
                    decision = RemoteAccessDecision.ONLINE_REQUIRED,
                    email = email,
                    message = "Conecte-se à internet para validar novamente sua autorização.",
                )
            }
        }
    }

    private fun ensureFirebaseClients(): Pair<FirebaseAuth, FirebaseFirestore>? {
        if (!BuildConfig.REMOTE_ACCESS_REQUIRED) return null

        val firebaseApp = FirebaseApp
            .getApps(context)
            .firstOrNull()
            ?: FirebaseApp.initializeApp(context)

        if (firebaseApp == null) {
            _state.value = RemoteAccessState(
                decision = RemoteAccessDecision.CONFIGURATION_ERROR,
                message = "Firebase não está configurado nesta instalação.",
            )
            return null
        }

        val resolvedAuth = auth ?: FirebaseAuth.getInstance(firebaseApp).also {
            auth = it
        }
        val resolvedFirestore = firestore ?: FirebaseFirestore.getInstance(firebaseApp).also {
            firestore = it
        }

        return resolvedAuth to resolvedFirestore
    }

    private fun appVersion(): String = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            ?: "desconhecida"
    }.getOrDefault("desconhecida")

    private fun deviceLabel(): String =
        listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Android" }

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Falha em operação Firebase."),
                    )
                }
            }
        }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val DEVICES_COLLECTION = "devices"

        private const val FIELD_STATUS = "status"
        private const val FIELD_MAX_DEVICES = "maxDevices"
        private const val FIELD_INSTALLATION_ID = "installationId"
        private const val FIELD_DEVICE_STATUS = "status"
        private const val FIELD_ACTIVE = "active"
        private const val FIELD_MODEL = "model"
        private const val FIELD_APP_VERSION = "appVersion"
        private const val FIELD_FIRST_SEEN_AT = "firstSeenAt"
        private const val FIELD_LAST_SEEN_AT = "lastSeenAt"

        private const val STATUS_ACTIVE = "ACTIVE"
        private const val STATUS_SUSPENDED = "SUSPENDED"
        private const val STATUS_BLOCKED = "BLOCKED"

        private const val DEVICE_STATUS_PENDING = "PENDING"
        private const val DEVICE_STATUS_ACTIVE = "ACTIVE"

        @Volatile
        private var instance: RemoteAccessManager? = null

        fun getInstance(context: Context): RemoteAccessManager =
            instance ?: synchronized(this) {
                instance ?: RemoteAccessManager(
                    context.applicationContext,
                ).also { instance = it }
            }
    }
}

private class RemoteAccessSessionStore(
    context: Context,
) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun markVerified(
        uid: String,
        installationId: String,
        verifiedAtMillis: Long,
    ) {
        preferences.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_INSTALLATION_ID, installationId)
            .putLong(KEY_VERIFIED_AT, verifiedAtMillis)
            .apply()
    }

    fun lastVerifiedAt(
        uid: String,
        installationId: String,
    ): Long? {
        val sameIdentity =
            preferences.getString(KEY_UID, null) == uid &&
                preferences.getString(KEY_INSTALLATION_ID, null) == installationId

        if (!sameIdentity) return null

        val value = preferences.getLong(KEY_VERIFIED_AT, 0L)
        return value.takeIf { it > 0L }
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "remote_access_session"
        private const val KEY_UID = "uid"
        private const val KEY_INSTALLATION_ID = "installation_id"
        private const val KEY_VERIFIED_AT = "verified_at"
    }
}
