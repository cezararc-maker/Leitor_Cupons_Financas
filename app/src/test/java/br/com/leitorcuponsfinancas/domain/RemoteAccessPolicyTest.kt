package br.com.leitorcuponsfinancas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteAccessPolicyTest {

    @Test
    fun noPreviousValidationRequiresOnlineAccess() {
        assertFalse(
            RemoteAccessPolicy.isOfflineGraceValid(
                lastVerifiedAtMillis = null,
                nowMillis = 1_000L,
                graceMillis = 500L,
            ),
        )
    }

    @Test
    fun accessInsideGraceWindowIsAllowed() {
        assertTrue(
            RemoteAccessPolicy.isOfflineGraceValid(
                lastVerifiedAtMillis = 1_000L,
                nowMillis = 1_400L,
                graceMillis = 500L,
            ),
        )
    }

    @Test
    fun accessAtGraceBoundaryIsAllowed() {
        assertTrue(
            RemoteAccessPolicy.isOfflineGraceValid(
                lastVerifiedAtMillis = 1_000L,
                nowMillis = 1_500L,
                graceMillis = 500L,
            ),
        )
    }

    @Test
    fun expiredGraceRequiresOnlineAccess() {
        assertFalse(
            RemoteAccessPolicy.isOfflineGraceValid(
                lastVerifiedAtMillis = 1_000L,
                nowMillis = 1_501L,
                graceMillis = 500L,
            ),
        )
    }

    @Test
    fun clockRollbackDoesNotExtendOfflineAccess() {
        assertFalse(
            RemoteAccessPolicy.isOfflineGraceValid(
                lastVerifiedAtMillis = 1_000L,
                nowMillis = 999L,
                graceMillis = 500L,
            ),
        )
    }

    @Test
    fun remainingGraceNeverBecomesNegative() {
        assertEquals(
            0L,
            RemoteAccessPolicy.offlineGraceRemainingMillis(
                lastVerifiedAtMillis = 1_000L,
                nowMillis = 2_000L,
                graceMillis = 500L,
            ),
        )
    }
}
