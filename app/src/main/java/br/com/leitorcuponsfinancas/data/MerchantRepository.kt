package br.com.leitorcuponsfinancas.data

import br.com.leitorcuponsfinancas.domain.ProductNormalizer

class MerchantRepository(
    private val merchantDao: MerchantDao,
) {
    val merchants = merchantDao.observeActive()

    suspend fun resolveOrCreate(
        name: String?,
        cnpj: String?,
    ): MerchantEntity? {
        val displayName = ProductNormalizer.displayName(name.orEmpty())
            .ifBlank { "Estabelecimento" }
        val cnpjDigits = cnpj
            ?.filter(Char::isDigit)
            ?.takeIf { it.length == 14 }
        val searchKey = ProductNormalizer.searchKey(displayName)
        if (searchKey.isBlank() && cnpjDigits == null) return null

        val existing = cnpjDigits
            ?.let { merchantDao.findByCnpj(it) }
            ?: merchantDao.findBySearchKey(searchKey)

        if (existing != null) return existing

        val now = System.currentTimeMillis()
        val candidate = MerchantEntity(
            displayName = displayName,
            cnpjDigits = cnpjDigits,
            searchKey = searchKey,
            createdAt = now,
            updatedAt = now,
        )

        val id = merchantDao.insert(candidate)
        if (id != -1L) return candidate.copy(id = id)

        return cnpjDigits
            ?.let { merchantDao.findByCnpj(it) }
            ?: merchantDao.findBySearchKey(searchKey)
    }

    suspend fun rename(
        merchant: MerchantEntity,
        newName: String,
    ): String? {
        val cleanName = ProductNormalizer.displayName(newName)
        if (cleanName.isBlank()) return "Informe um nome válido."

        val newKey = ProductNormalizer.searchKey(cleanName)
        val conflict = merchantDao.listActiveOnce().firstOrNull {
            it.id != merchant.id &&
                it.searchKey == newKey &&
                (merchant.cnpjDigits == null || it.cnpjDigits == merchant.cnpjDigits)
        }
        if (conflict != null) {
            return "Já existe um estabelecimento mestre chamado \"${conflict.displayName}\"."
        }

        merchantDao.update(
            merchant.copy(
                displayName = cleanName,
                searchKey = newKey,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return null
    }

    suspend fun setSegment(
        merchant: MerchantEntity,
        segmentNodeId: Long?,
    ) {
        merchantDao.update(
            merchant.copy(
                segmentNodeId = segmentNodeId,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}
