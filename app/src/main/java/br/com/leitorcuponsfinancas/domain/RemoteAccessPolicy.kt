package br.com.leitorcuponsfinancas.domain

object RemoteAccessPolicy {

    const val OFFLINE_GRACE_MILLIS: Long = 72L * 60L * 60L * 1000L

    fun isOfflineGraceValid(
        lastVerifiedAtMillis: Long?,
        nowMillis: Long = System.currentTimeMillis(),
        graceMillis: Long = OFFLINE_GRACE_MILLIS,
    ): Boolean {
        if (lastVerifiedAtMillis == null || lastVerifiedAtMillis <= 0L) return false
        if (graceMillis <= 0L) return false
        if (nowMillis < lastVerifiedAtMillis) return false

        return nowMillis - lastVerifiedAtMillis <= graceMillis
    }

    fun offlineGraceRemainingMillis(
        lastVerifiedAtMillis: Long?,
        nowMillis: Long = System.currentTimeMillis(),
        graceMillis: Long = OFFLINE_GRACE_MILLIS,
    ): Long {
        if (!isOfflineGraceValid(lastVerifiedAtMillis, nowMillis, graceMillis)) return 0L
        return (lastVerifiedAtMillis!! + graceMillis - nowMillis).coerceAtLeast(0L)
    }
}
