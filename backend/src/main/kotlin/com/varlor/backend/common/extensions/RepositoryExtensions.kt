package com.varlor.backend.common.extensions

import com.varlor.backend.common.repository.SoftDeleteRepositoryMethods
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.io.Serializable

/**
 * Vérifie qu'une entité existe et n'est pas supprimée (soft-delete).
 * 
 * Cette extension centralise la logique de vérification d'existence pour les entités
 * qui supportent le soft-delete, en lançant une exception si l'entité n'existe pas
 * ou a été supprimée.
 * 
 * @param id l'identifiant de l'entité à vérifier
 * @param entityName le nom de l'entité pour le message d'erreur (par défaut "Entité")
 * @throws ResponseStatusException si l'entité n'existe pas ou a été supprimée
 * 
 * @example
 * ```
 * override fun validateBeforeCreate(dto: CreateUserDto) {
 *     (clientRepository as SoftDeleteRepositoryMethods<Client, UUID>)
 *         .requireExists(dto.clientId, "Client")
 *     // ...
 * }
 * ```
 */
fun <ID : Serializable> SoftDeleteRepositoryMethods<*, ID>.requireExists(
    id: ID,
    entityName: String = "Entité"
) {
    if (!existsByIdAndDeletedAtIsNull(id)) {
        throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "$entityName associé introuvable ou supprimé."
        )
    }
}

