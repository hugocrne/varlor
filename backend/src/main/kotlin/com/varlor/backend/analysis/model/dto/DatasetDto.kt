package com.varlor.backend.analysis.model.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.varlor.backend.analysis.model.Dataset
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Représentation sérialisée d'un dataset tabulaire.")
data class DatasetDto(
    @field:NotEmpty(message = "La liste des colonnes ne peut pas être vide.")
    @field:Schema(
        description = "Liste ordonnée des noms de colonnes.",
        example = "[\"temperature\", \"status\"]"
    )
    val columns: List<String>,

    @field:NotNull(message = "La liste des lignes ne peut pas être nulle.")
    @field:Schema(
        description = "Liste des points de données, chaque entrée étant une carte de colonne/valeur.",
        example = "[{\"temperature\": 21.3, \"status\": \"OK\"}]"
    )
    val rows: List<Map<String, Any?>>
) {

    @JsonIgnore
    private val columnSet: Set<String> = columns.toSet()

    @AssertTrue(message = "Chaque colonne doit avoir un nom non vide.")
    fun hasValidColumnNames(): Boolean = columns.all { it.isNotBlank() }

    @AssertTrue(message = "Les lignes ne doivent contenir que des colonnes déclarées.")
    fun rowsMatchColumns(): Boolean = rows.all { row ->
        row.keys.all { key -> key.isNotBlank() && columnSet.contains(key) }
    }

    fun toModel(): Dataset {
        val normalizedRows = rows.map { row ->
            row.filterKeys { key -> key.isNotBlank() && columnSet.contains(key) }
        }
        return Dataset(columns, normalizedRows)
    }
}

