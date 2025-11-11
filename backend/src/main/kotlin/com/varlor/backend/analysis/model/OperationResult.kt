package com.varlor.backend.analysis.model

import java.time.Instant

data class OperationResult(
    val expr: String,
    val result: Any?,
    val status: OperationStatus,
    val executedAt: Instant,
    val alias: String? = null,
    val errorMessage: String? = null
)

