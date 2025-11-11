#pragma once

#include <optional>
#include <string>
#include <unordered_map>

/**
 * @file OperationDefinition.hpp
 * @brief Décrit une opération d'analyse demandée par le client.
 *
 * Une opération combine une expression mathématique à évaluer et, optionnellement,
 * un alias ou des paramètres supplémentaires. Les expressions peuvent référencer
 * des colonnes du dataset ou invoquer des fonctions prédéfinies.
 */

namespace varlor::models {

/**
 * @struct OperationDefinition
 * @brief Paramètres déclaratifs d'une opération du moteur d'indicateurs.
 *
 * Chaque opération est interprétée par le moteur afin d'invoquer soit une fonction
 * prédéfinie (cf. BuiltinOperations), soit l'évaluation d'une expression libre.
 */
struct OperationDefinition {
    /**
     * @brief Expression mathématique telle que reçue du client.
     *
     * Peut prendre la forme d'un appel de fonction (`mean(price)`) ou d'une
     * expression libre (`price * clicks / 100`). La validation et la compilation
     * sont réalisées par le `MathOperationParser`.
     */
    std::string expr;

    /**
     * @brief Alias optionnel pour nommer le résultat.
     *
     * Lorsque renseigné, l'alias est utilisé dans les rapports et facilite la
     * corrélation côté client.
     */
    std::optional<std::string> alias;

    /**
     * @brief Paramètres additionnels transmis au moteur.
     *
     * Les valeurs sont exprimées sous forme de chaîne pour conserver la flexibilité
     * (ex. percentile → \"95\", method → \"pearson\"). L'interprétation concrète est
     * réalisée par les composants d'exécution en fonction de l'opération.
     */
    std::optional<std::unordered_map<std::string, std::string>> params;
};

} // namespace varlor::models



