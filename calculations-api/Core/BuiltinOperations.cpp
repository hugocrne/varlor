#include "BuiltinOperations.hpp"

#include <algorithm>
#include <cmath>
#include <numeric>
#include <stdexcept>
#include <utility>
#include <vector>

namespace varlor::core {

namespace {

[[nodiscard]] bool columnExists(const models::Dataset& dataset, const std::string& column) {
    const auto& columns = dataset.getColumnNames();
    return std::find(columns.begin(), columns.end(), column) != columns.end();
}

[[nodiscard]] std::vector<double> extractNumericColumn(
    const models::Dataset& dataset,
    const std::string& column) {
    if (!columnExists(dataset, column)) {
        throw std::invalid_argument("Colonne \"" + column + "\" introuvable dans le dataset.");
    }

    std::vector<double> values;
    values.reserve(dataset.getRowCount());

    for (const auto& point : dataset) {
        const auto field = point.getField(column);
        if (!field.has_value()) {
            continue;
        }

        if (std::holds_alternative<double>(*field)) {
            values.push_back(std::get<double>(*field));
        } else if (std::holds_alternative<std::nullptr_t>(*field)) {
            continue;
        } else {
            throw std::invalid_argument(
                "La colonne \"" + column + "\" contient des valeurs non numériques.");
        }
    }

    if (values.empty()) {
        throw std::invalid_argument(
            "La colonne \"" + column + "\" ne contient aucune valeur numérique exploitable.");
    }

    return values;
}

[[nodiscard]] std::pair<std::vector<double>, std::vector<double>> extractNumericPair(
    const models::Dataset& dataset,
    const std::string& columnX,
    const std::string& columnY) {
    if (!columnExists(dataset, columnX) || !columnExists(dataset, columnY)) {
        throw std::invalid_argument("Au moins une des colonnes demandées est absente du dataset.");
    }

    std::vector<double> valuesX;
    std::vector<double> valuesY;
    valuesX.reserve(dataset.getRowCount());
    valuesY.reserve(dataset.getRowCount());

    for (const auto& point : dataset) {
        const auto fieldX = point.getField(columnX);
        const auto fieldY = point.getField(columnY);

        if (!fieldX.has_value() || !fieldY.has_value()) {
            continue;
        }

        if (std::holds_alternative<double>(*fieldX) && std::holds_alternative<double>(*fieldY)) {
            valuesX.push_back(std::get<double>(*fieldX));
            valuesY.push_back(std::get<double>(*fieldY));
        } else if (
            (std::holds_alternative<std::nullptr_t>(*fieldX) ||
             std::holds_alternative<std::nullptr_t>(*fieldY))) {
            continue;
        } else {
            throw std::invalid_argument(
                "Les colonnes \"" + columnX + "\" et \"" + columnY +
                "\" doivent contenir uniquement des données numériques.");
        }
    }

    if (valuesX.empty() || valuesY.empty()) {
        throw std::invalid_argument(
            "Les colonnes fournies ne contiennent pas suffisamment de données numériques.");
    }

    if (valuesX.size() != valuesY.size()) {
        throw std::runtime_error(
            "Des lignes possèdent des valeurs manquantes asymétriques pour les colonnes fournies.");
    }

    return {std::move(valuesX), std::move(valuesY)};
}

[[nodiscard]] double computeMean(const std::vector<double>& values) {
    const auto sum = std::accumulate(values.begin(), values.end(), 0.0);
    return sum / static_cast<double>(values.size());
}

[[nodiscard]] double computeVariance(const std::vector<double>& values, double meanValue) {
    const auto squareSum = std::accumulate(
        values.begin(),
        values.end(),
        0.0,
        [meanValue](double acc, double value) {
            const auto diff = value - meanValue;
            return acc + diff * diff;
        });

    return squareSum / static_cast<double>(values.size());
}

[[nodiscard]] double computePercentile(std::vector<double> values, double percentile) {
    if (percentile < BuiltinOperations::kMinPercentile ||
        percentile > BuiltinOperations::kMaxPercentile) {
        throw std::invalid_argument("Le percentile doit être compris entre 0 et 100.");
    }

    if (values.empty()) {
        throw std::invalid_argument("Aucune valeur n'est disponible pour calculer le percentile.");
    }

    std::sort(values.begin(), values.end());

    if (percentile == BuiltinOperations::kMaxPercentile) {
        return values.back();
    }

    const double rank =
        (percentile / 100.0) * static_cast<double>(values.size() - 1); // méthode d'interpolation linéaire
    const auto lowerIndex = static_cast<std::size_t>(std::floor(rank));
    const auto upperIndex = static_cast<std::size_t>(std::ceil(rank));

    if (lowerIndex == upperIndex) {
        return values[lowerIndex];
    }

    const double weight = rank - static_cast<double>(lowerIndex);
    return values[lowerIndex] + weight * (values[upperIndex] - values[lowerIndex]);
}

} // namespace

double BuiltinOperations::mean(const models::Dataset& dataset, const std::string& column) {
    const auto values = extractNumericColumn(dataset, column);
    return computeMean(values);
}

double BuiltinOperations::median(const models::Dataset& dataset, const std::string& column) {
    auto values = extractNumericColumn(dataset, column);
    std::sort(values.begin(), values.end());

    const auto size = values.size();
    if (size % 2 == 0U) {
        const auto mid1 = values[size / 2 - 1];
        const auto mid2 = values[size / 2];
        return (mid1 + mid2) / 2.0;
    }

    return values[size / 2];
}

double BuiltinOperations::variance(const models::Dataset& dataset, const std::string& column) {
    const auto values = extractNumericColumn(dataset, column);
    if (values.size() < 2) {
        throw std::invalid_argument("Au moins deux valeurs sont nécessaires pour la variance.");
    }
    const auto meanValue = computeMean(values);
    return computeVariance(values, meanValue);
}

double BuiltinOperations::stddev(const models::Dataset& dataset, const std::string& column) {
    const auto varianceValue = variance(dataset, column);
    return std::sqrt(varianceValue);
}

double BuiltinOperations::correlation(
    const models::Dataset& dataset,
    const std::string& columnX,
    const std::string& columnY) {
    auto [valuesX, valuesY] = extractNumericPair(dataset, columnX, columnY);

    if (valuesX.size() != valuesY.size()) {
        throw std::runtime_error(
            "Les colonnes doivent être alignées : même nombre de valeurs non nulles.");
    }
    if (valuesX.size() < 2) {
        throw std::invalid_argument("Au moins deux couples de valeurs sont nécessaires.");
    }

    const auto meanX = computeMean(valuesX);
    const auto meanY = computeMean(valuesY);

    double numerator = 0.0;
    double denominatorX = 0.0;
    double denominatorY = 0.0;
    for (std::size_t i = 0; i < valuesX.size(); ++i) {
        const auto diffX = valuesX[i] - meanX;
        const auto diffY = valuesY[i] - meanY;
        numerator += diffX * diffY;
        denominatorX += diffX * diffX;
        denominatorY += diffY * diffY;
    }

    if (denominatorX == 0.0 || denominatorY == 0.0) {
        throw std::invalid_argument("La variance de l'une des colonnes est nulle.");
    }

    return numerator / std::sqrt(denominatorX * denominatorY);
}

double BuiltinOperations::min(const models::Dataset& dataset, const std::string& column) {
    const auto values = extractNumericColumn(dataset, column);
    return *std::min_element(values.begin(), values.end());
}

double BuiltinOperations::max(const models::Dataset& dataset, const std::string& column) {
    const auto values = extractNumericColumn(dataset, column);
    return *std::max_element(values.begin(), values.end());
}

double BuiltinOperations::percentile(
    const models::Dataset& dataset,
    const std::string& column,
    double percentileValue) {
    const auto values = extractNumericColumn(dataset, column);
    return computePercentile(values, percentileValue);
}

} // namespace varlor::core


