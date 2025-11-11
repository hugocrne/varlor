#include <catch2/catch_test_macros.hpp>
#include <catch2/catch_approx.hpp>

#include "../../Core/IndicatorEngine.hpp"
#include "../../Models/DataPoint.hpp"
#include "../../Models/Dataset.hpp"
#include "../../Models/OperationDefinition.hpp"
#include "../../Models/OperationResult.hpp"

#include <optional>
#include <string>
#include <unordered_map>
#include <vector>

namespace {

varlor::models::Dataset buildDataset() {
    varlor::models::Dataset dataset({ "price", "clicks", "duration" });

    varlor::models::DataPoint row1;
    row1.setField("price", 10.0);
    row1.setField("clicks", 100.0);
    row1.setField("duration", 5.0);

    varlor::models::DataPoint row2;
    row2.setField("price", 20.0);
    row2.setField("clicks", 150.0);
    row2.setField("duration", 7.0);

    varlor::models::DataPoint row3;
    row3.setField("price", 30.0);
    row3.setField("clicks", 200.0);
    row3.setField("duration", 9.0);

    dataset.addDataPoint(std::move(row1));
    dataset.addDataPoint(std::move(row2));
    dataset.addDataPoint(std::move(row3));

    return dataset;
}

} // namespace

TEST_CASE("IndicatorEngine - mix opérations builtin et expression", "[IndicatorEngine]") {
    auto dataset = buildDataset();

    std::vector<varlor::models::OperationDefinition> ops;
    ops.push_back(varlor::models::OperationDefinition{
        .expr = "mean(price)",
        .alias = std::optional<std::string>{"prix_moyen"},
        .params = std::nullopt});
    ops.push_back(varlor::models::OperationDefinition{
        .expr = "price * clicks / 100",
        .alias = std::optional<std::string>{"score"},
        .params = std::nullopt});
    ops.push_back(varlor::models::OperationDefinition{
        .expr = "(max(price) - min(price)) / mean(price)",
        .alias = std::nullopt,
        .params = std::nullopt});

    varlor::core::IndicatorEngine engine;
    const auto results = engine.execute(dataset, ops);

    REQUIRE(results.size() == 3);

    SECTION("Résultat builtin") {
        const auto& meanResult = results[0];
        REQUIRE(meanResult.expr == "prix_moyen");
        REQUIRE(meanResult.status == varlor::models::OperationStatus::Success);
        REQUIRE(std::holds_alternative<double>(meanResult.result));
        REQUIRE(std::get<double>(meanResult.result) == Catch::Approx(20.0));
        REQUIRE_FALSE(meanResult.errorMessage.has_value());
    }

    SECTION("Résultat expression série") {
        const auto& exprResult = results[1];
        REQUIRE(exprResult.expr == "score");
        REQUIRE(exprResult.status == varlor::models::OperationStatus::Success);
        REQUIRE(std::holds_alternative<std::vector<double>>(exprResult.result));
        const auto& values = std::get<std::vector<double>>(exprResult.result);
        REQUIRE(values.size() == dataset.getRowCount());
        REQUIRE(values[0] == Catch::Approx(10.0));
        REQUIRE(values[1] == Catch::Approx(30.0));
        REQUIRE(values[2] == Catch::Approx(60.0));
    }

    SECTION("Résultat expression scalaire combinant agrégats") {
        const auto& scalarResult = results[2];
        REQUIRE(scalarResult.expr == "(max(price) - min(price)) / mean(price)");
        REQUIRE(scalarResult.status == varlor::models::OperationStatus::Success);
        REQUIRE(std::holds_alternative<double>(scalarResult.result));
        REQUIRE(std::get<double>(scalarResult.result) == Catch::Approx(1.0));
    }
}

TEST_CASE("IndicatorEngine - gestion des erreurs", "[IndicatorEngine]") {
    auto dataset = buildDataset();

    std::vector<varlor::models::OperationDefinition> ops;
    ops.push_back(varlor::models::OperationDefinition{
        .expr = "mean(undefinedField)",
        .alias = std::nullopt,
        .params = std::nullopt});
    ops.push_back(varlor::models::OperationDefinition{
        .expr = "price * unknown",
        .alias = std::nullopt,
        .params = std::nullopt});

    varlor::core::IndicatorEngine engine;
    const auto results = engine.execute(dataset, ops);

    REQUIRE(results.size() == 2);

    const auto& builtinError = results[0];
    REQUIRE(builtinError.status == varlor::models::OperationStatus::Error);
    REQUIRE(std::holds_alternative<std::monostate>(builtinError.result));
    REQUIRE(builtinError.errorMessage.has_value());

    const auto& expressionError = results[1];
    REQUIRE(expressionError.status == varlor::models::OperationStatus::Error);
    REQUIRE(std::holds_alternative<std::monostate>(expressionError.result));
    REQUIRE(expressionError.errorMessage.has_value());
}


