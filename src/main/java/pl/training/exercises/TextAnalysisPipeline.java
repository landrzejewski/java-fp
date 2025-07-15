package pl.training.exercises;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.time.*;
import java.util.stream.Collectors;

class Try<T> {
    private final T value;
    private final Exception error;

    private Try(T value, Exception error) {
        this.value = value;
        this.error = error;
    }

    public static <T> Try<T> success(T value) {
        return new Try<>(value, null);
    }

    public static <T> Try<T> failure(Exception error) {
        return new Try<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isFailure() {
        return error != null;
    }

    public T get() {
        return value;
    }

    public Exception getError() {
        return error;
    }

    public <R> Try<R> map(Function<T, R> mapper) {
        if (isSuccess()) {
            try {
                return success(mapper.apply(value));
            } catch (Exception e) {
                return failure(e);
            }
        }
        return failure(error);
    }

    public <R> Try<R> flatMap(Function<T, Try<R>> mapper) {
        if (isSuccess()) {
            try {
                return mapper.apply(value);
            } catch (Exception e) {
                return failure(e);
            }
        }
        return failure(error);
    }
}

@FunctionalInterface
interface Pipeline<T, R> {
    Try<R> process(T input);

    default <V> Pipeline<T, V> andThen(Pipeline<R, V> next) {
        return input -> this.process(input).flatMap(next::process);
    }

    static <T> Pipeline<T, T> identity() {
        return input -> Try.success(input);
    }
}

class CacheEntry<T> {
    private final T value;
    private final Instant timestamp;

    public CacheEntry(T value) {
        this.value = value;
        this.timestamp = Instant.now();
    }

    public T getValue() {
        return value;
    }

    public boolean isExpired(Duration ttl) {
        return Duration.between(timestamp, Instant.now()).compareTo(ttl) > 0;
    }
}

class MemoizedPipeline<T, R> implements Pipeline<T, R> {
    private final Pipeline<T, R> delegate;
    private final Map<T, CacheEntry<R>> cache;
    private final Duration ttl;
    private final int maxSize;
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    public MemoizedPipeline(Pipeline<T, R> delegate, Duration ttl, int maxSize) {
        this.delegate = delegate;
        this.ttl = ttl;
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<T, CacheEntry<R>>(maxSize + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<T, CacheEntry<R>> eldest) {
                return size() > maxSize;
            }
        };
    }

    @Override
    public Try<R> process(T input) {
        synchronized (cache) {
            CacheEntry<R> entry = cache.get(input);
            if (entry != null && !entry.isExpired(ttl)) {
                hits.incrementAndGet();
                return Try.success(entry.getValue());
            }
        }

        misses.incrementAndGet();
        Try<R> result = delegate.process(input);

        if (result.isSuccess()) {
            synchronized (cache) {
                cache.put(input, new CacheEntry<>(result.get()));
            }
        }

        return result;
    }

    public long getHits() {
        return hits.get();
    }

    public long getMisses() {
        return misses.get();
    }

    public double getHitRate() {
        long total = hits.get() + misses.get();
        return total == 0 ? 0 : (double) hits.get() / total;
    }
}

class DocumentStats {
    final int wordCount;
    final double avgWordLength;
    final int uniqueWords;
    final Map<String, Integer> wordFrequency;
    final String sentiment;

    public DocumentStats(int wordCount, double avgWordLength, int uniqueWords,
                         Map<String, Integer> wordFrequency, String sentiment) {
        this.wordCount = wordCount;
        this.avgWordLength = avgWordLength;
        this.uniqueWords = uniqueWords;
        this.wordFrequency = wordFrequency;
        this.sentiment = sentiment;
    }

    @Override
    public String toString() {
        return String.format(
                "DocumentStats{words=%d, avgLength=%.2f, unique=%d, sentiment=%s}",
                wordCount, avgWordLength, uniqueWords, sentiment
        );
    }
}

class PipelineStages {
    // Text normalizer
    static final Pipeline<String, String> NORMALIZER = text -> {
        try {
            String normalized = text.toLowerCase()
                    .trim()
                    .replaceAll("\\s+", " ")
                    .replaceAll("[^a-z0-9\\s]", "");
            return Try.success(normalized);
        } catch (Exception e) {
            return Try.failure(e);
        }
    };

    // Word frequency counter
    static final Pipeline<String, Map<String, Integer>> WORD_COUNTER = text -> {
        try {
            Map<String, Integer> frequency = Arrays.stream(text.split("\\s+"))
                    .filter(word -> !word.isEmpty())
                    .collect(Collectors.groupingBy(
                            Function.identity(),
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    ));
            return Try.success(frequency);
        } catch (Exception e) {
            return Try.failure(e);
        }
    };

    // Sentiment analyzer (mock implementation)
    static final Pipeline<Map<String, Integer>, String> SENTIMENT_ANALYZER = wordFreq -> {
        try {
            // Mock sentiment analysis based on word frequency
            Set<String> positiveWords = Set.of("good", "great", "excellent", "happy", "love");
            Set<String> negativeWords = Set.of("bad", "terrible", "hate", "sad", "awful");

            int positiveScore = wordFreq.entrySet().stream()
                    .filter(e -> positiveWords.contains(e.getKey()))
                    .mapToInt(Map.Entry::getValue)
                    .sum();

            int negativeScore = wordFreq.entrySet().stream()
                    .filter(e -> negativeWords.contains(e.getKey()))
                    .mapToInt(Map.Entry::getValue)
                    .sum();

            String sentiment = positiveScore > negativeScore ? "positive" :
                    negativeScore > positiveScore ? "negative" : "neutral";

            return Try.success(sentiment);
        } catch (Exception e) {
            return Try.failure(e);
        }
    };

    // Statistics calculator
    static final Pipeline<Map<String, Integer>, DocumentStats> STATS_CALCULATOR = wordFreq -> {
        try {
            int wordCount = wordFreq.values().stream().mapToInt(Integer::intValue).sum();
            double avgWordLength = wordFreq.entrySet().stream()
                    .mapToDouble(e -> e.getKey().length() * e.getValue())
                    .sum() / wordCount;
            int uniqueWords = wordFreq.size();

            // Get sentiment by processing through sentiment analyzer
            String sentiment = SENTIMENT_ANALYZER.process(wordFreq).get();

            return Try.success(new DocumentStats(
                    wordCount, avgWordLength, uniqueWords, wordFreq, sentiment
            ));
        } catch (Exception e) {
            return Try.failure(e);
        }
    };
}

// Pipeline executor with retry logic
class PipelineExecutor {
    private final ExecutorService executor;
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final AtomicInteger executionCount = new AtomicInteger(0);

    public PipelineExecutor(int threadPoolSize) {
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
    }

    public <T, R> CompletableFuture<Try<R>> executeAsync(Pipeline<T, R> pipeline, T input) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Try<R> result = pipeline.process(input);
            long duration = System.currentTimeMillis() - startTime;

            totalExecutionTime.addAndGet(duration);
            executionCount.incrementAndGet();

            return result;
        }, executor);
    }

    public <T, R> CompletableFuture<Try<R>> executeWithRetry(
            Pipeline<T, R> pipeline, T input, int maxRetries, Duration backoffDelay) {

        return CompletableFuture.supplyAsync(() -> {
            Try<R> result = null;
            Exception lastError = null;

            for (int i = 0; i <= maxRetries; i++) {
                result = pipeline.process(input);
                if (result.isSuccess()) {
                    return result;
                }

                lastError = result.getError();
                if (i < maxRetries) {
                    try {
                        long delay = (long) (backoffDelay.toMillis() * Math.pow(2, i));
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return Try.failure(e);
                    }
                }
            }

            return Try.failure(new RuntimeException(
                    "Failed after " + (maxRetries + 1) + " attempts", lastError));
        }, executor);
    }

    public double getAverageExecutionTime() {
        int count = executionCount.get();
        return count == 0 ? 0 : (double) totalExecutionTime.get() / count;
    }

    public void shutdown() {
        executor.shutdown();
    }
}

public class TextAnalysisPipeline {
    public static void main(String[] args) throws Exception {
        // Create pipeline with memoization
        Pipeline<String, String> normalizer = new MemoizedPipeline<>(
                PipelineStages.NORMALIZER, Duration.ofMinutes(5), 100
        );

        Pipeline<String, Map<String, Integer>> wordCounter = new MemoizedPipeline<>(
                PipelineStages.WORD_COUNTER, Duration.ofMinutes(5), 100
        );

        Pipeline<Map<String, Integer>, DocumentStats> statsCalculator =
                new MemoizedPipeline<>(
                        PipelineStages.STATS_CALCULATOR, Duration.ofMinutes(5), 50
                );

        // Compose full pipeline
        Pipeline<String, DocumentStats> fullPipeline = normalizer
                .andThen(wordCounter)
                .andThen(statsCalculator);

        // Test documents
        List<String> documents = Arrays.asList(
                "This is a great example of text processing. Great work!",
                "Bad implementation leads to terrible results and awful performance.",
                "The quick brown fox jumps over the lazy dog.",
                "I love programming in Java. It's excellent for building robust applications.",
                "This is a great example of text processing. Great work!" // Duplicate for cache test
        );

        // Create executor
        PipelineExecutor executor = new PipelineExecutor(4);

        // Process documents in parallel
        System.out.println("Processing documents...\n");

        List<CompletableFuture<Try<DocumentStats>>> futures = documents.stream()
                .map(doc -> executor.executeWithRetry(fullPipeline, doc, 3, Duration.ofMillis(100)))
                .collect(Collectors.toList());

        // Collect results
        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        allDone.thenRun(() -> {
            System.out.println("Document Analysis Results:");
            System.out.println("========================");

            for (int i = 0; i < documents.size(); i++) {
                String doc = documents.get(i);
                Try<DocumentStats> result = futures.get(i).join();

                System.out.println("\nDocument " + (i + 1) + ": " +
                        doc.substring(0, Math.min(doc.length(), 50)) + "...");

                if (result.isSuccess()) {
                    DocumentStats stats = result.get();
                    System.out.println("  " + stats);
                    System.out.println("  Top words: " +
                            stats.wordFrequency.entrySet().stream()
                                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                    .limit(3)
                                    .map(e -> e.getKey() + "(" + e.getValue() + ")")
                                    .collect(Collectors.joining(", ")));
                } else {
                    System.out.println("  Error: " + result.getError().getMessage());
                }
            }

            // Print metrics
            System.out.println("\nPipeline Metrics:");
            System.out.println("=================");
            System.out.printf("Average execution time: %.2f ms%n",
                    executor.getAverageExecutionTime());

            if (normalizer instanceof MemoizedPipeline) {
                MemoizedPipeline<String, String> memoized =
                        (MemoizedPipeline<String, String>) normalizer;
                System.out.printf("Normalizer cache hit rate: %.2f%%%n",
                        memoized.getHitRate() * 100);
            }

            executor.shutdown();
        }).join();

        // Aggregate results example
        Map<String, Integer> aggregatedWords = futures.stream()
                .map(CompletableFuture::join)
                .filter(Try::isSuccess)
                .map(Try::get)
                .flatMap(stats -> stats.wordFrequency.entrySet().stream())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.summingInt(Map.Entry::getValue)
                ));

        System.out.println("\nTop 10 words across all documents:");
        aggregatedWords.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> System.out.printf("  %s: %d%n", e.getKey(), e.getValue()));
    }

}