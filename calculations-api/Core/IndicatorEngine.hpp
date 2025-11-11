#pragma once

#include "../Models/Dataset.hpp"
#include "../Models/OperationDefinition.hpp"
#include "../Models/OperationResult.hpp"

#include <vector>

/**
 * @file IndicatorEngine.hpp
 * @brief Orchestration complète des opérations analytiques dynamiques.
 */

namespace varlor::core {

/**
 * @class IndicatorEngine
 * @brief Exécute une liste d'opérations mathématiques sur un dataset nettoyé.
 *
 * L'engine délègue :
 * - Aux fonctions prédéfinies (`BuiltinOperations`) lorsque l'expression correspond à une
 *   fonction connue simple.
 * - Au couple `MathOperationParser` / `ExpressionExecutor` pour les expressions libres.
 */
class IndicatorEngine final {
public:
    /**
     * @brief Exécute une série d'opérations et retourne les résultats détaillés.
     *
     * @param data Dataset nettoyé issu du prétraitement.
     * @param operations Liste déclarative d'opérations à évaluer.
     * @return Résultats structurés ordonnés selon la liste fournie.
     */
    [[nodiscard]] std::vector<models::OperationResult> execute(
        const models::Dataset& data,
        const std::vector<models::OperationDefinition>& operations) const;
};

} // namespace varlor::core



