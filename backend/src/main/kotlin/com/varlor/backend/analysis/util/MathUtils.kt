package com.varlor.backend.analysis.util

import kotlin.math.pow
import kotlin.math.sqrt

object MathUtils {
    fun mean(values: List<Double>): Double? = values.takeIf { it.isNotEmpty() }?.average()

    fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }

    fun percentile(values: List<Double>, percentile: Double): Double? {
        if (values.isEmpty()) return null
        require(percentile in 0.0..100.0) { "Le percentile doit être compris entre 0 et 100." }
        val sorted = values.sorted()
        if (percentile == 100.0) return sorted.last()
        val rank = percentile / 100.0 * (sorted.size - 1)
        val lowerIndex = rank.toInt()
        val upperIndex = lowerIndex + 1
        val fractional = rank - lowerIndex
        return if (upperIndex < sorted.size) {
            sorted[lowerIndex] + (sorted[upperIndex] - sorted[lowerIndex]) * fractional
        } else {
            sorted[lowerIndex]
        }
    }

    fun variance(values: List<Double>): Double? {
        if (values.size < 2) return null
        val mean = mean(values) ?: return null
        return values.sumOf { (it - mean).pow(2) } / (values.size - 1)
    }

    fun standardDeviation(values: List<Double>): Double? = variance(values)?.let { sqrt(it) }

    fun correlation(x: List<Double>, y: List<Double>): Double? {
        if (x.size != y.size || x.size < 2) return null
        val meanX = mean(x) ?: return null
        val meanY = mean(y) ?: return null
        val numerator = x.indices.sumOf { (x[it] - meanX) * (y[it] - meanY) }
        val denominator = sqrt(
            x.sumOf { (it - meanX).pow(2) } * y.sumOf { (it - meanY).pow(2) }
        )
        return if (denominator == 0.0) null else numerator / denominator
    }

    fun interquartileRange(values: List<Double>): Double? {
        if (values.size < 4) return null
        val q1 = percentile(values, 25.0) ?: return null
        val q3 = percentile(values, 75.0) ?: return null
        return q3 - q1
    }

    fun medianOfNumbers(values: List<Number>): Double? = median(values.map { it.toDouble() })

    fun percentileOfNumbers(values: List<Number>, percentile: Double): Double? =
        percentile(values.map { it.toDouble() }, percentile)

    fun standardDeviationOfNumbers(values: List<Number>): Double? =
        standardDeviation(values.map { it.toDouble() })

    fun correlationOfNumbers(x: List<Number>, y: List<Number>): Double? =
        correlation(x.map { it.toDouble() }, y.map { it.toDouble() })
}

