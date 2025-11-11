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
    /// Jeu de données nettoyé et prêt pour les analyses statistiques
    Dataset cleanedDataset;

    /// Jeu de données contenant uniquement les outliers identifiés
    Dataset outliersDataset;

    /// Rapport détaillé retraçant l'intégralité du prétraitement
    PreprocessingReport report;
};

} // namespace varlor::models


