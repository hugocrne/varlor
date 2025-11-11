#pragma once

#include "MathOperationParser.hpp"

#include "../Models/Dataset.hpp"

#include <variant>
#include <vector>

/**
 * @file ExpressionExecutor.hpp
 * @brief Évaluation des expressions dynamiques sur un dataset.
 */

namespace varlor::core {

/**
 * @class ExpressionExecutor
 * @brief Évalue les expressions compilées par `MathOperationParser`.
 *
 * Les expressions produisent soit une série (calcul ligne par ligne), soit une valeur
 * scalaire lorsque seules des constantes demeurent après substitution.
 */
class ExpressionExecutor final {
public:
    /// Résultat d'exécution : scalaire ou série.
    using EvaluationResult = std::variant<double, std::vector<double>>;

    /**
     * @brief Évalue une expression sur le dataset fourni.
     *
     * @param parsedExpression Expression compilée à exécuter.
     * @param dataset Dataset analysé.
     * @return Une valeur scalaire ou une série de résultats selon les variables utilisées.
     *
     * @throws std::runtime_error si une valeur manquante ou non numérique empêche l'évaluation.
     */
    [[nodiscard]] EvaluationResult evaluate(
        MathOperationParser::ParsedExpression& parsedExpression,
        const models::Dataset& dataset) const;
};

} // namespace varlor::core



