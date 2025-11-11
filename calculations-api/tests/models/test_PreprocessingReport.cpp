#include <catch2/catch_test_macros.hpp>
#include "../../Models/PreprocessingReport.hpp"
#include <string>

TEST_CASE("PreprocessingReport construction", "[PreprocessingReport]") {
    SECTION("Default constructor initializes to zero") {
        varlor::models::PreprocessingReport report;
        
        REQUIRE(report.getInputRowCount() == 0);
        REQUIRE(report.getOutputRowCount() == 0);
        REQUIRE(report.getOutliersRemoved() == 0);
        REQUIRE(report.getMissingValuesReplaced() == 0);
        REQUIRE(report.getNormalizedFields().empty());
    }

    SECTION("Constructor with row counts") {
        varlor::models::PreprocessingReport report(100, 95);
        
        REQUIRE(report.getInputRowCount() == 100);
        REQUIRE(report.getOutputRowCount() == 95);
        REQUIRE(report.getOutliersRemoved() == 0);
        REQUIRE(report.getMissingValuesReplaced() == 0);
    }
}

TEST_CASE("PreprocessingReport setters and getters", "[PreprocessingReport]") {
    varlor::models::PreprocessingReport report;

    SECTION("Set input row count") {
        report.setInputRowCount(1000);
        REQUIRE(report.getInputRowCount() == 1000);
    }

    SECTION("Set output row count") {
        report.setOutputRowCount(950);
        REQUIRE(report.getOutputRowCount() == 950);
    }

    SECTION("Set outliers removed") {
        report.setOutliersRemoved(50);
        REQUIRE(report.getOutliersRemoved() == 50);
    }

    SECTION("Set missing values replaced") {
        report.setMissingValuesReplaced(25);
        REQUIRE(report.getMissingValuesReplaced() == 25);
    }

    SECTION("Set all values") {
        report.setInputRowCount(200);
        report.setOutputRowCount(180);
        report.setOutliersRemoved(15);
        report.setMissingValuesReplaced(5);

        REQUIRE(report.getInputRowCount() == 200);
        REQUIRE(report.getOutputRowCount() == 180);
        REQUIRE(report.getOutliersRemoved() == 15);
        REQUIRE(report.getMissingValuesReplaced() == 5);
    }
}

TEST_CASE("PreprocessingReport increment operations", "[PreprocessingReport]") {
    varlor::models::PreprocessingReport report;

    SECTION("Increment outliers removed") {
        report.incrementOutliersRemoved();
        REQUIRE(report.getOutliersRemoved() == 1);
        
        report.incrementOutliersRemoved(4);
        REQUIRE(report.getOutliersRemoved() == 5);
    }

    SECTION("Increment missing values replaced") {
        report.incrementMissingValuesReplaced();
        REQUIRE(report.getMissingValuesReplaced() == 1);
        
        report.incrementMissingValuesReplaced(9);
        REQUIRE(report.getMissingValuesReplaced() == 10);
    }

    SECTION("Multiple increments") {
        for (int i = 0; i < 10; ++i) {
            report.incrementOutliersRemoved();
        }
        REQUIRE(report.getOutliersRemoved() == 10);

        for (int i = 0; i < 5; ++i) {
            report.incrementMissingValuesReplaced(2);
        }
        REQUIRE(report.getMissingValuesReplaced() == 10);
    }
}

TEST_CASE("PreprocessingReport normalized fields", "[PreprocessingReport]") {
    varlor::models::PreprocessingReport report;

    SECTION("Add normalized field") {
        report.addNormalizedField("age");
        REQUIRE(report.getNormalizedFields().size() == 1);
        REQUIRE(report.getNormalizedFields()[0] == "age");
    }

    SECTION("Add multiple normalized fields") {
        report.addNormalizedField("name");
        report.addNormalizedField("email");
        report.addNormalizedField("phone");

        const auto& fields = report.getNormalizedFields();
        REQUIRE(fields.size() == 3);
        REQUIRE(fields[0] == "name");
        REQUIRE(fields[1] == "email");
        REQUIRE(fields[2] == "phone");
    }

    SECTION("Add normalized field with move") {
        std::string fieldName = "large_field_name";
        report.addNormalizedField(std::move(fieldName));
        
        REQUIRE(report.getNormalizedFields().size() == 1);
        REQUIRE(fieldName.empty()); // Moved from
    }

    SECTION("Clear normalized fields") {
        report.addNormalizedField("field1");
        report.addNormalizedField("field2");
        REQUIRE(report.getNormalizedFields().size() == 2);

        report.clearNormalizedFields();
        REQUIRE(report.getNormalizedFields().empty());
    }
}

TEST_CASE("PreprocessingReport rows removed calculation", "[PreprocessingReport]") {
    SECTION("Calculate rows removed when input > output") {
        varlor::models::PreprocessingReport report(100, 90);
        REQUIRE(report.getRowsRemoved() == 10);
    }

    SECTION("Calculate rows removed when input == output") {
        varlor::models::PreprocessingReport report(100, 100);
        REQUIRE(report.getRowsRemoved() == 0);
    }

    SECTION("Calculate rows removed when input < output (should not happen but handled)") {
        varlor::models::PreprocessingReport report(100, 110);
        REQUIRE(report.getRowsRemoved() == 0); // Should not be negative
    }

    SECTION("Rows removed with zero input") {
        varlor::models::PreprocessingReport report(0, 0);
        REQUIRE(report.getRowsRemoved() == 0);
    }
}

TEST_CASE("PreprocessingReport reset", "[PreprocessingReport]") {
    varlor::models::PreprocessingReport report;

    // Set some values
    report.setInputRowCount(100);
    report.setOutputRowCount(90);
    report.setOutliersRemoved(5);
    report.setMissingValuesReplaced(5);
    report.addNormalizedField("field1");
    report.addNormalizedField("field2");

    // Verify values are set
    REQUIRE(report.getInputRowCount() == 100);
    REQUIRE(report.getOutliersRemoved() == 5);
    REQUIRE(report.getNormalizedFields().size() == 2);

    // Reset
    report.reset();

    // Verify all values are zero/empty
    REQUIRE(report.getInputRowCount() == 0);
    REQUIRE(report.getOutputRowCount() == 0);
    REQUIRE(report.getOutliersRemoved() == 0);
    REQUIRE(report.getMissingValuesReplaced() == 0);
    REQUIRE(report.getNormalizedFields().empty());
}

TEST_CASE("PreprocessingReport complete scenario", "[PreprocessingReport]") {
    varlor::models::PreprocessingReport report;

    // Simulate preprocessing workflow
    report.setInputRowCount(1000);
    
    // Remove outliers
    for (int i = 0; i < 50; ++i) {
        report.incrementOutliersRemoved();
    }
    
    // Replace missing values
    for (int i = 0; i < 25; ++i) {
        report.incrementMissingValuesReplaced();
    }
    
    // Normalize fields
    report.addNormalizedField("age");
    report.addNormalizedField("salary");
    report.addNormalizedField("name");

    // Set final output count
    report.setOutputRowCount(950);

    // Verify final state
    REQUIRE(report.getInputRowCount() == 1000);
    REQUIRE(report.getOutputRowCount() == 950);
    REQUIRE(report.getOutliersRemoved() == 50);
    REQUIRE(report.getMissingValuesReplaced() == 25);
    REQUIRE(report.getRowsRemoved() == 50);
    REQUIRE(report.getNormalizedFields().size() == 3);
}

