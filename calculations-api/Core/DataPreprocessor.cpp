#include "DataPreprocessor.hpp"

#include <algorithm>
#include <cctype>
#include <cmath>
#include <limits>
#include <sstream>
#include <stdexcept>

namespace varlor::core {

namespace {
constexpr const char* kImputationReason = "missing_value_replacement";
constexpr const char* kOutlierReason = "iqr_detection";
constexpr const char* kColumnsSection = "columns";
constexpr const char* kStatusSection = "status";
constexpr const char* kImputationSection = "imputation";
} // namespace

DataPreprocessor::DataPreprocessor(double outlierThresholdMultiplier)
    : outlierThresholdMultiplier_(outlierThresholdMultiplier) {
    if (outlierThresholdMultiplier_ <= 0.0) {
        throw std::invalid_argument("Le multiplicateur d'outliers doit être strictement positif.");
    }
}

models::PreprocessingResult DataPreprocessor::process(const models::Dataset& dataset) const {
    models::PreprocessingResult result;
    result.cleanedDataset = dataset;
    result.outliersDataset = models::Dataset(dataset.getColumnNames());

    models::PreprocessingReport report;
    report.setInputRowCount(dataset.getRowCount());

    const auto& columnNames = dataset.getColumnNames();
    std::unordered_map<std::string, ColumnProfile> profiles;
    profiles.reserve(columnNames.size());

    for (const auto& columnName : columnNames) {
        profiles.emplace(columnName, analyseAndNormalizeColumn(dataset, result.cleanedDataset, columnName, report));
    }

    auto outlierMask = buildOutlierMask(profiles, dataset.getRowCount());
    const std::size_t outliersCount = splitOutliers(outlierMask, result.cleanedDataset, result.outliersDataset);
    if (outliersCount > 0U) {
        report.incrementOutliersRemoved(outliersCount);
    }

    const std::size_t imputedCount = imputeMissingValues(profiles, result.cleanedDataset);
    if (imputedCount > 0U) {
        report.incrementMissingValuesReplaced(imputedCount);
    }

    report.setOutputRowCount(result.cleanedDataset.getRowCount());
    result.report = std::move(report);

    return result;
}

DataPreprocessor::ColumnProfile DataPreprocessor::analyseAndNormalizeColumn(
    const models::Dataset& source,
    models::Dataset& target,
    const std::string& columnName,
    models::PreprocessingReport& report) const {

    ColumnProfile profile;
    const std::size_t rowCount = source.getRowCount();
    if (rowCount == 0U) {
        return profile;
    }

    struct Observation {
        bool isMissing{false};
        std::optional<double> numericValue{};
        std::optional<bool> booleanValue{};
        std::optional<std::string> textValue{};
    };

    std::vector<Observation> observations(rowCount);
    std::size_t numericConvertible = 0U;
    std::size_t booleanConvertible = 0U;
    std::size_t textOnly = 0U;

    for (std::size_t row = 0; row < rowCount; ++row) {
        const auto& originalPoint = source.getDataPoint(row);
        const auto maybeField = originalPoint.getField(columnName);
        if (!maybeField.has_value() || std::holds_alternative<std::nullptr_t>(maybeField.value())) {
            observations[row].isMissing = true;
            continue;
        }

        const auto& value = maybeField.value();
        double numericCandidate{};
        if (tryParseDouble(value, numericCandidate)) {
            observations[row].numericValue = numericCandidate;
            ++numericConvertible;
        }

        bool booleanCandidate{};
        if (tryParseBoolean(value, booleanCandidate)) {
            observations[row].booleanValue = booleanCandidate;
            ++booleanConvertible;
        }

        if (std::holds_alternative<std::string>(value)) {
            const auto& text = std::get<std::string>(value);
            observations[row].textValue = text;
            if (!observations[row].numericValue.has_value() && !observations[row].booleanValue.has_value()) {
                ++textOnly;
            }
        } else if (std::holds_alternative<double>(value) || std::holds_alternative<bool>(value)) {
            observations[row].textValue = toStringValue(value);
        }
    }

    if (numericConvertible == 0U && booleanConvertible == 0U && textOnly == 0U) {
        profile.type = models::FieldType::Unknown;
        return profile;
    }

    if (textOnly > 0U && (numericConvertible > 0U || booleanConvertible > 0U)) {
        profile.type = models::FieldType::Unknown;
        return profile;
    }

    if (numericConvertible >= booleanConvertible && numericConvertible >= textOnly) {
        profile.type = models::FieldType::Numeric;
    } else if (booleanConvertible >= numericConvertible && booleanConvertible >= textOnly) {
        profile.type = models::FieldType::Boolean;
    } else {
        profile.type = models::FieldType::Text;
    }

    auto& targetPoints = target.getDataPoints();
    if (targetPoints.size() != rowCount) {
        return profile;
    }

    if (profile.type != models::FieldType::Unknown) {
        report.addNormalizedField(columnName);
        profile.normalized = true;
    }

    for (std::size_t row = 0; row < rowCount; ++row) {
        auto& point = targetPoints[row];
        switch (profile.type) {
        case models::FieldType::Numeric:
            if (observations[row].numericValue.has_value()) {
                const double normalizedValue = observations[row].numericValue.value();
                point.setField(columnName, normalizedValue);
                profile.numericSamples.emplace_back(row, normalizedValue);
            } else {
                point.setField(columnName, std::nullptr_t{});
            }
            break;

        case models::FieldType::Boolean:
            if (observations[row].booleanValue.has_value()) {
                point.setField(columnName, observations[row].booleanValue.value());
            } else {
                point.setField(columnName, std::nullptr_t{});
            }
            break;

        case models::FieldType::Text:
            if (observations[row].isMissing) {
                point.setField(columnName, std::nullptr_t{});
                break;
            }
            if (observations[row].textValue.has_value()) {
                point.setField(columnName, observations[row].textValue.value());
            } else if (observations[row].numericValue.has_value()) {
                std::ostringstream stream;
                stream << observations[row].numericValue.value();
                point.setField(columnName, stream.str());
            } else if (observations[row].booleanValue.has_value()) {
                point.setField(columnName, observations[row].booleanValue.value() ? std::string("true") : std::string("false"));
            } else {
                point.setField(columnName, std::string{});
            }
            break;

        case models::FieldType::Unknown:
        default:
            point.setField(columnName, std::nullptr_t{});
            break;
        }
    }

    return profile;
}

std::vector<bool> DataPreprocessor::buildOutlierMask(
    const std::unordered_map<std::string, ColumnProfile>& profiles,
    std::size_t rowCount) const {

    std::vector<bool> mask(rowCount, false);
    for (const auto& [columnName, profile] : profiles) {
        (void)columnName;
        if (profile.type != models::FieldType::Numeric) {
            continue;
        }
        if (profile.numericSamples.size() < 4U) {
            continue;
        }

        std::vector<double> values;
        values.reserve(profile.numericSamples.size());
        for (const auto& sample : profile.numericSamples) {
            values.push_back(sample.second);
        }

        std::sort(values.begin(), values.end());

        const auto quartiles = computeQuartiles(values);
        const double q1 = quartiles.first;
        const double q3 = quartiles.second;
        const double iqr = q3 - q1;
        const double lowerBound = q1 - outlierThresholdMultiplier_ * iqr;
        const double upperBound = q3 + outlierThresholdMultiplier_ * iqr;

        for (const auto& [rowIndex, value] : profile.numericSamples) {
            if (value < lowerBound || value > upperBound) {
                if (rowIndex < mask.size()) {
                    mask[rowIndex] = true;
                }
            }
        }
    }

    return mask;
}

std::size_t DataPreprocessor::splitOutliers(
    const std::vector<bool>& outlierMask,
    models::Dataset& cleanedDataset,
    models::Dataset& outliersDataset) const {

    auto& points = cleanedDataset.getDataPoints();
    if (points.empty()) {
        return 0U;
    }

    std::vector<models::DataPoint> retained;
    retained.reserve(points.size());

    std::size_t moved = 0U;
    for (std::size_t index = 0; index < points.size(); ++index) {
        const bool isOutlier = (index < outlierMask.size()) ? outlierMask[index] : false;
        if (isOutlier) {
            auto point = std::move(points[index]);
            annotateOutlier(point);
            outliersDataset.addDataPoint(std::move(point));
            ++moved;
        } else {
            retained.push_back(std::move(points[index]));
        }
    }

    points = std::move(retained);
    return moved;
}

std::size_t DataPreprocessor::imputeMissingValues(
    const std::unordered_map<std::string, ColumnProfile>& profiles,
    models::Dataset& cleanedDataset) const {

    std::size_t total = 0U;
    for (const auto& [columnName, profile] : profiles) {
        switch (profile.type) {
        case models::FieldType::Numeric:
            total += imputeNumericColumn(cleanedDataset, columnName);
            break;
        case models::FieldType::Boolean:
            total += imputeBooleanColumn(cleanedDataset, columnName);
            break;
        case models::FieldType::Text:
            total += imputeTextColumn(cleanedDataset, columnName);
            break;
        default:
            break;
        }
    }
    return total;
}

std::size_t DataPreprocessor::imputeNumericColumn(models::Dataset& dataset, const std::string& columnName) const {
    auto& points = dataset.getDataPoints();
    std::vector<double> values;
    values.reserve(points.size());

    for (const auto& point : points) {
        const auto field = point.getField(columnName);
        if (field.has_value() && std::holds_alternative<double>(field.value())) {
            values.push_back(std::get<double>(field.value()));
        }
    }

    if (values.empty()) {
        values.push_back(0.0);
    }

    const double medianValue = computeMedian(values);
    const models::FieldValue metaValue{medianValue};

    std::size_t imputed = 0U;
    for (auto& point : points) {
        const auto field = point.getField(columnName);
        if (!field.has_value() || std::holds_alternative<std::nullptr_t>(field.value())) {
            point.setField(columnName, medianValue);
            annotateImputation(point, columnName, "median", metaValue);
            ++imputed;
        }
    }

    return imputed;
}

std::size_t DataPreprocessor::imputeBooleanColumn(models::Dataset& dataset, const std::string& columnName) const {
    auto& points = dataset.getDataPoints();
    std::size_t trueCount = 0U;
    std::size_t falseCount = 0U;

    for (const auto& point : points) {
        const auto field = point.getField(columnName);
        if (field.has_value() && std::holds_alternative<bool>(field.value())) {
            if (std::get<bool>(field.value())) {
                ++trueCount;
            } else {
                ++falseCount;
            }
        }
    }

    const bool imputedValue = (trueCount >= falseCount);
    const models::FieldValue metaValue{imputedValue};

    std::size_t imputed = 0U;
    for (auto& point : points) {
        const auto field = point.getField(columnName);
        if (!field.has_value() || std::holds_alternative<std::nullptr_t>(field.value())) {
            point.setField(columnName, imputedValue);
            annotateImputation(point, columnName, "mode_boolean", metaValue);
            ++imputed;
        }
    }

    return imputed;
}

std::size_t DataPreprocessor::imputeTextColumn(models::Dataset& dataset, const std::string& columnName) const {
    auto& points = dataset.getDataPoints();
    std::unordered_map<std::string, std::size_t> frequencies;

    for (const auto& point : points) {
        const auto field = point.getField(columnName);
        if (field.has_value() && std::holds_alternative<std::string>(field.value())) {
            ++frequencies[std::get<std::string>(field.value())];
        }
    }

    std::string imputedValue;
    std::size_t bestCount = 0U;
    for (const auto& [value, count] : frequencies) {
        if (count > bestCount || (count == bestCount && value < imputedValue)) {
            imputedValue = value;
            bestCount = count;
        }
    }

    const models::FieldValue metaValue{imputedValue};
    std::size_t imputed = 0U;
    for (auto& point : points) {
        const auto field = point.getField(columnName);
        if (!field.has_value() || std::holds_alternative<std::nullptr_t>(field.value())) {
            point.setField(columnName, imputedValue);
            annotateImputation(point, columnName, "mode_text", metaValue);
            ++imputed;
        }
    }

    return imputed;
}

void DataPreprocessor::annotateOutlier(models::DataPoint& point) const {
    auto& statusSection = point.getMeta().ensureSection(kStatusSection);
    statusSection.setLeaf("outlier", true);
    statusSection.setLeaf("reason", std::string(kOutlierReason));
    statusSection.setLeaf("method", std::string("iqr"));
}

void DataPreprocessor::annotateImputation(
    models::DataPoint& point,
    const std::string& columnName,
    const std::string& strategy,
    const models::FieldValue& imputedValue) const {

    auto& columnsSection = point.getMeta().ensureSection(kColumnsSection);
    auto& columnSection = columnsSection.ensureSection(columnName);
    auto& imputationSection = columnSection.ensureSection(kImputationSection);

    imputationSection.setLeaf("imputed", true);
    imputationSection.setLeaf("reason", std::string(kImputationReason));
    imputationSection.setLeaf("strategy", strategy);
    imputationSection.setLeaf("value", imputedValue);
}

double DataPreprocessor::computeMedian(std::vector<double> values) const {
    if (values.empty()) {
        return 0.0;
    }

    std::sort(values.begin(), values.end());
    const std::size_t mid = values.size() / 2;
    if (values.size() % 2 == 0U) {
        return (values[mid - 1] + values[mid]) / 2.0;
    }
    return values[mid];
}

std::pair<double, double> DataPreprocessor::computeQuartiles(std::vector<double> values) const {
    if (values.empty()) {
        return {0.0, 0.0};
    }

    std::sort(values.begin(), values.end());
    const std::size_t mid = values.size() / 2;

    auto medianOfRange = [](std::vector<double>::const_iterator begin, std::vector<double>::const_iterator end) -> double {
        const std::size_t length = static_cast<std::size_t>(std::distance(begin, end));
        if (length == 0U) {
            return 0.0;
        }
        const std::size_t localMid = length / 2;
        if (length % 2 == 0U) {
            return (*(begin + static_cast<std::ptrdiff_t>(localMid - 1)) + *(begin + static_cast<std::ptrdiff_t>(localMid))) / 2.0;
        }
        return *(begin + static_cast<std::ptrdiff_t>(localMid));
    };

    const bool even = (values.size() % 2 == 0U);
    const auto lowerEnd = values.begin() + static_cast<std::ptrdiff_t>(mid);
    const auto upperBegin = values.begin() + static_cast<std::ptrdiff_t>(even ? mid : mid + 1);

    const double q1 = medianOfRange(values.begin(), lowerEnd);
    const double q3 = medianOfRange(upperBegin, values.end());

    return {q1, q3};
}

bool DataPreprocessor::tryParseDouble(const models::FieldValue& value, double& out) const {
    if (std::holds_alternative<double>(value)) {
        out = std::get<double>(value);
        return true;
    }
    if (std::holds_alternative<std::string>(value)) {
        const std::string trimmed = trim(std::get<std::string>(value));
        if (trimmed.empty()) {
            return false;
        }
        try {
            size_t processed = 0U;
            const double converted = std::stod(trimmed, &processed);
            if (processed != trimmed.size()) {
                return false;
            }
            out = converted;
            return true;
        } catch (const std::exception&) {
            return false;
        }
    }
    return false;
}

bool DataPreprocessor::tryParseBoolean(const models::FieldValue& value, bool& out) const {
    if (std::holds_alternative<bool>(value)) {
        out = std::get<bool>(value);
        return true;
    }
    if (std::holds_alternative<std::string>(value)) {
        std::string lowered = trim(std::get<std::string>(value));
        std::transform(lowered.begin(), lowered.end(), lowered.begin(), [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
        if (lowered == "true" || lowered == "yes" || lowered == "1") {
            out = true;
            return true;
        }
        if (lowered == "false" || lowered == "no" || lowered == "0") {
            out = false;
            return true;
        }
    }
    if (std::holds_alternative<double>(value)) {
        const double numeric = std::get<double>(value);
        if (numeric == 0.0 || numeric == 1.0) {
            out = (numeric != 0.0);
            return true;
        }
    }
    return false;
}

std::string DataPreprocessor::toStringValue(const models::FieldValue& value) const {
    if (std::holds_alternative<std::string>(value)) {
        return std::get<std::string>(value);
    }
    if (std::holds_alternative<double>(value)) {
        std::ostringstream stream;
        stream << std::get<double>(value);
        return stream.str();
    }
    if (std::holds_alternative<bool>(value)) {
        return std::get<bool>(value) ? "true" : "false";
    }
    return {};
}

std::string DataPreprocessor::trim(const std::string& value) const {
    auto front = value.begin();
    auto back = value.end();

    while (front != back && std::isspace(static_cast<unsigned char>(*front)) != 0) {
        ++front;
    }
    while (front != back) {
        auto prev = back;
        --prev;
        if (std::isspace(static_cast<unsigned char>(*prev)) != 0) {
            back = prev;
        } else {
            break;
        }
    }

    return std::string(front, back);
}

} // namespace varlor::core


