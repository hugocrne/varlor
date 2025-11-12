package com.varlor.backend.common.extensions

import kotlin.reflect.full.memberProperties
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Vérifie si tous les champs d'un DTO de mise à jour sont null.
 * Utilise la réflexion pour inspecter les propriétés.
 * 
 * @throws ResponseStatusException si tous les champs sont null
 * @return le DTO lui-même pour permettre le chaînage
 * 
 * @example
 * ```
 * override fun validateBeforeUpdate(id: UUID, dto: UpdateClientDto, entity: Client) {
 *     dto.requireAtLeastOneField()
 *     // Validation spécifique...
 * }
 * ```
 */
inline fun <reified T : Any> T.requireAtLeastOneField(): T {
    val fields = T::class.memberProperties
    val hasNonNullField = fields.any { it.get(this) != null }
    if (!hasNonNullField) {
        throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Aucune donnée de mise à jour fournie."
        )
    }
    return this
}

