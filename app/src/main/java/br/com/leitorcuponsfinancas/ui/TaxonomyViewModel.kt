package br.com.leitorcuponsfinancas.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.TaxonomyImportResult
import br.com.leitorcuponsfinancas.data.TaxonomyLevel
import br.com.leitorcuponsfinancas.data.TaxonomyNodeEntity
import br.com.leitorcuponsfinancas.data.TaxonomyRepository
import br.com.leitorcuponsfinancas.data.TaxonomyTransferManager
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaxonomyActionState(
    val working: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class TaxonomyViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = TaxonomyRepository(
        taxonomyDao = database.taxonomyDao(),
        productDao = database.productDao(),
    )
    private val transferManager = TaxonomyTransferManager(
        taxonomyDao = database.taxonomyDao(),
        productDao = database.productDao(),
    )

    val nodes: StateFlow<List<TaxonomyNodeEntity>> =
        repository.nodes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val productLinks =
        repository.productLinks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _actionState = MutableStateFlow(TaxonomyActionState())
    val actionState: StateFlow<TaxonomyActionState> = _actionState.asStateFlow()

    init {
        viewModelScope.launch {
            val created = repository.ensureBaseTaxonomy()
            if (created > 0) {
                _actionState.value = TaxonomyActionState(
                    message = "Taxonomia base preparada com $created classificação(ões).",
                )
            }
        }
    }

    fun clearMessage() {
        _actionState.value = TaxonomyActionState()
    }

    fun createNode(
        parent: TaxonomyNodeEntity?,
        level: TaxonomyLevel,
        name: String,
    ) {
        if (_actionState.value.working) return
        _actionState.value = TaxonomyActionState(working = true)

        viewModelScope.launch {
            repository.createNode(
                parentId = parent?.id,
                level = level,
                name = name,
            ).fold(
                onSuccess = {
                    _actionState.value = TaxonomyActionState(
                        message = "${level.label} \"${it.name}\" adicionado sem alterar os cadastros existentes.",
                    )
                },
                onFailure = {
                    _actionState.value = TaxonomyActionState(
                        error = it.message ?: "Não foi possível adicionar a classificação.",
                    )
                },
            )
        }
    }

    fun renameNode(
        node: TaxonomyNodeEntity,
        name: String,
    ) {
        if (_actionState.value.working) return
        _actionState.value = TaxonomyActionState(working = true)

        viewModelScope.launch {
            val error = repository.renameNode(node, name)
            _actionState.value = if (error == null) {
                TaxonomyActionState(message = "Classificação atualizada.")
            } else {
                TaxonomyActionState(error = error)
            }
        }
    }

    fun restoreBase() {
        if (_actionState.value.working) return
        _actionState.value = TaxonomyActionState(working = true)

        viewModelScope.launch {
            val created = repository.ensureBaseTaxonomy()
            _actionState.value = TaxonomyActionState(
                message = if (created == 0) {
                    "A taxonomia base já está completa."
                } else {
                    "$created classificação(ões) da base foram acrescentadas."
                },
            )
        }
    }

    fun importFrom(uri: Uri) {
        if (_actionState.value.working) return
        _actionState.value = TaxonomyActionState(working = true)

        viewModelScope.launch {
            runCatching {
                val resolver = getApplication<Application>().contentResolver
                val text = resolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(StandardCharsets.UTF_8)
                } ?: error("Não foi possível abrir o arquivo.")

                transferManager.importText(text)
            }.onSuccess { result ->
                _actionState.value = TaxonomyActionState(
                    message = importSummary(result),
                    error = result.warnings
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString("\n"),
                )
            }.onFailure {
                _actionState.value = TaxonomyActionState(
                    error = it.message ?: "Falha ao importar a taxonomia.",
                )
            }
        }
    }

    fun exportTo(uri: Uri) {
        if (_actionState.value.working) return
        _actionState.value = TaxonomyActionState(working = true)

        viewModelScope.launch {
            runCatching {
                val resolver = getApplication<Application>().contentResolver
                val text = transferManager.exportText()
                resolver.openOutputStream(uri, "wt")?.use { output ->
                    output.write(text.toByteArray(StandardCharsets.UTF_8))
                } ?: error("Não foi possível gravar o arquivo.")
            }.onSuccess {
                _actionState.value = TaxonomyActionState(
                    message = "Taxonomia exportada. O arquivo pode ser compartilhado e importado em outro aparelho.",
                )
            }.onFailure {
                _actionState.value = TaxonomyActionState(
                    error = it.message ?: "Falha ao exportar a taxonomia.",
                )
            }
        }
    }

    fun exportTemplateTo(uri: Uri) {
        if (_actionState.value.working) return
        _actionState.value = TaxonomyActionState(working = true)

        viewModelScope.launch {
            runCatching {
                val resolver = getApplication<Application>().contentResolver
                val text = transferManager.templateText()
                resolver.openOutputStream(uri, "wt")?.use { output ->
                    output.write(text.toByteArray(StandardCharsets.UTF_8))
                } ?: error("Não foi possível gravar o modelo.")
            }.onSuccess {
                _actionState.value = TaxonomyActionState(
                    message = "Modelo de importação criado.",
                )
            }.onFailure {
                _actionState.value = TaxonomyActionState(
                    error = it.message ?: "Falha ao criar o modelo.",
                )
            }
        }
    }

    private fun importSummary(result: TaxonomyImportResult): String =
        buildString {
            append("Importação concluída de forma incremental. ")
            append("${result.nodesAdded} classificação(ões), ")
            append("${result.productsAdded} Produto(s) Mestre e ")
            append("${result.productLinksAdded} vínculo(s) acrescentados. ")
            append("${result.ignored} registro(s) já existentes foram preservados/ignorados.")
        }
}
