package com.varlor.backend.analysis.model

data class PreprocessingResult(
    val cleanedDataset: Dataset,
    val outliersDataset: Dataset,
    val report: PreprocessingReport
)

