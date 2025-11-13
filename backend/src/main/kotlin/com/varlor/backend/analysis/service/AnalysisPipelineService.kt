package com.varlor.backend.analysis.service

import com.varlor.backend.analysis.model.FullAnalysisResult
import com.varlor.backend.analysis.model.IndicatorRequest
import org.springframework.stereotype.Service

/**
 * Service de pipeline d'analyse complet.
 *
 * Orchestre l'exécution d'un pipeline d'analyse complet en enchaînant :
 * 1. Le prétraitement du dataset (nettoyage, normalisation, imputation)
 * 2. Le calcul des indicateurs sur le dataset prétraité
 *
 * Ce service combine les fonctionnalités de [DataPreprocessorService] et
 * [IndicatorEngineService] pour offrir une analyse complète en une seule opération.
 *
 * @property dataPreprocessorService Service de prétraitement
 * @property indicatorEngineService Service d'exécution d'indicateurs
 */
@Service
class AnalysisPipelineService(
    private val dataPreprocessorService: DataPreprocessorService,
    private val indicatorEngineService: IndicatorEngineService
) {

    /**
     * Exécute un pipeline d'analyse complet.
     *
     * Prétraite le dataset puis calcule les indicateurs sur le dataset nettoyé.
     *
     * @param request Requête contenant le dataset et les opérations à exécuter
     * @return Résultat complet incluant le prétraitement et les indicateurs calculés
     */
    fun executeFullAnalysis(request: IndicatorRequest): FullAnalysisResult {
        val preprocessing = dataPreprocessorService.preprocess(request.dataset)
        val operations = indicatorEngineService.execute(request.operations, preprocessing.cleanedDataset)
        return FullAnalysisResult(
            preprocessing = preprocessing,
            operations = operations
        )
    }
}

