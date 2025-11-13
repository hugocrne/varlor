package com.varlor.backend.analysis.service

import com.ezylang.evalex.Expression
import com.varlor.backend.analysis.model.Dataset
import com.varlor.backend.analysis.model.OperationDefinition
import com.varlor.backend.analysis.model.OperationResult
import com.varlor.backend.analysis.model.OperationStatus
import com.varlor.backend.analysis.util.MathUtils
import com.varlor.backend.common.util.toBigDecimalOrThrow
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

/**
 * Service d'exécution d'indicateurs et d'expressions sur des datasets.
 *
 * Exécute une liste d'opérations sur un dataset. Chaque opération peut être :
 * - Une fonction builtin (mean, median, variance, stddev, min, max, correlation, percentile)
 * - Une expression EvalEx utilisant les colonnes du dataset
 *
 * Les opérations sont exécutées dans l'ordre et chaque résultat inclut :
 * - L'expression ou l'alias utilisé
 * - Le résultat (scalaire ou liste)
 * - Le statut (SUCCESS ou ERROR)
 * - Un message d'erreur si applicable
 *
 * Fonctions builtin supportées :
 * - `mean(column)` : Moyenne
 * - `median(column)` : Médiane
 * - `variance(column)` : Variance
 * - `stddev(column)` : Écart-type
 * - `min(column)` / `max(column)` : Minimum/Maximum
 * - `correlation(columnX, columnY)` : Corrélation de Pearson
 * - `percentile(column, p)` : Percentile (p entre 0 et 100)
 *
 * Expressions EvalEx :
 * - Supportent les opérations mathématiques standard
 * - Peuvent référencer les colonnes du dataset par leur nom
 * - Exemple : `(temperature + humidity) / 2`
 *
 * @see OperationDefinition
 * @see OperationResult
 */
@Service
class IndicatorEngineService {

    /**
     * Exécute une liste d'opérations sur un dataset.
     *
     * @param operations Liste des opérations à exécuter
     * @param dataset Dataset sur lequel exécuter les opérations
     * @return Liste des résultats d'exécution, dans le même ordre que les opérations
     */
    fun execute(operations: List<OperationDefinition>, dataset: Dataset): List<OperationResult> {
        return operations.map { definition ->
            val timestamp = Instant.now()
            val alias = definition.alias?.takeIf { it.isNotBlank() }
            val displayExpression = alias ?: definition.expr
            try {
                val result = executeOperation(definition, dataset)
                OperationResult(
                    expr = displayExpression,
                    result = result,
                    status = OperationStatus.SUCCESS,
                    executedAt = timestamp,
                    alias = alias
                )
            } catch (ex: Exception) {
                OperationResult(
                    expr = displayExpression,
                    result = null,
                    status = OperationStatus.ERROR,
                    executedAt = timestamp,
                    alias = alias,
                    errorMessage = ex.message
                )
            }
        }
    }

    private fun executeOperation(definition: OperationDefinition, dataset: Dataset): Any? {
        val builtin = BuiltinOperations.detectCall(definition.expr)
        return if (builtin != null) {
            BuiltinOperations.execute(dataset, definition, builtin)
        } else {
            evaluateExpression(definition, dataset)
        }
    }

    private fun evaluateExpression(definition: OperationDefinition, dataset: Dataset): Any? {
        val prepared = ExpressionPreparator.prepare(definition, dataset)
        val expressionText = prepared.expression

        return if (prepared.columns.isEmpty()) {
            evaluateScalarExpression(expressionText)
        } else {
            evaluateRowExpression(expressionText, prepared.columns, dataset)
        }
    }

    private fun evaluateScalarExpression(expression: String): Double =
        try {
            Expression(expression).evaluate().numberValue.toDouble()
        } catch (ex: Exception) {
            throw IllegalArgumentException("Expression invalide : ${ex.message}", ex)
        }

    private fun evaluateRowExpression(
        expression: String,
        columns: Set<String>,
        dataset: Dataset
    ): List<Double> {
        val results = mutableListOf<Double>()
        try {
            for (row in dataset.rows) {
                val evaluator = Expression(expression)
                for (column in columns) {
                    val rawValue = row[column]
                    val value = rawValue?.let { toBigDecimal(column, it) }
                        ?: throw IllegalArgumentException("Valeur manquante pour la colonne \"$column\".")
                    evaluator.with(column, value)
                }
                results += evaluator.evaluate().numberValue.toDouble()
            }
        } catch (ex: Exception) {
            throw IllegalArgumentException("Expression invalide : ${ex.message}", ex)
        }
        return results
    }

    private fun toBigDecimal(column: String, value: Any?): BigDecimal {
        return value.toBigDecimalOrThrow(column)
    }

    private object BuiltinOperations {
        private val builtinPattern =
            Regex("""^\s*([A-Za-z_][A-Za-z0-9_]*)\s*\((.*)\)\s*$""")
        private val supportedNames = setOf(
            "mean",
            "median",
            "variance",
            "stddev",
            "correlation",
            "min",
            "max",
            "percentile"
        )

        fun detectCall(expr: String): BuiltinCall? {
            val match = builtinPattern.matchEntire(expr) ?: return null
            val name = match.groupValues[1]
            if (name !in supportedNames) return null
            val args = match.groupValues[2].takeIf { it.isNotBlank() }
                ?.splitArguments()
                ?: emptyList()
            return BuiltinCall(name, args)
        }

        fun execute(dataset: Dataset, definition: OperationDefinition, call: BuiltinCall): Double {
            return when (call.name) {
                "mean" -> {
                    require(call.args.size == 1) { "mean attend exactement une colonne." }
                    mean(dataset, call.args.first())
                }

                "median" -> {
                    require(call.args.size == 1) { "median attend exactement une colonne." }
                    median(dataset, call.args.first())
                }

                "variance" -> {
                    require(call.args.size == 1) { "variance attend exactement une colonne." }
                    variance(dataset, call.args.first())
                }

                "stddev" -> {
                    require(call.args.size == 1) { "stddev attend exactement une colonne." }
                    stddev(dataset, call.args.first())
                }

                "min" -> {
                    require(call.args.size == 1) { "min attend exactement une colonne." }
                    min(dataset, call.args.first())
                }

                "max" -> {
                    require(call.args.size == 1) { "max attend exactement une colonne." }
                    max(dataset, call.args.first())
                }

                "correlation" -> {
                    require(call.args.size == 2) { "correlation attend deux colonnes." }
                    correlation(dataset, call.args[0], call.args[1])
                }

                "percentile" -> {
                    require(call.args.isNotEmpty()) { "percentile attend une colonne." }
                    val column = call.args.first()
                    val percentile = when {
                        call.args.size >= 2 -> call.args[1].toDoubleStrict("percentile")
                        definition.params["percentile"] != null ->
                            definition.params.getValue("percentile").toDoubleStrict("percentile")
                        definition.params["p"] != null ->
                            definition.params.getValue("p").toDoubleStrict("percentile")
                        else -> throw IllegalArgumentException(
                            "percentile nécessite un second argument ou un paramètre `percentile`."
                        )
                    }
                    percentile(dataset, column, percentile)
                }

                else -> throw IllegalArgumentException("Fonction builtin inconnue : ${call.name}")
            }
        }

        private fun String.splitArguments(): List<String> {
            val tokens = mutableListOf<String>()
            var depth = 0
            var start = 0
            for (i in indices) {
                when (this[i]) {
                    '(' -> depth++
                    ')' -> depth--
                    ',' -> if (depth == 0) {
                        tokens += substring(start, i).trim()
                        start = i + 1
                    }
                }
            }
            tokens += substring(start).trim()
            return tokens.filter { it.isNotEmpty() }
        }

        fun aggregateValue(function: String, dataset: Dataset, column: String): Double {
            return when (function) {
                "mean" -> mean(dataset, column)
                "median" -> median(dataset, column)
                "variance" -> variance(dataset, column)
                "stddev" -> stddev(dataset, column)
                "min" -> min(dataset, column)
                "max" -> max(dataset, column)
                else -> throw IllegalArgumentException("Fonction inconnue : $function")
            }
        }

        fun mean(dataset: Dataset, column: String): Double =
            numericColumn(dataset, column).average()

        fun median(dataset: Dataset, column: String): Double =
            MathUtils.median(numericColumn(dataset, column))
                ?: throw IllegalArgumentException("Impossible de calculer la médiane pour $column.")

        fun variance(dataset: Dataset, column: String): Double =
            MathUtils.variance(numericColumn(dataset, column))
                ?: throw IllegalArgumentException("Impossible de calculer la variance pour $column.")

        fun stddev(dataset: Dataset, column: String): Double =
            MathUtils.standardDeviation(numericColumn(dataset, column))
                ?: throw IllegalArgumentException("Impossible de calculer l'écart-type pour $column.")

        fun min(dataset: Dataset, column: String): Double =
            numericColumn(dataset, column).minOrNull()
                ?: throw IllegalArgumentException("Impossible de calculer le minimum pour $column.")

        fun max(dataset: Dataset, column: String): Double =
            numericColumn(dataset, column).maxOrNull()
                ?: throw IllegalArgumentException("Impossible de calculer le maximum pour $column.")

        fun correlation(dataset: Dataset, columnX: String, columnY: String): Double =
            MathUtils.correlation(
                numericColumn(dataset, columnX),
                numericColumn(dataset, columnY)
            )
                ?: throw IllegalArgumentException("Impossible de calculer la corrélation entre $columnX et $columnY.")

        fun percentile(dataset: Dataset, column: String, percentile: Double): Double =
            MathUtils.percentile(numericColumn(dataset, column), percentile)
                ?: throw IllegalArgumentException("Impossible de calculer le percentile $percentile pour $column.")

        private fun numericColumn(dataset: Dataset, column: String): List<Double> {
            val columnIndex = dataset.columns.indexOf(column)
            if (columnIndex < 0) {
                throw IllegalArgumentException("Colonne inconnue : $column")
            }
            val values = dataset.rows.mapIndexedNotNull { rowIndex, row ->
                val value = row[column]
                    ?: throw IllegalArgumentException("Valeur manquante dans la colonne \"$column\" (ligne $rowIndex).")
                when (value) {
                    is Number -> value.toDouble()
                    is String -> value.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                        ?: throw IllegalArgumentException(
                            "Valeur non numérique \"$value\" dans la colonne \"$column\" (ligne $rowIndex)."
                        )
                    else -> throw IllegalArgumentException(
                        "Valeur non numérique dans la colonne \"$column\" (ligne $rowIndex)."
                    )
                }
            }
            if (values.isEmpty()) {
                throw IllegalArgumentException("La colonne \"$column\" ne contient aucune valeur numérique.")
            }
            return values
        }

        private fun String.toDoubleStrict(context: String): Double = toDoubleOrNull()
            ?: throw IllegalArgumentException("Impossible d'interpréter \"$this\" comme nombre pour $context.")
    }

    private object ExpressionPreparator {
        private val unaryAggregateRegex =
            Regex("""\b(mean|median|variance|stddev|min|max)\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*\)""")
        private val correlationRegex =
            Regex("""\bcorrelation\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*,\s*([A-Za-z_][A-Za-z0-9_]*)\s*\)""")
        private val percentileRegex =
            Regex("""\bpercentile\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*(?:,\s*([0-9]*\.?[0-9]+)\s*)?\)""")

        fun prepare(definition: OperationDefinition, dataset: Dataset): PreparedExpression {
            var expression = definition.expr

            expression = unaryAggregateRegex.replace(expression) { match ->
                val column = match.groupValues[2]
                BuiltinOperations.aggregateValue(match.groupValues[1], dataset, column).toString()
            }

            expression = correlationRegex.replace(expression) { match ->
                val columnX = match.groupValues[1]
                val columnY = match.groupValues[2]
                BuiltinOperations.correlation(dataset, columnX, columnY).toString()
            }

            expression = percentileRegex.replace(expression) { match ->
                val column = match.groupValues[1]
                val percentileArg = match.groupValues[2]
                val percentileValue = percentileArg.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                    ?: definition.params["percentile"]?.toDoubleOrNull()
                    ?: definition.params["p"]?.toDoubleOrNull()
                    ?: throw IllegalArgumentException(
                        "percentile nécessite un second argument ou un paramètre `percentile`."
                    )
                BuiltinOperations.percentile(dataset, column, percentileValue).toString()
            }

            val usedColumns = collectUsedColumns(expression, dataset.columns.toSet())

            return PreparedExpression(expression, usedColumns)
        }

        private fun collectUsedColumns(expression: String, columns: Set<String>): Set<String> {
            val used = mutableSetOf<String>()
            for (column in columns) {
                val pattern = Regex("""\b${Regex.escape(column)}\b""")
                if (pattern.containsMatchIn(expression)) {
                    used += column
                }
            }
            return used
        }
    }

    private data class BuiltinCall(
        val name: String,
        val args: List<String>
    )

    private data class PreparedExpression(
        val expression: String,
        val columns: Set<String>
    )
}

