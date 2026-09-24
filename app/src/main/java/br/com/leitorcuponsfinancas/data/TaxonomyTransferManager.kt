package br.com.leitorcuponsfinancas.data

import br.com.leitorcuponsfinancas.domain.ProductNormalizer

data class TaxonomyImportResult(
    val nodesAdded: Int = 0,
    val productsAdded: Int = 0,
    val productLinksAdded: Int = 0,
    val ignored: Int = 0,
    val warnings: List<String> = emptyList(),
)

class TaxonomyTransferManager(
    private val taxonomyDao: TaxonomyDao,
    private val productDao: ProductDao,
) {
    suspend fun exportText(): String {
        val nodes = taxonomyDao.listAllNodes()
            .filter { it.active }
            .sortedWith(compareBy<TaxonomyNodeEntity>({ levelOrder(it.level) }, { it.name.lowercase() }))
        val links = taxonomyDao.listProductLinks()
        val products = productDao.listActiveOnce().associateBy { it.id }

        return buildString {
            appendLine("LCF_TAXONOMIA;1")
            appendLine("TIPO;CHAVE;PAI;NOME;UNIDADE")

            nodes.forEach { node ->
                val parentKey = node.parentId
                    ?.let { id -> nodes.firstOrNull { it.id == id }?.stableKey }
                    .orEmpty()
                appendLine(
                    listOf(
                        levelToTransferType(node.level),
                        escape(node.stableKey),
                        escape(parentKey),
                        escape(node.name),
                        "",
                    ).joinToString(";"),
                )
            }

            links.forEach { link ->
                val node = nodes.firstOrNull { it.id == link.taxonomyNodeId } ?: return@forEach
                val product = products[link.productId] ?: return@forEach
                appendLine(
                    listOf(
                        "PRODUTO",
                        escape("product.${ProductNormalizer.searchKey(product.normalizedName).replace(' ', '_')}"),
                        escape(node.stableKey),
                        escape(product.normalizedName),
                        escape(product.unit),
                    ).joinToString(";"),
                )
            }
        }
    }

    suspend fun importText(text: String): TaxonomyImportResult {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .toList()

        if (lines.isEmpty() || !lines.first().equals("LCF_TAXONOMIA;1", ignoreCase = true)) {
            return TaxonomyImportResult(
                warnings = listOf("Arquivo inválido. O cabeçalho esperado é LCF_TAXONOMIA;1."),
            )
        }

        var nodesAdded = 0
        var productsAdded = 0
        var productLinksAdded = 0
        var ignored = 0
        val warnings = mutableListOf<String>()

        lines.drop(1)
            .filterNot { it.startsWith("TIPO;", ignoreCase = true) }
            .forEachIndexed { index, line ->
                val parts = parseLine(line)
                if (parts.size < 4) {
                    warnings += "Linha ${index + 3}: quantidade de colunas inválida."
                    return@forEachIndexed
                }

                val type = parts[0].uppercase()
                val stableKey = parts[1].trim()
                val parentKey = parts[2].trim()
                val name = ProductNormalizer.displayName(parts[3])
                val unit = parts.getOrNull(4)?.trim()?.uppercase().orEmpty()

                if (type == "PRODUTO") {
                    val parent = taxonomyDao.findByStableKey(parentKey)
                    if (
                        parent == null ||
                        name.isBlank() ||
                        parent.level !in setOf(
                            TaxonomyLevel.CATEGORY.code,
                            TaxonomyLevel.SUBCATEGORY.code,
                        )
                    ) {
                        warnings += "Linha ${index + 3}: produto deve apontar para Categoria ou Subcategoria válida."
                        return@forEachIndexed
                    }

                    val existingProduct = productDao.listActiveOnce().firstOrNull {
                        ProductNormalizer.searchKey(it.normalizedName) ==
                            ProductNormalizer.searchKey(name)
                    }

                    val product = if (existingProduct != null) {
                        existingProduct
                    } else {
                        val ancestry = ancestry(parent.id)
                        val department = ancestry.firstOrNull {
                            it.level == TaxonomyLevel.DEPARTMENT.code
                        }?.name ?: "Outros"
                        val category = ancestry.firstOrNull {
                            it.level == TaxonomyLevel.CATEGORY.code
                        }?.name ?: parent.name
                        val subcategory = ancestry.firstOrNull {
                            it.level == TaxonomyLevel.SUBCATEGORY.code
                        }?.name
                        val now = System.currentTimeMillis()
                        val candidate = ProductEntity(
                            normalizedName = name,
                            sector = department,
                            category = category,
                            subcategory = subcategory,
                            unit = unit.ifBlank { "UN" },
                            createdAt = now,
                            updatedAt = now,
                        )
                        val id = productDao.insert(candidate)
                        productsAdded++
                        candidate.copy(id = id)
                    }

                    val result = taxonomyDao.insertProductLink(
                        TaxonomyProductLinkEntity(
                            taxonomyNodeId = parent.id,
                            productId = product.id,
                        ),
                    )
                    if (result == -1L) ignored++ else productLinksAdded++
                    return@forEachIndexed
                }

                val level = transferTypeToLevel(type)
                if (level == null || stableKey.isBlank() || name.isBlank()) {
                    warnings += "Linha ${index + 3}: tipo, chave ou nome inválido."
                    return@forEachIndexed
                }

                if (taxonomyDao.findByStableKey(stableKey) != null) {
                    ignored++
                    return@forEachIndexed
                }

                val parent = if (level == TaxonomyLevel.SEGMENT) {
                    null
                } else {
                    taxonomyDao.findByStableKey(parentKey)
                }
                val parentId = parent?.id

                if (level != TaxonomyLevel.SEGMENT && parentId == null) {
                    warnings += "Linha ${index + 3}: pai \"$parentKey\" não encontrado."
                    return@forEachIndexed
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
                    warnings += "Linha ${index + 3}: hierarquia inválida para ${level.label}."
                    return@forEachIndexed
                }

                val sibling = taxonomyDao.listChildren(parentId).firstOrNull {
                    it.level == level.code &&
                        ProductNormalizer.searchKey(it.name) == ProductNormalizer.searchKey(name)
                }
                if (sibling != null) {
                    ignored++
                    return@forEachIndexed
                }

                val now = System.currentTimeMillis()
                val inserted = taxonomyDao.insertNode(
                    TaxonomyNodeEntity(
                        stableKey = stableKey,
                        parentId = parentId,
                        level = level.code,
                        name = name,
                        searchKey = ProductNormalizer.searchKey(name),
                        builtIn = false,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                if (inserted == -1L) ignored++ else nodesAdded++
            }

        return TaxonomyImportResult(
            nodesAdded = nodesAdded,
            productsAdded = productsAdded,
            productLinksAdded = productLinksAdded,
            ignored = ignored,
            warnings = warnings,
        )
    }

    fun templateText(): String = buildString {
        appendLine("LCF_TAXONOMIA;1")
        appendLine("TIPO;CHAVE;PAI;NOME;UNIDADE")
        appendLine("SEGMENTO;segment.exemplo;;Exemplo;")
        appendLine("DEPARTAMENTO;exemplo.alimentos;segment.exemplo;Alimentos;")
        appendLine("CATEGORIA;exemplo.alimentos.hortifruti;exemplo.alimentos;Hortifruti;")
        appendLine("SUBCATEGORIA;exemplo.alimentos.hortifruti.frutas;exemplo.alimentos.hortifruti;Frutas;")
        appendLine("PRODUTO;product.banana;exemplo.alimentos.hortifruti.frutas;Banana;KG")
    }

    private suspend fun ancestry(nodeId: Long): List<TaxonomyNodeEntity> {
        val result = mutableListOf<TaxonomyNodeEntity>()
        var current = taxonomyDao.findById(nodeId)
        while (current != null) {
            result += current
            current = current.parentId?.let { taxonomyDao.findById(it) }
        }
        return result.reversed()
    }

    private fun levelOrder(level: String): Int = when (level) {
        TaxonomyLevel.SEGMENT.code -> 0
        TaxonomyLevel.DEPARTMENT.code -> 1
        TaxonomyLevel.CATEGORY.code -> 2
        TaxonomyLevel.SUBCATEGORY.code -> 3
        else -> 9
    }

    private fun levelToTransferType(level: String): String = when (level) {
        TaxonomyLevel.SEGMENT.code -> "SEGMENTO"
        TaxonomyLevel.DEPARTMENT.code -> "DEPARTAMENTO"
        TaxonomyLevel.CATEGORY.code -> "CATEGORIA"
        TaxonomyLevel.SUBCATEGORY.code -> "SUBCATEGORIA"
        else -> level
    }

    private fun transferTypeToLevel(type: String): TaxonomyLevel? = when (type) {
        "SEGMENTO" -> TaxonomyLevel.SEGMENT
        "DEPARTAMENTO" -> TaxonomyLevel.DEPARTMENT
        "CATEGORIA" -> TaxonomyLevel.CATEGORY
        "SUBCATEGORIA" -> TaxonomyLevel.SUBCATEGORY
        else -> null
    }

    private fun escape(value: String): String =
        if (value.contains(';') || value.contains('"')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }

    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == ';' && !quoted -> {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        result += current.toString()
        return result
    }

    companion object {
        val MANUAL = """
            Layout LCF Taxonomia v1

            Arquivo texto UTF-8 separado por ponto e vírgula.

            Primeira linha obrigatória:
            LCF_TAXONOMIA;1

            Segunda linha recomendada:
            TIPO;CHAVE;PAI;NOME;UNIDADE

            Tipos aceitos:
            SEGMENTO, DEPARTAMENTO, CATEGORIA, SUBCATEGORIA e PRODUTO.

            Regras:
            - CHAVE deve ser única e estável.
            - PAI recebe a CHAVE do nível imediatamente anterior.
            - SEGMENTO não possui PAI.
            - UNIDADE só é usada em PRODUTO; quando vazia, assume UN.
            - A importação é sempre incremental.
            - Registros existentes não são apagados nem sobrescritos.
            - Chaves ou classificações já existentes são ignoradas.
            - Produtos existentes com o mesmo nome são reutilizados e apenas recebem o novo vínculo.

            Exemplo:
            LCF_TAXONOMIA;1
            TIPO;CHAVE;PAI;NOME;UNIDADE
            SEGMENTO;segment.market;;Mercado;
            DEPARTAMENTO;market.food;segment.market;Alimentos;
            CATEGORIA;market.food.produce;market.food;Hortifruti;
            SUBCATEGORIA;market.food.produce.fruit;market.food.produce;Frutas;
            PRODUTO;product.banana;market.food.produce.fruit;Banana;KG
        """.trimIndent()
    }
}
