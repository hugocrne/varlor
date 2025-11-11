package com.varlor.backend.analysis.model

data class PreprocessingReport(
    val inputRows: Int,
    val outputRows: Int,
    val outliersRemoved: Int,
    val missingValuesReplaced: Int,
    val normalizedFields: List<String>
)

