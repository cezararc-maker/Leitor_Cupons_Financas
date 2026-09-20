package br.com.leitorcuponsfinancas.data

import br.com.leitorcuponsfinancas.domain.ProductNormalizer

class ProductRepository(
    private val productDao: ProductDao,
) {
    val products = productDao.observeActive()

    suspend fun findDuplicateName(
        name: String,
        excludingId: Long = 0,
    ): ProductEntity? {
        val targetKey = ProductNormalizer.searchKey(name)
        if (targetKey.isBlank()) return null

        return productDao.listActiveOnce().firstOrNull { product ->
            product.id != excludingId &&
                ProductNormalizer.searchKey(product.normalizedName) == targetKey
        }
    }

    suspend fun save(product: ProductEntity): Long {
        return if (product.id == 0L) {
            productDao.insert(product)
        } else {
            productDao.update(product.copy(updatedAt = System.currentTimeMillis()))
            product.id
        }
    }

    suspend fun deactivate(productId: Long) {
        productDao.deactivate(productId, System.currentTimeMillis())
    }
}
