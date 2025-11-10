#pragma once

#include <string>
#include <unordered_map>
#include <variant>
#include <optional>

/**
 * @file DataPoint.hpp
 * @brief Représente une ligne individuelle du jeu de données.
 * 
 * Un DataPoint contient une collection de paires clé/valeur où :
 * - La clé est le nom de la colonne (std::string)
 * - La valeur peut être de plusieurs types (numérique, texte, booléen, null)
 */

namespace varlor::models {

/**
 * @typedef FieldValue
 * @brief Type variant représentant une valeur de champ.
 * 
 * Peut contenir :
 * - double : valeur numérique
 * - std::string : valeur texte
 * - bool : valeur booléenne
 * - std::nullptr_t : valeur null (représentée par nullptr)
 */
using FieldValue = std::variant<double, std::string, bool, std::nullptr_t>;

/**
 * @class DataPoint
 * @brief Représente une ligne individuelle du jeu de données.
 * 
 * Permet un accès rapide aux champs par leur nom via une table de hachage.
 * Chaque champ peut contenir une valeur de type variant (numérique, texte, booléen, null).
 */
class DataPoint {
public:
    /**
     * @brief Constructeur par défaut.
     */
    DataPoint() = default;

    /**
     * @brief Constructeur avec initialisation des champs.
     * @param fields Map initiale des champs (nom de colonne → valeur)
     */
    explicit DataPoint(const std::unordered_map<std::string, FieldValue>& fields)
        : fields_(fields) {}

    /**
     * @brief Constructeur par déplacement.
     * @param fields Map des champs à déplacer
     */
    explicit DataPoint(std::unordered_map<std::string, FieldValue>&& fields)
        : fields_(std::move(fields)) {}

    /**
     * @brief Récupère la valeur d'un champ par son nom.
     * @param fieldName Nom de la colonne
     * @return Option contenant la valeur si le champ existe, std::nullopt sinon
     */
    [[nodiscard]] std::optional<FieldValue> getField(const std::string& fieldName) const {
        auto it = fields_.find(fieldName);
        if (it != fields_.end()) {
            return it->second;
        }
        return std::nullopt;
    }

    /**
     * @brief Définit ou met à jour la valeur d'un champ.
     * @param fieldName Nom de la colonne
     * @param value Valeur à assigner
     */
    void setField(const std::string& fieldName, const FieldValue& value) {
        fields_[fieldName] = value;
    }

    /**
     * @brief Définit ou met à jour la valeur d'un champ (version par déplacement).
     * @param fieldName Nom de la colonne
     * @param value Valeur à déplacer
     */
    void setField(const std::string& fieldName, FieldValue&& value) {
        fields_[fieldName] = std::move(value);
    }

    /**
     * @brief Vérifie si un champ existe.
     * @param fieldName Nom de la colonne
     * @return true si le champ existe, false sinon
     */
    [[nodiscard]] bool hasField(const std::string& fieldName) const {
        return fields_.find(fieldName) != fields_.end();
    }

    /**
     * @brief Supprime un champ.
     * @param fieldName Nom de la colonne à supprimer
     * @return true si le champ a été supprimé, false s'il n'existait pas
     */
    bool removeField(const std::string& fieldName) {
        return fields_.erase(fieldName) > 0;
    }

    /**
     * @brief Récupère le nombre de champs dans ce DataPoint.
     * @return Nombre de champs
     */
    [[nodiscard]] std::size_t size() const {
        return fields_.size();
    }

    /**
     * @brief Vérifie si le DataPoint est vide.
     * @return true si aucun champ n'est présent, false sinon
     */
    [[nodiscard]] bool empty() const {
        return fields_.empty();
    }

    /**
     * @brief Accès direct à la map des champs (lecture seule).
     * @return Référence constante à la map des champs
     */
    [[nodiscard]] const std::unordered_map<std::string, FieldValue>& getFields() const {
        return fields_;
    }

    /**
     * @brief Accès direct à la map des champs (modifiable).
     * @return Référence à la map des champs
     */
    std::unordered_map<std::string, FieldValue>& getFields() {
        return fields_;
    }

private:
    /// Map des champs : nom de colonne → valeur
    std::unordered_map<std::string, FieldValue> fields_;
};

} // namespace varlor::models

