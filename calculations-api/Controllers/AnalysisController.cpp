#include "AnalysisController.hpp"

#include "../Core/DataPreprocessor.hpp"
#include "../Core/IndicatorEngine.hpp"
#include "../Models/PreprocessingResult.hpp"
#include "../Models/OperationDefinition.hpp"
#include "../Models/OperationResult.hpp"

#include "oatpp/core/macro/codegen.hpp"
#include "oatpp/web/protocol/http/Http.hpp"

#include <yaml-cpp/yaml.h>

#include <algorithm>
#include <chrono>
#include <cctype>
#include <iomanip>
#include <optional>
#include <sstream>
#include <stdexcept>
#include <string>
#include <unordered_set>
#include <utility>
#include <variant>
#include <vector>

// ------ Génération du code pour les endpoints Oat++ ------
#include OATPP_CODEGEN_BEGIN(ApiController)

namespace varlor::controllers {

namespace {

using varlor::models::Dataset;
using varlor::models::DataPoint;
using varlor::models::FieldValue;
using varlor::models::PreprocessingResult;
using varlor::models::PreprocessingReport;
using varlor::models::MetaInfo;
using varlor::core::IndicatorEngine;

std::string toStdString(const oatpp::String& value) {
    if (!value) {
        return {};
    }
    return std::string(*value);
}

enum class BodyFormat {
    Json,
    Yaml
};

enum class ResponseFormat {
    Json,
    Yaml
};

class RequestValidationError final : public std::runtime_error {
public:
    explicit RequestValidationError(const std::string& message)
        : std::runtime_error(message) {}
};

class BadRequestError final : public std::runtime_error {
public:
    explicit BadRequestError(const std::string& message)
        : std::runtime_error(message) {}
};

struct ParsedRequest {
    std::string origin;
    std::string declaredContentType;
    bool autodetect{false};
    std::optional<double> outlierMultiplier;
    Dataset dataset;
    std::vector<varlor::models::OperationDefinition> operations;
};

struct ColumnCollector {
    void registerColumn(const std::string& name) {
        if (name.empty()) {
            throw RequestValidationError("Le nom d'une colonne ne peut pas être vide.");
        }
        if (observed_.insert(name).second) {
            ordered_.push_back(name);
        }
    }

    [[nodiscard]] const std::vector<std::string>& ordered() const noexcept {
        return ordered_;
    }

private:
    std::vector<std::string> ordered_;
    std::unordered_set<std::string> observed_;
};

constexpr std::string_view kMimeJson{"application/json"};
constexpr std::string_view kMimeYamlPrimary{"application/x-yaml"};
constexpr std::string_view kMimeYamlAlt{"application/yaml"};
constexpr std::string_view kMimeYamlText{"text/yaml"};

std::string trimCopy(const std::string& value) {
    const auto first = value.find_first_not_of(" \t\r\n");
    if (first == std::string::npos) {
        return {};
    }
    const auto last = value.find_last_not_of(" \t\r\n");
    return value.substr(first, last - first + 1U);
}

std::string toLowerCopy(std::string value) {
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char c) {
        return static_cast<char>(std::tolower(c));
    });
    return value;
}

std::string normalizeMime(const std::string& headerValue) {
    if (headerValue.empty()) {
        return {};
    }
    const auto separator = headerValue.find(';');
    const auto raw = (separator == std::string::npos)
        ? headerValue
        : headerValue.substr(0, separator);
    return toLowerCopy(trimCopy(raw));
}

bool isYamlMime(const std::string& mime) {
    return mime == kMimeYamlPrimary
        || mime == kMimeYamlAlt
        || mime == kMimeYamlText;
}

BodyFormat detectBodyFormat(const std::string& normalizedMime) {
    if (normalizedMime == kMimeJson) {
        return BodyFormat::Json;
    }
    if (isYamlMime(normalizedMime)) {
        return BodyFormat::Yaml;
    }
    throw RequestValidationError("Content-Type non supporté : " + normalizedMime);
}

ResponseFormat selectResponseFormat(const oatpp::String& acceptHeader) {
    if (acceptHeader == nullptr) {
        return ResponseFormat::Json;
    }
    const std::string lowered = toLowerCopy(toStdString(acceptHeader));
    if (lowered.find(kMimeYamlPrimary.data()) != std::string::npos
        || lowered.find(kMimeYamlAlt.data()) != std::string::npos
        || lowered.find(kMimeYamlText.data()) != std::string::npos) {
        return ResponseFormat::Yaml;
    }
    return ResponseFormat::Json;
}

std::string isoTimestampUtc() {
    using clock = std::chrono::system_clock;
    const auto now = clock::now();
    const auto seconds = clock::to_time_t(now);
    const auto remaining = now - clock::from_time_t(seconds);
    const auto millis = std::chrono::duration_cast<std::chrono::milliseconds>(remaining).count();

    std::tm tm{};
#if defined(_WIN32)
    gmtime_s(&tm, &seconds);
#else
    gmtime_r(&seconds, &tm);
#endif

    std::ostringstream stream;
    stream << std::put_time(&tm, "%Y-%m-%dT%H:%M:%S");
    stream << '.' << std::setw(3) << std::setfill('0') << millis << "Z";
    return stream.str();
}

double extractOutlierMultiplier(const std::optional<double>& value) {
    if (!value.has_value()) {
        return 1.5;
    }
    return value.value();
}

bool isTruthyString(const std::string& value) {
    const std::string lowered = toLowerCopy(value);
    return lowered == "true" || lowered == "yes" || lowered == "1" || lowered == "on";
}

bool isFalsyString(const std::string& value) {
    const std::string lowered = toLowerCopy(value);
    return lowered == "false" || lowered == "no" || lowered == "0" || lowered == "off";
}

FieldValue convertYamlNodeToFieldValue(const YAML::Node& node) {
    if (!node || node.IsNull()) {
        return std::nullptr_t{};
    }
    if (node.IsSequence() || node.IsMap()) {
        throw RequestValidationError("Les valeurs imbriquées ne sont pas supportées dans le dataset YAML.");
    }

    const std::string scalar = node.Scalar();
    if (isTruthyString(scalar)) {
        return true;
    }
    if (isFalsyString(scalar)) {
        return false;
    }

    try {
        size_t processed = 0U;
        const double numeric = std::stod(scalar, &processed);
        if (processed == scalar.size()) {
            return numeric;
        }
    } catch (const std::exception&) {
        // Ignoré : la valeur reste une chaîne.
    }

    return scalar;
}

FieldValue convertJsonAnyToFieldValue(const oatpp::Any& anyValue) {
    if (anyValue == nullptr) {
        return std::nullptr_t{};
    }

    const oatpp::Type* storedType = anyValue.getStoredType();
    if (storedType == nullptr) {
        return std::nullptr_t{};
    }

    if (storedType == oatpp::String::Class::getType()) {
        auto str = anyValue.retrieve<oatpp::String>();
        return toStdString(str);
    }
    if (storedType == oatpp::Float64::Class::getType()) {
        const double numeric = static_cast<double>(*anyValue.retrieve<oatpp::Float64>());
        return numeric;
    }
    if (storedType == oatpp::Float32::Class::getType()) {
        const double numeric = static_cast<double>(*anyValue.retrieve<oatpp::Float32>());
        return numeric;
    }
    if (storedType == oatpp::Int64::Class::getType()) {
        const double numeric = static_cast<double>(*anyValue.retrieve<oatpp::Int64>());
        return numeric;
    }
    if (storedType == oatpp::Int32::Class::getType()) {
        const double numeric = static_cast<double>(*anyValue.retrieve<oatpp::Int32>());
        return numeric;
    }
    if (storedType == oatpp::UInt64::Class::getType()) {
        const double numeric = static_cast<double>(*anyValue.retrieve<oatpp::UInt64>());
        return numeric;
    }
    if (storedType == oatpp::UInt32::Class::getType()) {
        const double numeric = static_cast<double>(*anyValue.retrieve<oatpp::UInt32>());
        return numeric;
    }
    if (storedType == oatpp::Boolean::Class::getType()) {
        const bool flag = static_cast<bool>(*anyValue.retrieve<oatpp::Boolean>());
        return flag;
    }

    throw RequestValidationError("Type de valeur JSON non supporté dans le dataset.");
}

oatpp::Any fieldValueToAny(const FieldValue& value) {
    return std::visit([](const auto& stored) -> oatpp::Any {
        using T = std::decay_t<decltype(stored)>;
        if constexpr (std::is_same_v<T, double>) {
            return oatpp::Any(oatpp::Float64(stored));
        } else if constexpr (std::is_same_v<T, std::string>) {
            return oatpp::Any(oatpp::String(stored));
        } else if constexpr (std::is_same_v<T, bool>) {
            return oatpp::Any(oatpp::Boolean(stored));
        } else {
            return oatpp::Any(nullptr);
        }
    }, value);
}

YAML::Node fieldValueToYaml(const FieldValue& value) {
    YAML::Node node;
    std::visit([&node](const auto& stored) {
        using T = std::decay_t<decltype(stored)>;
        if constexpr (std::is_same_v<T, std::nullptr_t>) {
            node = YAML::Node();
            node = YAML::Null;
        } else {
            node = stored;
        }
    }, value);
    return node;
}

oatpp::Any metaToAny(const MetaInfo& meta);
YAML::Node metaToYaml(const MetaInfo& meta);

FieldValue convertYamlMetaLeaf(const YAML::Node& node) {
    return convertYamlNodeToFieldValue(node);
}

MetaInfo buildMetaFromYaml(const YAML::Node& node);
MetaInfo buildMetaFromJson(const oatpp::Fields<oatpp::Any>& fields);

MetaInfo buildMetaFromYaml(const YAML::Node& node) {
    MetaInfo meta;
    if (!node || !node.IsMap()) {
        return meta;
    }
    for (auto it = node.begin(); it != node.end(); ++it) {
        const std::string key = it->first.Scalar();
        const YAML::Node& valueNode = it->second;
        if (valueNode.IsMap()) {
            meta.ensureSection(key) = buildMetaFromYaml(valueNode);
        } else if (valueNode.IsSequence()) {
            throw RequestValidationError("Les séquences ne sont pas supportées dans `_meta` pour YAML.");
        } else if (valueNode.IsNull()) {
            meta.setLeaf(key, std::nullptr_t{});
        } else {
            meta.setLeaf(key, convertYamlMetaLeaf(valueNode));
        }
    }
    return meta;
}

MetaInfo buildMetaFromJson(const oatpp::Fields<oatpp::Any>& fields) {
    MetaInfo meta;
    for (const auto& entry : *fields) {
        if (!entry.first) {
            throw RequestValidationError("Clé invalide dans `_meta` JSON.");
        }
        const std::string key = toStdString(entry.first);
        const oatpp::Any& anyValue = entry.second;
        if (anyValue == nullptr) {
            meta.setLeaf(key, std::nullptr_t{});
            continue;
        }

        const oatpp::Type* storedType = anyValue.getStoredType();
        if (storedType == oatpp::Fields<oatpp::Any>::Class::getType()) {
            auto nested = anyValue.retrieve<oatpp::Fields<oatpp::Any>>();
            meta.ensureSection(key) = buildMetaFromJson(nested);
        } else {
            meta.setLeaf(key, convertJsonAnyToFieldValue(anyValue));
        }
    }
    return meta;
}

oatpp::Any metaToAny(const MetaInfo& meta) {
    const auto& entries = meta.entries();
    if (entries.empty()) {
        return oatpp::Any(nullptr);
    }

    auto map = oatpp::Fields<oatpp::Any>::createShared();
    for (const auto& pair : entries) {
        const auto& key = pair.first;
        const auto& value = pair.second;
        oatpp::Any mapped;
        if (std::holds_alternative<MetaInfo::LeafValue>(value)) {
            mapped = fieldValueToAny(std::get<MetaInfo::LeafValue>(value));
        } else {
            const auto& section = std::get<std::shared_ptr<MetaInfo>>(value);
            mapped = (section != nullptr) ? metaToAny(*section) : oatpp::Any(nullptr);
        }
        map->push_back({oatpp::String(key), mapped});
    }
    return oatpp::Any(map);
}

YAML::Node metaToYaml(const MetaInfo& meta) {
    YAML::Node node(YAML::NodeType::Map);
    const auto& entries = meta.entries();
    if (entries.empty()) {
        return YAML::Node();
    }
    for (const auto& pair : entries) {
        const auto& key = pair.first;
        const auto& value = pair.second;
        if (std::holds_alternative<MetaInfo::LeafValue>(value)) {
            node[key] = fieldValueToYaml(std::get<MetaInfo::LeafValue>(value));
        } else {
            const auto& section = std::get<std::shared_ptr<MetaInfo>>(value);
            if (section) {
                node[key] = metaToYaml(*section);
            } else {
                node[key] = YAML::Node();
                node[key] = YAML::Null;
            }
        }
    }
    return node;
}

Dataset buildDatasetFromYaml(const YAML::Node& dataNode) {
    if (!dataNode || !dataNode.IsSequence()) {
        throw RequestValidationError("Le champ `data` doit être une séquence YAML.");
    }

    Dataset dataset;
    ColumnCollector columns;

    for (const YAML::Node& rowNode : dataNode) {
        if (!rowNode.IsMap()) {
            throw RequestValidationError("Chaque enregistrement YAML doit être un mapping.");
        }

        DataPoint point;
        for (auto it = rowNode.begin(); it != rowNode.end(); ++it) {
            const std::string key = it->first.Scalar();
            if (key == "_meta") {
                point.getMeta() = buildMetaFromYaml(it->second);
                continue;
            }
            columns.registerColumn(key);
            point.setField(key, convertYamlNodeToFieldValue(it->second));
        }
        dataset.addDataPoint(std::move(point));
    }

    dataset.setColumnNames(columns.ordered());
    return dataset;
}

Dataset buildDatasetFromJson(const oatpp::Any& dataAny) {
    if (dataAny == nullptr) {
        throw RequestValidationError("Le champ `data` ne peut pas être nul.");
    }

    oatpp::List<oatpp::Any> rows;
    try {
        rows = dataAny.retrieve<oatpp::List<oatpp::Any>>();
    } catch (const std::exception&) {
        throw RequestValidationError("Le champ `data` doit être un tableau JSON.");
    }

    Dataset dataset;
    ColumnCollector columns;

    for (const oatpp::Any& rowAny : *rows) {
        if (rowAny == nullptr) {
            throw RequestValidationError("Une ligne du dataset JSON est nulle.");
        }

        oatpp::Fields<oatpp::Any> fields;
        try {
            fields = rowAny.retrieve<oatpp::Fields<oatpp::Any>>();
        } catch (const std::exception&) {
            throw RequestValidationError("Chaque élément de `data` doit être un objet JSON.");
        }

        DataPoint point;
        for (const auto& entry : *fields) {
            if (!entry.first) {
                throw RequestValidationError("Nom de champ JSON invalide.");
            }
            const std::string key = toStdString(entry.first);
            if (key == "_meta") {
                const oatpp::Type* valueType = entry.second.getStoredType();
                if (valueType != oatpp::Fields<oatpp::Any>::Class::getType()) {
                    throw RequestValidationError("Le champ `_meta` doit être un objet JSON.");
                }
                auto metaFields = entry.second.retrieve<oatpp::Fields<oatpp::Any>>();
                point.getMeta() = buildMetaFromJson(metaFields);
                continue;
            }
            columns.registerColumn(key);
            point.setField(key, convertJsonAnyToFieldValue(entry.second));
        }
        dataset.addDataPoint(std::move(point));
    }

    dataset.setColumnNames(columns.ordered());
    return dataset;
}

std::vector<varlor::models::OperationDefinition> parseOperationsFromJson(
    const oatpp::List<oatpp::Object<AnalysisOperationDefinitionDto>>& operationsDto) {
    std::vector<varlor::models::OperationDefinition> operations;
    if (!operationsDto) {
        return operations;
    }

    operations.reserve(operationsDto->size());
    for (const auto& opDto : *operationsDto) {
        if (!opDto || !opDto->expr) {
            throw RequestValidationError("Chaque opération doit contenir le champ `expr`.");
        }

        varlor::models::OperationDefinition operation;
        operation.expr = trimCopy(toStdString(opDto->expr));
        if (operation.expr.empty()) {
            throw RequestValidationError("Le champ `expr` d'une opération ne peut pas être vide.");
        }

        if (opDto->alias && !opDto->alias->empty()) {
            const auto alias = trimCopy(toStdString(opDto->alias));
            if (!alias.empty()) {
                operation.alias = alias;
            }
        }

        if (opDto->params && !opDto->params->empty()) {
            std::unordered_map<std::string, std::string> params;
            for (const auto& entry : *opDto->params) {
                if (!entry.first) {
                    throw RequestValidationError("Clé de paramètre invalide dans `operations.params`.");
                }
                params.emplace(toStdString(entry.first), toStdString(entry.second));
            }
            if (!params.empty()) {
                operation.params = std::move(params);
            }
        }

        operations.push_back(std::move(operation));
    }

    return operations;
}

std::vector<varlor::models::OperationDefinition> parseOperationsFromYaml(const YAML::Node& root) {
    std::vector<varlor::models::OperationDefinition> operations;
    const YAML::Node operationsNode = root["operations"];
    if (!operationsNode) {
        return operations;
    }

    if (!operationsNode.IsSequence()) {
        throw RequestValidationError("Le champ `operations` doit être une séquence YAML.");
    }

    for (const auto& item : operationsNode) {
        if (!item.IsMap()) {
            throw RequestValidationError("Chaque entrée de `operations` doit être un mapping.");
        }

        const YAML::Node exprNode = item["expr"];
        if (!exprNode || exprNode.IsNull()) {
            throw RequestValidationError("Chaque opération doit contenir le champ `expr`.");
        }

        varlor::models::OperationDefinition operation;
        operation.expr = trimCopy(exprNode.as<std::string>());
        if (operation.expr.empty()) {
            throw RequestValidationError("Le champ `expr` d'une opération ne peut pas être vide.");
        }

        if (const YAML::Node aliasNode = item["alias"]; aliasNode && !aliasNode.IsNull()) {
            const auto alias = trimCopy(aliasNode.as<std::string>());
            if (!alias.empty()) {
                operation.alias = alias;
            }
        }

        if (const YAML::Node paramsNode = item["params"]; paramsNode && paramsNode.IsMap()) {
            std::unordered_map<std::string, std::string> params;
            for (auto it = paramsNode.begin(); it != paramsNode.end(); ++it) {
                params.emplace(it->first.Scalar(), it->second.Scalar());
            }
            if (!params.empty()) {
                operation.params = std::move(params);
            }
        }

        operations.push_back(std::move(operation));
    }

    return operations;
}

ParsedRequest parseJsonRequest(const oatpp::String& body, const std::shared_ptr<oatpp::parser::json::mapping::ObjectMapper>& mapper) {
    oatpp::Object<AnalysisPreprocessRequestDto> requestDto;
    try {
        requestDto = mapper->readFromString<oatpp::Object<AnalysisPreprocessRequestDto>>(body);
    } catch (const std::exception& ex) {
        throw BadRequestError(std::string("Le corps JSON est invalide : ") + ex.what());
    }
    if (!requestDto) {
        throw BadRequestError("Le corps JSON est invalide ou vide.");
    }

    if (!requestDto->data_descriptor) {
        throw RequestValidationError("Le champ `data_descriptor` est obligatoire.");
    }

    ParsedRequest request;
    request.origin = requestDto->data_descriptor->origin ? toStdString(requestDto->data_descriptor->origin) : std::string{};
    request.declaredContentType = requestDto->data_descriptor->content_type ? toStdString(requestDto->data_descriptor->content_type) : std::string{};
    request.autodetect = requestDto->data_descriptor->autodetect ? static_cast<bool>(*requestDto->data_descriptor->autodetect) : false;

    if (request.origin.empty()) {
        throw RequestValidationError("Le champ `data_descriptor.origin` est obligatoire.");
    }

    if (requestDto->options && requestDto->options->drop_outliers_percent && requestDto->options->drop_outliers_percent.get() != nullptr) {
        request.outlierMultiplier = static_cast<double>(*requestDto->options->drop_outliers_percent);
    }

    if (requestDto->data == nullptr) {
        throw RequestValidationError("Le champ `data` est obligatoire.");
    }

    request.dataset = buildDatasetFromJson(requestDto->data);
    request.operations = parseOperationsFromJson(requestDto->operations);
    return request;
}

ParsedRequest parseYamlRequest(const oatpp::String& body) {
    const std::string payload = toStdString(body);
    YAML::Node root;
    try {
        root = YAML::Load(payload);
    } catch (const YAML::ParserException& ex) {
        throw BadRequestError(std::string("Le corps YAML est invalide : ") + ex.what());
    }

    if (!root || !root.IsMap()) {
        throw BadRequestError("Le corps YAML doit être un mapping.");
    }

    ParsedRequest request;
    const YAML::Node descriptor = root["data_descriptor"];
    if (!descriptor || !descriptor.IsMap()) {
        throw RequestValidationError("Le champ `data_descriptor` est obligatoire dans la requête YAML.");
    }

    request.origin = descriptor["origin"] ? descriptor["origin"].Scalar() : std::string{};
    request.declaredContentType = descriptor["content_type"] ? descriptor["content_type"].Scalar() : std::string{};
    request.autodetect = descriptor["autodetect"] ? isTruthyString(descriptor["autodetect"].Scalar()) : false;

    if (request.origin.empty()) {
        throw RequestValidationError("Le champ `data_descriptor.origin` est obligatoire.");
    }

    const YAML::Node options = root["options"];
    if (options && options.IsMap()) {
        const YAML::Node dropNode = options["drop_outliers_percent"];
        if (dropNode && !dropNode.IsNull()) {
            try {
                request.outlierMultiplier = dropNode.as<double>();
            } catch (const YAML::BadConversion&) {
                throw RequestValidationError("`drop_outliers_percent` doit être un nombre.");
            }
        }
    }

    request.dataset = buildDatasetFromYaml(root["data"]);
    request.operations = parseOperationsFromYaml(root);
    return request;
}

oatpp::Object<AnalysisDatasetDto> datasetToDto(const Dataset& dataset) {
    auto dto = AnalysisDatasetDto::createShared();

    auto columns = oatpp::List<oatpp::String>::createShared();
    for (const auto& column : dataset.getColumnNames()) {
        columns->push_back(oatpp::String(column));
    }
    dto->columns = columns;

    auto rows = oatpp::List<oatpp::Object<AnalysisDataPointDto>>::createShared();
    for (const auto& point : dataset.getDataPoints()) {
        auto rowDto = AnalysisDataPointDto::createShared();
        auto values = oatpp::Fields<oatpp::Any>::createShared();
        for (const auto& field : point.getFields()) {
            values->push_back({oatpp::String(field.first), fieldValueToAny(field.second)});
        }
        rowDto->values = values;
        rowDto->_meta = metaToAny(point.getMeta());
        rows->push_back(rowDto);
    }
    dto->rows = rows;

    return dto;
}

std::string operationStatusToString(varlor::models::OperationStatus status) {
    return status == varlor::models::OperationStatus::Success ? "success" : "error";
}

oatpp::Any operationResultValueToAny(const varlor::models::OperationResult& result) {
    if (std::holds_alternative<double>(result.result)) {
        return oatpp::Any(oatpp::Float64(std::get<double>(result.result)));
    }
    if (std::holds_alternative<std::vector<double>>(result.result)) {
        auto list = oatpp::List<oatpp::Float64>::createShared();
        for (double value : std::get<std::vector<double>>(result.result)) {
            list->push_back(oatpp::Float64(value));
        }
        return oatpp::Any(list);
    }
    return oatpp::Any(nullptr);
}

oatpp::List<oatpp::Object<AnalysisOperationResultDto>> operationsResultToDto(
    const std::vector<varlor::models::OperationResult>& operations) {
    if (operations.empty()) {
        return nullptr;
    }

    auto list = oatpp::List<oatpp::Object<AnalysisOperationResultDto>>::createShared();
    for (const auto& operation : operations) {
        auto dto = AnalysisOperationResultDto::createShared();
        dto->expr = oatpp::String(operation.expr);
        dto->status = oatpp::String(operationStatusToString(operation.status));
        dto->result = operationResultValueToAny(operation);
        if (operation.errorMessage.has_value()) {
            dto->error_message = oatpp::String(operation.errorMessage.value());
        }
        dto->executed_at = oatpp::String(operation.executedAt);
        list->push_back(dto);
    }
    return list;
}

YAML::Node operationsResultToYaml(const std::vector<varlor::models::OperationResult>& operations) {
    YAML::Node node(YAML::NodeType::Sequence);
    for (const auto& operation : operations) {
        YAML::Node entry;
        entry["expr"] = operation.expr;
        entry["status"] = operationStatusToString(operation.status);

        if (std::holds_alternative<double>(operation.result)) {
            entry["result"] = std::get<double>(operation.result);
        } else if (std::holds_alternative<std::vector<double>>(operation.result)) {
            YAML::Node values(YAML::NodeType::Sequence);
            for (double value : std::get<std::vector<double>>(operation.result)) {
                values.push_back(value);
            }
            entry["result"] = values;
        } else {
            entry["result"] = YAML::Node();
            entry["result"] = YAML::Null;
        }

        if (operation.errorMessage.has_value()) {
            entry["error_message"] = operation.errorMessage.value();
        }

        entry["executed_at"] = operation.executedAt;
        node.push_back(entry);
    }
    return node;
}

oatpp::Object<AnalysisPreprocessingReportDto> reportToDto(const PreprocessingReport& report) {
    auto dto = AnalysisPreprocessingReportDto::createShared();
    dto->input_row_count = oatpp::Int64(static_cast<v_int64>(report.getInputRowCount()));
    dto->output_row_count = oatpp::Int64(static_cast<v_int64>(report.getOutputRowCount()));
    dto->outliers_removed = oatpp::Int64(static_cast<v_int64>(report.getOutliersRemoved()));
    dto->missing_values_replaced = oatpp::Int64(static_cast<v_int64>(report.getMissingValuesReplaced()));

    auto normalized = oatpp::List<oatpp::String>::createShared();
    for (const auto& name : report.getNormalizedFields()) {
        normalized->push_back(oatpp::String(name));
    }
    dto->normalized_fields = normalized;
    return dto;
}

oatpp::Object<AnalysisPreprocessResponseDto> resultToDto(
    const PreprocessingResult& result,
    const std::vector<varlor::models::OperationResult>& operations) {
    auto dto = AnalysisPreprocessResponseDto::createShared();
    dto->cleaned_dataset = datasetToDto(result.cleanedDataset);
    dto->outliers_dataset = datasetToDto(result.outliersDataset);
    dto->report = reportToDto(result.report);
    if (auto list = operationsResultToDto(operations); list) {
        dto->operation_results = list;
    }
    return dto;
}

YAML::Node datasetToYaml(const Dataset& dataset) {
    YAML::Node node;
    YAML::Node columns(YAML::NodeType::Sequence);
    for (const auto& column : dataset.getColumnNames()) {
        columns.push_back(column);
    }
    node["columns"] = columns;

    YAML::Node rows(YAML::NodeType::Sequence);
    for (const auto& point : dataset.getDataPoints()) {
        YAML::Node rowNode;
        YAML::Node values(YAML::NodeType::Map);
        for (const auto& field : point.getFields()) {
            values[field.first] = fieldValueToYaml(field.second);
        }
        if (!values.IsNull() && values.size() > 0) {
            rowNode["values"] = values;
        }
        const auto metaNode = metaToYaml(point.getMeta());
        if (!metaNode.IsNull() && metaNode.size() > 0) {
            rowNode["_meta"] = metaNode;
        }
        rows.push_back(rowNode);
    }
    node["rows"] = rows;
    return node;
}

YAML::Node responseToYaml(
    const PreprocessingResult& result,
    const std::vector<varlor::models::OperationResult>& operations) {
    YAML::Node root;
    root["report"]["input_row_count"] = static_cast<long long>(result.report.getInputRowCount());
    root["report"]["output_row_count"] = static_cast<long long>(result.report.getOutputRowCount());
    root["report"]["outliers_removed"] = static_cast<long long>(result.report.getOutliersRemoved());
    root["report"]["missing_values_replaced"] = static_cast<long long>(result.report.getMissingValuesReplaced());

    YAML::Node normalized(YAML::NodeType::Sequence);
    for (const auto& name : result.report.getNormalizedFields()) {
        normalized.push_back(name);
    }
    root["report"]["normalized_fields"] = normalized;

    root["cleaned_dataset"] = datasetToYaml(result.cleanedDataset);
    root["outliers_dataset"] = datasetToYaml(result.outliersDataset);
    if (!operations.empty()) {
        root["operation_results"] = operationsResultToYaml(operations);
    }
    return root;
}

YAML::Node errorToYaml(const std::string& code, const std::string& details, const std::string& timestamp) {
    YAML::Node node;
    node["error"] = code;
    node["details"] = details;
    node["timestamp"] = timestamp;
    return node;
}

} // namespace

AnalysisController::AnalysisController(const std::shared_ptr<oatpp::parser::json::mapping::ObjectMapper>& jsonMapper)
    : oatpp::web::server::api::ApiController(jsonMapper) {}

std::shared_ptr<AnalysisController> AnalysisController::createShared(const std::shared_ptr<oatpp::parser::json::mapping::ObjectMapper>& jsonMapper) {
    return std::make_shared<AnalysisController>(jsonMapper);
}

std::shared_ptr<AnalysisController::OutgoingResponse> AnalysisController::handlePreprocess(const std::shared_ptr<IncomingRequest>& request) {
    const auto contentTypeHeader = request->getHeader(oatpp::web::protocol::http::Header::CONTENT_TYPE);
    if (contentTypeHeader == nullptr || contentTypeHeader->empty()) {
        auto response = AnalysisErrorResponseDto::createShared();
        response->error = "invalid_request";
        response->details = "Le header Content-Type est obligatoire.";
        response->timestamp = isoTimestampUtc();
        return createDtoResponse(oatpp::web::protocol::http::Status::CODE_400, response);
    }

    const std::string normalizedMime = normalizeMime(toStdString(contentTypeHeader));
    const ResponseFormat responseFormat = selectResponseFormat(request->getHeader(oatpp::web::protocol::http::Header::ACCEPT));

    oatpp::String body;
    try {
        body = request->readBodyToString();
    } catch (const std::exception& ex) {
        auto response = AnalysisErrorResponseDto::createShared();
        response->error = "invalid_request";
        response->details = ex.what();
        response->timestamp = isoTimestampUtc();
        return createDtoResponse(oatpp::web::protocol::http::Status::CODE_400, response);
    }

    if (body == nullptr || body->empty()) {
        auto response = AnalysisErrorResponseDto::createShared();
        response->error = "invalid_request";
        response->details = "Le corps de la requête est vide.";
        response->timestamp = isoTimestampUtc();
        return createDtoResponse(oatpp::web::protocol::http::Status::CODE_400, response);
    }

    try {
        const BodyFormat bodyFormat = detectBodyFormat(normalizedMime);

        ParsedRequest parsed;
        if (bodyFormat == BodyFormat::Json) {
            auto jsonMapper = std::dynamic_pointer_cast<oatpp::parser::json::mapping::ObjectMapper>(getDefaultObjectMapper());
            if (!jsonMapper) {
                throw std::runtime_error("Failed to cast ObjectMapper to JsonObjectMapper.");
            }
            parsed = parseJsonRequest(body, jsonMapper);
        } else {
            parsed = parseYamlRequest(body);
        }

        const std::string actualMime = (bodyFormat == BodyFormat::Json)
            ? std::string(kMimeJson)
            : std::string(kMimeYamlPrimary);

        const std::string declaredNormalized = normalizeMime(parsed.declaredContentType);
        if (!declaredNormalized.empty() && declaredNormalized != actualMime) {
            if (!parsed.autodetect) {
                throw RequestValidationError("`data_descriptor.content_type` ne correspond pas au format du corps.");
            }
        }

        varlor::core::DataPreprocessor preprocessor(extractOutlierMultiplier(parsed.outlierMultiplier));
        PreprocessingResult result = preprocessor.process(parsed.dataset);

        std::vector<varlor::models::OperationResult> operationResults;
        if (!parsed.operations.empty()) {
            IndicatorEngine engine;
            operationResults = engine.execute(result.cleanedDataset, parsed.operations);
        }

        if (responseFormat == ResponseFormat::Yaml) {
            const YAML::Node yaml = responseToYaml(result, operationResults);
            const std::string serialized = YAML::Dump(yaml);
            auto response = createResponse(oatpp::web::protocol::http::Status::CODE_200, oatpp::String(serialized));
            response->putHeader(oatpp::web::protocol::http::Header::CONTENT_TYPE, std::string(kMimeYamlPrimary));
            return response;
        }

        auto dto = resultToDto(result, operationResults);
        auto response = createDtoResponse(oatpp::web::protocol::http::Status::CODE_200, dto);
        response->putHeader(oatpp::web::protocol::http::Header::CONTENT_TYPE, std::string(kMimeJson));
        return response;
    } catch (const BadRequestError& badRequest) {
        const std::string timestamp = isoTimestampUtc();
        const std::string details = badRequest.what();
        if (responseFormat == ResponseFormat::Yaml) {
            const std::string serialized = YAML::Dump(errorToYaml("invalid_request", details, timestamp));
            auto response = createResponse(oatpp::web::protocol::http::Status::CODE_400, oatpp::String(serialized));
            response->putHeader(oatpp::web::protocol::http::Header::CONTENT_TYPE, std::string(kMimeYamlPrimary));
            return response;
        }
        auto dto = AnalysisErrorResponseDto::createShared();
        dto->error = "invalid_request";
        dto->details = details;
        dto->timestamp = timestamp;
        auto response = createDtoResponse(oatpp::web::protocol::http::Status::CODE_400, dto);
        response->putHeader(oatpp::web::protocol::http::Header::CONTENT_TYPE, std::string(kMimeJson));
        return response;
    } catch (const RequestValidationError& validationError) {
        const std::string timestamp = isoTimestampUtc();
        const std::string details = validationError.what();
        if (responseFormat == ResponseFormat::Yaml) {
            const std::string serialized = YAML::Dump(errorToYaml("unprocessable_entity", details, timestamp));
            auto response = createResponse(oatpp::web::protocol::http::Status::CODE_422, oatpp::String(serialized));
            response->putHeader(oatpp::web::protocol::http::Header::CONTENT_TYPE, std::string(kMimeYamlPrimary));
            return response;
        }
        auto dto = AnalysisErrorResponseDto::createShared();
        dto->error = "unprocessable_entity";
        dto->details = details;
        dto->timestamp = timestamp;
        auto response = createDtoResponse(oatpp::web::protocol::http::Status::CODE_422, dto);
        response->putHeader(oatpp::web::protocol::http::Header::CONTENT_TYPE, std::string(kMimeJson));
        return response;
    } catch (const std::exception& ex) {
        const std::string timestamp = isoTimestampUtc();
        const std::string details = ex.what();
        if (responseFormat == ResponseFormat::Yaml) {
            const std::string serialized = YAML::Dump(errorToYaml("internal_error", details, timestamp));
            auto response = createResponse(oatpp::web::protocol::http::Status::CODE_500, oatpp::String(serialized));
            response->putHeader(oatpp::web::protocol::http::Header::CONTENT_TYPE, std::string(kMimeYamlPrimary));
            return response;
        }
        auto dto = AnalysisErrorResponseDto::createShared();
        dto->error = "internal_error";
        dto->details = details;
        dto->timestamp = timestamp;
        auto response = createDtoResponse(oatpp::web::protocol::http::Status::CODE_500, dto);
        response->putHeader(oatpp::web::protocol::http::Header::CONTENT_TYPE, std::string(kMimeJson));
        return response;
    }
}

} // namespace varlor::controllers

#include OATPP_CODEGEN_END(ApiController)
