#pragma once

#include "Dataset.hpp"
#include "PreprocessingReport.hpp"

/**
 * @file PreprocessingResult.hpp
 * @brief Regroupe les artefacts produits par le DataPreprocessor.
 *
 * Cette structure réunit le jeu de données nettoyé, les enregistrements
 * identifiés comme outliers ainsi que le rapport détaillé du traitement.
 */

namespace varlor::models {

/**
 * @struct PreprocessingResult
 * @brief Résultat complet d'une opération de prétraitement.
 *
 * Le DataPreprocessor renvoie cette structure afin de garantir qu'aucune
 * information n'est perdue : les données nettoyées, les outliers et le rapport
 * peuvent être exploités conjointement par les couches supérieures.
 */
struct PreprocessingResult {
    /**
     * @brief Jeu de données nettoyé et prêt pour les analyses statistiques.
     *
     * Contient toutes les lignes non considérées comme outliers, avec les
     * valeurs normalisées et imputées lorsqu'approprié.
     */
    Dataset cleanedDataset;

    /**
     * @brief Jeu de données contenant uniquement les outliers identifiés.
     *
     * Chaque `DataPoint` y est annoté dans `_meta.status` afin de permettre
     * l'audit ou une réintégration manuelle ultérieure.
     */
    Dataset outliersDataset;

    /**
     * @brief Rapport détaillé retraçant l'intégralité du prétraitement.
     *
     * Synthétise le nombre de lignes en entrée/sortie, les imputations,
     * les normalisations et la quantité d'outliers déplacés.
     */
    PreprocessingReport report;
};

} // namespace varlor::models


