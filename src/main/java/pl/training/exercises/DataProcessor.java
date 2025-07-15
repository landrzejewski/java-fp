package pl.training.exercises;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

// Data record class
class DataRecord {
    private final String id;
    private final String timestamp;
    private final String value;
    private final String category;

    public DataRecord(String id, String timestamp, String value, String category) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
        this.category = category;
    }

    public String getId() { return id; }
    public String getTimestamp() { return timestamp; }
    public String getValue() { return value; }
    public String getCategory() { return category; }

    @Override
    public String toString() {
        return String.format("Record{id=%s, timestamp=%s, value=%s, category=%s}", id, timestamp, value, category);
    }
}

// Validated record with parsed values
class ValidatedRecord {
    private final String id;
    private final LocalDateTime timestamp;
    private final double value;
    private final String category;

    public ValidatedRecord(String id, LocalDateTime timestamp, double value, String category) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
        this.category = category;
    }

    public String getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public double getValue() { return value; }
    public String getCategory() { return category; }
}

// Category statistics
class CategoryStatistics {
    private final String category;
    private final long count;
    private final double min;
    private final double max;
    private final double average;
    private final double stdDev;

    public CategoryStatistics(String category, long count, double min, double max, double average, double stdDev) {
        this.category = category;
        this.count = count;
        this.min = min;
        this.max = max;
        this.average = average;
        this.stdDev = stdDev;
    }

    @Override
    public String toString() {
        return String.format("CategoryStats{category='%s', count=%d, min=%.2f, max=%.2f, avg=%.2f, stdDev=%.2f}",
                category, count, min, max, average, stdDev);
    }

    public double getAverage() { return average; }
    public double getStdDev() { return stdDev; }
}

// Validation result wrapper
class ValidationResult<T> {
    private final T value;
    private final List<String> errors;
    private final boolean success;

    private ValidationResult(T value, List<String> errors) {
        this.value = value;
        this.errors = errors;
        this.success = errors.isEmpty();
    }

    static <T> ValidationResult<T> success(T value) {
        return new ValidationResult<>(value, Collections.emptyList());
    }

    static <T> ValidationResult<T> failure(List<String> errors) {
        return new ValidationResult<>(null, errors);
    }

    public boolean isSuccess() { return success; }
    public T getValue() { return value; }
    public List<String> getErrors() { return errors; }
}

// Custom collector for category statistics
class CategoryStatsCollector implements Collector<ValidatedRecord,
        CategoryStatsCollector.Accumulator, CategoryStatistics> {

    private final String category;

    static class Accumulator {
        final List<Double> values = new ArrayList<>();
        long count = 0;
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        void add(double value) {
            values.add(value);
            count++;
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        Accumulator combine(Accumulator other) {
            values.addAll(other.values);
            count += other.count;
            sum += other.sum;
            min = Math.min(min, other.min);
            max = Math.max(max, other.max);
            return this;
        }

        CategoryStatistics finish(String category) {
            if (count == 0) {
                return new CategoryStatistics(category, 0, 0, 0, 0, 0);
            }

            double average = sum / count;
            double variance = values.stream()
                    .mapToDouble(v -> Math.pow(v - average, 2))
                    .average()
                    .orElse(0.0);
            double stdDev = Math.sqrt(variance);

            return new CategoryStatistics(category, count, min, max, average, stdDev);
        }
    }

    public CategoryStatsCollector(String category) {
        this.category = category;
    }

    @Override
    public Supplier<Accumulator> supplier() {
        return Accumulator::new;
    }

    @Override
    public BiConsumer<Accumulator, ValidatedRecord> accumulator() {
        return (acc, record) -> acc.add(record.getValue());
    }

    @Override
    public BinaryOperator<Accumulator> combiner() {
        return Accumulator::combine;
    }

    @Override
    public Function<Accumulator, CategoryStatistics> finisher() {
        return acc -> acc.finish(category);
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}

// pl.training.Main data processor
public class DataProcessor {
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final AtomicInteger retryCount = new AtomicInteger(0);

    // Validate a single record
    private ValidationResult<ValidatedRecord> validateRecord(DataRecord record) {
        List<String> errors = new ArrayList<>();

        if (record.getId() == null || record.getId().isEmpty()) {
            errors.add("ID is required");
        }

        LocalDateTime timestamp = null;
        try {
            timestamp = LocalDateTime.parse(record.getTimestamp(), TIMESTAMP_FORMAT);
        } catch (DateTimeParseException e) {
            errors.add("Invalid timestamp format");
        }

        double value = 0;
        try {
            value = Double.parseDouble(record.getValue());
        } catch (NumberFormatException e) {
            errors.add("Value must be numeric");
        }

        if (!errors.isEmpty()) {
            return ValidationResult.failure(errors);
        }

        return ValidationResult.success(
                new ValidatedRecord(record.getId(), timestamp, value, record.getCategory())
        );
    }

    // Process records with error handling
    public Map<String, CategoryStatistics> processRecords(List<List<DataRecord>> sources) {
        // Merge all sources and process in parallel
        List<ValidatedRecord> validRecords = sources.parallelStream()
                .flatMap(List::stream)
                .map(this::validateWithRetry)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());

        // Group by category and collect statistics using the custom collector
        return validRecords.stream()
                .collect(groupingBy(ValidatedRecord::getCategory))
                .entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().stream().collect(new CategoryStatsCollector(e.getKey()))));
    }

    // Validate with retry logic
    private Optional<ValidatedRecord> validateWithRetry(DataRecord record) {
        int maxRetries = 3;
        long backoffMs = 100;

        for (int i = 0; i <= maxRetries; i++) {
            ValidationResult<ValidatedRecord> result = validateRecord(record);

            if (result.isSuccess()) {
                return Optional.of(result.getValue());
            }

            if (i < maxRetries) {
                System.err.printf("Validation failed for record %s, attempt %d/%d: %s%n",
                        record.getId(), i + 1, maxRetries, result.getErrors());

                try {
                    Thread.sleep(backoffMs * (long) Math.pow(2, i));
                    retryCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        System.err.printf("Record %s failed validation after %d attempts%n",
                record.getId(), maxRetries);
        return Optional.empty();
    }

    // Detect anomalies
    public Map<Boolean, List<ValidatedRecord>> detectAnomalies(List<ValidatedRecord> records, Map<String, CategoryStatistics> stats) {

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // Filter recent records
        Stream<ValidatedRecord> recentRecords = records.parallelStream()
                .filter(r -> r.getTimestamp().isAfter(thirtyDaysAgo));

        // Partition into normal and anomalous
        return recentRecords.collect(
                partitioningBy(record -> {
                    CategoryStatistics catStats = stats.get(record.getCategory());
                    if (catStats == null) return true;

                    double deviation = Math.abs(record.getValue() - catStats.getAverage());
                    return deviation <= 2 * catStats.getStdDev();
                })
        );
    }

    public static void main(String[] args) {
        // Create sample data sources
        List<DataRecord> source1 = Arrays.asList(
                new DataRecord("001", "2024-01-15 10:30:00", "100.5", "CategoryA"),
                new DataRecord("002", "2024-01-15 10:31:00", "95.2", "CategoryA"),
                new DataRecord("003", "invalid-timestamp", "98.7", "CategoryA"),
                new DataRecord("004", "2024-01-15 10:32:00", "150.0", "CategoryB"),
                new DataRecord("", "2024-01-15 10:33:00", "102.3", "CategoryA"),
                new DataRecord("006", "2024-01-15 10:34:00", "not-a-number", "CategoryB")
        );

        List<DataRecord> source2 = Arrays.asList(
                new DataRecord("007", "2024-01-15 10:35:00", "97.8", "CategoryA"),
                new DataRecord("008", "2024-01-15 10:36:00", "145.5", "CategoryB"),
                new DataRecord("009", "2024-01-15 10:37:00", "250.0", "CategoryA"), // Anomaly
                new DataRecord("010", "2024-01-15 10:38:00", "99.1", "CategoryA")
        );

        DataProcessor processor = new DataProcessor();

        // Process records
        System.out.println("=== Processing Records ===");
        Map<String, CategoryStatistics> stats = processor.processRecords(
                Arrays.asList(source1, source2)
        );

        System.out.println("\n=== Category Statistics ===");
        stats.forEach((category, stat) -> System.out.println(stat));

        // For anomaly detection, we need to collect valid records first
        List<ValidatedRecord> allValidRecords = Stream.concat(source1.stream(), source2.stream())
                .map(processor::validateWithRetry)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());

        // Detect anomalies
        System.out.println("\n=== Anomaly Detection ===");
        Map<Boolean, List<ValidatedRecord>> partitioned =
                processor.detectAnomalies(allValidRecords, stats);

        System.out.println("Normal records: " + partitioned.get(true).size());
        System.out.println("Anomalous records: " + partitioned.get(false).size());

        partitioned.get(false).forEach(record ->
                System.out.printf("Anomaly detected: %s, value=%.2f in %s%n",
                        record.getId(), record.getValue(), record.getCategory())
        );

        System.out.println("\nTotal retry attempts: " + processor.retryCount.get());
    }
}