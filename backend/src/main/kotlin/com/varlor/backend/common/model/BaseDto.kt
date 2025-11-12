package com.varlor.backend.common.model

import java.io.Serializable
import java.time.Instant

/**
 * Interface de base pour les DTOs de lecture avec champs communs.
 * 
 * Cette interface standardise les champs communs présents dans tous les DTOs
 * de lecture : l'identifiant, la date de création et la date de mise à jour.
 * 
 * @param ID le type de l'identifiant (doit être Serializable)
 * 
 * @example
 * ```
 * data class UserPreferenceDto(
 *     override val id: UUID,
 *     // ...
 *     override val createdAt: Instant?,
 *     override val updatedAt: Instant?
 * ) : BaseDto<UUID>
 * ```
 */
interface BaseDto<ID : Serializable> : IdentifiableDto<ID> {
    val createdAt: Instant?
    val updatedAt: Instant?
}

/**
 * Interface pour les DTOs soft-deletable.
 * 
 * Cette interface étend [BaseDto] en ajoutant le champ `deletedAt` pour
 * les entités qui supportent le soft-delete.
 * 
 * @param ID le type de l'identifiant (doit être Serializable)
 * 
 * @example
 * ```
 * data class ClientDto(
 *     override val id: UUID,
 *     // ...
 *     override val createdAt: Instant?,
 *     override val updatedAt: Instant?,
 *     override val deletedAt: Instant?
 * ) : SoftDeletableDto<UUID>
 * ```
 */
interface SoftDeletableDto<ID : Serializable> : BaseDto<ID> {
    val deletedAt: Instant?
}

