#pragma once

#include <optional>
#include <string>
#include <variant>
#include <vector>

/**
 * @file OperationResult.hpp
 * @brief Structure représentant le résultat d'une opération analytique.
 */

namespace varlor::models {

/**
 * @enum OperationStatus
 * @brief Indique l'issue de l'exécution d'une opération.
 */
enum class OperationStatus {
    /// L'opération s'est exécutée correctement.
    Success,
    /// L'opération a échoué (erreur de validation ou d'exécution).
    Error
};

/**
 * @struct OperationResult
 * @brief Résultat détaillé retourné par l'IndicatorEngine pour chaque opération.
 *
 * Le moteur renseigne la valeur calculée, l'état d'exécution et la date ISO 8601
 * de traitement afin d'assurer la traçabilité complète de chaque requête.
 */
struct OperationResult {
    /**
     * @brief Expression originale ayant conduit à ce résultat.
     */
    std::string expr;

    /**
     * @brief Valeur calculée.
     *
     * Un résultat scalaire est exprimé en `double`, une série en `std::vector<double>`.
     * Le type `std::monostate` signale l'absence de valeur exploitable (typiquement
     * suite à une erreur).
     */
    std::variant<double, std::vector<double>, std::monostate> result{std::monostate{}};

    /**
     * @brief Statut d'exécution (succès ou erreur).
     */
    OperationStatus status{OperationStatus::Success};

    /**
     * @brief Message d'erreur optionnel détaillant la cause de l'échec.
     */
    std::optional<std::string> errorMessage;

    /**
     * @brief Instant d'exécution formaté en ISO 8601 (UTC).
     */
    std::string executedAt;
};

} // namespace varlor::models



