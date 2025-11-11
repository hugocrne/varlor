#pragma once

#include <algorithm>
#include <cctype>
#include <cmath>
#include <cstdlib>
#include <limits>
#include <stdexcept>
#include <string>
#include <string_view>
#include <unordered_map>
#include <utility>
#include <vector>

namespace exprtk {

namespace details {

enum class TokenKind {
    Number,
    Variable,
    Operator,
    Function
};

enum class FunctionKind {
    Sin,
    Cos,
    Tan,
    ASin,
    ACos,
    ATan,
    Abs,
    Sqrt,
    Exp,
    Log10,
    Ln,
    Pow,
    Floor,
    Ceil,
    Round,
    Min,
    Max
};

template<typename T>
struct Token {
    TokenKind kind{TokenKind::Number};
    T number{};
    const T* variable{nullptr};
    char op{'\0'};
    FunctionKind function{};
    std::size_t arg_count{0};
};

struct OperatorEntry {
    char symbol;
    int precedence;
    bool right_associative;
    bool is_function_parenthesis;
};

struct FunctionDefinition {
    FunctionKind kind;
    std::size_t min_args;
    std::size_t max_args;
    bool has_max;
};

struct FunctionState {
    FunctionKind kind;
    std::size_t arg_count{1};
    bool has_tokens{false};
    std::string name;
};

inline int precedence(char op) {
    switch (op) {
        case '^': return 4;
        case '*':
        case '/': return 3;
        case '+':
        case '-': return 2;
        default: return 0;
    }
}

inline bool is_right_associative(char op) {
    return op == '^';
}

inline bool is_operator(char c) {
    return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
}

} // namespace details

template<typename T>
class symbol_table {
public:
    using value_type = T;

    bool add_variable(const std::string& name, T& ref) {
        return variables_.emplace(name, &ref).second;
    }

    void add_constants() {
        static T pi = static_cast<T>(3.14159265358979323846);
        static T e = static_cast<T>(2.71828182845904523536);
        variables_["pi"] = &pi;
        variables_["e"] = &e;
    }

    T* resolve(const std::string& name) const {
        const auto it = variables_.find(name);
        if (it == variables_.end()) {
            return nullptr;
        }
        return it->second;
    }

private:
    template<typename U> friend class parser;
    template<typename U> friend class expression;

    std::unordered_map<std::string, T*> variables_;
};

template<typename T>
class expression {
public:
    using value_type = T;
    using symbol_table_type = symbol_table<T>;
    using token_type = details::Token<T>;

    void register_symbol_table(symbol_table_type& table) {
        symbol_table_ = &table;
    }

    symbol_table_type* get_symbol_table() {
        return symbol_table_;
    }

    const symbol_table_type* get_symbol_table() const {
        return symbol_table_;
    }

    T value() const {
        if (!symbol_table_) {
            throw std::runtime_error("exprtk::expression - symbol table not registered");
        }
        std::vector<T> stack;
        stack.reserve(rpn_.size());

        for (const auto& token : rpn_) {
            switch (token.kind) {
                case details::TokenKind::Number:
                    stack.push_back(token.number);
                    break;
                case details::TokenKind::Variable:
                    if (!token.variable) {
                        throw std::runtime_error("exprtk::expression - unresolved variable");
                    }
                    stack.push_back(*token.variable);
                    break;
                case details::TokenKind::Operator: {
                    if (stack.size() < 2) {
                        throw std::runtime_error("exprtk::expression - insufficient operands");
                    }
                    const T rhs = stack.back();
                    stack.pop_back();
                    const T lhs = stack.back();
                    stack.pop_back();
                    switch (token.op) {
                        case '+': stack.push_back(lhs + rhs); break;
                        case '-': stack.push_back(lhs - rhs); break;
                        case '*': stack.push_back(lhs * rhs); break;
                        case '/': stack.push_back(lhs / rhs); break;
                        case '^': stack.push_back(std::pow(lhs, rhs)); break;
                        default:
                            throw std::runtime_error("exprtk::expression - unknown operator");
                    }
                    break;
                }
                case details::TokenKind::Function: {
                    if (stack.size() < token.arg_count) {
                        throw std::runtime_error("exprtk::expression - insufficient arguments");
                    }
                    const auto start = stack.end() - static_cast<std::ptrdiff_t>(token.arg_count);
                    std::vector<T> args(start, stack.end());
                    stack.erase(start, stack.end());

                    const auto apply = [&](const auto& fn) {
                        stack.push_back(fn(args));
                    };

                    switch (token.function) {
                        case details::FunctionKind::Sin: apply([](const std::vector<T>& a){ return std::sin(a[0]); }); break;
                        case details::FunctionKind::Cos: apply([](const std::vector<T>& a){ return std::cos(a[0]); }); break;
                        case details::FunctionKind::Tan: apply([](const std::vector<T>& a){ return std::tan(a[0]); }); break;
                        case details::FunctionKind::ASin: apply([](const std::vector<T>& a){ return std::asin(a[0]); }); break;
                        case details::FunctionKind::ACos: apply([](const std::vector<T>& a){ return std::acos(a[0]); }); break;
                        case details::FunctionKind::ATan: apply([](const std::vector<T>& a){ return std::atan(a[0]); }); break;
                        case details::FunctionKind::Abs: apply([](const std::vector<T>& a){ return std::fabs(a[0]); }); break;
                        case details::FunctionKind::Sqrt: apply([](const std::vector<T>& a){ return std::sqrt(a[0]); }); break;
                        case details::FunctionKind::Exp: apply([](const std::vector<T>& a){ return std::exp(a[0]); }); break;
                        case details::FunctionKind::Log10: apply([](const std::vector<T>& a){ return std::log10(a[0]); }); break;
                        case details::FunctionKind::Ln: apply([](const std::vector<T>& a){ return std::log(a[0]); }); break;
                        case details::FunctionKind::Pow: apply([](const std::vector<T>& a){ return std::pow(a[0], a[1]); }); break;
                        case details::FunctionKind::Floor: apply([](const std::vector<T>& a){ return std::floor(a[0]); }); break;
                        case details::FunctionKind::Ceil: apply([](const std::vector<T>& a){ return std::ceil(a[0]); }); break;
                        case details::FunctionKind::Round: apply([](const std::vector<T>& a){ return std::round(a[0]); }); break;
                        case details::FunctionKind::Min:
                            apply([](const std::vector<T>& a){
                                return *std::min_element(a.begin(), a.end());
                            });
                            break;
                        case details::FunctionKind::Max:
                            apply([](const std::vector<T>& a){
                                return *std::max_element(a.begin(), a.end());
                            });
                            break;
                        default:
                            throw std::runtime_error("exprtk::expression - unsupported function");
                    }
                    break;
                }
            }
        }

        if (stack.size() != 1) {
            throw std::runtime_error("exprtk::expression - evaluation failed");
        }
        return stack.back();
    }

private:
    template<typename U> friend class parser;

    symbol_table_type* symbol_table_{nullptr};
    std::vector<token_type> rpn_;
};

template<typename T>
class parser {
public:
    using expression_type = expression<T>;
    using symbol_table_type = symbol_table<T>;
    using token_type = typename expression_type::token_type;

    bool compile(const std::string& expression_string, expression_type& expr) {
        last_error_.clear();
        auto* table = expr.get_symbol_table();
        if (table == nullptr) {
            last_error_ = "symbol table not registered";
            return false;
        }

        const auto function_map = make_function_map();
        std::vector<token_type> output;
        output.reserve(expression_string.size());
        std::vector<details::OperatorEntry> operators;
        std::vector<details::FunctionState> functions;

        auto push_operator = [&](char op) {
            const int prec = details::precedence(op);
            const bool right = details::is_right_associative(op);
            while (!operators.empty()) {
                const auto& top = operators.back();
                if (top.symbol == '(') {
                    break;
                }
                if ((top.precedence > prec) || (top.precedence == prec && !right)) {
                    output.push_back(token_type{details::TokenKind::Operator, T{}, nullptr, top.symbol, {}, 0});
                    operators.pop_back();
                } else {
                    break;
                }
            }
            operators.push_back(details::OperatorEntry{op, prec, right, false});
        };

        auto apply_function = [&](const details::FunctionState& state) -> bool {
            const auto it = function_map.find(state.name);
            if (it == function_map.end()) {
                last_error_ = "unknown function: " + state.name;
                return false;
            }

            const auto& def = it->second;
            std::size_t arg_count = state.arg_count;
            if (!state.has_tokens) {
                if (arg_count == 1) {
                    arg_count = 0;
                } else {
                    last_error_ = "empty argument in function: " + state.name;
                    return false;
                }
            }

            if (arg_count < def.min_args) {
                last_error_ = "not enough arguments for function: " + state.name;
                return false;
            }
            if (def.has_max && arg_count > def.max_args) {
                last_error_ = "too many arguments for function: " + state.name;
                return false;
            }

            token_type token;
            token.kind = details::TokenKind::Function;
            token.function = def.kind;
            token.arg_count = arg_count;
            output.push_back(token);
            return true;
        };

        bool expect_operand = true;
        for (std::size_t i = 0; i < expression_string.size();) {
            const char c = expression_string[i];
            if (std::isspace(static_cast<unsigned char>(c))) {
                ++i;
                continue;
            }

            if (std::isdigit(static_cast<unsigned char>(c)) || c == '.') {
                const std::size_t start = i;
                char* end_ptr = nullptr;
                const double value = std::strtod(&expression_string[start], &end_ptr);
                if (start == static_cast<std::size_t>(end_ptr - &expression_string[0])) {
                    last_error_ = "invalid numeric literal";
                    return false;
                }
                const std::size_t consumed = static_cast<std::size_t>(end_ptr - &expression_string[0]) - start;
                i = start + consumed;
                output.push_back(token_type{details::TokenKind::Number, static_cast<T>(value), nullptr, {}, {}, 0});
                if (!functions.empty()) {
                    functions.back().has_tokens = true;
                }
                expect_operand = false;
                continue;
            }

            if (std::isalpha(static_cast<unsigned char>(c)) || c == '_') {
                const std::size_t start = i;
                ++i;
                while (i < expression_string.size()) {
                    const char nc = expression_string[i];
                    if (std::isalnum(static_cast<unsigned char>(nc)) || nc == '_') {
                        ++i;
                    } else {
                        break;
                    }
                }
                std::string identifier = expression_string.substr(start, i - start);
                std::size_t lookahead = i;
                while (lookahead < expression_string.size() &&
                       std::isspace(static_cast<unsigned char>(expression_string[lookahead]))) {
                    ++lookahead;
                }
                if (lookahead < expression_string.size() && expression_string[lookahead] == '(') {
                    const auto it = function_map.find(identifier);
                    if (it == function_map.end()) {
                        last_error_ = "unknown function: " + identifier;
                        return false;
                    }
                    functions.push_back(details::FunctionState{it->second.kind, 1, false, identifier});
                    operators.push_back(details::OperatorEntry{'(', 0, false, true});
                    expect_operand = true;
                } else {
                    auto* variable_ptr = table->resolve(identifier);
                    if (!variable_ptr) {
                        last_error_ = "unknown symbol: " + identifier;
                        return false;
                    }
                    output.push_back(token_type{details::TokenKind::Variable, T{}, variable_ptr, {}, {}, 0});
                    if (!functions.empty()) {
                        functions.back().has_tokens = true;
                    }
                    expect_operand = false;
                }
                continue;
            }

            if (c == '(') {
                operators.push_back(details::OperatorEntry{'(', 0, false, false});
                ++i;
                expect_operand = true;
                continue;
            }

            if (c == ')') {
                bool found_parenthesis = false;
                while (!operators.empty()) {
                    const auto top = operators.back();
                    if (top.symbol == '(') {
                        operators.pop_back();
                        found_parenthesis = true;
                        if (top.is_function_parenthesis) {
                            const auto state = functions.back();
                            functions.pop_back();
                            if (!apply_function(state)) {
                                return false;
                            }
                        }
                        break;
                    } else {
                        output.push_back(token_type{details::TokenKind::Operator, T{}, nullptr, top.symbol, {}, 0});
                        operators.pop_back();
                    }
                }
                if (!found_parenthesis) {
                    last_error_ = "mismatched parentheses";
                    return false;
                }
                ++i;
                expect_operand = false;
                continue;
            }

            if (c == ',') {
                if (functions.empty()) {
                    last_error_ = "unexpected comma";
                    return false;
                }
                auto& state = functions.back();
                if (!state.has_tokens) {
                    last_error_ = "empty argument in function: " + state.name;
                    return false;
                }
                state.arg_count += 1;
                state.has_tokens = false;

                while (!operators.empty() && operators.back().symbol != '(') {
                    const auto top = operators.back();
                    output.push_back(token_type{details::TokenKind::Operator, T{}, nullptr, top.symbol, {}, 0});
                    operators.pop_back();
                }
                if (operators.empty()) {
                    last_error_ = "mismatched parentheses";
                    return false;
                }

                ++i;
                expect_operand = true;
                continue;
            }

            if (details::is_operator(c)) {
                if (expect_operand) {
                    if (c == '+') {
                        ++i;
                        continue;
                    }
                    if (c == '-') {
                        output.push_back(token_type{details::TokenKind::Number, T(0), nullptr, {}, {}, 0});
                        if (!functions.empty()) {
                            functions.back().has_tokens = true;
                        }
                        push_operator('-');
                        ++i;
                        expect_operand = true;
                        continue;
                    }
                    last_error_ = "unexpected operator";
                    return false;
                }
                push_operator(c);
                ++i;
                expect_operand = true;
                continue;
            }

            last_error_ = std::string("unexpected character: ") + c;
            return false;
        }

        while (!operators.empty()) {
            const auto top = operators.back();
            if (top.symbol == '(') {
                last_error_ = "mismatched parentheses";
                return false;
            }
            output.push_back(token_type{details::TokenKind::Operator, T{}, nullptr, top.symbol, {}, 0});
            operators.pop_back();
        }

        if (!functions.empty()) {
            last_error_ = "mismatched function call";
            return false;
        }

        expr.rpn_ = std::move(output);
        return true;
    }

    const std::string& error() const {
        return last_error_;
    }

private:
    static std::unordered_map<std::string, details::FunctionDefinition> make_function_map() {
        using details::FunctionDefinition;
        using details::FunctionKind;
        return {
            {"sin",   FunctionDefinition{FunctionKind::Sin,   1, 1, true}},
            {"cos",   FunctionDefinition{FunctionKind::Cos,   1, 1, true}},
            {"tan",   FunctionDefinition{FunctionKind::Tan,   1, 1, true}},
            {"asin",  FunctionDefinition{FunctionKind::ASin,  1, 1, true}},
            {"acos",  FunctionDefinition{FunctionKind::ACos,  1, 1, true}},
            {"atan",  FunctionDefinition{FunctionKind::ATan,  1, 1, true}},
            {"abs",   FunctionDefinition{FunctionKind::Abs,   1, 1, true}},
            {"sqrt",  FunctionDefinition{FunctionKind::Sqrt,  1, 1, true}},
            {"exp",   FunctionDefinition{FunctionKind::Exp,   1, 1, true}},
            {"log",   FunctionDefinition{FunctionKind::Log10, 1, 1, true}},
            {"ln",    FunctionDefinition{FunctionKind::Ln,    1, 1, true}},
            {"pow",   FunctionDefinition{FunctionKind::Pow,   2, 2, true}},
            {"floor", FunctionDefinition{FunctionKind::Floor, 1, 1, true}},
            {"ceil",  FunctionDefinition{FunctionKind::Ceil,  1, 1, true}},
            {"round", FunctionDefinition{FunctionKind::Round, 1, 1, true}},
            {"min",   FunctionDefinition{FunctionKind::Min,   1, 0, false}},
            {"max",   FunctionDefinition{FunctionKind::Max,   1, 0, false}}
        };
    }

    std::string last_error_;
};

} // namespace exprtk


