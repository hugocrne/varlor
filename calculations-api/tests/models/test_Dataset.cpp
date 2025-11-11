#include <catch2/catch_test_macros.hpp>
#include "../../Models/Dataset.hpp"
#include "../../Models/DataPoint.hpp"
#include <string>
#include <vector>

TEST_CASE("Dataset construction", "[Dataset]") {
    SECTION("Default constructor creates empty dataset") {
        varlor::models::Dataset dataset;
        REQUIRE(dataset.empty());
        REQUIRE(dataset.getRowCount() == 0);
        REQUIRE(dataset.getColumnCount() == 0);
    }

    SECTION("Constructor with column names") {
        std::vector<std::string> columns = {"col1", "col2", "col3"};
        varlor::models::Dataset dataset(columns);
        
        REQUIRE(dataset.getColumnCount() == 3);
        REQUIRE(dataset.getRowCount() == 0);
        REQUIRE(dataset.empty());
    }

    SECTION("Constructor with move semantics") {
        std::vector<std::string> columns = {"a", "b"};
        varlor::models::Dataset dataset(std::move(columns));
        
        REQUIRE(dataset.getColumnCount() == 2);
        REQUIRE(columns.empty()); // Moved from
    }
}

TEST_CASE("Dataset column management", "[Dataset]") {
    varlor::models::Dataset dataset;

    SECTION("Add column names") {
        dataset.addColumnName("name");
        dataset.addColumnName("age");
        
        REQUIRE(dataset.getColumnCount() == 2);
        REQUIRE(dataset.getColumnNames()[0] == "name");
        REQUIRE(dataset.getColumnNames()[1] == "age");
    }

    SECTION("Set column names") {
        std::vector<std::string> columns = {"x", "y", "z"};
        dataset.setColumnNames(columns);
        
        REQUIRE(dataset.getColumnCount() == 3);
        const auto& names = dataset.getColumnNames();
        REQUIRE(names[0] == "x");
        REQUIRE(names[1] == "y");
        REQUIRE(names[2] == "z");
    }

    SECTION("Set column names with move") {
        std::vector<std::string> columns = {"a", "b"};
        dataset.setColumnNames(std::move(columns));
        
        REQUIRE(dataset.getColumnCount() == 2);
        REQUIRE(columns.empty()); // Moved from
    }

    SECTION("Modify column names via reference") {
        dataset.addColumnName("test");
        auto& columns = dataset.getColumnNames();
        columns.push_back("new");
        
        REQUIRE(dataset.getColumnCount() == 2);
    }
}

TEST_CASE("Dataset data point management", "[Dataset]") {
    varlor::models::Dataset dataset;
    dataset.addColumnName("value");

    SECTION("Add data point") {
        varlor::models::DataPoint point;
        point.setField("value", 10.0);
        
        dataset.addDataPoint(point);
        REQUIRE(dataset.getRowCount() == 1);
        REQUIRE_FALSE(dataset.empty());
    }

    SECTION("Add data point with move") {
        varlor::models::DataPoint point;
        point.setField("value", 20.0);
        
        dataset.addDataPoint(std::move(point));
        REQUIRE(dataset.getRowCount() == 1);
        REQUIRE(point.empty()); // Moved from
    }

    SECTION("Get data point by index") {
        varlor::models::DataPoint point1;
        point1.setField("value", 1.0);
        dataset.addDataPoint(point1);

        varlor::models::DataPoint point2;
        point2.setField("value", 2.0);
        dataset.addDataPoint(point2);

        auto& retrieved = dataset.getDataPoint(0);
        auto value = retrieved.getField("value");
        REQUIRE(std::get<double>(value.value()) == 1.0);

        const auto& constRetrieved = dataset.getDataPoint(1);
        auto value2 = constRetrieved.getField("value");
        REQUIRE(std::get<double>(value2.value()) == 2.0);
    }

    SECTION("Get data point throws on invalid index") {
        REQUIRE_THROWS_AS(dataset.getDataPoint(0), std::out_of_range);
    }

    SECTION("Remove data point") {
        varlor::models::DataPoint point;
        point.setField("value", 5.0);
        dataset.addDataPoint(point);
        
        bool removed = dataset.removeDataPoint(0);
        REQUIRE(removed);
        REQUIRE(dataset.empty());
    }

    SECTION("Remove data point with invalid index") {
        bool removed = dataset.removeDataPoint(0);
        REQUIRE_FALSE(removed);
    }

    SECTION("Remove middle data point") {
        for (int i = 0; i < 3; ++i) {
            varlor::models::DataPoint point;
            point.setField("value", static_cast<double>(i));
            dataset.addDataPoint(point);
        }

        REQUIRE(dataset.getRowCount() == 3);
        dataset.removeDataPoint(1);
        REQUIRE(dataset.getRowCount() == 2);
        
        // Check remaining points
        auto value0 = dataset.getDataPoint(0).getField("value");
        REQUIRE(std::get<double>(value0.value()) == 0.0);
        auto value1 = dataset.getDataPoint(1).getField("value");
        REQUIRE(std::get<double>(value1.value()) == 2.0);
    }
}

TEST_CASE("Dataset iteration", "[Dataset]") {
    varlor::models::Dataset dataset;
    dataset.addColumnName("value");

    SECTION("Iterate over empty dataset") {
        int count = 0;
        for (const auto& point : dataset) {
            (void)point;
            ++count;
        }
        REQUIRE(count == 0);
    }

    SECTION("Iterate over dataset with points") {
        for (int i = 0; i < 5; ++i) {
            varlor::models::DataPoint point;
            point.setField("value", static_cast<double>(i));
            dataset.addDataPoint(point);
        }

        int count = 0;
        for (const auto& point : dataset) {
            auto value = point.getField("value");
            REQUIRE(std::get<double>(value.value()) == static_cast<double>(count));
            ++count;
        }
        REQUIRE(count == 5);
    }

    SECTION("Modify points via iterator") {
        varlor::models::DataPoint point;
        point.setField("value", 10.0);
        dataset.addDataPoint(point);

        for (auto& point : dataset) {
            point.setField("value", 99.0);
        }

        auto value = dataset.getDataPoint(0).getField("value");
        REQUIRE(std::get<double>(value.value()) == 99.0);
    }
}

TEST_CASE("Dataset clear and access", "[Dataset]") {
    varlor::models::Dataset dataset;
    dataset.addColumnName("col1");
    dataset.addColumnName("col2");

    varlor::models::DataPoint point;
    point.setField("col1", 1.0);
    dataset.addDataPoint(point);

    REQUIRE(dataset.getRowCount() == 1);
    REQUIRE(dataset.getColumnCount() == 2);

    SECTION("Clear dataset") {
        dataset.clear();
        REQUIRE(dataset.empty());
        REQUIRE(dataset.getRowCount() == 0);
        REQUIRE(dataset.getColumnCount() == 0);
    }

    SECTION("Access data points via getDataPoints()") {
        const auto& points = dataset.getDataPoints();
        REQUIRE(points.size() == 1);

        auto& mutablePoints = dataset.getDataPoints();
        mutablePoints.push_back(varlor::models::DataPoint());
        REQUIRE(dataset.getRowCount() == 2);
    }
}

TEST_CASE("Dataset complex scenario", "[Dataset]") {
    varlor::models::Dataset dataset;
    
    // Setup columns
    std::vector<std::string> columns = {"name", "age", "active"};
    dataset.setColumnNames(columns);

    // Add multiple data points
    for (int i = 0; i < 10; ++i) {
        varlor::models::DataPoint point;
        point.setField("name", std::string("User" + std::to_string(i)));
        point.setField("age", static_cast<double>(20 + i));
        point.setField("active", (i % 2 == 0));
        dataset.addDataPoint(point);
    }

    REQUIRE(dataset.getRowCount() == 10);
    REQUIRE(dataset.getColumnCount() == 3);

    // Verify data integrity
    for (std::size_t i = 0; i < dataset.getRowCount(); ++i) {
        const auto& point = dataset.getDataPoint(i);
        REQUIRE(point.hasField("name"));
        REQUIRE(point.hasField("age"));
        REQUIRE(point.hasField("active"));
    }
}

