cmake_minimum_required(VERSION 3.16)
project(calculations_api)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ✅ Trouver Oat++ automatiquement (optionnel pour permettre les tests sans Oat++)
find_package(oatpp QUIET CONFIG)
find_package(yaml-cpp QUIET)
if(oatpp_FOUND)
    if(NOT yaml-cpp_FOUND)
        message(FATAL_ERROR "yaml-cpp est requis pour construire l'exécutable principal.")
    endif()
    # ✅ Ajoute ton code source principal
    add_executable(calculations_api
        main.cpp
        Core/DataPreprocessor.cpp
        Controllers/AnalysisController.cpp
    )
    # ✅ Lier Oat++ automatiquement
    target_link_libraries(calculations_api PRIVATE oatpp::oatpp yaml-cpp::yaml-cpp)
else()
    message(WARNING "Oat++ not found - main executable will not be built, but tests can still run")
endif()

# ========== Tests ==========
enable_testing()

# Télécharger Catch2 via FetchContent
include(FetchContent)
FetchContent_Declare(
    Catch2
    GIT_REPOSITORY https://github.com/catchorg/Catch2.git
    GIT_TAG        v3.5.2
)
FetchContent_MakeAvailable(Catch2)

# Inclure le module Catch2 pour catch_discover_tests
list(APPEND CMAKE_MODULE_PATH ${catch2_SOURCE_DIR}/extras)
include(CTest)
include(Catch)

# Tests pour les modèles
add_executable(test_models
    tests/models/test_FieldType.cpp
    tests/models/test_DataPoint.cpp
    tests/models/test_Dataset.cpp
    tests/models/test_PreprocessingReport.cpp
)

target_link_libraries(test_models PRIVATE Catch2::Catch2WithMain)

# Enregistrer les tests avec CTest
catch_discover_tests(test_models)

# Tests pour le cœur de logique
add_executable(test_core
    tests/core/test_DataPreprocessor.cpp
    Core/DataPreprocessor.cpp
)

target_link_libraries(test_core PRIVATE Catch2::Catch2WithMain)

catch_discover_tests(test_core)

add_executable(test_controllers
    tests/controllers/test_AnalysisController.cpp
    Controllers/AnalysisController.cpp
    Core/DataPreprocessor.cpp
)

target_link_libraries(test_controllers PRIVATE Catch2::Catch2WithMain oatpp::oatpp oatpp::oatpp-test yaml-cpp::yaml-cpp)

catch_discover_tests(test_controllers)