package br.com.leitorcuponsfinancas

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckInitializer {

    fun initialize(context: Context) {
        val firebaseApp = FirebaseApp
            .getApps(context)
            .firstOrNull()
            ?: FirebaseApp.initializeApp(context)
            ?: return

        FirebaseAppCheck
            .getInstance(firebaseApp)
            .installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance(),
            )
    }
}
