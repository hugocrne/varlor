#pragma once

#include "../Models/Dataset.hpp"

#include <exprtk.hpp>

#include <memory>
#include <string>
#include <vector>

/**
 * @file MathOperationParser.hpp
 * @brief Parser sécurisé pour compiler des expressions mathématiques via ExprTk.
 */

namespace varlor::core {

/**
 * @class MathOperationParser
 * @brief Prépare et valide les expressions fournies au moteur d'indicateurs.
 *
 * Le parser assure :
 * - La substitution des fonctions agrégées (`mean`, `median`, ...) par les valeurs calculées
 *   via `BuiltinOperations`.
 * - La validation lexicale (caractères autorisés, colonnes existantes, fonctions sûres).
 * - La compilation de l'expression en un artefact exécutable par `ExpressionExecutor`.
 */
class MathOperationParser final {
public:
    /**
     * @struct ParsedExpression
     * @brief Artefact compilé prêt pour l'exécution.
     *
     * Contient l'expression ExprTk ainsi que les structures nécessaires pour alimenter les
     * variables avant évaluation. L'instance est non copiable afin de garantir la stabilité
     * des pointeurs internes utilisés par ExprTk.
     */
    struct ParsedExpression {
        ParsedExpression() = default;

        ParsedExpression(const ParsedExpression&) = delete;
        ParsedExpression& operator=(const ParsedExpression&) = delete;

        ParsedExpression(ParsedExpression&&) noexcept = default;
        ParsedExpression& operator=(ParsedExpression&&) noexcept = default;

        /// Expression initiale telle que reçue.
        std::string originalExpression;
        /// Expression après substitutions et normalisation.
        std::string normalizedExpression;
        /// Expression compilée prêt à être évaluée.
        exprtk::expression<double> expression;
        /// Table des symboles, partagée pour garantir la durée de vie des variables.
        std::shared_ptr<exprtk::symbol_table<double>> symbolTable;
        /// Stockage sous-jacent des variables (une entrée par colonne du dataset).
        std::shared_ptr<std::vector<double>> variableStorage;
        /// Indices des colonnes effectivement utilisées par l'expression.
        std::vector<std::size_t> usedColumnIndices;
    };

    /**
     * @brief Compile une expression mathématique sécurisée.
     *
     * @param expr Expression reçue du client.
     * @param dataset Dataset de référence pour valider les colonnes et calculer les agrégats.
     * @return Expression compilée prête pour l'exécution.
     *
     * @throws std::invalid_argument en cas d'expression vide, invalide ou référencant des colonnes inexistantes.
     * @throws std::runtime_error si la compilation ExprTk échoue.
     */
    [[nodiscard]] ParsedExpression parse(const std::string& expr, const models::Dataset& dataset) const;
};

} // namespace varlor::core



