#include "MathOperationParser.hpp"

#include "BuiltinOperations.hpp"

#include <algorithm>
#include <array>
#include <cctype>
#include <iomanip>
#include <optional>
#include <regex>
#include <sstream>
#include <stdexcept>
#include <string_view>
#include <unordered_map>
#include <unordered_set>
#include <utility>

namespace varlor::core {

namespace {

using models::Dataset;

constexpr std::array kUnaryAggregates{
    std::pair<std::string_view, double (*)(const Dataset&, const std::string&)>{
        "mean", &BuiltinOperations::mean},
    std::pair<std::string_view, double (*)(const Dataset&, const std::string&)>{
        "median", &BuiltinOperations::median},
    std::pair<std::string_view, double (*)(const Dataset&, const std::string&)>{
        "variance", &BuiltinOperations::variance},
    std::pair<std::string_view, double (*)(const Dataset&, const std::string&)>{
        "stddev", &BuiltinOperations::stddev},
    std::pair<std::string_view, double (*)(const Dataset&, const std::string&)>{
        "min", &BuiltinOperations::min},
    std::pair<std::string_view, double (*)(const Dataset&, const std::string&)>{
        "max", &BuiltinOperations::max},
};

constexpr std::array kAllowedIntrinsicFunctions{
    "sin",
    "cos",
    "tan",
    "asin",
    "acos",
    "atan",
    "abs",
    "sqrt",
    "exp",
    "log",
    "ln",
    "pow",
    "floor",
    "ceil",
    "round",
    "min",
    "max"
};

constexpr std::array kAllowedConstants{"pi", "e"};

[[nodiscard]] std::string trim(std::string_view value) {
    const auto begin = value.find_first_not_of(" \t\n\r");
    if (begin == std::string_view::npos) {
        return {};
    }
    const auto end = value.find_last_not_of(" \t\n\r");
    return std::string(value.substr(begin, end - begin + 1));
}

struct ArgumentExtractionResult {
    std::vector<std::string> arguments;
    std::size_t closingIndex{std::string::npos};
};

[[nodiscard]] ArgumentExtractionResult extractArguments(const std::string& expr, std::size_t openIndex) {
    ArgumentExtractionResult result;
    if (openIndex >= expr.size() || expr[openIndex] != '(') {
        throw std::invalid_argument("Expression invalide : parenthèses attendues.");
    }

    int depth = 1;
    std::size_t cursor = openIndex + 1;
    std::size_t tokenStart = cursor;

    while (cursor < expr.size() && depth > 0) {
        const char current = expr[cursor];
        if (current == '(') {
            ++depth;
        } else if (current == ')') {
            --depth;
            if (depth == 0) {
                const auto token = trim(std::string_view(expr).substr(tokenStart, cursor - tokenStart));
                if (!token.empty()) {
                    result.arguments.emplace_back(token);
                }
                result.closingIndex = cursor;
                break;
            }
        } else if (current == ',' && depth == 1) {
            const auto token = trim(std::string_view(expr).substr(tokenStart, cursor - tokenStart));
            if (!token.empty()) {
                result.arguments.emplace_back(token);
            }
            tokenStart = cursor + 1;
        }
        ++cursor;
    }

    if (depth != 0 || result.closingIndex == std::string::npos) {
        throw std::invalid_argument("Expression invalide : parenthèses non équilibrées.");
    }

    return result;
}

[[nodiscard]] bool isIdentifierBoundary(char c) {
    return !std::isalnum(static_cast<unsigned char>(c)) && c != '_';
}

[[nodiscard]] bool matchesFunctionAt(const std::string& expr, std::size_t pos, std::string_view function) {
    if (pos != 0) {
        const char prev = expr[pos - 1];
        if (!isIdentifierBoundary(prev)) {
            return false;
        }
    }

    if (expr.compare(pos, function.size(), function) != 0) {
        return false;
    }

    const auto endPos = pos + function.size();
    if (endPos >= expr.size()) {
        return false;
    }

    const char next = expr[endPos];
    return std::isspace(static_cast<unsigned char>(next)) || next == '(';
}

[[nodiscard]] std::string formatDouble(double value) {
    std::ostringstream oss;
    oss << std::setprecision(15) << std::defaultfloat << value;
    return oss.str();
}

void replaceUnaryAggregates(std::string& expr, const Dataset& dataset) {
    for (const auto& [name, fn] : kUnaryAggregates) {
        std::size_t pos = 0;
        while ((pos = expr.find(std::string(name), pos)) != std::string::npos) {
            if (!matchesFunctionAt(expr, pos, name)) {
                pos += name.size();
                continue;
            }

            const auto openIndex = expr.find('(', pos + name.size());
            if (openIndex == std::string::npos) {
                throw std::invalid_argument("Expression invalide : parenthèses manquantes pour " + std::string(name));
            }

            const auto args = extractArguments(expr, openIndex);
            if (args.arguments.size() != 1) {
                throw std::invalid_argument(
                    "La fonction " + std::string(name) + " attend exactement un argument.");
            }

            const auto column = args.arguments.front();
            const auto value = fn(dataset, column);
            const auto replacement = formatDouble(value);
            expr.replace(pos, args.closingIndex - pos + 1, replacement);
            pos += replacement.size();
        }
    }
}

void replaceCorrelation(std::string& expr, const Dataset& dataset) {
    constexpr std::string_view name{"correlation"};
    std::size_t pos = 0;
    while ((pos = expr.find(std::string(name), pos)) != std::string::npos) {
        if (!matchesFunctionAt(expr, pos, name)) {
            pos += name.size();
            continue;
        }

        const auto openIndex = expr.find('(', pos + name.size());
        if (openIndex == std::string::npos) {
            throw std::invalid_argument("Expression invalide : parenthèses manquantes pour correlation");
        }

        const auto args = extractArguments(expr, openIndex);
        if (args.arguments.size() != 2) {
            throw std::invalid_argument("correlation attend exactement deux colonnes.");
        }

        const auto value = BuiltinOperations::correlation(dataset, args.arguments[0], args.arguments[1]);
        const auto replacement = formatDouble(value);
        expr.replace(pos, args.closingIndex - pos + 1, replacement);
        pos += replacement.size();
    }
}

void replacePercentile(std::string& expr, const Dataset& dataset) {
    constexpr std::string_view name{"percentile"};
    std::size_t pos = 0;
    while ((pos = expr.find(std::string(name), pos)) != std::string::npos) {
        if (!matchesFunctionAt(expr, pos, name)) {
            pos += name.size();
            continue;
        }

        const auto openIndex = expr.find('(', pos + name.size());
        if (openIndex == std::string::npos) {
            throw std::invalid_argument("Expression invalide : parenthèses manquantes pour percentile");
        }

        const auto args = extractArguments(expr, openIndex);
        if (args.arguments.size() != 2) {
            throw std::invalid_argument("percentile attend deux arguments : colonne et pourcentage.");
        }

        double percentileValue;
        try {
            percentileValue = std::stod(args.arguments[1]);
        } catch (const std::exception&) {
            throw std::invalid_argument("Le second argument de percentile doit être un nombre.");
        }

        const auto value = BuiltinOperations::percentile(dataset, args.arguments[0], percentileValue);
        const auto replacement = formatDouble(value);
        expr.replace(pos, args.closingIndex - pos + 1, replacement);
        pos += replacement.size();
    }
}

void ensureAllowedCharacters(const std::string& expr) {
    static const std::string_view allowedChars =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_+-*/%^(),. \t\n\r";

    for (const char c : expr) {
        if (allowedChars.find(c) == std::string_view::npos) {
            throw std::invalid_argument(
                std::string("Caractère non autorisé dans l'expression : '") + c + "'");
        }
    }
}

[[nodiscard]] std::vector<std::size_t> collectColumns(
    const std::string& expr,
    const Dataset& dataset) {
    const std::regex identifierRegex(R"([A-Za-z_][A-Za-z0-9_]*)");
    const auto begin = std::sregex_iterator(expr.begin(), expr.end(), identifierRegex);
    const auto end = std::sregex_iterator();

    std::unordered_set<std::string> seenColumns;
    std::vector<std::size_t> indices;

    const auto& columnNames = dataset.getColumnNames();

    for (auto it = begin; it != end; ++it) {
        const auto& match = *it;
        const auto name = match.str();

        const auto nextIndex =
            static_cast<std::size_t>(match.position() + match.length());
        const bool isFunctionCall = nextIndex < expr.size() && expr[nextIndex] == '(';

        if (std::find(kAllowedIntrinsicFunctions.begin(), kAllowedIntrinsicFunctions.end(), name) !=
            kAllowedIntrinsicFunctions.end()) {
            continue;
        }

        if (std::find(kAllowedConstants.begin(), kAllowedConstants.end(), name) !=
            kAllowedConstants.end()) {
            continue;
        }

        const auto columnIt =
            std::find(columnNames.begin(), columnNames.end(), name);
        if (columnIt != columnNames.end()) {
            if (seenColumns.insert(name).second) {
                indices.push_back(static_cast<std::size_t>(
                    std::distance(columnNames.begin(), columnIt)));
            }
            continue;
        }

        if (isFunctionCall) {
            throw std::invalid_argument("Fonction non autorisée détectée : " + name);
        }

        throw std::invalid_argument("Référence à un identifiant inconnu : " + name);
    }

    std::sort(indices.begin(), indices.end());
    return indices;
}

} // namespace

MathOperationParser::ParsedExpression MathOperationParser::parse(
    const std::string& expr,
    const Dataset& dataset) const {
    auto trimmed = trim(expr);
    if (trimmed.empty()) {
        throw std::invalid_argument("L'expression fournie est vide.");
    }

    ensureAllowedCharacters(trimmed);

    replaceUnaryAggregates(trimmed, dataset);
    replaceCorrelation(trimmed, dataset);
    replacePercentile(trimmed, dataset);

    ensureAllowedCharacters(trimmed);

    auto usedColumns = collectColumns(trimmed, dataset);

    ParsedExpression result;
    result.originalExpression = expr;
    result.normalizedExpression = trimmed;
    result.variableStorage = std::make_shared<std::vector<double>>(dataset.getColumnCount(), 0.0);
    result.symbolTable = std::make_shared<exprtk::symbol_table<double>>();

    const auto& columnNames = dataset.getColumnNames();
    for (std::size_t i = 0; i < columnNames.size(); ++i) {
        result.symbolTable->add_variable(columnNames[i], (*result.variableStorage)[i]);
    }
    result.symbolTable->add_constants();

    result.expression.register_symbol_table(*result.symbolTable);

    exprtk::parser<double> parser;
    const bool success = parser.compile(result.normalizedExpression, result.expression);
    if (!success) {
        std::ostringstream error;
        error << "Erreur de compilation ExprTk : " << parser.error();
        throw std::runtime_error(error.str());
    }

    result.usedColumnIndices = std::move(usedColumns);
    return result;
}

} // namespace varlor::core



