#pragma once

/**
 * @file FieldType.hpp
 * @brief Définit les types de champs supportés par le moteur de pré-analyse.
 */

namespace varlor::models {

/**
 * @enum FieldType
 * @brief Énumération des types de données supportés pour les champs.
 * 
 * Utilisée pour identifier automatiquement le type de chaque colonne
 * lors de la phase de détection de schéma.
 */
enum class FieldType {
    /// Type numérique (entier ou décimal)
    Numeric,
    
    /// Type texte (chaîne de caractères)
    Text,
    
    /// Type booléen (vrai/faux)
    Boolean,
    
    /// Type inconnu ou non détecté
    Unknown
};

} // namespace varlor::models

