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
 * Les étapes principales sont :
 * - Normalisation des types de colonnes
 * - Détection des outliers par IQR
 * - Gestion des valeurs manquantes via imputation
 * - Production d'un rapport détaillé et traçabilité via `_meta`
 */
class DataPreprocessor {
public:
    /**
     * @brief Traite un dataset et renvoie le résultat complet du prétraitement.
     * @param dataset Jeu de données brut (non modifié)
     * @return Structure contenant le dataset nettoyé, les outliers et le rapport
     */
    [[nodiscard]] models::PreprocessingResult process(const models::Dataset& dataset) const;

private:
    struct ColumnProfile {
        models::FieldType type{models::FieldType::Unknown};
        std::vector<std::pair<std::size_t, double>> numericSamples;
        bool normalized{false};
    };

    [[nodiscard]] ColumnProfile analyseAndNormalizeColumn(
        const models::Dataset& source,
        models::Dataset& target,
        const std::string& columnName,
        models::PreprocessingReport& report) const;

    [[nodiscard]] std::vector<bool> buildOutlierMask(
        const std::unordered_map<std::string, ColumnProfile>& profiles,
        std::size_t rowCount) const;

    std::size_t splitOutliers(
        const std::vector<bool>& outlierMask,
        models::Dataset& cleanedDataset,
        models::Dataset& outliersDataset) const;

    std::size_t imputeMissingValues(
        const std::unordered_map<std::string, ColumnProfile>& profiles,
        models::Dataset& cleanedDataset) const;

    std::size_t imputeNumericColumn(models::Dataset& dataset, const std::string& columnName) const;
    std::size_t imputeBooleanColumn(models::Dataset& dataset, const std::string& columnName) const;
    std::size_t imputeTextColumn(models::Dataset& dataset, const std::string& columnName) const;

    void annotateOutlier(models::DataPoint& point) const;
    void annotateImputation(
        models::DataPoint& point,
        const std::string& columnName,
        const std::string& strategy,
        const models::FieldValue& imputedValue) const;

    [[nodiscard]] double computeMedian(std::vector<double> values) const;
    [[nodiscard]] std::pair<double, double> computeQuartiles(std::vector<double> values) const;

    [[nodiscard]] bool tryParseDouble(const models::FieldValue& value, double& out) const;
    [[nodiscard]] bool tryParseBoolean(const models::FieldValue& value, bool& out) const;
    [[nodiscard]] std::string toStringValue(const models::FieldValue& value) const;
    [[nodiscard]] std::string trim(const std::string& value) const;
};

} // namespace varlor::core


