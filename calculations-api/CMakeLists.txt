cmake_minimum_required(VERSION 3.16)
project(calculations_api)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ✅ Trouver Oat++ automatiquement
find_package(oatpp 1.3.0 REQUIRED)

# ✅ Ajoute ton code source principal
add_executable(calculations_api main.cpp)

# ✅ Lier Oat++ automatiquement
target_link_libraries(calculations_api PRIVATE oatpp::oatpp)