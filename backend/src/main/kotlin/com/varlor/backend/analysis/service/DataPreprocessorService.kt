package com.varlor.backend.analysis.service

import com.varlor.backend.analysis.model.DataPoint
import com.varlor.backend.analysis.model.Dataset
import com.varlor.backend.analysis.model.PreprocessingReport
import com.varlor.backend.analysis.model.PreprocessingResult
import com.varlor.backend.analysis.util.MathUtils
import com.varlor.backend.common.util.toDoubleOrNull
import org.springframework.stereotype.Service

@Service
class DataPreprocessorService {

    fun preprocess(dataset: Dataset): PreprocessingResult {
        val columnTypes = inferColumnTypes(dataset)
        val normalizedFields = mutableSetOf<String>()
        val normalizedRows = dataset.rows.map { normalizeRow(dataset.columns, it, columnTypes, normalizedFields) }
        val outlierIndexes = detectOutliers(dataset.columns, columnTypes, normalizedRows)
        val mutableRows = normalizedRows.map { it.toMutableMap() }
        val missingValuesCount = imputeMissingValues(dataset.columns, columnTypes, mutableRows)

        outlierIndexes.forEach { index ->
            mutableRows.getOrNull(index)?.let { annotateOutlier(it) }
        }

        val outlierSet = outlierIndexes
        val cleanedRows = mutableListOf<Map<String, Any?>>()
        val outliersRows = mutableListOf<Map<String, Any?>>()
        mutableRows.forEachIndexed { index, row ->
            if (outlierSet.contains(index)) {
                outliersRows += row
            } else {
                cleanedRows += row
            }
        }

        val report = PreprocessingReport(
            inputRows = dataset.rowCount(),
            outputRows = cleanedRows.size,
            outliersRemoved = outlierSet.size,
            missingValuesReplaced = missingValuesCount,
            normalizedFields = normalizedFields.sorted()
        )

        return PreprocessingResult(
            cleanedDataset = Dataset(dataset.columns, cleanedRows),
            outliersDataset = Dataset(dataset.columns, outliersRows),
            report = report
        )
    }

    private fun normalizeRow(
        columns: List<String>,
        row: DataPoint,
        columnTypes: Map<String, FieldType>,
        normalizedFields: MutableSet<String>
    ): MutableMap<String, Any?> {
        val normalized = mutableMapOf<String, Any?>()
        for (column in columns) {
            val type = columnTypes[column] ?: FieldType.TEXT
            val value = row[column]
            val normalizedValue = when (type) {
                FieldType.NUMERIC -> normalizeNumeric(value)
                FieldType.BOOLEAN -> normalizeBoolean(value)
                FieldType.TEXT -> normalizeText(value)
            }
            if (normalizedValue != value && (normalizedValue != null || value != null)) {
                normalizedFields.add(column)
            }
            normalized[column] = normalizedValue
        }
        return normalized
    }

    private fun normalizeNumeric(value: Any?): Double? = value.toDoubleOrNull()

    private fun normalizeBoolean(value: Any?): Boolean? = when (value) {
        null -> null
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.trim().takeIf { it.isNotEmpty() }?.let {
            when (it.lowercase()) {
                "true", "1", "yes", "y", "vrai" -> true
                "false", "0", "no", "n", "faux" -> false
                else -> null
            }
        }
        else -> null
    }

    private fun normalizeText(value: Any?): String? = when (value) {
        null -> null
        is String -> value.trim().ifEmpty { null }
        else -> value.toString()
    }

    private fun inferColumnTypes(dataset: Dataset): Map<String, FieldType> {
        return dataset.columns.associateWith { column ->
            val values = dataset.rows.mapNotNull { it[column] }
            when {
                values.isEmpty() -> FieldType.TEXT
                values.all { it.isBooleanLike() } -> FieldType.BOOLEAN
                values.all { it.isNumericLike() } -> FieldType.NUMERIC
                else -> FieldType.TEXT
            }
        }
    }

    private fun Any?.isNumericLike(): Boolean = when (this) {
        null -> false
        is Number -> true
        is String -> this.trim().toDoubleOrNull() != null
        else -> false
    }

    private fun Any?.isBooleanLike(): Boolean = when (this) {
        null -> false
        is Boolean -> true
        is Number -> this.toInt() in 0..1
        is String -> this.trim().lowercase() in setOf("true", "false", "1", "0", "yes", "no", "y", "n", "vrai", "faux")
        else -> false
    }

    private fun detectOutliers(
        columns: List<String>,
        columnTypes: Map<String, FieldType>,
        rows: List<Map<String, Any?>>
    ): Set<Int> {
        val outliers = mutableSetOf<Int>()
        for (column in columns) {
            if (columnTypes[column] != FieldType.NUMERIC) continue
            val values = rows.mapNotNull { (it[column] as? Number)?.toDouble() }
            if (values.size < 4) continue

            val q1 = MathUtils.percentile(values, 25.0) ?: continue
            val q3 = MathUtils.percentile(values, 75.0) ?: continue
            val iqr = q3 - q1
            if (iqr == 0.0) continue
            val lowerBound = q1 - 1.5 * iqr
            val upperBound = q3 + 1.5 * iqr

            rows.forEachIndexed { index, row ->
                val value = (row[column] as? Number)?.toDouble() ?: return@forEachIndexed
                if (value < lowerBound || value > upperBound) {
                    outliers.add(index)
                }
            }
        }
        return outliers
    }

    private fun imputeMissingValues(
        columns: List<String>,
        columnTypes: Map<String, FieldType>,
        rows: List<MutableMap<String, Any?>>
    ): Int {
        var replacements = 0

        for (column in columns) {
            when (columnTypes[column]) {
                FieldType.NUMERIC -> {
                    replacements += imputeNumericColumn(rows, column)
                }

                FieldType.BOOLEAN -> {
                    replacements += imputeBooleanColumn(rows, column)
                }

                FieldType.TEXT -> {
                    replacements += imputeTextColumn(rows, column)
                }

                null -> continue
            }
        }

        return replacements
    }

    private fun imputeNumericColumn(
        rows: List<MutableMap<String, Any?>>,
        column: String
    ): Int {
        val values = rows.mapNotNull { (it[column] as? Number)?.toDouble() }
        val median = MathUtils.median(values) ?: return 0
        var replacements = 0
        rows.forEach { row ->
            if (!row.containsKey(column) || row[column] == null) {
                row[column] = median
                annotateImputation(row, column, "median", median)
                replacements++
            }
        }
        return replacements
    }

    private fun imputeBooleanColumn(
        rows: List<MutableMap<String, Any?>>,
        column: String
    ): Int {
        val values = rows.mapNotNull { it[column] as? Boolean }
        if (values.isEmpty()) return 0
        val trueCount = values.count { it }
        val falseCount = values.size - trueCount
        val imputedValue = trueCount >= falseCount
        var replacements = 0
        rows.forEach { row ->
            if (!row.containsKey(column) || row[column] == null) {
                row[column] = imputedValue
                annotateImputation(row, column, "mode_boolean", imputedValue)
                replacements++
            }
        }
        return replacements
    }

    private fun imputeTextColumn(
        rows: List<MutableMap<String, Any?>>,
        column: String
    ): Int {
        val values = rows.mapNotNull { it[column] as? String }
        val modeValue = mode(values) ?: return 0
        var replacements = 0
        rows.forEach { row ->
            if (!row.containsKey(column) || row[column] == null) {
                row[column] = modeValue
                annotateImputation(row, column, "mode_text", modeValue)
                replacements++
            }
        }
        return replacements
    }

    private fun annotateOutlier(row: MutableMap<String, Any?>) {
        val meta = ensureMeta(row)
        val statusSection = ensureNestedMap(meta, STATUS_SECTION)
        statusSection["outlier"] = true
        statusSection["reason"] = OUTLIER_REASON
        statusSection["method"] = "iqr"
    }

    private fun annotateImputation(
        row: MutableMap<String, Any?>,
        columnName: String,
        strategy: String,
        imputedValue: Any?
    ) {
        val meta = ensureMeta(row)
        val columnsSection = ensureNestedMap(meta, COLUMNS_SECTION)
        val columnSection = ensureNestedMap(columnsSection, columnName)
        val imputationSection = ensureNestedMap(columnSection, IMPUTATION_SECTION)
        imputationSection["imputed"] = true
        imputationSection["reason"] = IMPUTATION_REASON
        imputationSection["strategy"] = strategy
        imputationSection["value"] = imputedValue
    }

    @Suppress("UNCHECKED_CAST")
    private fun ensureMeta(row: MutableMap<String, Any?>): MutableMap<String, Any?> {
        val existing = row[META_KEY]
        return when (existing) {
            is MutableMap<*, *> -> existing as? MutableMap<String, Any?>
                ?: (existing.entries.associate { it.key.toString() to it.value }.toMutableMap().also { row[META_KEY] = it })
            is Map<*, *> -> existing.entries.associate { it.key.toString() to it.value }.toMutableMap()
                .also { row[META_KEY] = it }
            null -> mutableMapOf<String, Any?>().also { row[META_KEY] = it }
            else -> mutableMapOf<String, Any?>().also { row[META_KEY] = it }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun ensureNestedMap(
        parent: MutableMap<String, Any?>,
        key: String
    ): MutableMap<String, Any?> {
        val existing = parent[key]
        return when (existing) {
            is MutableMap<*, *> -> existing as? MutableMap<String, Any?>
                ?: (existing.entries.associate { it.key.toString() to it.value }.toMutableMap().also { parent[key] = it })
            is Map<*, *> -> existing.entries.associate { it.key.toString() to it.value }.toMutableMap()
                .also { parent[key] = it }
            null -> mutableMapOf<String, Any?>().also { parent[key] = it }
            else -> mutableMapOf<String, Any?>().also { parent[key] = it }
        }
    }

    private fun <T> mode(values: List<T>): T? {
        if (values.isEmpty()) return null
        val counts = values.groupingBy { it }.eachCount()
        val maxCount = counts.values.maxOrNull() ?: return null
        return counts.entries.firstOrNull { it.value == maxCount }?.key
    }

    private companion object {
        private const val META_KEY = "_meta"
        private const val STATUS_SECTION = "status"
        private const val COLUMNS_SECTION = "columns"
        private const val IMPUTATION_SECTION = "imputation"
        private const val IMPUTATION_REASON = "missing_value_replacement"
        private const val OUTLIER_REASON = "iqr_detection"
    }

    private enum class FieldType {
        NUMERIC, BOOLEAN, TEXT
    }
}

