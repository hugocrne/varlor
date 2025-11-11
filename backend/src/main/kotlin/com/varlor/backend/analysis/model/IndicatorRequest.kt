package com.varlor.backend.analysis.model

data class IndicatorRequest(
    val dataset: Dataset,
    val operations: List<OperationDefinition>
) {
    init {
        require(operations.isNotEmpty()) { "La liste des opérations ne peut pas être vide." }
    }
}

