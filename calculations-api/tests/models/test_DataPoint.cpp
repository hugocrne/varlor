#include <catch2/catch_test_macros.hpp>
#include "../../Models/DataPoint.hpp"
#include <string>
#include <variant>

TEST_CASE("DataPoint construction", "[DataPoint]") {
    SECTION("Default constructor creates empty DataPoint") {
        varlor::models::DataPoint point;
        REQUIRE(point.empty());
        REQUIRE(point.size() == 0);
    }

    SECTION("Constructor with fields map") {
        std::unordered_map<std::string, varlor::models::FieldValue> fields;
        fields["age"] = 25.0;
        fields["name"] = std::string("John");

        varlor::models::DataPoint point(fields);
        REQUIRE(point.size() == 2);
        REQUIRE_FALSE(point.empty());
    }

    SECTION("Constructor with move semantics") {
        std::unordered_map<std::string, varlor::models::FieldValue> fields;
        fields["value"] = 42.0;

        varlor::models::DataPoint point(std::move(fields));
        REQUIRE(point.size() == 1);
        REQUIRE(fields.empty()); // Moved from
    }
}

TEST_CASE("DataPoint field operations", "[DataPoint]") {
    varlor::models::DataPoint point;

    SECTION("Set and get numeric field") {
        point.setField("age", 30.0);
        REQUIRE(point.hasField("age"));
        
        auto value = point.getField("age");
        REQUIRE(value.has_value());
        REQUIRE(std::holds_alternative<double>(value.value()));
        REQUIRE(std::get<double>(value.value()) == 30.0);
    }

    SECTION("Set and get string field") {
        point.setField("name", std::string("Alice"));
        REQUIRE(point.hasField("name"));
        
        auto value = point.getField("name");
        REQUIRE(value.has_value());
        REQUIRE(std::holds_alternative<std::string>(value.value()));
        REQUIRE(std::get<std::string>(value.value()) == "Alice");
    }

    SECTION("Set and get boolean field") {
        point.setField("active", true);
        REQUIRE(point.hasField("active"));
        
        auto value = point.getField("active");
        REQUIRE(value.has_value());
        REQUIRE(std::holds_alternative<bool>(value.value()));
        REQUIRE(std::get<bool>(value.value()) == true);
    }

    SECTION("Set and get null field") {
        point.setField("optional", nullptr);
        REQUIRE(point.hasField("optional"));
        
        auto value = point.getField("optional");
        REQUIRE(value.has_value());
        REQUIRE(std::holds_alternative<std::nullptr_t>(value.value()));
    }

    SECTION("Get non-existent field returns nullopt") {
        auto value = point.getField("nonexistent");
        REQUIRE_FALSE(value.has_value());
        REQUIRE_FALSE(point.hasField("nonexistent"));
    }

    SECTION("Update existing field") {
        point.setField("count", 10.0);
        point.setField("count", 20.0);
        
        auto value = point.getField("count");
        REQUIRE(value.has_value());
        REQUIRE(std::get<double>(value.value()) == 20.0);
        REQUIRE(point.size() == 1); // Still one field
    }

    SECTION("Remove field") {
        point.setField("temp", 1.0);
        REQUIRE(point.hasField("temp"));
        
        bool removed = point.removeField("temp");
        REQUIRE(removed);
        REQUIRE_FALSE(point.hasField("temp"));
        REQUIRE(point.empty());
    }

    SECTION("Remove non-existent field returns false") {
        bool removed = point.removeField("nonexistent");
        REQUIRE_FALSE(removed);
    }
}

TEST_CASE("DataPoint with multiple field types", "[DataPoint]") {
    varlor::models::DataPoint point;

    SECTION("Store all field types simultaneously") {
        point.setField("numeric", 42.5);
        point.setField("text", std::string("Hello"));
        point.setField("boolean", false);
        point.setField("null", nullptr);

        REQUIRE(point.size() == 4);
        REQUIRE(std::holds_alternative<double>(point.getField("numeric").value()));
        REQUIRE(std::holds_alternative<std::string>(point.getField("text").value()));
        REQUIRE(std::holds_alternative<bool>(point.getField("boolean").value()));
        REQUIRE(std::holds_alternative<std::nullptr_t>(point.getField("null").value()));
    }
}

TEST_CASE("DataPoint field access", "[DataPoint]") {
    varlor::models::DataPoint point;
    point.setField("a", 1.0);
    point.setField("b", std::string("test"));
    point.setField("c", true);

    SECTION("Get all fields via getFields()") {
        const auto& fields = point.getFields();
        REQUIRE(fields.size() == 3);
        REQUIRE(fields.find("a") != fields.end());
        REQUIRE(fields.find("b") != fields.end());
        REQUIRE(fields.find("c") != fields.end());
    }

    SECTION("Modify fields via getFields()") {
        auto& fields = point.getFields();
        fields["d"] = 99.0;
        REQUIRE(point.hasField("d"));
        REQUIRE(point.size() == 4);
    }
}

TEST_CASE("DataPoint move semantics", "[DataPoint]") {
    SECTION("Move field value") {
        varlor::models::DataPoint point;
        std::string largeString(1000, 'x');
        
        point.setField("large", std::move(largeString));
        REQUIRE(point.hasField("large"));
        REQUIRE(largeString.empty()); // Moved from
    }
}

