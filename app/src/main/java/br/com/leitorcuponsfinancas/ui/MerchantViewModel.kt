package br.com.leitorcuponsfinancas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.MerchantEntity
import br.com.leitorcuponsfinancas.data.MerchantRepository
import br.com.leitorcuponsfinancas.data.TaxonomyNodeEntity
import br.com.leitorcuponsfinancas.data.TaxonomyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MerchantEditState(
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class MerchantViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = MerchantRepository(database.merchantDao())
    private val taxonomyRepository = TaxonomyRepository(
        taxonomyDao = database.taxonomyDao(),
        productDao = database.productDao(),
    )

    val merchants: StateFlow<List<MerchantEntity>> =
        repository.merchants.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val segments: StateFlow<List<TaxonomyNodeEntity>> =
        taxonomyRepository.segments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _editState = MutableStateFlow(MerchantEditState())
    val editState: StateFlow<MerchantEditState> = _editState.asStateFlow()

    init {
        viewModelScope.launch {
            taxonomyRepository.ensureBaseTaxonomy()
        }
    }

    fun clearMessage() {
        _editState.value = MerchantEditState()
    }

    fun save(
        merchant: MerchantEntity,
        name: String,
        segmentNodeId: Long?,
    ) {
        if (_editState.value.saving) return
        _editState.value = MerchantEditState(saving = true)

        viewModelScope.launch {
            val renameError = repository.rename(merchant, name)
            if (renameError != null) {
                _editState.value = MerchantEditState(error = renameError)
                return@launch
            }

            val updated = merchant.copy(
                displayName = name.trim(),
                segmentNodeId = segmentNodeId,
            )
            repository.setSegment(updated, segmentNodeId)
            _editState.value = MerchantEditState(
                message = "Estabelecimento e segmento atualizados.",
            )
        }
    }}
