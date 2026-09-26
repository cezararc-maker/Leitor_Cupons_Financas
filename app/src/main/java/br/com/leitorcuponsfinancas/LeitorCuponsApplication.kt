package br.com.leitorcuponsfinancas

import android.app.Application
import br.com.leitorcuponsfinancas.data.InstallationIdentityStore

class LeitorCuponsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        AppCheckInitializer.initialize(this)

        InstallationIdentityStore
            .getInstance(this)
            .getOrCreateInstallationId()
    }
}
