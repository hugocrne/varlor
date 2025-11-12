package com.varlor.backend.common.util

import java.math.BigDecimal
import kotlin.text.toDoubleOrNull as stringToDoubleOrNull

/**
 * Convertit une valeur en Double, en gérant différents types d'entrée.
 * 
 * Cette fonction utilitaire centralise la logique de conversion numérique
 * en gérant différents types d'entrée : Number, String, Boolean, et null.
 * 
 * @return la valeur convertie en Double, ou null si la conversion n'est pas possible
 * 
 * @example
 * ```
 * val value1: Any? = 42
 * val double1 = value1.toDoubleOrNull() // 42.0
 * 
 * val value2: Any? = "3.14"
 * val double2 = value2.toDoubleOrNull() // 3.14
 * 
 * val value3: Any? = true
 * val double3 = value3.toDoubleOrNull() // 1.0
 * ```
 */
fun Any?.toDoubleOrNull(): Double? = when (this) {
    null -> null
    is Number -> this.toDouble()
    is String -> this.trim().takeIf { it.isNotEmpty() }?.stringToDoubleOrNull()
    is Boolean -> if (this) 1.0 else 0.0
    else -> null
}

/**
 * Convertit une valeur en BigDecimal, en lançant une exception si la conversion échoue.
 * 
 * Cette fonction utilitaire centralise la logique de conversion vers BigDecimal
 * en gérant différents types d'entrée et en fournissant des messages d'erreur
 * descriptifs en cas d'échec.
 * 
 * @param columnName le nom de la colonne pour le message d'erreur
 * @return la valeur convertie en BigDecimal
 * @throws IllegalArgumentException si la valeur est null ou ne peut pas être convertie
 * 
 * @example
 * ```
 * val value1: Any? = 42
 * val bigDecimal1 = value1.toBigDecimalOrThrow("age") // BigDecimal(42.0)
 * 
 * val value2: Any? = "3.14"
 * val bigDecimal2 = value2.toBigDecimalOrThrow("price") // BigDecimal(3.14)
 * 
 * val value3: Any? = null
 * val bigDecimal3 = value3.toBigDecimalOrThrow("amount") // IllegalArgumentException
 * ```
 */
fun Any?.toBigDecimalOrThrow(columnName: String): BigDecimal {
    return when (val value = this) {
        null -> throw IllegalArgumentException("Valeur manquante pour la colonne \"$columnName\".")
        is Number -> BigDecimal.valueOf(value.toDouble())
        is String -> value.trim().takeIf { it.isNotEmpty() }?.stringToDoubleOrNull()
            ?.let { BigDecimal.valueOf(it) }
            ?: throw IllegalArgumentException("Valeur non numérique dans la colonne \"$columnName\" : $value")
        else -> throw IllegalArgumentException("Valeur non numérique dans la colonne \"$columnName\" : $value")
    }
}

