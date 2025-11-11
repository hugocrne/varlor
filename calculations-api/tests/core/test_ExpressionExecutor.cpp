#include <catch2/catch_test_macros.hpp>
#include <catch2/catch_approx.hpp>

#include "../../Core/ExpressionExecutor.hpp"
#include "../../Core/MathOperationParser.hpp"
#include "../../Models/DataPoint.hpp"
#include "../../Models/Dataset.hpp"

#include <string>
#include <vector>

namespace {

varlor::models::Dataset makeDataset(std::initializer_list<std::string> columns) {
    return varlor::models::Dataset(std::vector<std::string>(columns));
}

void addRow(
    varlor::models::Dataset& dataset,
    std::initializer_list<std::pair<std::string, varlor::models::FieldValue>> values) {
    varlor::models::DataPoint row;
    for (const auto& [key, value] : values) {
        row.setField(key, value);
    }
    dataset.addDataPoint(std::move(row));
}

} // namespace

TEST_CASE("ExpressionExecutor - produit ligne par ligne", "[ExpressionExecutor]") {
    auto dataset = makeDataset({"price", "clicks"});
    addRow(dataset, {{"price", 10.0}, {"clicks", 2.0}});
    addRow(dataset, {{"price", 15.0}, {"clicks", 4.0}});
    addRow(dataset, {{"price", 20.0}, {"clicks", 5.0}});

    varlor::core::MathOperationParser parser;
    auto parsed = parser.parse("price * clicks / 10", dataset);

    varlor::core::ExpressionExecutor executor;
    auto result = executor.evaluate(parsed, dataset);

    REQUIRE(std::holds_alternative<std::vector<double>>(result));
    const auto& values = std::get<std::vector<double>>(result);
    REQUIRE(values.size() == dataset.getRowCount());
    REQUIRE(values[0] == Catch::Approx(2.0));
    REQUIRE(values[1] == Catch::Approx(6.0));
    REQUIRE(values[2] == Catch::Approx(10.0));
}

TEST_CASE("ExpressionExecutor - agrégats substitués", "[ExpressionExecutor]") {
    auto dataset = makeDataset({"price"});
    addRow(dataset, {{"price", 10.0}});
    addRow(dataset, {{"price", 20.0}});
    addRow(dataset, {{"price", 30.0}});

    varlor::core::MathOperationParser parser;
    auto parsed = parser.parse("(max(price) - min(price)) / mean(price)", dataset);

    varlor::core::ExpressionExecutor executor;
    auto result = executor.evaluate(parsed, dataset);

    REQUIRE(std::holds_alternative<double>(result));
    REQUIRE(std::get<double>(result) == Catch::Approx(1.0));
}

TEST_CASE("ExpressionExecutor - colonnes ou fonctions invalides", "[ExpressionExecutor]") {
    auto dataset = makeDataset({"price"});
    addRow(dataset, {{"price", 10.0}});

    varlor::core::MathOperationParser parser;

    SECTION("Colonne inconnue") {
        REQUIRE_THROWS_AS(parser.parse("quantity + 1", dataset), std::invalid_argument);
    }

    SECTION("Fonction non autorisée") {
        REQUIRE_THROWS_AS(parser.parse("system(price)", dataset), std::invalid_argument);
    }

    SECTION("Valeur manquante à l'exécution") {
        auto parsed = parser.parse("price * 2", dataset);
        dataset.getDataPoint(0).setField("price", nullptr);

        varlor::core::ExpressionExecutor executor;
        REQUIRE_THROWS_AS(executor.evaluate(parsed, dataset), std::runtime_error);
    }
}


