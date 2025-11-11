#pragma once

#include "DataPoint.hpp"
#include <vector>
#include <string>
#include <algorithm>
#include <iterator>

/**
 * @file Dataset.hpp
 * @brief Représente l'ensemble des données à traiter.
 * 
 * Un Dataset contient :
 * - Une liste de DataPoint (chaque ligne du jeu de données)
 * - La liste des noms de colonnes détectées ou déclarées
 * 
 * Conçu pour être parcouru, filtré et modifié par la classe DataPreprocessor.
 */

namespace varlor::models {

/**
 * @class Dataset
 * @brief Représente l'ensemble complet des données à analyser.
 * 
 * Permet de stocker et manipuler un jeu de données structuré,
 * avec accès aux colonnes et aux lignes individuelles.
 * 
 * @note Complexité temporelle :
 *       - addDataPoint() : O(1) amorti
 *       - getDataPoint() : O(1)
 *       - removeDataPoint() : O(n) où n est le nombre de lignes
 *       - Itération : O(n) pour n lignes
 * 
 * @note Le Dataset est conçu pour être modifié par le DataPreprocessor.
 *       Les noms de colonnes peuvent être définis avant ou après l'ajout
 *       des données, mais il est recommandé de les définir en premier.
 * 
 * @note Les itérateurs permettent l'utilisation de range-based for loops :
 *       @code
 *       for (const auto& point : dataset) {
 *           // Traiter chaque ligne
 *       }
 *       @endcode
 * 
 * @see DataPoint pour la structure d'une ligne
 * @see PreprocessingReport pour tracer les modifications
 */
class Dataset {
public:
    /**
     * @brief Constructeur par défaut.
     */
    Dataset() = default;

    /**
     * @brief Constructeur avec initialisation des colonnes.
     * @param columnNames Liste des noms de colonnes
     */
    explicit Dataset(const std::vector<std::string>& columnNames)
        : columnNames_(columnNames) {}

    /**
     * @brief Constructeur avec initialisation des colonnes (par déplacement).
     * @param columnNames Liste des noms de colonnes à déplacer
     */
    explicit Dataset(std::vector<std::string>&& columnNames)
        : columnNames_(std::move(columnNames)) {}

    /**
     * @brief Ajoute un DataPoint (ligne) au dataset.
     * @param dataPoint Point de données à ajouter
     */
    void addDataPoint(const DataPoint& dataPoint) {
        dataPoints_.push_back(dataPoint);
    }

    /**
     * @brief Ajoute un DataPoint (ligne) au dataset (par déplacement).
     * @param dataPoint Point de données à déplacer
     */
    void addDataPoint(DataPoint&& dataPoint) {
        dataPoints_.push_back(std::move(dataPoint));
    }

    /**
     * @brief Récupère un DataPoint par son index.
     * @param index Index de la ligne (0-based)
     * @return Référence au DataPoint à l'index donné
     * @throws std::out_of_range si l'index est invalide
     */
    [[nodiscard]] DataPoint& getDataPoint(std::size_t index) {
        return dataPoints_.at(index);
    }

    /**
     * @brief Récupère un DataPoint par son index (version constante).
     * @param index Index de la ligne (0-based)
     * @return Référence constante au DataPoint à l'index donné
     * @throws std::out_of_range si l'index est invalide
     */
    [[nodiscard]] const DataPoint& getDataPoint(std::size_t index) const {
        return dataPoints_.at(index);
    }

    /**
     * @brief Supprime un DataPoint par son index.
     * @param index Index de la ligne à supprimer
     * @return true si la suppression a réussi, false si l'index est invalide
     */
    bool removeDataPoint(std::size_t index) {
        if (index >= dataPoints_.size()) {
            return false;
        }
        dataPoints_.erase(dataPoints_.begin() + static_cast<std::ptrdiff_t>(index));
        return true;
    }

    /**
     * @brief Récupère le nombre de lignes (DataPoints) dans le dataset.
     * @return Nombre de lignes
     */
    [[nodiscard]] std::size_t getRowCount() const {
        return dataPoints_.size();
    }

    /**
     * @brief Récupère le nombre de colonnes.
     * @return Nombre de colonnes
     */
    [[nodiscard]] std::size_t getColumnCount() const {
        return columnNames_.size();
    }

    /**
     * @brief Vérifie si le dataset est vide.
     * @return true si aucune ligne n'est présente, false sinon
     */
    [[nodiscard]] bool empty() const {
        return dataPoints_.empty();
    }

    /**
     * @brief Efface toutes les données du dataset.
     */
    void clear() {
        dataPoints_.clear();
        columnNames_.clear();
    }

    /**
     * @brief Définit les noms de colonnes.
     * @param columnNames Liste des noms de colonnes
     */
    void setColumnNames(const std::vector<std::string>& columnNames) {
        columnNames_ = columnNames;
    }

    /**
     * @brief Définit les noms de colonnes (par déplacement).
     * @param columnNames Liste des noms de colonnes à déplacer
     */
    void setColumnNames(std::vector<std::string>&& columnNames) {
        columnNames_ = std::move(columnNames);
    }

    /**
     * @brief Récupère la liste des noms de colonnes.
     * @return Référence constante au vecteur des noms de colonnes
     */
    [[nodiscard]] const std::vector<std::string>& getColumnNames() const {
        return columnNames_;
    }

    /**
     * @brief Récupère la liste des noms de colonnes (modifiable).
     * @return Référence au vecteur des noms de colonnes
     */
    std::vector<std::string>& getColumnNames() {
        return columnNames_;
    }

    /**
     * @brief Ajoute un nom de colonne.
     * @param columnName Nom de la colonne à ajouter
     */
    void addColumnName(const std::string& columnName) {
        columnNames_.push_back(columnName);
    }

    /**
     * @brief Ajoute un nom de colonne (par déplacement).
     * @param columnName Nom de la colonne à déplacer
     */
    void addColumnName(std::string&& columnName) {
        columnNames_.push_back(std::move(columnName));
    }

    /**
     * @brief Accès direct à la liste des DataPoints (lecture seule).
     * @return Référence constante au vecteur des DataPoints
     */
    [[nodiscard]] const std::vector<DataPoint>& getDataPoints() const {
        return dataPoints_;
    }

    /**
     * @brief Accès direct à la liste des DataPoints (modifiable).
     * @return Référence au vecteur des DataPoints
     */
    std::vector<DataPoint>& getDataPoints() {
        return dataPoints_;
    }

    /**
     * @brief Itérateur de début (const).
     * @return Itérateur constant pointant vers le premier DataPoint
     */
    [[nodiscard]] std::vector<DataPoint>::const_iterator begin() const {
        return dataPoints_.begin();
    }

    /**
     * @brief Itérateur de fin (const).
     * @return Itérateur constant pointant après le dernier DataPoint
     */
    [[nodiscard]] std::vector<DataPoint>::const_iterator end() const {
        return dataPoints_.end();
    }

    /**
     * @brief Itérateur de début (modifiable).
     * @return Itérateur pointant vers le premier DataPoint
     */
    [[nodiscard]] std::vector<DataPoint>::iterator begin() {
        return dataPoints_.begin();
    }

    /**
     * @brief Itérateur de fin (modifiable).
     * @return Itérateur pointant après le dernier DataPoint
     */
    [[nodiscard]] std::vector<DataPoint>::iterator end() {
        return dataPoints_.end();
    }

private:
    /// Liste des DataPoints (lignes du dataset)
    std::vector<DataPoint> dataPoints_;
    
    /// Liste des noms de colonnes
    std::vector<std::string> columnNames_;
};

} // namespace varlor::models

