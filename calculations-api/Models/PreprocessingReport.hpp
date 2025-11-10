#pragma once

#include <string>
#include <vector>
#include <cstdint>

/**
 * @file PreprocessingReport.hpp
 * @brief Fournit un résumé du nettoyage des données effectué par le moteur.
 * 
 * Contient des statistiques sur le traitement des données :
 * - Nombre de lignes en entrée et en sortie
 * - Nombre d'outliers supprimés
 * - Nombre de valeurs manquantes remplacées
 * - Liste des champs normalisés
 * 
 * Sert à produire un rapport de traitement lisible côté API.
 * Conçu pour être facilement sérialisable en JSON.
 */

namespace varlor::models {

/**
 * @class PreprocessingReport
 * @brief Rapport détaillé des opérations de pré-traitement effectuées.
 * 
 * Permet de tracer toutes les transformations appliquées aux données
 * et de fournir un retour détaillé à l'utilisateur via l'API.
 */
class PreprocessingReport {
public:
    /**
     * @brief Constructeur par défaut.
     * Initialise tous les compteurs à zéro.
     */
    PreprocessingReport()
        : inputRowCount_(0)
        , outputRowCount_(0)
        , outliersRemoved_(0)
        , missingValuesReplaced_(0)
    {}

    /**
     * @brief Constructeur avec initialisation des compteurs.
     * @param inputRowCount Nombre de lignes en entrée
     * @param outputRowCount Nombre de lignes en sortie
     */
    PreprocessingReport(std::size_t inputRowCount, std::size_t outputRowCount)
        : inputRowCount_(inputRowCount)
        , outputRowCount_(outputRowCount)
        , outliersRemoved_(0)
        , missingValuesReplaced_(0)
    {}

    // ========== Getters ==========

    /**
     * @brief Récupère le nombre de lignes en entrée.
     * @return Nombre de lignes en entrée
     */
    [[nodiscard]] std::size_t getInputRowCount() const {
        return inputRowCount_;
    }

    /**
     * @brief Récupère le nombre de lignes en sortie.
     * @return Nombre de lignes en sortie
     */
    [[nodiscard]] std::size_t getOutputRowCount() const {
        return outputRowCount_;
    }

    /**
     * @brief Récupère le nombre d'outliers supprimés.
     * @return Nombre d'outliers supprimés
     */
    [[nodiscard]] std::size_t getOutliersRemoved() const {
        return outliersRemoved_;
    }

    /**
     * @brief Récupère le nombre de valeurs manquantes remplacées.
     * @return Nombre de valeurs manquantes remplacées
     */
    [[nodiscard]] std::size_t getMissingValuesReplaced() const {
        return missingValuesReplaced_;
    }

    /**
     * @brief Récupère la liste des champs normalisés.
     * @return Référence constante au vecteur des noms de champs normalisés
     */
    [[nodiscard]] const std::vector<std::string>& getNormalizedFields() const {
        return normalizedFields_;
    }

    // ========== Setters ==========

    /**
     * @brief Définit le nombre de lignes en entrée.
     * @param count Nombre de lignes
     */
    void setInputRowCount(std::size_t count) {
        inputRowCount_ = count;
    }

    /**
     * @brief Définit le nombre de lignes en sortie.
     * @param count Nombre de lignes
     */
    void setOutputRowCount(std::size_t count) {
        outputRowCount_ = count;
    }

    /**
     * @brief Définit le nombre d'outliers supprimés.
     * @param count Nombre d'outliers
     */
    void setOutliersRemoved(std::size_t count) {
        outliersRemoved_ = count;
    }

    /**
     * @brief Définit le nombre de valeurs manquantes remplacées.
     * @param count Nombre de valeurs
     */
    void setMissingValuesReplaced(std::size_t count) {
        missingValuesReplaced_ = count;
    }

    // ========== Méthodes utilitaires ==========

    /**
     * @brief Incrémente le compteur d'outliers supprimés.
     * @param count Nombre à ajouter (par défaut 1)
     */
    void incrementOutliersRemoved(std::size_t count = 1) {
        outliersRemoved_ += count;
    }

    /**
     * @brief Incrémente le compteur de valeurs manquantes remplacées.
     * @param count Nombre à ajouter (par défaut 1)
     */
    void incrementMissingValuesReplaced(std::size_t count = 1) {
        missingValuesReplaced_ += count;
    }

    /**
     * @brief Ajoute un champ à la liste des champs normalisés.
     * @param fieldName Nom du champ normalisé
     */
    void addNormalizedField(const std::string& fieldName) {
        normalizedFields_.push_back(fieldName);
    }

    /**
     * @brief Ajoute un champ à la liste des champs normalisés (par déplacement).
     * @param fieldName Nom du champ normalisé à déplacer
     */
    void addNormalizedField(std::string&& fieldName) {
        normalizedFields_.push_back(std::move(fieldName));
    }

    /**
     * @brief Efface tous les champs normalisés.
     */
    void clearNormalizedFields() {
        normalizedFields_.clear();
    }

    /**
     * @brief Calcule le nombre de lignes supprimées.
     * @return Différence entre les lignes d'entrée et de sortie
     */
    [[nodiscard]] std::size_t getRowsRemoved() const {
        return (inputRowCount_ > outputRowCount_) 
            ? (inputRowCount_ - outputRowCount_) 
            : 0;
    }

    /**
     * @brief Réinitialise tous les compteurs à zéro.
     */
    void reset() {
        inputRowCount_ = 0;
        outputRowCount_ = 0;
        outliersRemoved_ = 0;
        missingValuesReplaced_ = 0;
        normalizedFields_.clear();
    }

private:
    /// Nombre de lignes en entrée
    std::size_t inputRowCount_;
    
    /// Nombre de lignes en sortie
    std::size_t outputRowCount_;
    
    /// Nombre d'outliers supprimés
    std::size_t outliersRemoved_;
    
    /// Nombre de valeurs manquantes remplacées
    std::size_t missingValuesReplaced_;
    
    /// Liste des noms de champs qui ont été normalisés
    std::vector<std::string> normalizedFields_;
};

} // namespace varlor::models

