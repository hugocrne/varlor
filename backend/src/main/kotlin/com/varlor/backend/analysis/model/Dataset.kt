package com.varlor.backend.analysis.model

data class Dataset(
    val columns: List<String>,
    val rows: List<DataPoint>
) {
    init {
        require(columns.isNotEmpty()) { "Le dataset doit contenir au moins une colonne." }
    }

    fun rowCount(): Int = rows.size
}

