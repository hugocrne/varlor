#pragma once

#include "oatpp/core/Types.hpp"
#include "oatpp/parser/json/mapping/ObjectMapper.hpp"
#include "oatpp/core/macro/codegen.hpp"
#include "oatpp/web/server/api/ApiController.hpp"

#include <memory>
#include <string>

namespace varlor::controllers {

/// Début de la section générée pour les DTO Oat++.
#include OATPP_CODEGEN_BEGIN(DTO)

/**
 * @class AnalysisDataDescriptorDto
 * @brief Informations décrivant la source des données à prétraiter.
 */
class AnalysisDataDescriptorDto : public oatpp::DTO {
    DTO_INIT(AnalysisDataDescriptorDto, DTO)

    DTO_FIELD(oatpp::String, origin);
    DTO_FIELD(oatpp::String, content_type);
    DTO_FIELD(oatpp::Boolean, autodetect) = false;
};

/**
 * @class AnalysisOptionsDto
 * @brief Options facultatives de configuration du prétraitement.
 */
class AnalysisOptionsDto : public oatpp::DTO {
    DTO_INIT(AnalysisOptionsDto, DTO)

    DTO_FIELD(oatpp::Float64, drop_outliers_percent);
};

/**
 * @class AnalysisOperationDefinitionDto
 * @brief Opération analytique déclarée par le client.
 */
class AnalysisOperationDefinitionDto : public oatpp::DTO {
    DTO_INIT(AnalysisOperationDefinitionDto, DTO)

    DTO_FIELD(oatpp::String, expr);
    DTO_FIELD(oatpp::String, alias);
    DTO_FIELD(oatpp::Fields<oatpp::String>, params);
};

/**
 * @class AnalysisPreprocessRequestDto
 * @brief Structure complète attendue dans la requête REST.
 */
class AnalysisPreprocessRequestDto : public oatpp::DTO {
    DTO_INIT(AnalysisPreprocessRequestDto, DTO)

    DTO_FIELD(oatpp::Object<AnalysisDataDescriptorDto>, data_descriptor);
    DTO_FIELD(oatpp::Object<AnalysisOptionsDto>, options);
    DTO_FIELD(oatpp::Any, data);
    DTO_FIELD(oatpp::List<oatpp::Object<AnalysisOperationDefinitionDto>>, operations);
};

/**
 * @class AnalysisDataPointDto
 * @brief Représente une ligne de dataset retournée par l'API.
 */
class AnalysisDataPointDto : public oatpp::DTO {
    DTO_INIT(AnalysisDataPointDto, DTO)

    DTO_FIELD(oatpp::Fields<oatpp::Any>, values);
    DTO_FIELD(oatpp::Any, _meta);
};

/**
 * @class AnalysisDatasetDto
 * @brief Structure de dataset consolidée pour la réponse REST.
 */
class AnalysisDatasetDto : public oatpp::DTO {
    DTO_INIT(AnalysisDatasetDto, DTO)

    DTO_FIELD(oatpp::List<oatpp::String>, columns);
    DTO_FIELD(oatpp::List<oatpp::Object<AnalysisDataPointDto>>, rows);
};

/**
 * @class AnalysisPreprocessingReportDto
 * @brief Projection du rapport de prétraitement pour la réponse REST.
 */
class AnalysisPreprocessingReportDto : public oatpp::DTO {
    DTO_INIT(AnalysisPreprocessingReportDto, DTO)

    DTO_FIELD(oatpp::Int64, input_row_count);
    DTO_FIELD(oatpp::Int64, output_row_count);
    DTO_FIELD(oatpp::Int64, outliers_removed);
    DTO_FIELD(oatpp::Int64, missing_values_replaced);
    DTO_FIELD(oatpp::List<oatpp::String>, normalized_fields);
};

/**
 * @class AnalysisOperationResultDto
 * @brief Résultat d'une opération d'analyse exécutée.
 */
class AnalysisOperationResultDto : public oatpp::DTO {
    DTO_INIT(AnalysisOperationResultDto, DTO)

    DTO_FIELD(oatpp::String, expr);
    DTO_FIELD(oatpp::String, status);
    DTO_FIELD(oatpp::Any, result);
    DTO_FIELD(oatpp::String, error_message);
    DTO_FIELD(oatpp::String, executed_at);
};

/**
 * @class AnalysisPreprocessResponseDto
 * @brief Payload retourné lors d'un prétraitement réussi.
 */
class AnalysisPreprocessResponseDto : public oatpp::DTO {
    DTO_INIT(AnalysisPreprocessResponseDto, DTO)

    DTO_FIELD(oatpp::Object<AnalysisDatasetDto>, cleaned_dataset);
    DTO_FIELD(oatpp::Object<AnalysisDatasetDto>, outliers_dataset);
    DTO_FIELD(oatpp::Object<AnalysisPreprocessingReportDto>, report);
    DTO_FIELD(oatpp::List<oatpp::Object<AnalysisOperationResultDto>>, operation_results);
};

/**
 * @class AnalysisErrorResponseDto
 * @brief Structure standardisée des réponses d'erreur.
 */
class AnalysisErrorResponseDto : public oatpp::DTO {
    DTO_INIT(AnalysisErrorResponseDto, DTO)

    DTO_FIELD(oatpp::String, error);
    DTO_FIELD(oatpp::String, details);
    DTO_FIELD(oatpp::String, timestamp);
};

#include OATPP_CODEGEN_END(DTO)
/// Fin de la section générée pour les DTO.

#include OATPP_CODEGEN_BEGIN(ApiController)
/**
 * @brief Endpoint REST pour le prétraitement des données.
 * @details Gère les formats JSON et YAML, appelle `DataPreprocessor` et renvoie un `PreprocessingResult` complet.
 * @route /api/analyses/preprocess
 * @since v1.0
 */
class AnalysisController : public oatpp::web::server::api::ApiController {
public:
    explicit AnalysisController(const std::shared_ptr<oatpp::parser::json::mapping::ObjectMapper>& jsonMapper);

    static std::shared_ptr<AnalysisController> createShared(const std::shared_ptr<oatpp::parser::json::mapping::ObjectMapper>& jsonMapper);

    ENDPOINT_INFO(preprocess) {
        info->summary = "Prétraitement d'un dataset semi-structuré";
        info->addConsumes<oatpp::String>("application/json");
        info->addConsumes<oatpp::String>("application/x-yaml");
        info->addResponse<oatpp::Object<AnalysisPreprocessResponseDto>>(oatpp::web::protocol::http::Status::CODE_200, "application/json");
        info->addResponse<oatpp::Object<AnalysisErrorResponseDto>>(oatpp::web::protocol::http::Status::CODE_400, "application/json");
        info->addResponse<oatpp::Object<AnalysisErrorResponseDto>>(oatpp::web::protocol::http::Status::CODE_422, "application/json");
        info->addResponse<oatpp::Object<AnalysisErrorResponseDto>>(oatpp::web::protocol::http::Status::CODE_500, "application/json");
    }

    /**
     * @brief Traite le prétraitement d'un dataset semi-structuré.
     * @details
     * **Paramètres**
     * - `request` : requête HTTP entrante contenant les données, les métadonnées et les options de nettoyage.
     *
     * @return Réponse HTTP sérialisée en JSON ou YAML selon le header `Accept`.
     * @par Codes de retour
     * - 200 : succès, la réponse contient `AnalysisPreprocessResponseDto` (datasets nettoyé et outliers + rapport).
     * - 400 : requête invalide (JSON/YAML mal formé, header manquant) sous la forme `AnalysisErrorResponseDto`.
     * - 422 : incohérence de format déclarée/détectée (`AnalysisErrorResponseDto`).
     * - 500 : erreur interne lors de `DataPreprocessor::process` (`AnalysisErrorResponseDto`).
     * @par Exemple de réponse
     * @code{.json}
     * {
     *   "cleaned_dataset": { "columns": ["temperature"], "rows": [...] },
     *   "outliers_dataset": { ... },
     *   "report": { "input_row_count": 3, "output_row_count": 2, "outliers_removed": 1 }
     * }
     * @endcode
     */
    ENDPOINT("POST", "/api/analyses/preprocess", preprocess, REQUEST(std::shared_ptr<oatpp::web::protocol::http::incoming::Request>, request)) {
        return handlePreprocess(request);
    }

private:
    std::shared_ptr<OutgoingResponse> handlePreprocess(const std::shared_ptr<IncomingRequest>& request);
};

#include OATPP_CODEGEN_END(ApiController)

} // namespace varlor::controllers
