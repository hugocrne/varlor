#include "ExpressionExecutor.hpp"

#include <cmath>
#include <stdexcept>

namespace varlor::core {

ExpressionExecutor::EvaluationResult ExpressionExecutor::evaluate(
    MathOperationParser::ParsedExpression& parsedExpression,
    const models::Dataset& dataset) const {
    if (!parsedExpression.symbolTable || !parsedExpression.variableStorage) {
        throw std::runtime_error("Expression compilée invalide : symboles manquants.");
    }

    const auto& columnNames = dataset.getColumnNames();
    if (columnNames.size() != parsedExpression.variableStorage->size()) {
        throw std::runtime_error(
            "Le dataset ne correspond plus aux colonnes utilisées lors de la compilation.");
    }

    if (parsedExpression.usedColumnIndices.empty()) {
        const double value = parsedExpression.expression.value();
        if (!std::isfinite(value)) {
            throw std::runtime_error("L'expression a produit une valeur non finie.");
        }
        return value;
    }

    std::vector<double> results;
    results.reserve(dataset.getRowCount());

    auto& storage = *parsedExpression.variableStorage;
    for (const auto& point : dataset) {
        for (const auto columnIndex : parsedExpression.usedColumnIndices) {
            const auto& columnName = columnNames[columnIndex];
            const auto field = point.getField(columnName);
            if (!field.has_value() || !std::holds_alternative<double>(*field)) {
                throw std::runtime_error(
                    "La colonne \"" + columnName +
                    "\" contient une valeur manquante ou non numérique.");
            }
            storage[columnIndex] = std::get<double>(*field);
        }

        const double value = parsedExpression.expression.value();
        if (!std::isfinite(value)) {
            throw std::runtime_error("L'expression a produit une valeur non finie.");
        }
        results.push_back(value);
    }

    return results;
}

} // namespace varlor::core



