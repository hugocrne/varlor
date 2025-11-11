package com.varlor.backend.analysis.service

import com.varlor.backend.analysis.model.Dataset
import com.varlor.backend.analysis.model.OperationDefinition
import com.varlor.backend.analysis.model.OperationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IndicatorEngineServiceTest {

    private val service = IndicatorEngineService()

    private val dataset = Dataset(
        columns = listOf("temperature", "humidity"),
        rows = listOf(
            mapOf("temperature" to 21.0, "humidity" to 35),
            mapOf("temperature" to 24.0, "humidity" to 40),
            mapOf("temperature" to 27.0, "humidity" to 45)
        )
    )

    @Test
    fun `should compute builtin mean`() {
        val operations = listOf(OperationDefinition(expr = "mean(temperature)"))

        val result = service.execute(operations, dataset).first()

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(24.0, result.result)
    }

    @Test
    fun `should compute percentile with params`() {
        val operations = listOf(
            OperationDefinition(
                expr = "percentile(humidity)",
                params = mapOf("percentile" to "75")
            )
        )

        val result = service.execute(operations, dataset).first()

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(42.5, result.result)
    }

    @Test
    fun `should evaluate row level expression`() {
        val operations = listOf(OperationDefinition(expr = "(temperature + humidity) / 2"))

        val result = service.execute(operations, dataset).first()

        assertEquals(OperationStatus.SUCCESS, result.status)
        val values = result.result as? List<*>
        assertNotNull(values)
        assertEquals(listOf(28.0, 32.0, 36.0), values)
    }

    @Test
    fun `should return error when column missing`() {
        val operations = listOf(OperationDefinition(expr = "mean(unknown)"))

        val result = service.execute(operations, dataset).first()

        assertEquals(OperationStatus.ERROR, result.status)
        assertTrue(result.errorMessage?.contains("Colonne inconnue") == true)
    }

    @Test
    fun `should honour operation alias`() {
        val operations = listOf(
            OperationDefinition(
                expr = "mean(temperature)",
                alias = "avg_temp"
            )
        )

        val result = service.execute(operations, dataset).first()

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals("avg_temp", result.expr)
        assertEquals("avg_temp", result.alias)
        assertEquals(24.0, result.result)
    }
}

