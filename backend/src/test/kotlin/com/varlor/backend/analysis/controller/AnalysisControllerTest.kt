package com.varlor.backend.analysis.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.varlor.backend.analysis.config.AnalysisWebConfig
import com.varlor.backend.analysis.model.Dataset
import com.varlor.backend.analysis.model.OperationResult
import com.varlor.backend.analysis.model.OperationStatus
import com.varlor.backend.analysis.model.PreprocessingReport
import com.varlor.backend.analysis.model.PreprocessingResult
import com.varlor.backend.analysis.service.AnalysisPipelineService
import com.varlor.backend.analysis.service.DataPreprocessorService
import com.varlor.backend.analysis.service.IndicatorEngineService
import java.time.Instant
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AnalysisController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(AnalysisWebConfig::class)
class AnalysisControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockBean
    lateinit var dataPreprocessorService: DataPreprocessorService

    @MockBean
    lateinit var indicatorEngineService: IndicatorEngineService

    @MockBean
    lateinit var analysisPipelineService: AnalysisPipelineService

    @Test
    fun `should preprocess dataset in json`() {
        val dataset = Dataset(
            columns = listOf("temperature"),
            rows = listOf(
                mapOf("temperature" to 21.0),
                mapOf("temperature" to 45.0),
                mapOf("temperature" to null)
            )
        )
        val result = PreprocessingResult(
            cleanedDataset = dataset.copy(rows = dataset.rows.take(2)),
            outliersDataset = dataset.copy(rows = listOf(dataset.rows[1])),
            report = PreprocessingReport(
                inputRows = 3,
                outputRows = 2,
                outliersRemoved = 1,
                missingValuesReplaced = 1,
                normalizedFields = listOf("temperature")
            )
        )
        Mockito.doReturn(result)
            .`when`(dataPreprocessorService)
            .preprocess(Mockito.any(Dataset::class.java) ?: dataset)

        val payload = objectMapper.writeValueAsString(dataset)

        mockMvc.perform(
            post("/api/analyses/preprocess")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.report.outliersRemoved").value(1))
            .andExpect(jsonPath("$.cleanedDataset.rows", hasSize<Any>(2)))
    }

    @Test
    fun `should return indicators in yaml`() {
        val operationResults = listOf(
            OperationResult(
                expr = "mean(temperature)",
                result = 24.0,
                status = OperationStatus.SUCCESS,
                executedAt = Instant.parse("2025-11-11T12:00:00Z")
            )
        )

        val defaultDataset = Dataset(columns = listOf("temperature"), rows = emptyList())

        Mockito.doReturn(operationResults)
            .`when`(indicatorEngineService)
            .execute(Mockito.anyList(), Mockito.any(Dataset::class.java) ?: defaultDataset)

        val yamlPayload = """
            dataset:
              columns: [temperature]
              rows:
                - temperature: 21.0
                - temperature: 23.0
            operations:
              - expr: mean(temperature)
        """.trimIndent()

        mockMvc.perform(
            post("/api/analyses/indicators")
                .contentType(MediaType.parseMediaType("application/x-yaml"))
                .accept(MediaType.parseMediaType("application/x-yaml"))
                .content(yamlPayload)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("application/x-yaml"))
            .andExpect(content().string(containsString("mean(temperature)")))
    }

    @Test
    fun `should map service errors to 422`() {
        Mockito.doThrow(IllegalArgumentException("Colonne manquante"))
            .`when`(dataPreprocessorService)
            .preprocess(Mockito.any(Dataset::class.java) ?: Dataset(columns = listOf("value"), rows = emptyList()))

        val payload = """
            {
              "columns": ["value"],
              "rows": [{"value": 1}]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/analyses/preprocess")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error").value("IllegalArgumentException"))
            .andExpect(jsonPath("$.message").value(containsString("Colonne manquante")))
    }
}

