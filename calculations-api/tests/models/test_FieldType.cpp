#include <catch2/catch_test_macros.hpp>
#include "../../Models/FieldType.hpp"

TEST_CASE("FieldType enum values", "[FieldType]") {
    SECTION("All enum values are accessible") {
        auto numeric = varlor::models::FieldType::Numeric;
        auto text = varlor::models::FieldType::Text;
        auto boolean = varlor::models::FieldType::Boolean;
        auto unknown = varlor::models::FieldType::Unknown;

        REQUIRE(numeric == varlor::models::FieldType::Numeric);
        REQUIRE(text == varlor::models::FieldType::Text);
        REQUIRE(boolean == varlor::models::FieldType::Boolean);
        REQUIRE(unknown == varlor::models::FieldType::Unknown);
    }

    SECTION("Enum values are distinct") {
        REQUIRE(static_cast<int>(varlor::models::FieldType::Numeric) != 
                static_cast<int>(varlor::models::FieldType::Text));
        REQUIRE(static_cast<int>(varlor::models::FieldType::Text) != 
                static_cast<int>(varlor::models::FieldType::Boolean));
        REQUIRE(static_cast<int>(varlor::models::FieldType::Boolean) != 
                static_cast<int>(varlor::models::FieldType::Unknown));
    }

    SECTION("Enum can be used in switch statements") {
        varlor::models::FieldType type = varlor::models::FieldType::Numeric;
        int count = 0;

        switch (type) {
            case varlor::models::FieldType::Numeric:
                count = 1;
                break;
            case varlor::models::FieldType::Text:
                count = 2;
                break;
            case varlor::models::FieldType::Boolean:
                count = 3;
                break;
            case varlor::models::FieldType::Unknown:
                count = 4;
                break;
        }

        REQUIRE(count == 1);
    }
}

