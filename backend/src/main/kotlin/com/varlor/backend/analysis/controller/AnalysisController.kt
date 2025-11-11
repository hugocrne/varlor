package com.varlor.backend.analysis.controller

import com.varlor.backend.analysis.model.Dataset
import com.varlor.backend.analysis.model.FullAnalysisResult
import com.varlor.backend.analysis.model.IndicatorRequest
import com.varlor.backend.analysis.model.PreprocessingResult
import com.varlor.backend.analysis.model.OperationResult
import com.varlor.backend.analysis.service.DataPreprocessorService
import com.varlor.backend.analysis.service.IndicatorEngineService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.parameters.RequestBody as OpenApiRequestBody

@RestController
@RequestMapping("/api/analyses")
@Tag(name = "Analysis", description = "Opérations d'analyse de données (prétraitement, indicateurs, expressions dynamiques)")
class AnalysisController(
    private val dataPreprocessorService: DataPreprocessorService,
    private val indicatorEngineService: IndicatorEngineService
) {

    @PostMapping("/preprocess")
    @Operation(
        summary = "Prétraiter un dataset",
        description = "Détecte les types, normalise les données, retire les outliers et impute les valeurs manquantes.",
        requestBody = OpenApiRequestBody(
            required = true,
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Dataset::class),
                    examples = [
                        ExampleObject(
                            name = "JSON",
                            summary = "Dataset en JSON",
                            value = PREPROCESS_REQUEST_EXAMPLE_JSON
                        )
                    ]
                ),
                Content(
                    mediaType = "application/yaml",
                    schema = Schema(implementation = Dataset::class),
                    examples = [
                        ExampleObject(
                            name = "YAML",
                            summary = "Dataset en YAML",
                            value = PREPROCESS_REQUEST_EXAMPLE_YAML
                        )
                    ]
                )
            ]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Prétraitement effectué avec succès.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = PreprocessingResult::class),
                        examples = [
                            ExampleObject(
                                name = "Réponse",
                                value = PREPROCESS_RESPONSE_EXAMPLE_JSON
                            )
                        ]
                    )
                ]
            )
        ]
    )
    fun preprocess(@RequestBody dataset: Dataset): PreprocessingResult =
        dataPreprocessorService.preprocess(dataset)

    @PostMapping("/indicators")
    @Operation(
        summary = "Calculer des indicateurs",
        description = "Exécute une liste d'opérations sur un dataset (fonctions intégrées ou expressions EvalEx).",
        requestBody = OpenApiRequestBody(
            required = true,
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = IndicatorRequest::class),
                    examples = [
                        ExampleObject(
                            name = "JSON",
                            summary = "Requête indicateurs",
                            value = INDICATOR_REQUEST_EXAMPLE_JSON
                        )
                    ]
                )
            ]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Indicateurs calculés.",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = OperationResult::class)),
                        examples = [
                            ExampleObject(
                                name = "Réponse",
                                value = INDICATOR_RESPONSE_EXAMPLE_JSON
                            )
                        ]
                    )
                ]
            )
        ]
    )
    fun indicators(@RequestBody request: IndicatorRequest): List<OperationResult> =
        indicatorEngineService.execute(request.operations, request.dataset)

    @PostMapping("/full")
    @Operation(
        summary = "Pipeline complet d'analyse",
        description = "Enchaîne le prétraitement et le calcul des indicateurs sur le dataset fourni.",
        requestBody = OpenApiRequestBody(
            required = true,
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = IndicatorRequest::class),
                    examples = [
                        ExampleObject(
                            name = "JSON",
                            summary = "Requête pipeline complet",
                            value = FULL_REQUEST_EXAMPLE_JSON
                        )
                    ]
                )
            ]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Prétraitement et indicateurs calculés.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = FullAnalysisResult::class),
                        examples = [
                            ExampleObject(
                                name = "Réponse",
                                value = FULL_RESPONSE_EXAMPLE_JSON
                            )
                        ]
                    )
                ]
            )
        ]
    )
    fun full(@RequestBody request: IndicatorRequest): FullAnalysisResult {
        val preprocessing = dataPreprocessorService.preprocess(request.dataset)
        val operationResults = indicatorEngineService.execute(request.operations, preprocessing.cleanedDataset)
        return FullAnalysisResult(
            preprocessing = preprocessing,
            operations = operationResults
        )
    }

    companion object {
        private const val PREPROCESS_REQUEST_EXAMPLE_JSON =
            "{\n" +
                "  \"columns\": [\"temperature\", \"status\"],\n" +
                "  \"rows\": [\n" +
                "    {\"temperature\": 21.3, \"status\": \"OK\"},\n" +
                "    {\"temperature\": 45.1, \"status\": \"ALERT\"},\n" +
                "    {\"temperature\": 22.8, \"status\": null}\n" +
                "  ]\n" +
                "}"

        private const val PREPROCESS_REQUEST_EXAMPLE_YAML =
            "columns:\n" +
                "  - temperature\n" +
                "  - status\n" +
                "rows:\n" +
                "  - temperature: 21.3\n" +
                "    status: OK\n" +
                "  - temperature: 45.1\n" +
                "    status: ALERT\n" +
                "  - temperature: 22.8\n" +
                "    status:\n"

        private const val PREPROCESS_RESPONSE_EXAMPLE_JSON =
            "{\n" +
                "  \"cleanedDataset\": {\n" +
                "    \"columns\": [\"temperature\", \"status\"],\n" +
                "    \"rows\": [\n" +
                "      {\"temperature\": 21.3, \"status\": \"OK\"},\n" +
                "      {\"temperature\": 22.8, \"status\": \"ALERT\"}\n" +
                "    ]\n" +
                "  },\n" +
                "  \"outliersDataset\": {\n" +
                "    \"columns\": [\"temperature\", \"status\"],\n" +
                "    \"rows\": [\n" +
                "      {\"temperature\": 45.1, \"status\": \"ALERT\"}\n" +
                "    ]\n" +
                "  },\n" +
                "  \"report\": {\n" +
                "    \"inputRows\": 3,\n" +
                "    \"outputRows\": 2,\n" +
                "    \"outliersRemoved\": 1,\n" +
                "    \"missingValuesReplaced\": 1,\n" +
                "    \"normalizedFields\": [\"temperature\"]\n" +
                "  }\n" +
                "}"

        private const val INDICATOR_REQUEST_EXAMPLE_JSON =
            "{\n" +
                "  \"dataset\": {\n" +
                "    \"columns\": [\"temperature\", \"humidity\"],\n" +
                "    \"rows\": [\n" +
                "      {\"temperature\": 21.3, \"humidity\": 35},\n" +
                "      {\"temperature\": 22.8, \"humidity\": 40},\n" +
                "      {\"temperature\": 25.0, \"humidity\": 45}\n" +
                "    ]\n" +
                "  },\n" +
                "  \"operations\": [\n" +
                "    {\"expr\": \"mean(temperature)\", \"alias\": \"avg_temperature\"},\n" +
                "    {\"expr\": \"percentile(humidity, 75)\"},\n" +
                "    {\"expr\": \"(temperature + humidity) / 2\"}\n" +
                "  ]\n" +
                "}"

        private const val INDICATOR_RESPONSE_EXAMPLE_JSON =
            "[\n" +
                "  {\"expr\": \"avg_temperature\", \"alias\": \"avg_temperature\", \"result\": 23.03, \"status\": \"SUCCESS\", \"executedAt\": \"2025-11-11T12:00:00Z\"},\n" +
                "  {\"expr\": \"percentile(humidity, 75)\", \"alias\": null, \"result\": 42.5, \"status\": \"SUCCESS\", \"executedAt\": \"2025-11-11T12:00:00Z\"},\n" +
                "  {\"expr\": \"(temperature + humidity) / 2\", \"alias\": null, \"result\": [28.15, 31.4, 35.0], \"status\": \"SUCCESS\", \"executedAt\": \"2025-11-11T12:00:00Z\"}\n" +
                "]"

        private const val FULL_REQUEST_EXAMPLE_JSON =
            "{\n" +
                "  \"dataset\": {\n" +
                "    \"columns\": [\"temperature\", \"humidity\"],\n" +
                "    \"rows\": [\n" +
                "      {\"temperature\": 21.3, \"humidity\": 35},\n" +
                "      {\"temperature\": 22.8, \"humidity\": null},\n" +
                "      {\"temperature\": 45.0, \"humidity\": 42}\n" +
                "    ]\n" +
                "  },\n" +
                "  \"operations\": [\n" +
                "    {\"expr\": \"mean(temperature)\", \"alias\": \"avg_temp\"},\n" +
                "    {\"expr\": \"correlation(temperature, humidity)\"}\n" +
                "  ]\n" +
                "}"

        private const val FULL_RESPONSE_EXAMPLE_JSON =
            "{\n" +
                "  \"preprocessing\": {\n" +
                "    \"cleanedDataset\": {\n" +
                "      \"columns\": [\"temperature\", \"humidity\"],\n" +
                "      \"rows\": [\n" +
                "        {\"temperature\": 21.3, \"humidity\": 35.0},\n" +
                "        {\"temperature\": 22.8, \"humidity\": 38.5}\n" +
                "      ]\n" +
                "    },\n" +
                "    \"outliersDataset\": {\n" +
                "      \"columns\": [\"temperature\", \"humidity\"],\n" +
                "      \"rows\": [\n" +
                "        {\"temperature\": 45.0, \"humidity\": 42.0}\n" +
                "      ]\n" +
                "    },\n" +
                "    \"report\": {\n" +
                "      \"inputRows\": 3,\n" +
                "      \"outputRows\": 2,\n" +
                "      \"outliersRemoved\": 1,\n" +
                "      \"missingValuesReplaced\": 1,\n" +
                "      \"normalizedFields\": [\"humidity\", \"temperature\"]\n" +
                "    }\n" +
                "  },\n" +
                "  \"operations\": [\n" +
                "    {\"expr\": \"avg_temp\", \"alias\": \"avg_temp\", \"result\": 22.05, \"status\": \"SUCCESS\", \"executedAt\": \"2025-11-11T12:00:00Z\"},\n" +
                "    {\"expr\": \"correlation(temperature, humidity)\", \"alias\": null, \"result\": 0.98, \"status\": \"SUCCESS\", \"executedAt\": \"2025-11-11T12:00:00Z\"}\n" +
                "  ]\n" +
                "}"
    }
}

