package com.varlor.backend.analysis.service

import com.varlor.backend.analysis.model.Dataset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DataPreprocessorServiceTest {

    private val service = DataPreprocessorService()

    @Test
    fun `should normalize, detect outliers and impute missing values`() {
        val dataset = Dataset(
            columns = listOf("temperature", "status"),
            rows = listOf(
                mapOf("temperature" to "21.0", "status" to "OK"),
                mapOf("temperature" to "23.5", "status" to "OK"),
                mapOf("temperature" to "100.0", "status" to "ALERT"),
                mapOf("temperature" to "", "status" to "ALERT")
            )
        )

        val result = service.preprocess(dataset)

        assertEquals(4, result.report.inputRows)
        assertEquals(4, result.report.outputRows)
        assertEquals(0, result.report.outliersRemoved)
        assertEquals(1, result.report.missingValuesReplaced)
        assertTrue(result.report.normalizedFields.contains("temperature"))
        assertEquals(4, result.cleanedDataset.rows.size)
        assertEquals(0, result.outliersDataset.rows.size)

        val imputedRow = result.cleanedDataset.rows.firstOrNull { row ->
            val meta = row["_meta"] as? Map<*, *>
            val imputation = meta
                ?.get("columns")
                ?.let { it as? Map<*, *> }
                ?.get("temperature")
                ?.let { it as? Map<*, *> }
                ?.get("imputation") as? Map<*, *>
            imputation != null
        }

        val cleanedMeta = result.cleanedDataset.rows.mapNotNull { it["_meta"] as? Map<*, *> }
        val outliersMeta = result.outliersDataset.rows.mapNotNull { it["_meta"] as? Map<*, *> }
        val allMeta = cleanedMeta + outliersMeta
        assertTrue(allMeta.any { meta ->
            val columns = meta["columns"] as? Map<*, *>
            val temperature = columns?.get("temperature") as? Map<*, *>
            val imputation = temperature?.get("imputation") as? Map<*, *>
            imputation?.get("imputed") == true
        })
    }

    @Test
    fun `should impute boolean mode`() {
        val dataset = Dataset(
            columns = listOf("active"),
            rows = listOf(
                mapOf("active" to true),
                mapOf("active" to "false"),
                mapOf("active" to null)
            )
        )

        val result = service.preprocess(dataset)

        assertEquals(3, result.report.inputRows)
        assertEquals(3, result.report.outputRows)
        assertEquals(0, result.report.outliersRemoved)
        assertEquals(1, result.report.missingValuesReplaced)
        assertEquals(listOf("active"), result.report.normalizedFields)
        assertTrue(result.cleanedDataset.rows.all { it["active"] is Boolean })

        val imputedRow = result.cleanedDataset.rows.first { row ->
            row["_meta"] != null
        }
        val columnsMeta = (imputedRow["_meta"] as Map<*, *>)["columns"] as Map<*, *>
        val activeMeta = columnsMeta["active"] as Map<*, *>
        val imputation = activeMeta["imputation"] as Map<*, *>
        assertTrue(imputation["imputed"] == true)
        assertEquals("missing_value_replacement", imputation["reason"])
        assertEquals("mode_boolean", imputation["strategy"])
        assertEquals(true, imputation["value"])
    }
}

