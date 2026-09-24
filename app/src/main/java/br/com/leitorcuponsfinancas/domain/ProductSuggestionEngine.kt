package br.com.leitorcuponsfinancas.domain

import br.com.leitorcuponsfinancas.data.MerchantProductLinkEntity
import br.com.leitorcuponsfinancas.data.ProductEntity

sealed interface SmartProductSuggestion {
    val confidence: Int

    data class ExistingProduct(
        val product: ProductEntity,
        override val confidence: Int,
        val reason: String,
    ) : SmartProductSuggestion

    data class NewProduct(
        val name: String,
        val sector: String,
        val category: String,
        val subcategory: String?,
        val unit: String,
        override val confidence: Int,
        val reason: String,
    ) : SmartProductSuggestion
}

object ProductSuggestionEngine {

    private data class CatalogRule(
        val keywords: Set<String>,
        val name: String,
        val sector: String,
        val category: String,
        val subcategory: String? = null,
    )

    private val rules = listOf(
        CatalogRule(setOf("BATATA"), "Batata", "Alimentação", "Hortifruti", "Legumes"),
        CatalogRule(setOf("NISSIN", "MIOJO", "LAMEN", "LÁMEN", "MACARRAO INSTANTANEO", "MACARRÃO INSTANTÂNEO"), "Macarrão instantâneo", "Alimentação", "Mercado", "Massas"),
        CatalogRule(setOf("MACARRAO", "MACARRÃO"), "Macarrão", "Alimentação", "Mercado", "Massas"),
        CatalogRule(setOf("CHOCOLATE", "BOMBOM"), "Chocolate", "Alimentação", "Mercado", "Doces"),
        CatalogRule(setOf("ARROZ"), "Arroz", "Alimentação", "Mercado", "Mercearia"),
        CatalogRule(setOf("FEIJAO", "FEIJÃO"), "Feijão", "Alimentação", "Mercado", "Mercearia"),
        CatalogRule(setOf("PAO", "PÃO"), "Pão", "Alimentação", "Padaria"),
        CatalogRule(setOf("LEITE"), "Leite", "Alimentação", "Mercado", "Laticínios"),
        CatalogRule(setOf("QUEIJO"), "Queijo", "Alimentação", "Mercado", "Laticínios"),
        CatalogRule(setOf("CAFE", "CAFÉ"), "Café", "Alimentação", "Mercado", "Bebidas"),
        CatalogRule(setOf("REFRIGERANTE"), "Refrigerante", "Alimentação", "Mercado", "Bebidas"),
        CatalogRule(setOf("AGUA", "ÁGUA"), "Água", "Alimentação", "Mercado", "Bebidas"),
        CatalogRule(setOf("BANANA"), "Banana", "Alimentação", "Hortifruti", "Frutas"),
        CatalogRule(setOf("TOMATE"), "Tomate", "Alimentação", "Hortifruti", "Legumes"),
        CatalogRule(setOf("CEBOLA"), "Cebola", "Alimentação", "Hortifruti", "Legumes"),
        CatalogRule(setOf("OVO", "OVOS"), "Ovos", "Alimentação", "Mercado", "Frios e ovos"),
        CatalogRule(setOf("PAPEL HIGIENICO", "PAPEL HIGIÊNICO"), "Papel higiênico", "Casa", "Higiene e limpeza", "Papelaria sanitária"),
        CatalogRule(setOf("DETERGENTE"), "Detergente", "Casa", "Higiene e limpeza", "Limpeza"),
        CatalogRule(setOf("SABONETE"), "Sabonete", "Cuidados pessoais", "Higiene", null),
        CatalogRule(setOf("SHAMPOO"), "Shampoo", "Cuidados pessoais", "Higiene", null),
    )

    fun suggest(
        description: String,
        unit: String?,
        products: List<ProductEntity>,
        learnedLinks: List<MerchantProductLinkEntity>,
    ): SmartProductSuggestion? {
        val descriptionKey = ProductNormalizer.searchKey(description)
        if (descriptionKey.isBlank()) return null

        val existing = products
            .mapNotNull { product ->
                val score = scoreExistingProduct(
                    descriptionKey = descriptionKey,
                    product = product,
                    learnedLinks = learnedLinks.filter { it.productId == product.id },
                )
                score?.let { product to it }
            }
            .maxByOrNull { it.second }

        if (existing != null && existing.second >= 62) {
            return SmartProductSuggestion.ExistingProduct(
                product = existing.first,
                confidence = existing.second,
                reason = if (existing.second >= 90) {
                    "Nome ou descrição muito semelhante."
                } else {
                    "Há palavras em comum com este produto ou com aliases já aprendidos."
                },
            )
        }

        val rule = rules
            .mapNotNull { rule ->
                val matched = rule.keywords.any { keyword ->
                    containsWholeExpression(descriptionKey, ProductNormalizer.searchKey(keyword))
                }
                if (matched) rule else null
            }
            .firstOrNull()
            ?: return null

        val existingByRuleName = products.firstOrNull {
            ProductNormalizer.searchKey(it.normalizedName) == ProductNormalizer.searchKey(rule.name)
        }

        if (existingByRuleName != null) {
            return SmartProductSuggestion.ExistingProduct(
                product = existingByRuleName,
                confidence = 93,
                reason = "A descrição contém uma palavra-chave reconhecida para este produto.",
            )
        }

        return SmartProductSuggestion.NewProduct(
            name = rule.name,
            sector = rule.sector,
            category = rule.category,
            subcategory = rule.subcategory,
            unit = unit?.trim()?.uppercase()?.takeIf { it.isNotBlank() } ?: "UN",
            confidence = 88,
            reason = "A descrição contém uma palavra-chave conhecida, mas ainda não existe produto mestre compatível.",
        )
    }

    private fun scoreExistingProduct(
        descriptionKey: String,
        product: ProductEntity,
        learnedLinks: List<MerchantProductLinkEntity>,
    ): Int? {
        val productKey = ProductNormalizer.searchKey(product.normalizedName)
        if (productKey.isBlank()) return null

        if (descriptionKey == productKey) return 100
        if (containsWholeExpression(descriptionKey, productKey)) {
            return if (tokenize(productKey).size > 1) 96 else 84
        }

        product.fiscalDescription?.let { fiscal ->
            val fiscalKey = ProductNormalizer.searchKey(fiscal)
            if (fiscalKey.isNotBlank()) {
                if (descriptionKey == fiscalKey) return 99
                val similarity = tokenSimilarity(descriptionKey, fiscalKey)
                if (similarity >= 0.75) return 92
                if (similarity >= 0.50) return 76
            }
        }

        var best = 0
        learnedLinks.forEach { link ->
            val aliasKey = ProductNormalizer.searchKey(link.fiscalDescription)
            if (aliasKey.isBlank()) return@forEach
            if (descriptionKey == aliasKey) {
                best = maxOf(best, 99)
            } else {
                val similarity = tokenSimilarity(descriptionKey, aliasKey)
                best = maxOf(
                    best,
                    when {
                        similarity >= 0.80 -> 94
                        similarity >= 0.60 -> 82
                        similarity >= 0.45 -> 68
                        else -> 0
                    },
                )
            }
        }

        val nameSimilarity = tokenSimilarity(descriptionKey, productKey)
        best = maxOf(
            best,
            when {
                nameSimilarity >= 0.80 -> 90
                nameSimilarity >= 0.60 -> 78
                nameSimilarity >= 0.45 -> 65
                else -> 0
            },
        )

        return best.takeIf { it > 0 }
    }

    private fun tokenSimilarity(left: String, right: String): Double {
        val leftTokens = tokenize(left)
        val rightTokens = tokenize(right)
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0

        val intersection = leftTokens.intersect(rightTokens).size.toDouble()
        val union = leftTokens.union(rightTokens).size.toDouble()
        val coverage = intersection / rightTokens.size
        val jaccard = intersection / union
        return (coverage * 0.7) + (jaccard * 0.3)
    }

    private fun tokenize(value: String): Set<String> =
        ProductNormalizer.searchKey(value)
            .split(Regex("""[^A-Z0-9]+"""))
            .map { it.trim() }
            .filter { token ->
                token.length >= 2 &&
                    token !in stopWords &&
                    token.none { it.isDigit() }
            }
            .toSet()

    private fun containsWholeExpression(text: String, expression: String): Boolean {
        if (expression.isBlank()) return false
        if (text == expression) return true
        return text.startsWith("$expression ") ||
            text.endsWith(" $expression") ||
            text.contains(" $expression ")
    }

    private val stopWords = setOf(
        "UN", "UND", "KG", "G", "GR", "L", "ML", "PCT", "CX", "DZ",
        "DE", "DA", "DO", "DAS", "DOS", "COM", "SEM", "TIPO", "TP",
    )
}
