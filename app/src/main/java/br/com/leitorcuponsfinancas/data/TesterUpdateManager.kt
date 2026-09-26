package br.com.leitorcuponsfinancas.data

import android.util.Log
import br.com.leitorcuponsfinancas.BuildConfig
import com.google.firebase.appdistribution.FirebaseAppDistribution
import java.util.concurrent.atomic.AtomicBoolean

object TesterUpdateManager {

    private const val TAG = "TesterUpdateManager"
    private val checking = AtomicBoolean(false)

    fun checkForUpdates() {
        if (!BuildConfig.IN_APP_UPDATES_ENABLED) {
            return
        }

        if (!checking.compareAndSet(false, true)) {
            return
        }

        FirebaseAppDistribution
            .getInstance()
            .updateIfNewReleaseAvailable()
            .addOnCompleteListener { task ->
                checking.set(false)

                if (!task.isSuccessful) {
                    Log.w(
                        TAG,
                        "Nao foi possivel verificar/instalar atualizacao do canal de testadores.",
                        task.exception,
                    )
                }
            }
    }
}
