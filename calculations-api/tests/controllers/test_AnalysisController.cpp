#include <catch2/catch_test_macros.hpp>
#include <catch2/catch_approx.hpp>

#include "../../Controllers/AnalysisController.hpp"

#include "oatpp/core/base/Environment.hpp"
#include "oatpp/core/data/stream/BufferStream.hpp"
#include "oatpp/parser/json/mapping/ObjectMapper.hpp"
#include "oatpp/web/protocol/http/Http.hpp"
#include "oatpp/web/protocol/http/incoming/Request.hpp"
#include "oatpp/web/protocol/http/incoming/SimpleBodyDecoder.hpp"
#include "oatpp/web/protocol/http/outgoing/Response.hpp"

#include <yaml-cpp/yaml.h>

#include <optional>
#include <string>
#include <iostream>

namespace {

struct GlobalOatppEnvironment {
    GlobalOatppEnvironment() {
        oatpp::base::Environment::init();
    }

    ~GlobalOatppEnvironment() {
        oatpp::base::Environment::destroy();
    }
};

static GlobalOatppEnvironment oatppEnvGuard;

std::shared_ptr<oatpp::web::protocol::http::incoming::Request> makeRequest(
    const std::string& body,
    const std::string& contentType,
    const std::optional<std::string>& accept) {
    using namespace oatpp::web::protocol::http;

    Headers headers;
    auto bodyStream = std::make_shared<oatpp::data::stream::BufferInputStream>(oatpp::String(body.c_str()));

    RequestStartingLine startingLine;
    startingLine.method = oatpp::data::share::StringKeyLabel("POST");
    startingLine.path = oatpp::data::share::StringKeyLabel("/api/analyses/preprocess");
    startingLine.protocol = oatpp::data::share::StringKeyLabel("HTTP/1.1");

    auto decoder = std::make_shared<oatpp::web::protocol::http::incoming::SimpleBodyDecoder>();

    auto request = oatpp::web::protocol::http::incoming::Request::createShared(
        nullptr,
        startingLine,
        headers,
        bodyStream,
        decoder);

    request->putHeader(oatpp::String("Content-Type"), oatpp::String(contentType.c_str()));
    if (accept.has_value()) {
        request->putHeader(oatpp::String("Accept"), oatpp::String(accept->c_str()));
    }
    const auto lengthStr = std::to_string(body.size());
    request->putHeader(oatpp::String("Content-Length"), oatpp::String(lengthStr.c_str()));

    return request;
}

std::shared_ptr<oatpp::web::protocol::http::outgoing::Response> execute(
    const std::shared_ptr<varlor::controllers::AnalysisController>& controller,
    const std::string& body,
    const std::string& contentType,
    const std::optional<std::string>& accept = std::nullopt) {
    return controller->preprocess(makeRequest(body, contentType, accept));
}

oatpp::String readBody(const std::shared_ptr<oatpp::web::protocol::http::outgoing::Response>& response) {
    oatpp::data::stream::BufferOutputStream headersBuffer;
    oatpp::data::stream::BufferOutputStream bodyBuffer;
    response->send(&bodyBuffer, &headersBuffer, nullptr);
    auto raw = bodyBuffer.toString();
    if (!raw) {
        return nullptr;
    }
    std::string rawStr = *raw;
    auto separator = rawStr.find("\r\n\r\n");
    if (separator == std::string::npos) {
        separator = rawStr.find("\n\n");
        if (separator != std::string::npos) {
            separator += 2;
        }
    } else {
        separator += 4;
    }
    if (separator != std::string::npos) {
        rawStr = rawStr.substr(separator);
    }
    return oatpp::String(rawStr.c_str());
}

const char* kValidJsonPayload = "{\n"
"  \"data_descriptor\": {\n"
"    \"origin\": \"inline\",\n"
"    \"content_type\": \"application/json\",\n"
"    \"autodetect\": false\n"
"  },\n"
"  \"options\": {\n"
"    \"drop_outliers_percent\": 1.5\n"
"  },\n"
"  \"data\": [\n"
"    { \"value\": 10.0, \"flag\": true },\n"
"    { \"value\": 12.0, \"flag\": false },\n"
"    { \"value\": 11.0, \"flag\": true }\n"
"  ]\n"
"}";

const char* kJsonWithOperations = "{\n"
"  \"data_descriptor\": {\n"
"    \"origin\": \"inline\",\n"
"    \"content_type\": \"application/json\",\n"
"    \"autodetect\": false\n"
"  },\n"
"  \"data\": [\n"
"    { \"price\": 10.0, \"clicks\": 100.0 },\n"
"    { \"price\": 20.0, \"clicks\": 200.0 }\n"
"  ],\n"
"  \"operations\": [\n"
"    { \"expr\": \"mean(price)\", \"alias\": \"avg_price\" },\n"
"    { \"expr\": \"price * clicks / 10\" }\n"
"  ]\n"
"}";

const auto kValidYamlPayload = R"YAML(data_descriptor:
  origin: inline
  content_type: application/x-yaml
  autodetect: false
options:
  drop_outliers_percent: 1.5
data:
  - value: 10.0
    flag: true
  - value: 12.0
    flag: false
  - value: 11.0
    flag: true
)YAML";

const char* kMismatchContentTypeJson = "{\n"
"  \"data_descriptor\": {\n"
"    \"origin\": \"inline\",\n"
"    \"content_type\": \"text/csv\",\n"
"    \"autodetect\": false\n"
"  },\n"
"  \"options\": {\n"
"    \"drop_outliers_percent\": 1.5\n"
"  },\n"
"  \"data\": [\n"
"    { \"value\": 10.0, \"flag\": true }\n"
"  ]\n"
"}";

const char* kInvalidJsonPayload = "{\n"
"  \"data_descriptor\": {\n"
"    \"origin\": \"inline\",\n"
"    \"content_type\": \"application/json\"\n"
"  },\n"
"  \"options\": {\n"
"    \"drop_outliers_percent\": 1.5\n"
"  },\n"
"  \"data\": [ { \"value\": 10.0, \"flag\": true }\n";

const char* kInvalidMultiplierJson = "{\n"
"  \"data_descriptor\": {\n"
"    \"origin\": \"inline\",\n"
"    \"content_type\": \"application/json\",\n"
"    \"autodetect\": false\n"
"  },\n"
"  \"options\": {\n"
"    \"drop_outliers_percent\": 0.0\n"
"  },\n"
"  \"data\": [\n"
"    { \"value\": 10.0, \"flag\": true },\n"
"    { \"value\": 12.0, \"flag\": false }\n"
"  ]\n"
"}";

const char* kEmptyDatasetJson = "{\n"
"  \"data_descriptor\": {\n"
"    \"origin\": \"inline\",\n"
"    \"content_type\": \"application/json\",\n"
"    \"autodetect\": false\n"
"  },\n"
"  \"options\": {\n"
"    \"drop_outliers_percent\": 1.5\n"
"  },\n"
"  \"data\": []\n"
"}";

} // namespace

TEST_CASE("POST /api/analyses/preprocess - JSON succès", "[AnalysisController][Integration]") {
    auto jsonMapper = oatpp::parser::json::mapping::ObjectMapper::createShared();
    auto controller = varlor::controllers::AnalysisController::createShared(jsonMapper);

    auto response = execute(controller, kValidJsonPayload, "application/json", "application/json");
    REQUIRE(response);
    const auto statusCode = response->getStatus().code;
    auto contentType = response->getHeaders().get(oatpp::web::protocol::http::Header::CONTENT_TYPE);
    auto body = readBody(response);
    REQUIRE(body);
    const std::string bodyStr = *body;
    CAPTURE(statusCode, bodyStr);
    if (contentType) {
        CAPTURE(contentType->c_str());
    }
    std::cerr << "[integration][json-success] status=" << statusCode << " body=" << bodyStr << std::endl;

    REQUIRE(statusCode == oatpp::web::protocol::http::Status::CODE_200.code);
    REQUIRE(contentType);
    REQUIRE(contentType->find("application/json") != std::string::npos);

    auto dto = jsonMapper->readFromString<oatpp::Object<varlor::controllers::AnalysisPreprocessResponseDto>>(oatpp::String(bodyStr.c_str()));
    REQUIRE(dto);
    REQUIRE(dto->cleaned_dataset);
    REQUIRE(dto->outliers_dataset);
    REQUIRE(dto->report);

    const auto inputRows = dto->report->input_row_count ? static_cast<std::int64_t>(*dto->report->input_row_count) : 0;
    const auto outputRows = dto->report->output_row_count ? static_cast<std::int64_t>(*dto->report->output_row_count) : 0;

    const auto cleanedRows = dto->cleaned_dataset->rows ? static_cast<std::int64_t>((*dto->cleaned_dataset->rows).size()) : 0;
    const auto outlierRows = dto->outliers_dataset->rows ? static_cast<std::int64_t>((*dto->outliers_dataset->rows).size()) : 0;

    REQUIRE(outputRows == cleanedRows);
    REQUIRE(inputRows == cleanedRows + outlierRows);
    REQUIRE(inputRows == 3);
}

TEST_CASE("POST /api/analyses/preprocess - opérations custom", "[AnalysisController][Integration]") {
    auto jsonMapper = oatpp::parser::json::mapping::ObjectMapper::createShared();
    auto controller = varlor::controllers::AnalysisController::createShared(jsonMapper);

    auto response = execute(controller, kJsonWithOperations, "application/json", "application/json");
    REQUIRE(response);
    REQUIRE(response->getStatus().code == oatpp::web::protocol::http::Status::CODE_200.code);

    auto body = readBody(response);
    REQUIRE(body);

    auto dto = jsonMapper->readFromString<oatpp::Object<varlor::controllers::AnalysisPreprocessResponseDto>>(body);
    REQUIRE(dto);

    const auto yamlRoot = YAML::Load(body->c_str());
    REQUIRE(yamlRoot["operation_results"]);
    const auto ops = yamlRoot["operation_results"];
    REQUIRE(ops.IsSequence());
    REQUIRE(ops.size() == 2);

    const auto first = ops[0];
    REQUIRE(first["expr"].as<std::string>() == "avg_price");
    REQUIRE(first["status"].as<std::string>() == "success");
    REQUIRE(first["result"].as<double>() == Catch::Approx(15.0));
    REQUIRE_FALSE(first["executed_at"].as<std::string>().empty());
    const auto firstError = first["error_message"];
    const bool firstHasError = firstError && !firstError.IsNull();
    REQUIRE_FALSE(firstHasError);

    const auto second = ops[1];
    REQUIRE(second["expr"].as<std::string>() == "price * clicks / 10");
    REQUIRE(second["status"].as<std::string>() == "success");
    const auto secondError = second["error_message"];
    const bool secondHasError = secondError && !secondError.IsNull();
    REQUIRE_FALSE(secondHasError);
    REQUIRE(second["result"].IsSequence());
    std::vector<double> seriesValues;
    for (const auto& entry : second["result"]) {
        seriesValues.push_back(entry.as<double>());
    }
    REQUIRE(seriesValues == std::vector<double>{100.0, 400.0});
}

TEST_CASE("POST /api/analyses/preprocess - YAML succès", "[AnalysisController][Integration]") {
    auto jsonMapper = oatpp::parser::json::mapping::ObjectMapper::createShared();
    auto controller = varlor::controllers::AnalysisController::createShared(jsonMapper);

    auto response = execute(controller, kValidYamlPayload, "application/x-yaml", "application/x-yaml");
    REQUIRE(response);
    REQUIRE(response->getStatus().code == oatpp::web::protocol::http::Status::CODE_200.code);

    auto contentType = response->getHeaders().get(oatpp::web::protocol::http::Header::CONTENT_TYPE);
    REQUIRE(contentType);
    REQUIRE(contentType->find("application/x-yaml") != std::string::npos);

    auto body = readBody(response);
    REQUIRE(body);

    const auto yaml = YAML::Load(body->c_str());
    REQUIRE(yaml["cleaned_dataset"]);
    REQUIRE(yaml["outliers_dataset"]);
    REQUIRE(yaml["report"]);

    const auto cleanedRowsNode = yaml["cleaned_dataset"]["rows"];
    const auto outlierRowsNode = yaml["outliers_dataset"]["rows"];
    const auto cleanedRows = cleanedRowsNode ? cleanedRowsNode.size() : 0;
    const auto outlierRows = outlierRowsNode ? outlierRowsNode.size() : 0;

    REQUIRE(yaml["report"]["input_row_count"].as<int>() == cleanedRows + outlierRows);
    REQUIRE(yaml["report"]["output_row_count"].as<int>() == cleanedRows);
}

TEST_CASE("POST /api/analyses/preprocess - mismatch content_type", "[AnalysisController][Integration]") {
    auto jsonMapper = oatpp::parser::json::mapping::ObjectMapper::createShared();
    auto controller = varlor::controllers::AnalysisController::createShared(jsonMapper);

    auto response = execute(controller, kMismatchContentTypeJson, "application/json", "application/json");
    REQUIRE(response);
    REQUIRE(response->getStatus().code == oatpp::web::protocol::http::Status::CODE_422.code);

    auto body = readBody(response);
    REQUIRE(body);
    CAPTURE(body->c_str());
    if (response->getStatus().code != oatpp::web::protocol::http::Status::CODE_200.code) {
        auto diagnosticsMapper = oatpp::parser::json::mapping::ObjectMapper::createShared();
        try {
            auto errorDto = diagnosticsMapper->readFromString<oatpp::Object<varlor::controllers::AnalysisErrorResponseDto>>(oatpp::String(body->c_str()));
            if (errorDto && errorDto->error) {
                CAPTURE(errorDto->error->c_str());
            }
        } catch (const std::exception&) {
            // ignore decoding issues for diagnostics
        }
    }
    auto dto = jsonMapper->readFromString<oatpp::Object<varlor::controllers::AnalysisErrorResponseDto>>(body);
    REQUIRE(dto);
    REQUIRE(dto->error);
    REQUIRE(*dto->error == "unprocessable_entity");
}

TEST_CASE("POST /api/analyses/preprocess - format invalide", "[AnalysisController][Integration]") {
    auto jsonMapper = oatpp::parser::json::mapping::ObjectMapper::createShared();
    auto controller = varlor::controllers::AnalysisController::createShared(jsonMapper);

    auto response = execute(controller, kInvalidJsonPayload, "application/json", "application/json");
    REQUIRE(response);
    REQUIRE(response->getStatus().code == oatpp::web::protocol::http::Status::CODE_400.code);

    auto body = readBody(response);
    REQUIRE(body);
    CAPTURE(body->c_str());
    if (response->getStatus().code != oatpp::web::protocol::http::Status::CODE_200.code) {
        auto diagnosticsMapper = oatpp::parser::json::mapping::ObjectMapper::createShared();
        try {
            auto errorDto = diagnosticsMapper->readFromString<oatpp::Object<varlor::controllers::AnalysisErrorResponseDto>>(body);
            if (errorDto && errorDto->error) {
                CAPTURE(errorDto->error->c_str());
            }
        } catch (const std::exception&) {
            // ignore decoding issues for diagnostics
        }
    }
    auto dto = jsonMapper->readFromString<oatpp::Object<varlor::controllers::AnalysisErrorResponseDto>>(body);
    REQUIRE(dto);
    REQUIRE(dto->error);
    REQUIRE(*dto->error == "invalid_request");
}

TEST_CASE("POST /api/analyses/preprocess - erreur interne", "[AnalysisController][Integration]") {
    auto jsonMapper = oatpp::parser::json::mapping::ObjectMapper::createShared();
    auto controller = varlor::controllers::AnalysisController::createShared(jsonMapper);

    auto response = execute(controller, kInvalidMultiplierJson, "application/json", "application/json");
    REQUIRE(response);
    REQUIRE(response->getStatus().code == oatpp::web::protocol::http::Status::CODE_500.code);

    auto body = readBody(response);
    REQUIRE(body);
    CAPTURE(body->c_str());
    if (response->getStatus().code != oatpp::web::protocol::http::Status::CODE_200.code) {
        auto diagnosticsMapper = oatpp::parser::json::mapping::ObjectMapper::createShared();
        try {
            auto errorDto = diagnosticsMapper->readFromString<oatpp::Object<varlor::controllers::AnalysisErrorResponseDto>>(body);
            if (errorDto && errorDto->error) {
                CAPTURE(errorDto->error->c_str());
            }
        } catch (const std::exception&) {
            // ignore decoding issues for diagnostics
        }
    }
    auto dto = jsonMapper->readFromString<oatpp::Object<varlor::controllers::AnalysisErrorResponseDto>>(body);
    REQUIRE(dto);
    REQUIRE(dto->error);
    REQUIRE(*dto->error == "internal_error");
}

TEST_CASE("POST /api/analyses/preprocess - dataset vide", "[AnalysisController][Integration]") {
    auto jsonMapper = oatpp::parser::json::mapping::ObjectMapper::createShared();
    auto controller = varlor::controllers::AnalysisController::createShared(jsonMapper);

    auto response = execute(controller, kEmptyDatasetJson, "application/json");
    REQUIRE(response);
    REQUIRE(response->getStatus().code == oatpp::web::protocol::http::Status::CODE_200.code);

    auto body = readBody(response);
    REQUIRE(body);

    auto dto = jsonMapper->readFromString<oatpp::Object<varlor::controllers::AnalysisPreprocessResponseDto>>(body);
    REQUIRE(dto);
    REQUIRE(dto->report);

    const auto inputRows = dto->report->input_row_count ? static_cast<std::int64_t>(*dto->report->input_row_count) : -1;
    const auto outputRows = dto->report->output_row_count ? static_cast<std::int64_t>(*dto->report->output_row_count) : -1;

    REQUIRE(inputRows == 0);
    REQUIRE(outputRows == 0);
    REQUIRE(dto->cleaned_dataset);
    REQUIRE(dto->cleaned_dataset->rows);
    REQUIRE((*dto->cleaned_dataset->rows).empty());
}
