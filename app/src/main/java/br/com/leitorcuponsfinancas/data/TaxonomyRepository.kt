package br.com.leitorcuponsfinancas.data

import br.com.leitorcuponsfinancas.domain.ProductNormalizer
import java.util.UUID
import kotlinx.coroutines.flow.Flow

enum class TaxonomyLevel(val code: String, val label: String) {
    SEGMENT("SEGMENT", "Segmento"),
    DEPARTMENT("DEPARTMENT", "Departamento"),
    CATEGORY("CATEGORY", "Categoria"),
    SUBCATEGORY("SUBCATEGORY", "Subcategoria");

    fun nextOrNull(): TaxonomyLevel? = when (this) {
        SEGMENT -> DEPARTMENT
        DEPARTMENT -> CATEGORY
        CATEGORY -> SUBCATEGORY
        SUBCATEGORY -> null
    }

    companion object {
        fun fromCode(code: String): TaxonomyLevel? =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

data class TaxonomySeedItem(
    val stableKey: String,
    val parentStableKey: String?,
    val level: TaxonomyLevel,
    val name: String,
)

class TaxonomyRepository(
    private val taxonomyDao: TaxonomyDao,
    private val productDao: ProductDao,
) {
    val nodes: Flow<List<TaxonomyNodeEntity>> = taxonomyDao.observeActiveNodes()
    val segments: Flow<List<TaxonomyNodeEntity>> = taxonomyDao.observeSegments()
    val productLinks: Flow<List<TaxonomyProductLinkEntity>> = taxonomyDao.observeProductLinks()

    suspend fun ensureBaseTaxonomy(): Int {
        var created = 0

        for (item in BASE_TAXONOMY) {
            if (taxonomyDao.findByStableKey(item.stableKey) != null) continue

            val parentId = item.parentStableKey
                ?.let { taxonomyDao.findByStableKey(it)?.id }

            if (item.parentStableKey != null && parentId == null) continue

            val now = System.currentTimeMillis()
            val id = taxonomyDao.insertNode(
                TaxonomyNodeEntity(
                    stableKey = item.stableKey,
                    parentId = parentId,
                    level = item.level.code,
                    name = item.name,
                    searchKey = ProductNormalizer.searchKey(item.name),
                    builtIn = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            if (id != -1L) created++
        }

        return created
    }

    suspend fun createNode(
        parentId: Long?,
        level: TaxonomyLevel,
        name: String,
    ): Result<TaxonomyNodeEntity> {
        val cleanName = ProductNormalizer.displayName(name)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Informe um nome válido."))
        }

        val parent = parentId?.let { taxonomyDao.findById(it) }

        if (level == TaxonomyLevel.SEGMENT && parentId != null) {
            return Result.failure(IllegalArgumentException("Segmento não pode ter pai."))
        }

        if (level != TaxonomyLevel.SEGMENT && parent == null) {
            return Result.failure(IllegalArgumentException("Selecione o nível anterior."))
        }

        val expectedParentLevel = when (level) {
            TaxonomyLevel.SEGMENT -> null
            TaxonomyLevel.DEPARTMENT -> TaxonomyLevel.SEGMENT
            TaxonomyLevel.CATEGORY -> TaxonomyLevel.DEPARTMENT
            TaxonomyLevel.SUBCATEGORY -> TaxonomyLevel.CATEGORY
        }
        if (
            expectedParentLevel != null &&
            parent?.level != expectedParentLevel.code
        ) {
            return Result.failure(
                IllegalArgumentException(
                    "${level.label} deve ficar dentro de ${expectedParentLevel.label}.",
                ),
            )
        }

        val sibling = taxonomyDao.listChildren(parentId).firstOrNull {
            it.level == level.code &&
                ProductNormalizer.searchKey(it.name) == ProductNormalizer.searchKey(cleanName)
        }
        if (sibling != null) {
            return Result.failure(
                IllegalArgumentException("\"${sibling.name}\" já existe neste nível."),
            )
        }

        val now = System.currentTimeMillis()
        val stableKey = "user.${UUID.randomUUID()}"
        val candidate = TaxonomyNodeEntity(
            stableKey = stableKey,
            parentId = parentId,
            level = level.code,
            name = cleanName,
            searchKey = ProductNormalizer.searchKey(cleanName),
            builtIn = false,
            createdAt = now,
            updatedAt = now,
        )
        val id = taxonomyDao.insertNode(candidate)
        if (id == -1L) {
            return Result.failure(IllegalStateException("Não foi possível criar a classificação."))
        }

        return Result.success(candidate.copy(id = id))
    }

    suspend fun renameNode(
        node: TaxonomyNodeEntity,
        newName: String,
    ): String? {
        val clean = ProductNormalizer.displayName(newName)
        if (clean.isBlank()) return "Informe um nome válido."

        val conflict = taxonomyDao.listChildren(node.parentId).firstOrNull {
            it.id != node.id &&
                it.level == node.level &&
                ProductNormalizer.searchKey(it.name) == ProductNormalizer.searchKey(clean)
        }
        if (conflict != null) {
            return "Já existe \"${conflict.name}\" neste nível."
        }

        taxonomyDao.updateNode(
            node.copy(
                name = clean,
                searchKey = ProductNormalizer.searchKey(clean),
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return null
    }

    suspend fun setActive(node: TaxonomyNodeEntity, active: Boolean) {
        taxonomyDao.setNodeActive(
            id = node.id,
            active = active,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun linkProduct(
        taxonomyNodeId: Long,
        productId: Long,
    ) {
        taxonomyDao.insertProductLink(
            TaxonomyProductLinkEntity(
                taxonomyNodeId = taxonomyNodeId,
                productId = productId,
            ),
        )
    }

    suspend fun productsForNode(
        nodeId: Long,
    ): List<ProductEntity> {
        val productIds = taxonomyDao.listProductLinks()
            .filter { it.taxonomyNodeId == nodeId }
            .map { it.productId }
            .toSet()

        return productDao.listActiveOnce()
            .filter { it.id in productIds }
            .sortedBy { it.normalizedName.lowercase() }
    }

    suspend fun ancestry(nodeId: Long): List<TaxonomyNodeEntity> {
        val result = mutableListOf<TaxonomyNodeEntity>()
        var current = taxonomyDao.findById(nodeId)

        while (current != null) {
            result += current
            current = current.parentId?.let { taxonomyDao.findById(it) }
        }

        return result.reversed()
    }

    suspend fun allNodes(): List<TaxonomyNodeEntity> = taxonomyDao.listAllNodes()
    suspend fun allProductLinks(): List<TaxonomyProductLinkEntity> = taxonomyDao.listProductLinks()

    companion object {
        val BASE_TAXONOMY = listOf(
            TaxonomySeedItem("segment.market", null, TaxonomyLevel.SEGMENT, "Mercado"),
            TaxonomySeedItem("segment.pharmacy", null, TaxonomyLevel.SEGMENT, "Farmácia"),
            TaxonomySeedItem("segment.food_service", null, TaxonomyLevel.SEGMENT, "Alimentação fora do lar"),
            TaxonomySeedItem("segment.clothing", null, TaxonomyLevel.SEGMENT, "Vestuário"),
            TaxonomySeedItem("segment.pet_shop", null, TaxonomyLevel.SEGMENT, "Pet shop"),
            TaxonomySeedItem("segment.home_building", null, TaxonomyLevel.SEGMENT, "Casa e construção"),
            TaxonomySeedItem("segment.fuel", null, TaxonomyLevel.SEGMENT, "Combustíveis"),
            TaxonomySeedItem("segment.services", null, TaxonomyLevel.SEGMENT, "Serviços"),
            TaxonomySeedItem("segment.other", null, TaxonomyLevel.SEGMENT, "Outros"),

            TaxonomySeedItem("market.food", "segment.market", TaxonomyLevel.DEPARTMENT, "Alimentos"),
            TaxonomySeedItem("market.beverages", "segment.market", TaxonomyLevel.DEPARTMENT, "Bebidas"),
            TaxonomySeedItem("market.personal_care", "segment.market", TaxonomyLevel.DEPARTMENT, "Higiene e cuidados pessoais"),
            TaxonomySeedItem("market.cleaning", "segment.market", TaxonomyLevel.DEPARTMENT, "Limpeza"),
            TaxonomySeedItem("market.pet", "segment.market", TaxonomyLevel.DEPARTMENT, "Pet"),
            TaxonomySeedItem("market.household", "segment.market", TaxonomyLevel.DEPARTMENT, "Utilidades domésticas"),

            TaxonomySeedItem("market.food.produce", "market.food", TaxonomyLevel.CATEGORY, "Hortifruti"),
            TaxonomySeedItem("market.food.grocery", "market.food", TaxonomyLevel.CATEGORY, "Mercearia"),
            TaxonomySeedItem("market.food.dairy", "market.food", TaxonomyLevel.CATEGORY, "Laticínios"),
            TaxonomySeedItem("market.food.meat", "market.food", TaxonomyLevel.CATEGORY, "Carnes e pescados"),
            TaxonomySeedItem("market.food.bakery", "market.food", TaxonomyLevel.CATEGORY, "Padaria e confeitaria"),
            TaxonomySeedItem("market.food.frozen", "market.food", TaxonomyLevel.CATEGORY, "Congelados"),
            TaxonomySeedItem("market.food.sweets", "market.food", TaxonomyLevel.CATEGORY, "Doces e snacks"),

            TaxonomySeedItem("market.food.produce.fruit", "market.food.produce", TaxonomyLevel.SUBCATEGORY, "Frutas"),
            TaxonomySeedItem("market.food.produce.vegetables", "market.food.produce", TaxonomyLevel.SUBCATEGORY, "Legumes"),
            TaxonomySeedItem("market.food.produce.leaves", "market.food.produce", TaxonomyLevel.SUBCATEGORY, "Verduras e folhas"),
            TaxonomySeedItem("market.food.produce.roots", "market.food.produce", TaxonomyLevel.SUBCATEGORY, "Raízes e tubérculos"),
            TaxonomySeedItem("market.food.produce.herbs", "market.food.produce", TaxonomyLevel.SUBCATEGORY, "Ervas e temperos frescos"),

            TaxonomySeedItem("market.personal_care.body", "market.personal_care", TaxonomyLevel.CATEGORY, "Higiene pessoal"),
            TaxonomySeedItem("market.personal_care.oral", "market.personal_care", TaxonomyLevel.CATEGORY, "Cuidados bucais"),
            TaxonomySeedItem("market.cleaning.home", "market.cleaning", TaxonomyLevel.CATEGORY, "Limpeza da casa"),
            TaxonomySeedItem("market.cleaning.laundry", "market.cleaning", TaxonomyLevel.CATEGORY, "Lavanderia"),
            TaxonomySeedItem("market.pet.food", "market.pet", TaxonomyLevel.CATEGORY, "Alimentos para pets"),
            TaxonomySeedItem("market.pet.hygiene", "market.pet", TaxonomyLevel.CATEGORY, "Higiene para pets"),

            TaxonomySeedItem("pharmacy.health", "segment.pharmacy", TaxonomyLevel.DEPARTMENT, "Saúde e medicamentos"),
            TaxonomySeedItem("pharmacy.care", "segment.pharmacy", TaxonomyLevel.DEPARTMENT, "Higiene e cuidados"),
            TaxonomySeedItem("pharmacy.baby", "segment.pharmacy", TaxonomyLevel.DEPARTMENT, "Cuidados infantis"),

            TaxonomySeedItem("pharmacy.health.medicines", "pharmacy.health", TaxonomyLevel.CATEGORY, "Medicamentos"),
            TaxonomySeedItem("pharmacy.health.first_aid", "pharmacy.health", TaxonomyLevel.CATEGORY, "Curativos e primeiros socorros"),
            TaxonomySeedItem("pharmacy.health.supplements", "pharmacy.health", TaxonomyLevel.CATEGORY, "Suplementos"),
            TaxonomySeedItem("pharmacy.care.personal", "pharmacy.care", TaxonomyLevel.CATEGORY, "Higiene pessoal"),
            TaxonomySeedItem("pharmacy.care.oral", "pharmacy.care", TaxonomyLevel.CATEGORY, "Cuidados bucais"),
            TaxonomySeedItem("pharmacy.care.dermo", "pharmacy.care", TaxonomyLevel.CATEGORY, "Dermocosméticos"),

            TaxonomySeedItem("pharmacy.health.medicines.pain", "pharmacy.health.medicines", TaxonomyLevel.SUBCATEGORY, "Analgésicos e antitérmicos"),
            TaxonomySeedItem("pharmacy.health.medicines.allergy", "pharmacy.health.medicines", TaxonomyLevel.SUBCATEGORY, "Antialérgicos"),
            TaxonomySeedItem("pharmacy.health.first_aid.dressings", "pharmacy.health.first_aid", TaxonomyLevel.SUBCATEGORY, "Curativos"),
            TaxonomySeedItem("pharmacy.health.first_aid.antiseptics", "pharmacy.health.first_aid", TaxonomyLevel.SUBCATEGORY, "Antissépticos"),
            TaxonomySeedItem("pharmacy.care.personal.hair", "pharmacy.care.personal", TaxonomyLevel.SUBCATEGORY, "Cuidados capilares"),
            TaxonomySeedItem("pharmacy.care.personal.body", "pharmacy.care.personal", TaxonomyLevel.SUBCATEGORY, "Cuidados corporais"),

            TaxonomySeedItem("food_service.meals", "segment.food_service", TaxonomyLevel.DEPARTMENT, "Refeições"),
            TaxonomySeedItem("food_service.beverages", "segment.food_service", TaxonomyLevel.DEPARTMENT, "Bebidas"),
            TaxonomySeedItem("food_service.meals.fast_food", "food_service.meals", TaxonomyLevel.CATEGORY, "Fast-food"),
            TaxonomySeedItem("food_service.meals.restaurant", "food_service.meals", TaxonomyLevel.CATEGORY, "Restaurante"),
            TaxonomySeedItem("food_service.meals.snacks", "food_service.meals", TaxonomyLevel.CATEGORY, "Lanches"),

            TaxonomySeedItem("clothing.apparel", "segment.clothing", TaxonomyLevel.DEPARTMENT, "Roupas"),
            TaxonomySeedItem("clothing.footwear", "segment.clothing", TaxonomyLevel.DEPARTMENT, "Calçados"),
            TaxonomySeedItem("clothing.accessories", "segment.clothing", TaxonomyLevel.DEPARTMENT, "Acessórios"),

            TaxonomySeedItem("pet_shop.food", "segment.pet_shop", TaxonomyLevel.DEPARTMENT, "Alimentação"),
            TaxonomySeedItem("pet_shop.hygiene", "segment.pet_shop", TaxonomyLevel.DEPARTMENT, "Higiene e cuidados"),
            TaxonomySeedItem("pet_shop.accessories", "segment.pet_shop", TaxonomyLevel.DEPARTMENT, "Acessórios"),

            TaxonomySeedItem("home_building.materials", "segment.home_building", TaxonomyLevel.DEPARTMENT, "Materiais de construção"),
            TaxonomySeedItem("home_building.tools", "segment.home_building", TaxonomyLevel.DEPARTMENT, "Ferramentas"),
            TaxonomySeedItem("home_building.home", "segment.home_building", TaxonomyLevel.DEPARTMENT, "Casa"),

            TaxonomySeedItem("fuel.fuels", "segment.fuel", TaxonomyLevel.DEPARTMENT, "Combustíveis"),
            TaxonomySeedItem("fuel.store", "segment.fuel", TaxonomyLevel.DEPARTMENT, "Conveniência")
        )
    }
}
