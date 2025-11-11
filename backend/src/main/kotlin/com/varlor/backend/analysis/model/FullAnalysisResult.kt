package com.varlor.backend.analysis.model

data class FullAnalysisResult(
    val preprocessing: PreprocessingResult,
    val operations: List<OperationResult>
)

