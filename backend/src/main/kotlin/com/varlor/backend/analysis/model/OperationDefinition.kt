package com.varlor.backend.analysis.model

data class OperationDefinition(
    val expr: String,
    val params: Map<String, String> = emptyMap(),
    val alias: String? = null
)

