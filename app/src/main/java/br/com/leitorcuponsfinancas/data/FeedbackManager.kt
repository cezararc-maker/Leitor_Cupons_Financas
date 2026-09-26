package br.com.leitorcuponsfinancas.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import br.com.leitorcuponsfinancas.BuildConfig
import br.com.leitorcuponsfinancas.MainActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
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

enum class FeedbackStatus(
    val firestoreValue: String,
    val label: String,
) {
    RECEIVED("RECEIVED", "Recebido"),
    IN_REVIEW("IN_REVIEW", "Em Análise"),
    IN_DEVELOPMENT("IN_DEVELOPMENT", "Em Desenvolvimento"),
    TESTING("TESTING", "Fase de Testes"),
    DEPLOYMENT("DEPLOYMENT", "Fase de Implantação"),
    COMPLETED("COMPLETED", "Implantação Concluída"),
    DISCARDED("DISCARDED", "Descartado");

    companion object {
        fun fromFirestore(value: String?): FeedbackStatus =
            entries.firstOrNull {
                it.firestoreValue == value?.trim()?.uppercase(Locale.ROOT)
            } ?: RECEIVED
    }
}

data class FeedbackItem(
    val id: String,
    val authorUid: String,
    val authorEmail: String?,
    val message: String,
    val status: FeedbackStatus,
    val discardReason: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val adminSeen: Boolean,
    val userSeen: Boolean,
    val appVersion: String?,
    val deviceModel: String?,
)

data class FeedbackState(
    val available: Boolean = false,
    val isAdmin: Boolean = false,
    val loading: Boolean = false,
    val items: List<FeedbackItem> = emptyList(),
    val unreadCount: Int = 0,
    val submitting: Boolean = false,
    val message: String? = null,
)

class FeedbackManager private constructor(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(FeedbackState())
    val state: StateFlow<FeedbackState> = _state.asStateFlow()

    private var startedForUid: String? = null
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private val submitInProgress = AtomicBoolean(false)

    private val notificationStore = context.getSharedPreferences(
        "feedback_notification_state",
        Context.MODE_PRIVATE,
    )

    fun start() {
        val clients = clients() ?: run {
            _state.value = FeedbackState(available = false)
            return
        }
        val currentUser = clients.first.currentUser ?: run {
            _state.value = FeedbackState(available = false)
            return
        }

        if (startedForUid == currentUser.uid && listenerRegistration != null) {
            return
        }

        listenerRegistration?.remove()
        listenerRegistration = null
        startedForUid = currentUser.uid
        _state.value = FeedbackState(
            available = true,
            loading = true,
        )

        scope.launch {
            val isAdmin = runCatching {
                val userDoc = clients.second
                    .collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .get()
                    .awaitResult()
                userDoc.getString(FIELD_ROLE)
                    ?.trim()
                    ?.uppercase(Locale.ROOT) == ROLE_ADMIN
            }.getOrDefault(false)

            listen(
                firestore = clients.second,
                uid = currentUser.uid,
                isAdmin = isAdmin,
            )
        }
    }

    fun submitSuggestion(text: String) {
        val normalized = text.trim()
        if (normalized.length < MIN_MESSAGE_LENGTH) {
            _state.value = _state.value.copy(
                message = "Descreva a sugestão com pelo menos $MIN_MESSAGE_LENGTH caracteres.",
            )
            return
        }

        if (normalized.length > MAX_MESSAGE_LENGTH) {
            _state.value = _state.value.copy(
                message = "A sugestão pode ter no máximo $MAX_MESSAGE_LENGTH caracteres.",
            )
            return
        }

        if (!submitInProgress.compareAndSet(false, true)) return

        val clients = clients()
        val user = clients?.first?.currentUser
        if (clients == null || user == null) {
            submitInProgress.set(false)
            _state.value = _state.value.copy(
                message = "Faça login novamente para enviar a sugestão.",
            )
            return
        }

        _state.value = _state.value.copy(
            submitting = true,
            message = null,
        )

        scope.launch {
            try {
                clients.second.collection(FEEDBACK_COLLECTION).add(
                    mapOf(
                        FIELD_AUTHOR_UID to user.uid,
                        FIELD_AUTHOR_EMAIL to user.email,
                        FIELD_MESSAGE to normalized,
                        FIELD_STATUS to FeedbackStatus.RECEIVED.firestoreValue,
                        FIELD_DISCARD_REASON to null,
                        FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                        FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                        FIELD_ADMIN_SEEN to false,
                        FIELD_USER_SEEN to true,
                        FIELD_APP_VERSION to BuildConfig.VERSION_NAME,
                        FIELD_DEVICE_MODEL to deviceLabel(),
                    ),
                ).awaitResult()

                _state.value = _state.value.copy(
                    submitting = false,
                    message = "Sugestão enviada. Obrigado por ajudar a melhorar o app.",
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    submitting = false,
                    message = "Não foi possível enviar agora. Verifique a internet e tente novamente.",
                )
            } finally {
                submitInProgress.set(false)
            }
        }
    }

    fun updateStatus(
        feedbackId: String,
        status: FeedbackStatus,
        discardReason: String?,
    ) {
        if (!_state.value.isAdmin) return

        val reason = discardReason?.trim()
        if (status == FeedbackStatus.DISCARDED && reason.isNullOrBlank()) {
            _state.value = _state.value.copy(
                message = "Informe o motivo antes de descartar a sugestão.",
            )
            return
        }

        val firestore = clients()?.second ?: return
        scope.launch {
            try {
                firestore.collection(FEEDBACK_COLLECTION)
                    .document(feedbackId)
                    .update(
                        mapOf(
                            FIELD_STATUS to status.firestoreValue,
                            FIELD_DISCARD_REASON to if (status == FeedbackStatus.DISCARDED) {
                                reason
                            } else {
                                null
                            },
                            FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                            FIELD_ADMIN_SEEN to true,
                            FIELD_USER_SEEN to false,
                        ),
                    )
                    .awaitResult()

                _state.value = _state.value.copy(
                    message = "Status atualizado para ${status.label}.",
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    message = "Não foi possível atualizar o status.",
                )
            }
        }
    }

    fun markAdminSeen() {
        if (!_state.value.isAdmin) return
        val firestore = clients()?.second ?: return
        val unseen = _state.value.items.filter { !it.adminSeen }
        if (unseen.isEmpty()) return

        scope.launch {
            unseen.forEach { item ->
                runCatching {
                    firestore.collection(FEEDBACK_COLLECTION)
                        .document(item.id)
                        .update(
                            mapOf(
                                FIELD_ADMIN_SEEN to true,
                                FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                            ),
                        )
                        .awaitResult()
                }
            }
        }
    }

    fun markUserSeen() {
        if (_state.value.isAdmin) return
        val firestore = clients()?.second ?: return
        val unseen = _state.value.items.filter { !it.userSeen }
        if (unseen.isEmpty()) return

        scope.launch {
            unseen.forEach { item ->
                runCatching {
                    firestore.collection(FEEDBACK_COLLECTION)
                        .document(item.id)
                        .update(FIELD_USER_SEEN, true)
                        .awaitResult()
                }
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun listen(
        firestore: FirebaseFirestore,
        uid: String,
        isAdmin: Boolean,
    ) {
        val query = if (isAdmin) {
            firestore.collection(FEEDBACK_COLLECTION)
        } else {
            firestore.collection(FEEDBACK_COLLECTION)
                .whereEqualTo(FIELD_AUTHOR_UID, uid)
        }

        listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                _state.value = _state.value.copy(
                    available = true,
                    isAdmin = isAdmin,
                    loading = false,
                    message = "Não foi possível sincronizar as sugestões agora.",
                )
                return@addSnapshotListener
            }

            val items = snapshot.toFeedbackItems()
                .sortedByDescending { maxOf(it.updatedAtMillis, it.createdAtMillis) }

            val unread = if (isAdmin) {
                items.count { !it.adminSeen }
            } else {
                items.count { !it.userSeen }
            }

            _state.value = _state.value.copy(
                available = true,
                isAdmin = isAdmin,
                loading = false,
                items = items,
                unreadCount = unread,
            )

            if (isAdmin) {
                notifyAdmin(items.filter { !it.adminSeen })
            } else {
                notifyUser(items.filter { !it.userSeen })
            }
        }
    }

    private fun QuerySnapshot.toFeedbackItems(): List<FeedbackItem> =
        documents.map { doc ->
            FeedbackItem(
                id = doc.id,
                authorUid = doc.getString(FIELD_AUTHOR_UID).orEmpty(),
                authorEmail = doc.getString(FIELD_AUTHOR_EMAIL),
                message = doc.getString(FIELD_MESSAGE).orEmpty(),
                status = FeedbackStatus.fromFirestore(doc.getString(FIELD_STATUS)),
                discardReason = doc.getString(FIELD_DISCARD_REASON),
                createdAtMillis = doc.getTimestamp(FIELD_CREATED_AT)?.toDate()?.time ?: 0L,
                updatedAtMillis = doc.getTimestamp(FIELD_UPDATED_AT)?.toDate()?.time ?: 0L,
                adminSeen = doc.getBoolean(FIELD_ADMIN_SEEN) == true,
                userSeen = doc.getBoolean(FIELD_USER_SEEN) == true,
                appVersion = doc.getString(FIELD_APP_VERSION),
                deviceModel = doc.getString(FIELD_DEVICE_MODEL),
            )
        }

    private fun notifyAdmin(unseen: List<FeedbackItem>) {
        val already = notificationStore.getStringSet(KEY_ADMIN_NOTIFIED, emptySet())
            ?.toMutableSet()
            ?: mutableSetOf()

        val fresh = unseen.filter { it.id !in already }
        if (fresh.isEmpty()) return

        val latest = fresh.first()
        showNotification(
            id = latest.id.hashCode(),
            title = "Nova sugestão recebida",
            text = latest.message.take(120),
        )

        already += fresh.map { it.id }
        trimAndStore(KEY_ADMIN_NOTIFIED, already)
    }

    private fun notifyUser(unseen: List<FeedbackItem>) {
        unseen.forEach { item ->
            val key = "user_status_${item.id}"
            val lastStatus = notificationStore.getString(key, null)
            if (lastStatus == item.status.firestoreValue) return@forEach

            val detail = if (
                item.status == FeedbackStatus.DISCARDED &&
                !item.discardReason.isNullOrBlank()
            ) {
                "${item.status.label}: ${item.discardReason}"
            } else {
                "Sua sugestão agora está em: ${item.status.label}"
            }

            showNotification(
                id = item.id.hashCode(),
                title = "Atualização da sua sugestão",
                text = detail.take(160),
            )

            notificationStore.edit()
                .putString(key, item.status.firestoreValue)
                .apply()
        }
    }

    private fun showNotification(
        id: Int,
        title: String,
        text: String,
    ) {
        val manager = context.getSystemService(NotificationManager::class.java)
            ?: return

        if (
            Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Sugestões e melhorias",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Novas sugestões e mudanças de status."
                },
            )
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }

        manager.notify(
            id,
            builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(android.app.Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build(),
        )
    }

    private fun trimAndStore(
        key: String,
        values: MutableSet<String>,
    ) {
        val limited = values.takeLast(MAX_NOTIFICATION_HISTORY).toSet()
        notificationStore.edit().putStringSet(key, limited).apply()
    }

    private fun clients(): Pair<FirebaseAuth, FirebaseFirestore>? {
        val app = FirebaseApp.getApps(context).firstOrNull()
            ?: FirebaseApp.initializeApp(context)
            ?: return null

        return FirebaseAuth.getInstance(app) to FirebaseFirestore.getInstance(app)
    }

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
        private const val FEEDBACK_COLLECTION = "feedback"

        private const val FIELD_ROLE = "role"
        private const val ROLE_ADMIN = "ADMIN"

        private const val FIELD_AUTHOR_UID = "authorUid"
        private const val FIELD_AUTHOR_EMAIL = "authorEmail"
        private const val FIELD_MESSAGE = "message"
        private const val FIELD_STATUS = "status"
        private const val FIELD_DISCARD_REASON = "discardReason"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_ADMIN_SEEN = "adminSeen"
        private const val FIELD_USER_SEEN = "userSeen"
        private const val FIELD_APP_VERSION = "appVersion"
        private const val FIELD_DEVICE_MODEL = "deviceModel"

        private const val MIN_MESSAGE_LENGTH = 10
        private const val MAX_MESSAGE_LENGTH = 1500
        private const val MAX_NOTIFICATION_HISTORY = 200

        private const val NOTIFICATION_CHANNEL_ID = "feedback_updates"
        private const val KEY_ADMIN_NOTIFIED = "admin_notified_ids"

        @Volatile
        private var instance: FeedbackManager? = null

        fun getInstance(context: Context): FeedbackManager =
            instance ?: synchronized(this) {
                instance ?: FeedbackManager(
                    context.applicationContext,
                ).also { instance = it }
            }
    }
}
