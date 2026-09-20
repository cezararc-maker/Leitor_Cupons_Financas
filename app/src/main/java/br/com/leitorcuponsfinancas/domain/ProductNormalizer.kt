package br.com.leitorcuponsfinancas.domain

import java.text.Normalizer
import java.util.Locale

object ProductNormalizer {

    fun displayName(value: String): String =
        value.trim().replace(Regex("\\s+"), " ")

    fun searchKey(value: String): String {
        val withoutAccents = Normalizer.normalize(displayName(value), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

        return withoutAccents.uppercase(Locale.ROOT)
    }
}
