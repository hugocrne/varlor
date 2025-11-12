package com.varlor.backend.common.util

/**
 * Messages d'erreur standardisés pour éviter les fuites d'informations
 * et garantir une cohérence dans les réponses API.
 */
object ErrorMessages {
    const val INVALID_CREDENTIALS = "Identifiants invalides."
    const val USER_NOT_FOUND = "Identifiants invalides." // Même message pour éviter user enumeration
    const val USER_INACTIVE = "Identifiants invalides." // Même message pour éviter user enumeration
    const val INVALID_TOKEN = "Token invalide ou expiré."
    const val TOKEN_EXPIRED = "Token invalide ou expiré."
    const val TOKEN_REVOKED = "Token invalide ou expiré."
    const val UNAUTHORIZED = "Accès non autorisé."
    const val FORBIDDEN = "Accès interdit."
    const val NOT_FOUND = "Ressource introuvable."
    const val CONFLICT = "Conflit : la ressource existe déjà."
    const val VALIDATION_ERROR = "Erreur de validation."
    const val INTERNAL_ERROR = "Une erreur interne s'est produite."
}

