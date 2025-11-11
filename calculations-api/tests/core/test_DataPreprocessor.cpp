#include <catch2/catch_test_macros.hpp>
#include <catch2/catch_approx.hpp>

#include "../../Core/DataPreprocessor.hpp"

#include <algorithm>
#include <string>
#include <vector>

namespace {

varlor::models::Dataset makeDatasetWithColumns(std::initializer_list<std::string> columns) {
    return varlor::models::Dataset(std::vector<std::string>(columns));
}

bool containsNormalizedField(const varlor::models::PreprocessingReport& report, const std::string& field) {
    const auto& fields = report.getNormalizedFields();
    return std::find(fields.begin(), fields.end(), field) != fields.end();
}

} // namespace

TEST_CASE("DataPreprocessor - normalisation des types", "[DataPreprocessor]") {
    auto dataset = makeDatasetWithColumns({"numeric_col", "bool_col"});

    varlor::models::DataPoint row1;
    row1.setField("numeric_col", std::string("12.5"));
    row1.setField("bool_col", std::string("true"));

    varlor::models::DataPoint row2;
    row2.setField("numeric_col", 8.0);
    row2.setField("bool_col", std::string("false"));

    varlor::models::DataPoint row3;
    row3.setField("numeric_col", std::string("6.0"));
    row3.setField("bool_col", std::string("0"));

    dataset.addDataPoint(row1);
    dataset.addDataPoint(row2);
    dataset.addDataPoint(row3);

    varlor::core::DataPreprocessor preprocessor;
    auto result = preprocessor.process(dataset);

    auto numericField = result.cleanedDataset.getDataPoint(0).getField("numeric_col");
    REQUIRE(numericField.has_value());
    REQUIRE(std::holds_alternative<double>(numericField.value()));
    REQUIRE(std::get<double>(numericField.value()) == Catch::Approx(12.5));

    auto boolField = result.cleanedDataset.getDataPoint(1).getField("bool_col");
    REQUIRE(boolField.has_value());
    REQUIRE(std::holds_alternative<bool>(boolField.value()));
    REQUIRE_FALSE(std::get<bool>(boolField.value()));

    auto boolFieldNumeric = result.cleanedDataset.getDataPoint(2).getField("bool_col");
    REQUIRE(boolFieldNumeric.has_value());
    REQUIRE(std::holds_alternative<bool>(boolFieldNumeric.value()));
    REQUIRE_FALSE(std::get<bool>(boolFieldNumeric.value()));

    REQUIRE(containsNormalizedField(result.report, "numeric_col"));
    REQUIRE(containsNormalizedField(result.report, "bool_col"));
}

TEST_CASE("DataPreprocessor - détection d'outliers", "[DataPreprocessor]") {
    auto dataset = makeDatasetWithColumns({"value"});

    std::vector<double> baseline{10.0, 11.0, 12.0, 13.0, 14.0};
    for (double v : baseline) {
        varlor::models::DataPoint row;
        row.setField("value", v);
        dataset.addDataPoint(std::move(row));
    }

    varlor::models::DataPoint outlier;
    outlier.setField("value", 100.0);
    dataset.addDataPoint(outlier);

    varlor::core::DataPreprocessor preprocessor;
    auto result = preprocessor.process(dataset);

    REQUIRE(result.outliersDataset.getRowCount() == 1);
    REQUIRE(result.cleanedDataset.getRowCount() == dataset.getRowCount() - 1);

    const auto& outlierPoint = result.outliersDataset.getDataPoint(0);
    auto outlierMetaSection = outlierPoint.getMeta().getSection("status");
    REQUIRE(outlierMetaSection.has_value());
    auto outlierFlag = outlierMetaSection->get().getLeaf("outlier");
    REQUIRE(outlierFlag.has_value());
    REQUIRE(std::holds_alternative<bool>(outlierFlag.value()));
    REQUIRE(std::get<bool>(outlierFlag.value()));

    auto reason = outlierMetaSection->get().getLeaf("reason");
    REQUIRE(reason.has_value());
    REQUIRE(std::holds_alternative<std::string>(reason.value()));
    REQUIRE(std::get<std::string>(reason.value()) == "iqr_detection");
}

TEST_CASE("DataPreprocessor - imputation des valeurs manquantes", "[DataPreprocessor]") {
    auto dataset = makeDatasetWithColumns({"temperature", "status", "comment"});

    varlor::models::DataPoint row1;
    row1.setField("temperature", 10.0);
    row1.setField("status", true);
    row1.setField("comment", std::string("ok"));

    varlor::models::DataPoint row2;
    row2.setField("temperature", std::string("12.0"));
    row2.setField("status", std::string("false"));
    row2.setField("comment", nullptr);

    varlor::models::DataPoint row3;
    row3.setField("temperature", nullptr);
    row3.setField("status", nullptr);
    row3.setField("comment", std::string("pending"));

    dataset.addDataPoint(row1);
    dataset.addDataPoint(row2);
    dataset.addDataPoint(row3);

    varlor::core::DataPreprocessor preprocessor;
    auto result = preprocessor.process(dataset);

    const auto& cleanedRowTemperature = result.cleanedDataset.getDataPoint(2);
    const auto& cleanedRowComment = result.cleanedDataset.getDataPoint(1);

    auto temperatureField = cleanedRowTemperature.getField("temperature");
    REQUIRE(temperatureField.has_value());
    REQUIRE(std::holds_alternative<double>(temperatureField.value()));

    auto statusField = cleanedRowTemperature.getField("status");
    REQUIRE(statusField.has_value());
    REQUIRE(std::holds_alternative<bool>(statusField.value()));

    auto columnsSection = cleanedRowTemperature.getMeta().getSection("columns");
    REQUIRE(columnsSection.has_value());

    auto temperatureMeta = columnsSection->get().getSection("temperature");
    REQUIRE(temperatureMeta.has_value());
    auto temperatureImputation = temperatureMeta->get().getSection("imputation");
    REQUIRE(temperatureImputation.has_value());
    auto temperatureImputed = temperatureImputation->get().getLeaf("imputed");
    REQUIRE(temperatureImputed.has_value());
    REQUIRE(std::holds_alternative<bool>(temperatureImputed.value()));
    REQUIRE(std::get<bool>(temperatureImputed.value()));

    auto temperatureReason = temperatureImputation->get().getLeaf("reason");
    REQUIRE(temperatureReason.has_value());
    REQUIRE(std::holds_alternative<std::string>(temperatureReason.value()));
    REQUIRE(std::get<std::string>(temperatureReason.value()) == "missing_value_replacement");

    auto statusMeta = columnsSection->get().getSection("status");
    REQUIRE(statusMeta.has_value());
    auto statusImputation = statusMeta->get().getSection("imputation");
    REQUIRE(statusImputation.has_value());
    auto statusStrategy = statusImputation->get().getLeaf("strategy");
    REQUIRE(statusStrategy.has_value());
    REQUIRE(std::holds_alternative<std::string>(statusStrategy.value()));
    REQUIRE(std::get<std::string>(statusStrategy.value()) == "mode_boolean");

    auto commentField = cleanedRowComment.getField("comment");
    REQUIRE(commentField.has_value());
    REQUIRE(std::holds_alternative<std::string>(commentField.value()));

    auto commentColumnsSection = cleanedRowComment.getMeta().getSection("columns");
    REQUIRE(commentColumnsSection.has_value());

    auto commentMeta = commentColumnsSection->get().getSection("comment");
    REQUIRE(commentMeta.has_value());
    auto commentImputation = commentMeta->get().getSection("imputation");
    REQUIRE(commentImputation.has_value());
    auto commentStrategy = commentImputation->get().getLeaf("strategy");
    REQUIRE(commentStrategy.has_value());
    REQUIRE(std::holds_alternative<std::string>(commentStrategy.value()));
    REQUIRE(std::get<std::string>(commentStrategy.value()) == "mode_text");
}

TEST_CASE("DataPreprocessor - rapport et cohérence", "[DataPreprocessor]") {
    auto dataset = makeDatasetWithColumns({"value", "flag"});

    varlor::models::DataPoint p1;
    p1.setField("value", std::string("10.0"));
    p1.setField("flag", std::string("true"));

    varlor::models::DataPoint p2;
    p2.setField("value", std::string("11.0"));
    p2.setField("flag", std::string("false"));

    varlor::models::DataPoint p3;
    p3.setField("value", std::string("50.0"));
    p3.setField("flag", nullptr);

    dataset.addDataPoint(p1);
    dataset.addDataPoint(p2);
    dataset.addDataPoint(p3);

    varlor::core::DataPreprocessor preprocessor;
    auto result = preprocessor.process(dataset);

    const auto cleanedRows = result.cleanedDataset.getRowCount();
    const auto outlierRows = result.outliersDataset.getRowCount();

    REQUIRE(result.report.getInputRowCount() == dataset.getRowCount());
    REQUIRE(result.report.getOutputRowCount() == cleanedRows);
    REQUIRE(result.report.getOutliersRemoved() == outlierRows);
    REQUIRE(result.report.getInputRowCount() == cleanedRows + outlierRows);

    REQUIRE(containsNormalizedField(result.report, "value"));
    REQUIRE(containsNormalizedField(result.report, "flag"));
}

TEST_CASE("DataPreprocessor - comportement non destructif", "[DataPreprocessor]") {
    auto dataset = makeDatasetWithColumns({"value"});

    varlor::models::DataPoint originalRow;
    originalRow.setField("value", std::string("15.5"));
    dataset.addDataPoint(originalRow);

    varlor::core::DataPreprocessor preprocessor;
    auto result = preprocessor.process(dataset);

    // Le dataset d'entrée ne doit pas être modifié
    auto originalField = dataset.getDataPoint(0).getField("value");
    REQUIRE(originalField.has_value());
    REQUIRE(std::holds_alternative<std::string>(originalField.value()));
    REQUIRE(std::get<std::string>(originalField.value()) == "15.5");

    auto cleanedField = result.cleanedDataset.getDataPoint(0).getField("value");
    REQUIRE(cleanedField.has_value());
    REQUIRE(std::holds_alternative<double>(cleanedField.value()));
}

TEST_CASE("DataPreprocessor - cas limites", "[DataPreprocessor]") {
    SECTION("Dataset vide") {
        auto emptyDataset = makeDatasetWithColumns({"col"});
        varlor::core::DataPreprocessor preprocessor;
        auto result = preprocessor.process(emptyDataset);
        REQUIRE(result.cleanedDataset.getRowCount() == 0);
        REQUIRE(result.outliersDataset.getRowCount() == 0);
        REQUIRE(result.report.getInputRowCount() == 0);
        REQUIRE(result.report.getOutputRowCount() == 0);
    }

    SECTION("Colonne avec données mixtes") {
        auto dataset = makeDatasetWithColumns({"mixed"});

        varlor::models::DataPoint a;
        a.setField("mixed", std::string("42"));

        varlor::models::DataPoint b;
        b.setField("mixed", std::string("texte"));

        dataset.addDataPoint(a);
        dataset.addDataPoint(b);

        varlor::core::DataPreprocessor preprocessor;
        auto result = preprocessor.process(dataset);

        REQUIRE_FALSE(containsNormalizedField(result.report, "mixed"));

        auto field0 = result.cleanedDataset.getDataPoint(0).getField("mixed");
        auto field1 = result.cleanedDataset.getDataPoint(1).getField("mixed");

        REQUIRE(field0.has_value());
        REQUIRE(field1.has_value());

        REQUIRE(std::holds_alternative<std::string>(field0.value()));
        REQUIRE(std::holds_alternative<std::string>(field1.value()));
        REQUIRE(std::get<std::string>(field0.value()) == "42");
        REQUIRE(std::get<std::string>(field1.value()) == "texte");
    }
}


