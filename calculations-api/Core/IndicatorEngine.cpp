#include "IndicatorEngine.hpp"

#include "BuiltinOperations.hpp"
#include "ExpressionExecutor.hpp"
#include "MathOperationParser.hpp"

#include <chrono>
#include <ctime>
#include <iomanip>
#include <optional>
#include <regex>
#include <sstream>
#include <stdexcept>
#include <string_view>
#include <unordered_set>
#include <utility>
#include <variant>

namespace varlor::core {

namespace {

using models::OperationDefinition;
using models::OperationResult;

[[nodiscard]] std::string trim(std::string_view value) {
    const auto begin = value.find_first_not_of(" \t\n\r");
    if (begin == std::string_view::npos) {
        return {};
    }
    const auto end = value.find_last_not_of(" \t\n\r");
    return std::string(value.substr(begin, end - begin + 1));
}

[[nodiscard]] std::string toIsoTimestamp() {
    const auto now = std::chrono::system_clock::now();
    const auto seconds = std::chrono::time_point_cast<std::chrono::seconds>(now);
    const auto fractional = std::chrono::duration_cast<std::chrono::milliseconds>(now - seconds);

    const auto timeT = std::chrono::system_clock::to_time_t(now);
    std::tm tm{};
#if defined(_WIN32)
    gmtime_s(&tm, &timeT);
#else
    gmtime_r(&timeT, &tm);
#endif

    std::ostringstream oss;
    oss << std::put_time(&tm, "%FT%T");
    if (fractional.count() > 0) {
        oss << '.' << std::setw(3) << std::setfill('0') << fractional.count();
    }
    oss << 'Z';
    return oss.str();
}

struct BuiltinCall {
    std::string function;
    std::vector<std::string> arguments;
};

[[nodiscard]] std::vector<std::string> splitArguments(const std::string& args) {
    std::vector<std::string> tokens;
    std::size_t start = 0;
    int depth = 0;
    for (std::size_t i = 0; i < args.size(); ++i) {
        const char c = args[i];
        if (c == '(') {
            ++depth;
        } else if (c == ')') {
            --depth;
            if (depth < 0) {
                throw std::invalid_argument("Parenthèses non équilibrées dans l'expression.");
            }
        } else if (c == ',' && depth == 0) {
            tokens.emplace_back(trim(std::string_view(args).substr(start, i - start)));
            start = i + 1;
        }
    }

    const auto last = trim(std::string_view(args).substr(start));
    if (!last.empty()) {
        tokens.push_back(last);
    }

    return tokens;
}

[[nodiscard]] std::optional<BuiltinCall> detectBuiltinCall(const std::string& expression) {
    static const std::unordered_set<std::string> builtinNames{
        "mean", "median", "variance", "stddev", "correlation", "min", "max", "percentile"};

    const auto trimmed = trim(expression);
    const std::regex pattern(R"(^([A-Za-z_][A-Za-z0-9_]*)\s*\((.*)\)$)");
    std::smatch match;
    if (!std::regex_match(trimmed, match, pattern)) {
        return std::nullopt;
    }

    const auto function = match[1].str();
    if (!builtinNames.contains(function)) {
        return std::nullopt;
    }

    const auto args = splitArguments(match[2].str());
    return BuiltinCall{function, args};
}

[[nodiscard]] std::optional<std::string> getParam(
    const OperationDefinition& op,
    std::string_view key) {
    if (!op.params.has_value()) {
        return std::nullopt;
    }
    const auto& params = op.params.value();
    const auto it = params.find(std::string(key));
    if (it == params.end()) {
        return std::nullopt;
    }
    return it->second;
}

[[nodiscard]] double parseDouble(const std::string& value, const std::string& context) {
    try {
        std::size_t consumed = 0;
        const double result = std::stod(value, &consumed);
        if (consumed != value.size()) {
            throw std::invalid_argument("Valeur numérique invalide : " + value);
        }
        return result;
    } catch (const std::exception&) {
        throw std::invalid_argument("Impossible d'interpréter \"" + value + "\" comme nombre pour " + context);
    }
}

[[nodiscard]] double executeBuiltin(
    const models::Dataset& dataset,
    const OperationDefinition& op,
    const BuiltinCall& call) {
    const auto& args = call.arguments;
    if (call.function == "mean" || call.function == "median" || call.function == "variance" ||
        call.function == "stddev" || call.function == "min" || call.function == "max") {
        if (args.size() != 1) {
            throw std::invalid_argument(call.function + " attend exactement une colonne.");
        }
        const auto& column = args.front();
        if (call.function == "mean") {
            return BuiltinOperations::mean(dataset, column);
        }
        if (call.function == "median") {
            return BuiltinOperations::median(dataset, column);
        }
        if (call.function == "variance") {
            return BuiltinOperations::variance(dataset, column);
        }
        if (call.function == "stddev") {
            return BuiltinOperations::stddev(dataset, column);
        }
        if (call.function == "min") {
            return BuiltinOperations::min(dataset, column);
        }
        return BuiltinOperations::max(dataset, column);
    }

    if (call.function == "correlation") {
        if (args.size() != 2) {
            throw std::invalid_argument("correlation attend deux colonnes.");
        }
        return BuiltinOperations::correlation(dataset, args[0], args[1]);
    }

    if (call.function == "percentile") {
        if (args.empty() || args.size() > 2) {
            throw std::invalid_argument("percentile attend 1 ou 2 arguments.");
        }

        const auto& column = args.front();
        double percentileValue;
        if (args.size() == 2) {
            percentileValue = parseDouble(args[1], "percentile");
        } else {
            if (auto param = getParam(op, "percentile")) {
                percentileValue = parseDouble(*param, "percentile");
            } else if (auto paramAlt = getParam(op, "p")) {
                percentileValue = parseDouble(*paramAlt, "percentile");
            } else {
                throw std::invalid_argument(
                    "percentile nécessite un second argument ou un paramètre `percentile`.");
            }
        }
        return BuiltinOperations::percentile(dataset, column, percentileValue);
    }

    throw std::invalid_argument("Fonction builtin inconnue : " + call.function);
}

} // namespace

std::vector<OperationResult> IndicatorEngine::execute(
    const models::Dataset& data,
    const std::vector<OperationDefinition>& operations) const {
    std::vector<OperationResult> results;
    results.reserve(operations.size());

    MathOperationParser parser;
    ExpressionExecutor executor;

    for (const auto& operation : operations) {
        OperationResult result;
        result.expr = operation.alias.value_or(operation.expr);
        result.executedAt = toIsoTimestamp();

        try {
            if (const auto builtin = detectBuiltinCall(operation.expr)) {
                const double value = executeBuiltin(data, operation, *builtin);
                result.result = value;
                result.status = models::OperationStatus::Success;
            } else {
                auto parsed = parser.parse(operation.expr, data);
                auto evaluation = executor.evaluate(parsed, data);

                if (std::holds_alternative<double>(evaluation)) {
                    result.result = std::get<double>(evaluation);
                } else {
                    result.result = std::get<std::vector<double>>(evaluation);
                }
                result.status = models::OperationStatus::Success;
            }
        } catch (const std::exception& ex) {
            result.status = models::OperationStatus::Error;
            result.result = std::monostate{};
            result.errorMessage = ex.what();
        }

        results.push_back(std::move(result));
    }

    return results;
}

} // namespace varlor::core



