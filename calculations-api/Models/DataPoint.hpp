#pragma once

#include <string>
#include <unordered_map>
#include <variant>
#include <optional>
#include <memory>
#include <functional>

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
 * @class MetaInfo
 * @brief Contient les informations de traçabilité d'un DataPoint.
 *
 * Cette structure hiérarchique est conçue pour une représentation YAML lisible.
 * Elle permet de stocker des valeurs scalaires (`FieldValue`) ou des sous-sections
 * afin de conserver les détails des transformations appliquées.
 */
class MetaInfo {
public:
    /// Type pour les valeurs feuilles des métadonnées
    using LeafValue = FieldValue;

    /// Type variant pour stocker une feuille ou une sous-section
    using NodeValue = std::variant<LeafValue, std::shared_ptr<MetaInfo>>;

    /**
     * @brief Vérifie la présence d'une clé.
     * @param key Nom de la clé recherchée
     * @return true si la clé existe, false sinon
     */
    [[nodiscard]] bool hasKey(const std::string& key) const;

    /**
     * @brief Récupère une section existante ou la crée si nécessaire.
     * @param key Nom de la section
     * @return Référence vers la section
     */
    MetaInfo& ensureSection(const std::string& key);

    /**
     * @brief Récupère une section en écriture si elle existe.
     * @param key Nom de la section
     * @return Option contenant une référence vers la section
     */
    [[nodiscard]] std::optional<std::reference_wrapper<MetaInfo>> getSection(const std::string& key);

    /**
     * @brief Récupère une section en lecture seule si elle existe.
     * @param key Nom de la section
     * @return Option contenant une référence constante vers la section
     */
    [[nodiscard]] std::optional<std::reference_wrapper<const MetaInfo>> getSection(const std::string& key) const;

    /**
     * @brief Définit une valeur feuille.
     * @param key Nom de la clé
     * @param value Valeur à stocker
     */
    void setLeaf(const std::string& key, const LeafValue& value);

    /**
     * @brief Définit une valeur feuille (par déplacement).
     * @param key Nom de la clé
     * @param value Valeur à stocker (move)
     */
    void setLeaf(const std::string& key, LeafValue&& value);

    /**
     * @brief Lit une valeur feuille si elle existe.
     * @param key Nom de la clé
     * @return Option contenant la valeur si elle est présente et de type feuille
     */
    [[nodiscard]] std::optional<LeafValue> getLeaf(const std::string& key) const;

    /**
     * @brief Expose la map sous-jacente.
     * @return Référence constante vers la map
     */
    [[nodiscard]] const std::unordered_map<std::string, NodeValue>& entries() const noexcept {
        return entries_;
    }

    /**
     * @brief Efface toutes les métadonnées.
     */
    void clear() noexcept {
        entries_.clear();
    }

private:
    MetaInfo& emplaceSection(const std::string& key);

    std::unordered_map<std::string, NodeValue> entries_;
};

inline bool MetaInfo::hasKey(const std::string& key) const {
    return entries_.find(key) != entries_.end();
}

inline MetaInfo& MetaInfo::ensureSection(const std::string& key) {
    return emplaceSection(key);
}

inline std::optional<std::reference_wrapper<MetaInfo>> MetaInfo::getSection(const std::string& key) {
    auto it = entries_.find(key);
    if (it == entries_.end()) {
        return std::nullopt;
    }
    if (auto section = std::get_if<std::shared_ptr<MetaInfo>>(&it->second); section != nullptr && *section != nullptr) {
        return std::optional<std::reference_wrapper<MetaInfo>>{*(*section)};
    }
    return std::nullopt;
}

inline std::optional<std::reference_wrapper<const MetaInfo>> MetaInfo::getSection(const std::string& key) const {
    auto it = entries_.find(key);
    if (it == entries_.end()) {
        return std::nullopt;
    }
    if (auto section = std::get_if<std::shared_ptr<MetaInfo>>(&it->second); section != nullptr && *section != nullptr) {
        return std::optional<std::reference_wrapper<const MetaInfo>>{*(*section)};
    }
    return std::nullopt;
}

inline void MetaInfo::setLeaf(const std::string& key, const LeafValue& value) {
    entries_[key] = value;
}

inline void MetaInfo::setLeaf(const std::string& key, LeafValue&& value) {
    entries_[key] = std::move(value);
}

inline std::optional<MetaInfo::LeafValue> MetaInfo::getLeaf(const std::string& key) const {
    auto it = entries_.find(key);
    if (it == entries_.end()) {
        return std::nullopt;
    }
    if (auto leaf = std::get_if<LeafValue>(&it->second); leaf != nullptr) {
        return *leaf;
    }
    return std::nullopt;
}

inline MetaInfo& MetaInfo::emplaceSection(const std::string& key) {
    auto it = entries_.find(key);
    if (it != entries_.end()) {
        if (auto section = std::get_if<std::shared_ptr<MetaInfo>>(&it->second); section != nullptr && *section != nullptr) {
            return *(*section);
        }
    }
    auto& slot = entries_[key];
    auto section = std::make_shared<MetaInfo>();
    slot = section;
    return *section;
}

/**
 * @class DataPoint
 * @brief Représente une ligne individuelle du jeu de données.
 * 
 * Permet un accès rapide aux champs par leur nom via une table de hachage.
 * Chaque champ peut contenir une valeur de type variant (numérique, texte, booléen, null).
 * Un conteneur MetaInfo associé permet de tracer les opérations (_meta).
 * 
 * @note Complexité temporelle :
 *       - getField(), hasField(), setField() : O(1) en moyenne
 *       - removeField() : O(1) en moyenne
 * 
 * @note Les valeurs sont stockées dans un std::variant, permettant
 *       un stockage type-safe sans overhead de pointeurs.
 * 
 * @note Pour vérifier l'existence d'un champ avant accès, utiliser
 *       hasField() ou vérifier que getField() retourne une valeur.
 * 
 * @see FieldValue pour les types de valeurs supportés
 * @see Dataset pour voir comment les DataPoint sont organisés
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
     * 
     * @note Utiliser std::get<T>() pour extraire la valeur du variant :
     *       @code
     *       auto value = point.getField("age");
     *       if (value.has_value() && std::holds_alternative<double>(value.value())) {
     *           double age = std::get<double>(value.value());
     *       }
     *       @endcode
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

    /**
     * @brief Accède aux informations de traçabilité (_meta).
     * @return Référence modifiable vers MetaInfo
     */
    MetaInfo& getMeta() noexcept {
        return metaInfo_;
    }

    /**
     * @brief Accède aux informations de traçabilité (_meta) en lecture seule.
     * @return Référence constante vers MetaInfo
     */
    [[nodiscard]] const MetaInfo& getMeta() const noexcept {
        return metaInfo_;
    }

    /**
     * @brief Réinitialise entièrement les métadonnées de ce point.
     */
    void clearMeta() {
        metaInfo_.clear();
    }

private:
    /// Map des champs : nom de colonne → valeur
    std::unordered_map<std::string, FieldValue> fields_;

    /// Métadonnées hiérarchiques associées à ce point
    MetaInfo metaInfo_;
};

} // namespace varlor::models

