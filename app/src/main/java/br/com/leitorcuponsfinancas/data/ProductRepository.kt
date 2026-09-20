package br.com.leitorcuponsfinancas.data

class ProductRepository(
    private val productDao: ProductDao,
) {
    val products = productDao.observeActive()

    suspend fun save(product: ProductEntity) {
        if (product.id == 0L) {
            productDao.insert(product)
        } else {
            productDao.update(product.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deactivate(productId: Long) {
        productDao.deactivate(productId, System.currentTimeMillis())
    }
}
