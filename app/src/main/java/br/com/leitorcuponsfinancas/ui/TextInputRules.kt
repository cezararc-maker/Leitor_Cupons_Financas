package br.com.leitorcuponsfinancas.ui

internal object TextInputRules {

    fun capitalizeFirstLetter(value: String): String {
        val index = value.indexOfFirst(Char::isLetter)
        if (index < 0) return value

        val current = value[index]
        val upper = current.uppercaseChar()
        if (current == upper) return value

        return value.replaceRange(index, index + 1, upper.toString())
    }
}
