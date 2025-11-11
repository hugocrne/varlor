#include <catch2/catch_test_macros.hpp>
#include <catch2/catch_approx.hpp>

#include "../../Controllers/AnalysisController.hpp"
#include "../../Core/DataPreprocessor.hpp"

#include "oatpp/base/Environment.hpp"
#include "oatpp/core/macro/component.hpp"
#include "oatpp/parser/json/mapping/ObjectMapper.hpp"
#include "oatpp/network/tcp/server/ConnectionProvider.hpp"
#include "oatpp/network/tcp/client/ConnectionProvider.hpp"
#include "oatpp/web/server/HttpRouter.hpp"
#include "oatpp/web/server/HttpConnectionHandler.hpp"
#include "oatpp/web/client/HttpRequestExecutor.hpp"
#include "oatpp/oatpp-test/web/ClientServerTestRunner.hpp"

#include <yaml-cpp/yaml.h>

#include <string>
#include <utility>

namespace {

struct TestEnvironment {
    TestEnvironment() {
        oatpp::base::Environment::init();
    }

    ~TestEnvironment() {
        oatpp::base::Environment::destroy();
    }

    OATPP_CREATE_COMPONENT(std::shared_ptr<oatpp::parser::json::mapping::ObjectMapper>, jsonObjectMapper)([] {
        auto mapper = oatpp::parser::json::mapping::ObjectMapper::createShared();
        mapper->serializerConfig().useBeautifier = false;
        return mapper;
    }());

    OATPP_CREATE_COMPONENT(std::shared_ptr<oatpp::web::server::HttpRouter>, httpRouter)([] {
        return oatpp::web::server::HttpRouter::createShared();
    }());

    OATPP_CREATE_COMPONENT(std::shared_ptr<oatpp::network::ServerConnectionProvider>, serverConnectionProvider)([] {
        return oatpp::network::tcp::server::ConnectionProvider::createShared(
            {"127.0.0.1", 0, oatpp::network::Address::IP_4});
    }());

    OATPP_CREATE_COMPONENT(std::shared_ptr<oatpp::network::ConnectionHandler>, serverConnectionHandler)([] {
        OATPP_COMPONENT(std::shared_ptr<oatpp::web::server::HttpRouter>, router);
        return oatpp::web::server::HttpConnectionHandler::createShared(router);
    }());
};

TestEnvironment testEnvironment; // NOLINT(cert-err58-cpp)

#include OATPP_CODEGEN_BEGIN(ApiClient)

class AnalysisApiClient : public oatpp::web::client::ApiClient {
    API_CLIENT_INIT(AnalysisApiClient)

public:
    API_CALL("POST", "/api/analyses/preprocess", preprocess,
             BODY_STRING(oatpp::String, body),
             HEADER(oatpp::String, contentType, "Content-Type"),
             HEADER(oatpp::String, accept, "Accept"));
};

#include OATPP_CODEGEN_END(ApiClient)

std::shared_ptr<AnalysisApiClient> createClient() {
    OATPP_COMPONENT(std::shared_ptr<oatpp::network::ServerConnectionProvider>, serverConnectionProvider);
    auto portProperty = serverConnectionProvider->getProperty("port");
    auto portString = portProperty.toString();
    auto port = static_cast<v_uint16>(std::stoul(portString->std_str()));

    auto clientConnectionProvider = oatpp::network::tcp::client::ConnectionProvider::createShared(
        {"127.0.0.1", port, oatpp::network::Address::IP_4});
    auto requestExecutor = oatpp::web::client::HttpRequestExecutor::createShared(clientConnectionProvider);

    OATPP_COMPONENT(std::shared_ptr<oatpp::parser::json::mapping::ObjectMapper>, jsonMapper);
    return AnalysisApiClient::createShared(requestExecutor, jsonMapper);
}

oatpp::String readBodyToString(const std::shared_ptr<oatpp::web::protocol::http::incoming::Response>& response) {
    auto stream = response->readBodyToString();
    return stream ? stream : oatpp::String();
}

std::string validJsonPayload() {
    return R"JSON(
{
  "data_descriptor": {
    "origin": "inline",
    "content_type": "application/json",
    "autodetect": false
  },
  "options": {
    "drop_outliers_percent": 1.5
  },
  "data": [
    { "temperature": 20.5, "status": "nominal" },
    { "temperature": 21.0, "status": "nominal" },
    { "temperature": 19.8, "status": "alert" }
  ]
}
)JSON";
}

std::string validYamlPayload() {
    return R"YAML(
data_descriptor:
  origin: inline
  content_type: application/x-yaml
  autodetect: false
options:
  drop_outliers_percent: 1.5
data:
  - temperature: 20.5
    status: nominal
  - temperature: 21.0
    status: nominal
  - temperature: 19.8
    status: alert
)YAML";
}

std::string mismatchedContentTypePayload() {
    return R"JSON(
{
  "data_descriptor": {
    "origin": "inline",
    "content_type": "text/csv",
    "autodetect": false
  },
  "options": {
    "drop_outliers_percent": 1.5
  },
  "data": [
    { "temperature": 20.5, "status": "nominal" }
  ]
}
)JSON";
}

std::string malformedJsonPayload() {
    return R"JSON(
{
  "data_descriptor": {
    "origin": "inline",
    "content_type": "application/json",
    "autodetect": false
  },
  "options": {
    "drop_outliers_percent": 1.5
  },
  "data": [
    { "temperature": 20.5, "status": "nominal" }
  ]
)JSON"; // Missing closing brace intentionally
}

std::string internalErrorPayload() {
    return R"JSON(
{
  "data_descriptor": {
    "origin": "inline",
    "content_type": "application/json",
    "autodetect": false
  },
  "options": {
    "drop_outliers_percent": 1.5
  },
  "data": {
    "unexpected": "structure"
  }
}
)JSON";
}

std::string emptyDatasetPayload() {
    return R"JSON(
{
  "data_descriptor": {
    "origin": "inline",
    "content_type": "application/json",
    "autodetect": false
  },
  "options": {
    "drop_outliers_percent": 1.5
  },
  "data": []
}
)JSON";
}

} // namespace

TEST_CASE("POST /api/analyses/preprocess - JSON success", "[AnalysisController][integration]") {
    oatpp::test::web::ClientServerTestRunner runner;
    OATPP_COMPONENT(std::shared_ptr<oatpp::parser::json::mapping::ObjectMapper>, jsonMapper);
    auto controller = varlor::controllers::AnalysisController::createShared(jsonMapper);
    runner.addController(controller);

    runner.run([&] {
        auto client = createClient();
        auto response = client->preprocess(validJsonPayload().c_str(),
                                           oatpp::String("application/json"),
                                           oatpp::String("application/json"));

        REQUIRE(response->getStatusCode() == 200);
        auto contentType = response->getHeader("Content-Type");
        REQUIRE(contentType);
        REQUIRE(*contentType == "application/json");

        auto body = readBodyToString(response);
        REQUIRE(body);

        auto dto = jsonMapper->readFromString<oatpp::Object<varlor::controllers::AnalysisPreprocessResponseDto>>(body);
        REQUIRE(dto);
        REQUIRE(dto->cleaned_dataset);
        REQUIRE(dto->cleaned_dataset->columns);
        REQUIRE(dto->cleaned_dataset->columns->size() == 2);
        REQUIRE(dto->report);
        REQUIRE(dto->report->input_row_count == 3);
        REQUIRE(dto->report->output_row_count == 3);
        REQUIRE(dto->report->outliers_removed == 0);
        REQUIRE(dto->report->missing_values_replaced == 0);

        REQUIRE(dto->cleaned_dataset->rows);
        REQUIRE(dto->cleaned_dataset->rows->size() == 3);
        REQUIRE(dto->outliers_dataset);
        REQUIRE(dto->outliers_dataset->rows);
        REQUIRE(dto->outliers_dataset->rows->empty());
    });
}

TEST_CASE("POST /api/analyses/preprocess - YAML success", "[AnalysisController][integration]") {
    oatpp::test::web::ClientServerTestRunner runner;
    OATPP_COMPONENT(std::shared_ptr<oatpp::parser::json::mapping::ObjectMapper>, jsonMapper);
    auto controller = varlor::controllers::AnalysisController::createShared(jsonMapper);
    runner.addController(controller);

    runner.run([&] {
        auto client = createClient();
        auto response = client->preprocess(validYamlPayload().c_str(),
                                           oatpp::String("application/x-yaml"),
                                           oatpp::String("application/x-yaml"));

        REQUIRE(response->getStatusCode() == 200);
        auto contentType = response->getHeader("Content-Type");
        REQUIRE(contentType);
        REQUIRE(*contentType == "application/x-yaml");

        auto body = readBodyToString(response);
        REQUIRE(body);

        auto node = YAML::Load(body->std_str());
        REQUIRE(node["cleaned_dataset"]);
        REQUIRE(node["outliers_dataset"]);
        REQUIRE(node["report"]);
        REQUIRE(node["report"]["input_row_count"].as<long long>() == 3);
        REQUIRE(node["report"]["output_row_count"].as<long long>() == 3);
        REQUIRE(node["report"]["outliers_removed"].as<long long>() == 0);
    });
}

TEST_CASE("POST /api/analyses/preprocess - content type mismatch", "[AnalysisController][integration]") {
    oatpp::test::web::ClientServerTestRunner runner;
    OATPP_COMPONENT(std::shared_ptr<oatpp::parser::json::mapping::ObjectMapper>, jsonMapper);
    auto controller = varlor::controllers::AnalysisController::createShared(jsonMapper);
    runner.addController(controller);

    runner.run([&] {
        auto client = createClient();
        auto response = client->preprocess(mismatchedContentTypePayload().c_str(),
                                           oatpp::String("application/json"),
                                           oatpp::String("application/json"));

        REQUIRE(response->getStatusCode() == 422);
    });
}

TEST_CASE("POST /api/analyses/preprocess - malformed JSON", "[AnalysisController][integration]") {
    oatpp::test::web::ClientServerTestRunner runner;
    OATPP_COMPONENT(std::shared_ptr<oatpp::parser::json::mapping::ObjectMapper>, jsonMapper);
    auto controller = varlor::controllers::AnalysisController::createShared(jsonMapper);
    runner.addController(controller);

    runner.run([&] {
        auto client = createClient();
        auto response = client->preprocess(malformedJsonPayload().c_str(),
                                           oatpp::String("application/json"),
                                           oatpp::String("application/json"));

        REQUIRE(response->getStatusCode() == 400);
    });
}

TEST_CASE("POST /api/analyses/preprocess - internal error on invalid dataset structure", "[AnalysisController][integration]") {
    oatpp::test::web::ClientServerTestRunner runner;
    OATPP_COMPONENT(std::shared_ptr<oatpp::parser::json::mapping::ObjectMapper>, jsonMapper);
    auto controller = varlor::controllers::AnalysisController::createShared(jsonMapper);
    runner.addController(controller);

    runner.run([&] {
        auto client = createClient();
        auto response = client->preprocess(internalErrorPayload().c_str(),
                                           oatpp::String("application/json"),
                                           oatpp::String("application/json"));

        REQUIRE(response->getStatusCode() == 500);
        auto body = readBodyToString(response);
        REQUIRE(body);
        auto errorNode = jsonMapper->readFromString<oatpp::Object<varlor::controllers::AnalysisErrorResponseDto>>(body);
        REQUIRE(errorNode);
        REQUIRE(errorNode->error);
        REQUIRE(errorNode->details);
    });
}

TEST_CASE("POST /api/analyses/preprocess - empty dataset", "[AnalysisController][integration]") {
    oatpp::test::web::ClientServerTestRunner runner;
    OATPP_COMPONENT(std::shared_ptr<oatpp::parser::json::mapping::ObjectMapper>, jsonMapper);
    auto controller = varlor::controllers::AnalysisController::createShared(jsonMapper);
    runner.addController(controller);

    runner.run([&] {
        auto client = createClient();
        auto response = client->preprocess(emptyDatasetPayload().c_str(),
                                           oatpp::String("application/json"),
                                           oatpp::String("application/json"));

        REQUIRE(response->getStatusCode() == 200);
        auto body = readBodyToString(response);
        REQUIRE(body);
        auto dto = jsonMapper->readFromString<oatpp::Object<varlor::controllers::AnalysisPreprocessResponseDto>>(body);
        REQUIRE(dto);
        REQUIRE(dto->report);
        REQUIRE(dto->report->input_row_count == 0);
        REQUIRE(dto->report->output_row_count == 0);
    });
}

