#pragma once

#include "../Models/DataPoint.hpp"
#include "../Models/Dataset.hpp"
#include "../Models/FieldType.hpp"
#include "../Models/PreprocessingReport.hpp"
#include "../Models/PreprocessingResult.hpp"

#include <string>
#include <vector>
#include <unordered_map>
#include <optional>

/**
 * @file DataPreprocessor.hpp
 * @brief Déclaration de la classe responsable du nettoyage non destructif.
 */

namespace varlor::core {

/**
 * @class DataPreprocessor
 * @brief Orchestration complète du pipeline de prétraitement.
 *
 * La classe applique un pipeline non destructif comprenant :
 * - Normalisation des valeurs vers des types cohérents (double, bool, string)
 * - Détection des valeurs extrêmes via l'IQR et séparation des outliers
 * - Imputation des valeurs manquantes avec annotation dans `_meta`
 * - Génération d'un rapport synthétisant les opérations effectuées
 *
 * @note L'instance ne maintient pas d'état métier : seules les options de configuration
 *       (facteur d'outliers) sont conservées entre deux traitements.
 */
class DataPreprocessor {
public:
    /**
     * @brief Constructeur permettant d'ajuster le seuil de détection des outliers.
     *
     * @param outlierThresholdMultiplier Multiplicateur appliqué à l'IQR afin de définir
     *        les bornes de détection des valeurs extrêmes. La valeur par défaut (1.5)
     *        correspond au facteur de Tukey.
     * @throws std::invalid_argument si le multiplicateur est inférieur ou égal à zéro.
     */
    explicit DataPreprocessor(double outlierThresholdMultiplier = 1.5);

    /**
     * @brief Traite un dataset et renvoie le résultat complet du prétraitement.
     *
     * La méthode analyse le schéma des colonnes, normalise les valeurs,
     * déplace les outliers dans un dataset dédié, impute les valeurs manquantes
     * et renseigne les métadonnées `_meta` pour suivre chaque modification.
     *
     * @param dataset Jeu de données brut (non modifié)
     * @return Structure contenant le dataset nettoyé, les outliers et le rapport
     */
    [[nodiscard]] models::PreprocessingResult process(const models::Dataset& dataset) const;

private:
    /**
     * @struct ColumnProfile
     * @brief Informations agrégées lors de l'analyse d'une colonne.
     *
     * @var ColumnProfile::type
     *      Type de champ détecté pour la colonne.
     * @var ColumnProfile::numericSamples
     *      Couples (index, valeur) employés pour la détection des outliers.
     * @var ColumnProfile::normalized
     *      Indique si la colonne a été normalisée et doit apparaître dans le rapport.
     */
    struct ColumnProfile {
        models::FieldType type{models::FieldType::Unknown};
        std::vector<std::pair<std::size_t, double>> numericSamples;
        bool normalized{false};
    };

    /**
     * @brief Détecte le type d'une colonne et normalise ses valeurs.
     *
     * Produit un profil de colonne et alimente le dataset cible avec les valeurs
     * converties (ou null si la conversion échoue). Les colonnes reconnues sont
     * ajoutées au rapport comme champs normalisés.
     *
     * @param source Jeu de données source (lecture seule)
     * @param target Jeu de données cible à remplir
     * @param columnName Nom de la colonne analysée
     * @param report Rapport de prétraitement à enrichir
     * @return Profil décrivant la colonne
     */
    [[nodiscard]] ColumnProfile analyseAndNormalizeColumn(
        const models::Dataset& source,
        models::Dataset& target,
        const std::string& columnName,
        models::PreprocessingReport& report) const;

    /**
     * @brief Calcule un masque d'outliers à partir des profils de colonnes.
     * @param profiles Profils générés durant la normalisation
     * @param rowCount Nombre de lignes à évaluer
     * @return Vecteur booléen signalant les lignes identifiées comme outliers
     */
    [[nodiscard]] std::vector<bool> buildOutlierMask(
        const std::unordered_map<std::string, ColumnProfile>& profiles,
        std::size_t rowCount) const;

    /**
     * @brief Sépare les enregistrements outliers dans un dataset dédié.
     *
     * Les lignes marquées comme outliers sont déplacées vers `outliersDataset`
     * et annotées dans `_meta`; les autres lignes sont conservées.
     *
     * @param outlierMask Masque issu de buildOutlierMask()
     * @param cleanedDataset Jeu de données nettoyé (modifié)
     * @param outliersDataset Jeu de données des outliers (enrichi)
     * @return Nombre de lignes déplacées
     */
    std::size_t splitOutliers(
        const std::vector<bool>& outlierMask,
        models::Dataset& cleanedDataset,
        models::Dataset& outliersDataset) const;

    /**
     * @brief Impute les valeurs manquantes pour chaque colonne identifiée.
     *
     * Les colonnes numériques reçoivent la médiane, les booléennes la mode,
     * et les textuelles la valeur la plus fréquente. Chaque imputation est
     * tracée dans `_meta`.
     *
     * @param profiles Profils de colonnes
     * @param cleanedDataset Jeu de données nettoyé
     * @return Nombre total de valeurs imputées
     */
    std::size_t imputeMissingValues(
        const std::unordered_map<std::string, ColumnProfile>& profiles,
        models::Dataset& cleanedDataset) const;

    /**
     * @brief Impute une colonne numérique avec la médiane.
     * @return Nombre de valeurs imputées
     */
    std::size_t imputeNumericColumn(models::Dataset& dataset, const std::string& columnName) const;

    /**
     * @brief Impute une colonne booléenne avec la mode.
     * @return Nombre de valeurs imputées
     */
    std::size_t imputeBooleanColumn(models::Dataset& dataset, const std::string& columnName) const;

    /**
     * @brief Impute une colonne textuelle avec la valeur la plus fréquente.
     * @return Nombre de valeurs imputées
     */
    std::size_t imputeTextColumn(models::Dataset& dataset, const std::string& columnName) const;

    /**
     * @brief Marque un point de données comme outlier dans `_meta`.
     */
    void annotateOutlier(models::DataPoint& point) const;

    /**
     * @brief Enregistre l'imputation réalisée dans la section `_meta`.
     * @param point Point de données concerné
     * @param columnName Colonne imputée
     * @param strategy Stratégie employée (median, mode_boolean, ...)
     * @param imputedValue Valeur calculée
     */
    void annotateImputation(
        models::DataPoint& point,
        const std::string& columnName,
        const std::string& strategy,
        const models::FieldValue& imputedValue) const;

    /**
     * @brief Calcule la médiane d'un échantillon.
     */
    [[nodiscard]] double computeMedian(std::vector<double> values) const;

    /**
     * @brief Retourne les quartiles Q1 et Q3 d'un échantillon.
     */
    [[nodiscard]] std::pair<double, double> computeQuartiles(std::vector<double> values) const;

    /**
     * @brief Tente de convertir une valeur en double.
     * @return true si la conversion réussit
     */
    [[nodiscard]] bool tryParseDouble(const models::FieldValue& value, double& out) const;

    /**
     * @brief Tente de convertir une valeur en booléen.
     * @return true si la conversion réussit
     */
    [[nodiscard]] bool tryParseBoolean(const models::FieldValue& value, bool& out) const;

    /**
     * @brief Convertit une valeur en chaîne lisible.
     */
    [[nodiscard]] std::string toStringValue(const models::FieldValue& value) const;

    /**
     * @brief Supprime les espaces en début et fin de chaîne.
     */
    [[nodiscard]] std::string trim(const std::string& value) const;

private:
    double outlierThresholdMultiplier_;
};

} // namespace varlor::core


