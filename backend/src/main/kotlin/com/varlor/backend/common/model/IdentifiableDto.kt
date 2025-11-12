package com.varlor.backend.common.model

import java.io.Serializable

/**
 * Interface pour les DTOs qui ont un identifiant.
 * 
 * Cette interface standardise l'accès à l'identifiant des DTOs de lecture,
 * permettant d'éliminer la duplication de la méthode `getId()` dans les contrôleurs.
 * 
 * @param ID le type de l'identifiant (doit être Serializable)
 * 
 * @example
 * ```
 * data class ClientDto(
 *     override val id: UUID,
 *     // ...
 * ) : IdentifiableDto<UUID>
 * ```
 */
interface IdentifiableDto<ID : Serializable> {
    val id: ID
}

