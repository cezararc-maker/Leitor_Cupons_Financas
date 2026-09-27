package br.com.leitorcuponsfinancas.data

import android.content.Context
import android.util.Log
import br.com.leitorcuponsfinancas.BuildConfig
import com.google.firebase.appdistribution.FirebaseAppDistribution
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TesterUpdateRelease(
    val displayVersion: String,
    val versionCode: Long,
    val releaseNotes: String?,
)

data class TesterUpdateState(
    val checking: Boolean = false,
    val installing: Boolean = false,
    val availableRelease: TesterUpdateRelease? = null,
    val message: String? = null,
)

object TesterUpdateManager {

    private const val TAG = "TesterUpdateManager"
    private const val PREFS_NAME = "tester_update_preferences"
    private const val KEY_LAST_AUTOMATIC_CHECK_DATE = "last_automatic_check_date"

    private val checking = AtomicBoolean(false)
    private val _state = MutableStateFlow(TesterUpdateState())
    val state: StateFlow<TesterUpdateState> = _state.asStateFlow()

    fun checkForUpdates(
        context: Context,
        automatic: Boolean,
    ) {
        if (!BuildConfig.IN_APP_UPDATES_ENABLED) {
            return
        }

        val appContext = context.applicationContext
        if (automatic && alreadyCheckedToday(appContext)) {
            return
        }

        if (!checking.compareAndSet(false, true)) {
            return
        }

        _state.value = _state.value.copy(
            checking = true,
            message = null,
        )

        val appDistribution = FirebaseAppDistribution.getInstance()

        fun finishWithFailure(throwable: Throwable) {
            checking.set(false)
            _state.value = _state.value.copy(
                checking = false,
                installing = false,
                message = if (automatic) {
                    null
                } else {
                    "Não foi possível verificar atualizações agora."
                },
            )
            Log.w(
                TAG,
                "Nao foi possivel verificar atualizacao do canal de testadores.",
                throwable,
            )
        }

        fun performCheck() {
            appDistribution
                .checkForNewRelease()
                .addOnSuccessListener { release ->
                    checking.set(false)
                    if (automatic) {
                        markAutomaticCheckToday(appContext)
                    }

                    _state.value = if (release != null) {
                        TesterUpdateState(
                            checking = false,
                            availableRelease = TesterUpdateRelease(
                                displayVersion = release.displayVersion,
                                versionCode = release.versionCode,
                                releaseNotes = release.releaseNotes,
                            ),
                        )
                    } else {
                        TesterUpdateState(
                            checking = false,
                            message = if (automatic) {
                                null
                            } else {
                                "Você já está usando a versão mais recente disponível."
                            },
                        )
                    }
                }
                .addOnFailureListener(::finishWithFailure)
        }

        if (appDistribution.isTesterSignedIn) {
            performCheck()
        } else {
            appDistribution
                .signInTester()
                .addOnSuccessListener {
                    performCheck()
                }
                .addOnFailureListener(::finishWithFailure)
        }
    }

    fun installAvailableUpdate() {
        if (!BuildConfig.IN_APP_UPDATES_ENABLED) {
            return
        }

        if (_state.value.availableRelease == null) {
            return
        }

        _state.value = _state.value.copy(
            installing = true,
            availableRelease = null,
            message = null,
        )

        FirebaseAppDistribution
            .getInstance()
            .updateApp()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(
                        TAG,
                        "Nao foi possivel instalar a atualizacao do canal de testadores.",
                        task.exception,
                    )
                    _state.value = _state.value.copy(
                        installing = false,
                        message = "A instalação não foi concluída. Você pode tentar novamente em Configurações.",
                    )
                }
            }
    }

    fun remindLater(
        context: Context,
    ) {
        if (!BuildConfig.IN_APP_UPDATES_ENABLED) {
            return
        }

        markAutomaticCheckToday(context.applicationContext)
        _state.value = _state.value.copy(
            availableRelease = null,
            message = null,
        )
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun alreadyCheckedToday(
        context: Context,
    ): Boolean {
        val today = LocalDate.now().toString()
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_AUTOMATIC_CHECK_DATE, null) == today
    }

    private fun markAutomaticCheckToday(
        context: Context,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_AUTOMATIC_CHECK_DATE, LocalDate.now().toString())
            .apply()
    }
}
