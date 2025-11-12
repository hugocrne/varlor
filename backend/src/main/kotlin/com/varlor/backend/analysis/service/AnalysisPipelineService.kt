package com.varlor.backend.analysis.service

import com.varlor.backend.analysis.model.FullAnalysisResult
import com.varlor.backend.analysis.model.IndicatorRequest
import org.springframework.stereotype.Service

@Service
class AnalysisPipelineService(
    private val dataPreprocessorService: DataPreprocessorService,
    private val indicatorEngineService: IndicatorEngineService
) {

    fun executeFullAnalysis(request: IndicatorRequest): FullAnalysisResult {
        val preprocessing = dataPreprocessorService.preprocess(request.dataset)
        val operations = indicatorEngineService.execute(request.operations, preprocessing.cleanedDataset)
        return FullAnalysisResult(
            preprocessing = preprocessing,
            operations = operations
        )
    }
}

