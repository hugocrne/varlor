package com.varlor.backend.analysis.model.dto

import com.varlor.backend.analysis.model.IndicatorRequest
import com.varlor.backend.analysis.model.OperationDefinition
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Définition d'une opération à exécuter sur un dataset.")
data class OperationDefinitionDto(
    @field:NotBlank(message = "L'expression ne peut pas être vide.")
    @field:Schema(
        description = "Expression à évaluer (fonction builtin ou expression EvalEx).",
        example = "mean(temperature)"
    )
    val expr: String,
    @field:Schema(
        description = "Paramètres supplémentaires injectés dans l'opération.",
        example = "{\"percentile\": \"75\"}"
    )
    val params: Map<String, String> = emptyMap(),
    @field:Schema(
        description = "Alias optionnel pour nommer le résultat.",
        example = "avg_temperature",
        nullable = true
    )
    val alias: String? = null
) {
    fun toModel(): OperationDefinition = OperationDefinition(
        expr = expr,
        params = params,
        alias = alias?.takeIf { it.isNotBlank() }
    )
}

@Schema(description = "Requête d'analyse combinant un dataset et une liste d'opérations.")
data class IndicatorRequestDto(
    @field:Valid
    @field:Schema(description = "Dataset sur lequel exécuter les opérations.")
    val dataset: DatasetDto,

    @field:NotEmpty(message = "La liste des opérations ne peut pas être vide.")
    @field:Schema(
        description = "Liste ordonnée des opérations à exécuter.",
        example = "[{\"expr\": \"mean(temperature)\", \"alias\": \"avg_temp\"}]"
    )
    val operations: List<@Valid OperationDefinitionDto>
) {
    fun toModel(): IndicatorRequest = IndicatorRequest(
        dataset = dataset.toModel(),
        operations = operations.map { it.toModel() }
    )
}

