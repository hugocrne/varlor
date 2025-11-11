#include <catch2/catch_test_macros.hpp>
#include <catch2/catch_approx.hpp>

#include "../../Core/BuiltinOperations.hpp"
#include "../../Models/DataPoint.hpp"
#include "../../Models/Dataset.hpp"

#include <cmath>
#include <string>
#include <vector>

namespace {

varlor::models::Dataset makeDataset(std::initializer_list<std::string> columns) {
    return varlor::models::Dataset(std::vector<std::string>(columns));
}

void addRow(varlor::models::Dataset& dataset, std::initializer_list<double> values) {
    varlor::models::DataPoint row;
    std::size_t index = 0;
    for (double value : values) {
        row.setField(dataset.getColumnNames()[index++], value);
    }
    dataset.addDataPoint(std::move(row));
}

} // namespace

TEST_CASE("BuiltinOperations - mesures univariées", "[BuiltinOperations]") {
    auto dataset = makeDataset({"price"});
    addRow(dataset, {10.0});
    addRow(dataset, {15.0});
    addRow(dataset, {20.0});
    addRow(dataset, {25.0});

    REQUIRE(varlor::core::BuiltinOperations::mean(dataset, "price") == Catch::Approx(17.5));
    REQUIRE(varlor::core::BuiltinOperations::median(dataset, "price") == Catch::Approx(17.5));
    REQUIRE(varlor::core::BuiltinOperations::variance(dataset, "price") == Catch::Approx(31.25));
    REQUIRE(varlor::core::BuiltinOperations::stddev(dataset, "price") == Catch::Approx(std::sqrt(31.25)));
    REQUIRE(varlor::core::BuiltinOperations::min(dataset, "price") == Catch::Approx(10.0));
    REQUIRE(varlor::core::BuiltinOperations::max(dataset, "price") == Catch::Approx(25.0));

    REQUIRE(
        varlor::core::BuiltinOperations::percentile(dataset, "price", 90.0) ==
        Catch::Approx(23.5));
}

TEST_CASE("BuiltinOperations - corrélation", "[BuiltinOperations]") {
    auto dataset = makeDataset({"x", "y"});
    addRow(dataset, {1.0, 2.0});
    addRow(dataset, {2.0, 4.0});
    addRow(dataset, {3.0, 6.0});
    addRow(dataset, {4.0, 8.0});

    REQUIRE(
        varlor::core::BuiltinOperations::correlation(dataset, "x", "y") ==
        Catch::Approx(1.0).margin(1e-12));
}

TEST_CASE("BuiltinOperations - colonnes invalides", "[BuiltinOperations]") {
    auto dataset = makeDataset({"price"});
    addRow(dataset, {10.0});

    SECTION("Colonne inexistante") {
        REQUIRE_THROWS_AS(
            varlor::core::BuiltinOperations::mean(dataset, "unknown"),
            std::invalid_argument);
    }

    SECTION("Valeur non numérique détectée") {
        varlor::models::DataPoint row;
        row.setField("price", std::string("not-a-number"));
        dataset.addDataPoint(row);

        REQUIRE_THROWS_AS(
            varlor::core::BuiltinOperations::mean(dataset, "price"),
            std::invalid_argument);
    }
}


