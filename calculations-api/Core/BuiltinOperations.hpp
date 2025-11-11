#pragma once

#include "../Models/Dataset.hpp"

#include <string>

/**
 * @file BuiltinOperations.hpp
 * @brief Ensemble de fonctions statistiques prédéfinies pour l'IndicatorEngine.
 */

namespace varlor::core {

/**
 * @class BuiltinOperations
 * @brief Collection stateless d'opérations statistiques standards.
 *
 * Chaque méthode vérifie la présence de valeurs numériques dans la colonne ou les
 * colonnes ciblées et lève une exception `std::invalid_argument` en cas de données
 * invalides ou insuffisantes. Les résultats sont retournés sous forme de `double`.
 */
class BuiltinOperations final {
public:
    /// Valeur de percentile minimale acceptée.
    inline static constexpr double kMinPercentile = 0.0;
    /// Valeur de percentile maximale acceptée.
    inline static constexpr double kMaxPercentile = 100.0;

    [[nodiscard]] static double mean(const models::Dataset& dataset, const std::string& column);
    [[nodiscard]] static double median(const models::Dataset& dataset, const std::string& column);
    [[nodiscard]] static double variance(const models::Dataset& dataset, const std::string& column);
    [[nodiscard]] static double stddev(const models::Dataset& dataset, const std::string& column);
    [[nodiscard]] static double correlation(
        const models::Dataset& dataset,
        const std::string& columnX,
        const std::string& columnY);
    [[nodiscard]] static double min(const models::Dataset& dataset, const std::string& column);
    [[nodiscard]] static double max(const models::Dataset& dataset, const std::string& column);
    [[nodiscard]] static double percentile(
        const models::Dataset& dataset,
        const std::string& column,
        double percentile);

private:
    BuiltinOperations() = delete;
};

} // namespace varlor::core



