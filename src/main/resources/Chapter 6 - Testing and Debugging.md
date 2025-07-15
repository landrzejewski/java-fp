## Chapter 6: Testing and Debugging

Lambda expressions have revolutionized Java programming since Java 8, enabling functional programming paradigms and 
more concise code. However, their functional nature, combined with lazy evaluation and the lack of traditional 
breakpoint locations, requires different debugging strategies than traditional imperative code.

### Why Lambda Debugging is Different

Traditional debugging relies on:
- Setting breakpoints at specific lines
- Stepping through code line by line
- Inspecting variable states at each step

Lambda debugging challenges:
- **Anonymous nature**: Lambdas don't have meaningful names in stack traces
- **Lazy evaluation**: Stream operations only execute when a terminal operation is called
- **Chained operations**: Multiple operations are often chained together, making it hard to inspect intermediate states
- **Parallel execution**: Parallel streams execute on multiple threads, complicating debugging

## Understanding Lambda Debugging Challenges

Before diving into solutions, let's understand the specific challenges:

### 1. **Lazy Evaluation**
Streams don't execute immediately. They only run when a terminal operation (like `collect()`, `forEach()`, or `count()`) is called. This means:
- Setting a breakpoint in a `map()` operation won't hit until the stream is actually consumed
- The entire pipeline is optimized and may not execute in the order you wrote it

### 2. **Anonymous Functions**
Lambda expressions are compiled to anonymous classes or method references, resulting in cryptic stack traces like:
```
at MyClass.lambda$main$0(MyClass.java:15)
at java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:193)
```

### 3. **Limited Debugging Information**
- Local variables inside lambdas can be hard to inspect
- Intermediate stream states are not easily visible
- Conditional breakpoints are difficult to set

## Debugging with Peek

The `peek()` operation is one of the most valuable tools for debugging streams. It's specifically designed for debugging and allows you to observe elements as they flow through the stream pipeline without modifying them.

### Understanding `peek()`

**What `peek()` does:**
- It's an intermediate operation that returns a stream
- Performs an action on each element as it passes through
- Doesn't modify the elements (unlike `map()`)
- Only executes when the stream is consumed

**When to use `peek()`:**
- To print/log elements at various stages
- To verify transformations are working correctly
- To count elements passing through filters
- To debug performance issues

### Basic Peek Usage

```java
import java.util.*;
import java.util.stream.*;

public class BasicPeekExample {
    public static void main(String[] args) {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "Diana", "Eve");
        
        // Without peek - we can't see what's happening inside
        List<String> result1 = names.stream()
            .map(String::toUpperCase)
            .filter(name -> name.length() > 3)
            .sorted()
            .collect(Collectors.toList());
        
        System.out.println("Result without debugging: " + result1);
        
        // With peek - we can observe the flow
        System.out.println("\n=== With Peek Debugging ===");
        List<String> result2 = names.stream()
            .peek(name -> System.out.println("Original: " + name))
            .map(String::toUpperCase)
            .peek(name -> System.out.println("After uppercase: " + name))
            .filter(name -> name.length() > 3)
            .peek(name -> System.out.println("After filter (length > 3): " + name))
            .sorted()
            .peek(name -> System.out.println("After sort: " + name))
            .collect(Collectors.toList());
        
        System.out.println("\nFinal result: " + result2);
    }
}
```

**Explanation**:
- Each `peek()` shows the element at that stage of processing
- You can see which elements are filtered out and when
- The sorting operation becomes visible
- This helps identify where unexpected behavior occurs

### Advanced Peek Techniques

#### 1. **Counting Elements with Peek**

```java
import java.util.concurrent.atomic.AtomicInteger;

public class PeekCountingExample {
    public static void main(String[] args) {
        // Use AtomicInteger for thread-safe counting
        AtomicInteger filterCount = new AtomicInteger(0);
        AtomicInteger mapCount = new AtomicInteger(0);
        
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        double average = numbers.stream()
            .peek(n -> System.out.println("Processing: " + n))
            .filter(n -> {
                boolean isEven = n % 2 == 0;
                if (isEven) {
                    filterCount.incrementAndGet();
                    System.out.println("  " + n + " passed filter");
                }
                return isEven;
            })
            .map(n -> {
                mapCount.incrementAndGet();
                int squared = n * n;
                System.out.println("  " + n + " squared to " + squared);
                return squared;
            })
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
        
        System.out.println("\n=== Statistics ===");
        System.out.println("Average of squared even numbers: " + average);
        System.out.println("Elements that passed filter: " + filterCount);
        System.out.println("Elements that were mapped: " + mapCount);
    }
}
```

**Key Points**:
- `AtomicInteger` ensures thread-safety, especially important for parallel streams
- Counting helps verify that filters and operations work as expected
- You can identify performance bottlenecks by seeing how many elements reach each stage

#### 2. **Debugging Short-Circuit Operations**

Short-circuit operations like `findFirst()`, `anyMatch()`, and `limit()` stop processing once their condition is met. This can be confusing when debugging.

```java
public class ShortCircuitDebugging {
    public static void main(String[] args) {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlotte", "David", "Eve");
        
        System.out.println("=== Debugging anyMatch (short-circuit) ===");
        boolean hasLongName = names.stream()
            .peek(name -> System.out.println("Checking: " + name))
            .anyMatch(name -> {
                boolean isLong = name.length() > 7;
                System.out.println("  " + name + " length > 7? " + isLong);
                return isLong;
            });
        
        System.out.println("Result: " + hasLongName);
        System.out.println("Note: Processing stopped at Charlotte!");
        
        System.out.println("\n=== Debugging limit ===");
        List<String> firstThree = names.stream()
            .peek(name -> System.out.println("Processing: " + name))
            .map(String::toUpperCase)
            .peek(name -> System.out.println("  Uppercase: " + name))
            .limit(3)
            .collect(Collectors.toList());
        
        System.out.println("First three: " + firstThree);
        System.out.println("Note: Only 3 elements were processed!");
    }
}
```

**Important**: Short-circuit operations optimize performance but can make debugging confusing. Use `peek()` to understand exactly when processing stops.

### Debugging Parallel Streams

Parallel streams add complexity because:
- Operations execute on multiple threads
- Order of execution is non-deterministic
- Thread safety becomes crucial

```java
import java.util.*;
import java.util.stream.*;
import java.util.concurrent.ConcurrentHashMap;

public class ParallelStreamDebugging {
    public static void main(String[] args) {
        List<Integer> numbers = IntStream.rangeClosed(1, 20)
            .boxed()
            .collect(Collectors.toList());
        
        // Track which thread processes each element
        ConcurrentHashMap<Integer, String> threadMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> threadCounts = new ConcurrentHashMap<>();
        
        System.out.println("=== Parallel Stream Processing ===");
        List<Integer> result = numbers.parallelStream()
            .peek(n -> {
                String thread = Thread.currentThread().getName();
                threadMap.put(n, thread);
                threadCounts.merge(thread, 1, Integer::sum);
                System.out.printf("[%s] Processing: %d%n", thread, n);
            })
            .filter(n -> n % 2 == 0)
            .map(n -> n * n)
            .collect(Collectors.toList());
        
        System.out.println("\n=== Thread Distribution ===");
        threadCounts.forEach((thread, count) -> 
            System.out.println(thread + " processed " + count + " elements"));
        
        System.out.println("\n=== Results ===");
        System.out.println("Result order may vary: " + result);
    }
}
```

**Parallel Stream Debugging Tips**:
1. Use `ConcurrentHashMap` for thread-safe data collection
2. Include thread names in debug output
3. Be aware that execution order is non-deterministic
4. Use `.sequential()` temporarily to debug logic issues
5. Consider using custom thread pools for better control

### Creating Reusable Debug Utilities

Instead of writing `peek()` statements repeatedly, create reusable debugging utilities:

```java
import java.util.function.Consumer;

public class StreamDebugger<T> {
    private final String streamName;
    private final boolean verbose;
    private long elementCount = 0;
    
    public StreamDebugger(String streamName, boolean verbose) {
        this.streamName = streamName;
        this.verbose = verbose;
    }
    
    // Create a peek consumer with context
    public Consumer<T> peek(String stage) {
        return element -> {
            elementCount++;
            if (verbose) {
                System.out.printf("[%s] %s #%d: %s%n",
                    streamName, stage, elementCount, element);
            }
        };
    }
    
    // Conditional peek - only logs when condition is met
    public Consumer<T> peekIf(String stage, Predicate<T> condition, String message) {
        return element -> {
            if (condition.test(element)) {
                System.out.printf("[%s] %s - %s: %s%n",
                    streamName, stage, message, element);
            }
        };
    }
    
    // Usage example
    public static void main(String[] args) {
        StreamDebugger<String> debugger = new StreamDebugger<>("UserProcessor", true);
        
        List<String> users = Arrays.asList("Alice", "Bob", "Charlie", "Anna");
        
        users.stream()
            .peek(debugger.peek("1. Input"))
            .filter(name -> name.startsWith("A"))
            .peek(debugger.peek("2. After filter"))
            .map(String::toUpperCase)
            .peek(debugger.peek("3. After uppercase"))
            .collect(Collectors.toList());
    }
}
```

**Benefits of Debug Utilities**:
- Consistent debug output format
- Easy to enable/disable debugging
- Can add features like performance timing, element counting
- Reusable across different streams

## Debugging Utilities

Creating comprehensive debugging utilities helps standardize debugging approaches and makes debugging more efficient.

### Logging Wrappers for Functions

One of the biggest challenges with lambdas is understanding what they're doing. Wrapping functions with logging provides visibility:

```java
import java.util.function.*;
import java.util.logging.*;

public class LoggingWrappers {
    private static final Logger LOGGER = Logger.getLogger(LoggingWrappers.class.getName());
    
    /**
     * Wraps a Function with logging to track inputs, outputs, and execution time
     * @param function The function to wrap
     * @param functionName A descriptive name for logging
     * @return A wrapped function that logs its execution
     */
    public static <T, R> Function<T, R> logFunction(Function<T, R> function, 
                                                     String functionName) {
        return input -> {
            LOGGER.info(() -> String.format("Calling %s with input: %s", 
                functionName, input));
            
            long startTime = System.nanoTime();
            try {
                R result = function.apply(input);
                long duration = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
                
                LOGGER.info(() -> String.format("%s returned: %s (took %dms)", 
                    functionName, result, duration));
                return result;
            } catch (Exception e) {
                LOGGER.severe(() -> String.format("%s threw exception for input %s: %s", 
                    functionName, input, e));
                throw e; // Re-throw to maintain behavior
            }
        };
    }
    
    // Example usage
    public static void main(String[] args) {
        // Configure logging to see output
        LOGGER.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        LOGGER.addHandler(handler);
        
        // Wrap functions for debugging
        Function<String, Integer> parseLength = logFunction(
            String::length, 
            "stringLength"
        );
        
        Function<Integer, Double> squareRoot = logFunction(
            n -> Math.sqrt(n),
            "squareRoot"
        );
        
        // Use in a stream
        List<String> words = Arrays.asList("Hello", "World", "Lambda", "Debugging");
        
        List<Double> results = words.stream()
            .map(parseLength)
            .filter(len -> len > 4)
            .map(squareRoot)
            .collect(Collectors.toList());
        
        System.out.println("Results: " + results);
    }
}
```

**Why This Helps**:
1. **Visibility**: See exactly what inputs each function receives
2. **Performance**: Automatically measure execution time
3. **Error Tracking**: Capture which inputs cause exceptions
4. **Production Ready**: Can be enabled/disabled via logging configuration

### Breakpoint-Friendly Wrappers

IDEs struggle with setting breakpoints in lambda expressions. These wrappers make it easier:

```java
public class BreakpointFriendlyWrappers {
    
    /**
     * A debug point that's easy to set breakpoints on
     */
    public static class DebugPoint<T> {
        private final String name;
        
        public DebugPoint(String name) {
            this.name = name;
        }
        
        /**
         * Method specifically designed for setting breakpoints
         * Put your breakpoint on the println line or the return statement
         */
        public T debug(T value) {
            // BREAKPOINT HERE: Easy to set and will show 'value' in debugger
            System.out.printf("[BREAKPOINT] %s: %s%n", name, value);
            return value; // BREAKPOINT HERE: To inspect the value before returning
        }
        
        /**
         * Wraps a function to make it breakpoint-friendly
         */
        public <R> Function<T, R> wrapFunction(Function<T, R> function) {
            return input -> {
                // BREAKPOINT HERE: To see the input
                T debuggedInput = debug(input);
                
                // BREAKPOINT HERE: To step into the function
                R result = function.apply(debuggedInput);
                
                // BREAKPOINT HERE: To see the result
                System.out.printf("[RESULT] %s: %s -> %s%n", name, input, result);
                return result;
            };
        }
    }
    
    // Usage example
    public static void main(String[] args) {
        DebugPoint<String> stringDebugger = new DebugPoint<>("StringProcessor");
        DebugPoint<Integer> intDebugger = new DebugPoint<>("IntProcessor");
        
        List<String> words = Arrays.asList("Hello", "Debugging", "Lambda");
        
        // Now you can easily set breakpoints in the wrapper methods
        List<Integer> lengths = words.stream()
            .map(stringDebugger::debug) // Breakpoint-friendly
            .map(stringDebugger.wrapFunction(String::length)) // Breakpoint-friendly
            .filter(intDebugger.wrapFunction(len -> len > 5)) // Breakpoint-friendly
            .collect(Collectors.toList());
    }
}
```

**Breakpoint Strategy**:
1. Set breakpoints in the `debug()` method to inspect values
2. Use conditional breakpoints based on the value
3. Step through the wrapped function calls
4. The method structure makes it easy for IDEs to set breakpoints

### Performance Measurement Utilities

Understanding performance is crucial when debugging streams, especially with large datasets:

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceMonitor {
    private final Map<String, OperationStats> stats = new ConcurrentHashMap<>();
    
    /**
     * Tracks performance statistics for each operation
     */
    private static class OperationStats {
        private final String name;
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong count = new AtomicLong(0);
        private volatile long minTime = Long.MAX_VALUE;
        private volatile long maxTime = 0;
        
        OperationStats(String name) {
            this.name = name;
        }
        
        void record(long nanoTime) {
            totalTime.addAndGet(nanoTime);
            count.incrementAndGet();
            
            // Update min/max (not perfectly thread-safe but good enough for debugging)
            minTime = Math.min(minTime, nanoTime);
            maxTime = Math.max(maxTime, nanoTime);
        }
        
        @Override
        public String toString() {
            long operations = count.get();
            if (operations == 0) return name + ": No operations";
            
            double avgMs = (totalTime.get() / operations) / 1_000_000.0;
            double minMs = minTime / 1_000_000.0;
            double maxMs = maxTime / 1_000_000.0;
            
            return String.format("%s: count=%d, avg=%.3fms, min=%.3fms, max=%.3fms",
                name, operations, avgMs, minMs, maxMs);
        }
    }
    
    /**
     * Wraps a function to measure its performance
     */
    public <T, R> Function<T, R> measure(Function<T, R> function, String operationName) {
        stats.putIfAbsent(operationName, new OperationStats(operationName));
        
        return input -> {
            long start = System.nanoTime();
            try {
                return function.apply(input);
            } finally {
                long duration = System.nanoTime() - start;
                stats.get(operationName).record(duration);
            }
        };
    }
    
    /**
     * Prints performance statistics
     */
    public void printStats() {
        System.out.println("\n=== Performance Statistics ===");
        stats.values().stream()
            .sorted((a, b) -> a.name.compareTo(b.name))
            .forEach(System.out::println);
    }
    
    // Example usage
    public static void main(String[] args) {
        PerformanceMonitor monitor = new PerformanceMonitor();
        
        // Simulate operations with different performance characteristics
        Function<Integer, Double> slowOperation = monitor.measure(
            n -> {
                try {
                    Thread.sleep(n % 10); // Variable delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return Math.sqrt(n);
            },
            "slowOperation"
        );
        
        Function<Double, String> fastOperation = monitor.measure(
            d -> String.format("%.2f", d),
            "formatting"
        );
        
        // Process data
        IntStream.rangeClosed(1, 100)
            .boxed()
            .map(slowOperation)
            .map(fastOperation)
            .collect(Collectors.toList());
        
        monitor.printStats();
    }
}
```

**Performance Debugging Benefits**:
1. **Identify Bottlenecks**: See which operations take the most time
2. **Find Outliers**: Min/max times help identify unusual cases
3. **Optimization Targets**: Focus optimization on the slowest operations
4. **Regression Detection**: Compare performance across code changes

## Stack Trace Enhancement

Lambda expressions produce hard-to-read stack traces. Here's how to improve them:

### The Problem with Lambda Stack Traces

Standard lambda stack traces look like:
```
Exception in thread "main" java.lang.NumberFormatException: For input string: "abc"
    at java.lang.Integer.parseInt(Integer.java:652)
    at MyClass.lambda$main$0(MyClass.java:15)
    at java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:193)
    ...
```

The `lambda$main$0` naming provides no context about what the lambda does.

### Solution: Named Lambda Factory

```java
public class NamedLambdaFactory {
    
    /**
     * Creates a named function that provides better stack traces
     */
    public static <T, R> Function<T, R> namedFunction(String name, 
                                                       Function<T, R> lambda) {
        return new Function<T, R>() {
            @Override
            public R apply(T input) {
                try {
                    return lambda.apply(input);
                } catch (Exception e) {
                    // Wrap in a more descriptive exception
                    throw new LambdaExecutionException(name, input, e);
                }
            }
            
            @Override
            public String toString() {
                return "Function[" + name + "]";
            }
        };
    }
    
    /**
     * Custom exception with enhanced context
     */
    public static class LambdaExecutionException extends RuntimeException {
        private final String lambdaName;
        private final Object input;
        
        public LambdaExecutionException(String lambdaName, Object input, Throwable cause) {
            super(createDetailedMessage(lambdaName, input, cause), cause);
            this.lambdaName = lambdaName;
            this.input = input;
        }
        
        private static String createDetailedMessage(String lambdaName, Object input, 
                                                    Throwable cause) {
            return String.format(
                "Lambda '%s' failed%n" +
                "  Input: %s%n" +
                "  Input type: %s%n" +
                "  Cause: %s: %s",
                lambdaName, 
                input,
                input != null ? input.getClass().getName() : "null",
                cause.getClass().getName(),
                cause.getMessage()
            );
        }
    }
    
    // Example usage
    public static void main(String[] args) {
        List<String> data = Arrays.asList("10", "20", "abc", "30");
        
        try {
            List<Integer> results = data.stream()
                .map(namedFunction("parseToInteger", Integer::parseInt))
                .map(namedFunction("doubleValue", n -> n * 2))
                .map(namedFunction("validateRange", n -> {
                    if (n > 100) throw new IllegalArgumentException("Value too large");
                    return n;
                }))
                .collect(Collectors.toList());
        } catch (LambdaExecutionException e) {
            System.err.println("Lambda execution failed!");
            System.err.println(e.getMessage());
            // Now we know exactly which lambda failed and with what input!
        }
    }
}
```

**Enhanced Stack Trace Benefits**:
1. **Clear Lambda Identification**: Know exactly which operation failed
2. **Input Context**: See what input caused the failure
3. **Type Information**: Understand the data types involved
4. **Debugging Efficiency**: Quickly locate and fix issues

### Exception Context Wrapper

For more complex debugging scenarios, capture comprehensive context:

```java
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExceptionContextWrapper {
    
    /**
     * Captures comprehensive context when exceptions occur
     */
    public static class ContextualException extends RuntimeException {
        private final Map<String, Object> context;
        private final LocalDateTime timestamp;
        
        public ContextualException(String operation, Throwable cause, 
                                   Map<String, Object> context) {
            super(createMessage(operation, cause, context), cause);
            this.context = new LinkedHashMap<>(context);
            this.timestamp = LocalDateTime.now();
        }
        
        private static String createMessage(String operation, Throwable cause,
                                            Map<String, Object> context) {
            StringBuilder msg = new StringBuilder();
            msg.append("Operation '").append(operation).append("' failed\n");
            msg.append("Timestamp: ").append(LocalDateTime.now()).append("\n");
            msg.append("Context:\n");
            context.forEach((key, value) -> 
                msg.append("  ").append(key).append(": ").append(value).append("\n"));
            msg.append("Cause: ").append(cause.getClass().getName())
               .append(": ").append(cause.getMessage());
            return msg.toString();
        }
    }
    
    /**
     * Builds context for operations
     */
    public static class OperationContext {
        private final String operationName;
        private final Map<String, Object> context = new LinkedHashMap<>();
        
        public OperationContext(String operationName) {
            this.operationName = operationName;
            context.put("operation", operationName);
            context.put("thread", Thread.currentThread().getName());
            context.put("startTime", LocalDateTime.now());
        }
        
        public OperationContext with(String key, Object value) {
            context.put(key, value);
            return this;
        }
        
        public <T, R> Function<T, R> wrap(Function<T, R> function) {
            return input -> {
                Map<String, Object> execContext = new LinkedHashMap<>(context);
                execContext.put("input", input);
                execContext.put("inputType", input != null ? 
                    input.getClass().getName() : "null");
                
                try {
                    R result = function.apply(input);
                    execContext.put("result", result);
                    return result;
                } catch (Exception e) {
                    throw new ContextualException(operationName, e, execContext);
                }
            };
        }
    }
    
    // Example usage
    public static void main(String[] args) {
        OperationContext parseContext = new OperationContext("ParseUserData")
            .with("source", "user-input.csv")
            .with("expectedFormat", "numeric")
            .with("validationEnabled", true);
        
        List<String> userData = Arrays.asList("100", "200", "invalid", "300");
        
        try {
            userData.stream()
                .map(parseContext.wrap(Integer::parseInt))
                .collect(Collectors.toList());
        } catch (ContextualException e) {
            System.err.println("Detailed error information:");
            System.err.println(e.getMessage());
            // The exception now contains full context about what was happening
        }
    }
}
```

**Context Benefits**:
1. **Complete Picture**: Understand the full state when errors occur
2. **Debugging Efficiency**: All relevant information in one place
3. **Production Debugging**: Valuable for debugging production issues
4. **Audit Trail**: Track what operations were attempted

## Testing and Debugging Best Practices

### 1. Lambda Extraction

**Problem**: Inline lambdas are hard to test and debug.

**Solution**: Extract lambdas to named methods.

```java
public class LambdaExtractionExample {
    
    // Instead of this (hard to test and debug):
    public List<String> processDataInline(List<Order> orders) {
        return orders.stream()
            .filter(o -> o.getAmount() > 100 && o.getStatus() == Status.ACTIVE)
            .map(o -> String.format("Order %s: $%.2f", o.getId(), o.getAmount()))
            .collect(Collectors.toList());
    }
    
    // Do this (easy to test and debug):
    public List<String> processDataExtracted(List<Order> orders) {
        return orders.stream()
            .filter(this::isHighValueActiveOrder)
            .map(this::formatOrderSummary)
            .collect(Collectors.toList());
    }
    
    // Extracted methods are testable
    boolean isHighValueActiveOrder(Order order) {
        return order.getAmount() > 100 && order.getStatus() == Status.ACTIVE;
    }
    
    String formatOrderSummary(Order order) {
        return String.format("Order %s: $%.2f", order.getId(), order.getAmount());
    }
    
    // Now you can test the logic separately
    @Test
    public void testIsHighValueActiveOrder() {
        Order highValueActive = new Order("1", 150, Status.ACTIVE);
        assertTrue(isHighValueActiveOrder(highValueActive));
        
        Order lowValue = new Order("2", 50, Status.ACTIVE);
        assertFalse(isHighValueActiveOrder(lowValue));
        
        Order highValueInactive = new Order("3", 150, Status.INACTIVE);
        assertFalse(isHighValueActiveOrder(highValueInactive));
    }
}
```

**Benefits of Extraction**:
1. **Unit Testing**: Test complex logic in isolation
2. **Reusability**: Use the same logic in multiple places
3. **Readability**: Self-documenting method names
4. **Debugging**: Set breakpoints in named methods
5. **Performance**: JVM can optimize named methods better

### 2. Descriptive Naming Conventions

Good naming makes debugging much easier:

```java
public class NamingConventions {
    
    // Bad: Cryptic variable names
    public void processBad(List<String> d) {
        d.stream()
            .map(s -> s.trim())
            .filter(s -> s.length() > 0)
            .map(s -> s.toUpperCase())
            .forEach(s -> System.out.println(s));
    }
    
    // Good: Descriptive names explain intent
    public void processGood(List<String> rawUserInputs) {
        rawUserInputs.stream()
            .map(this::trimWhitespace)
            .filter(this::isNotEmpty)
            .map(this::normalizeToUpperCase)
            .forEach(this::displayProcessedInput);
    }
    
    private String trimWhitespace(String input) {
        return input.trim();
    }
    
    private boolean isNotEmpty(String text) {
        return text.length() > 0;
    }
    
    private String normalizeToUpperCase(String text) {
        return text.toUpperCase();
    }
    
    private void displayProcessedInput(String processed) {
        System.out.println("Processed: " + processed);
    }
}
```

**Naming Guidelines**:
1. **Predicates**: Start with `is`, `has`, `can` (e.g., `isValid`, `hasPermission`)
2. **Transformations**: Use verb phrases (e.g., `convertToUpperCase`, `parseAsInteger`)
3. **Variables**: Use full words, not abbreviations
4. **Collections**: Use plural nouns (e.g., `users`, `orders`)
5. **Constants**: Use UPPER_SNAKE_CASE

### 3. Custom Assertions for Functional Code

Standard assertions don't work well with functional code. Create custom ones:

```java
public class FunctionalAssertions {
    
    /**
     * Asserts that a function behaves correctly for multiple inputs
     */
    public static <T, R> void assertFunctionBehavior(
            Function<T, R> function,
            Map<T, R> inputsAndExpectedOutputs) {
        
        inputsAndExpectedOutputs.forEach((input, expected) -> {
            R actual = function.apply(input);
            if (!Objects.equals(expected, actual)) {
                throw new AssertionError(String.format(
                    "Function failed for input %s: expected %s but got %s",
                    input, expected, actual
                ));
            }
        });
    }
    
    /**
     * Asserts that a predicate correctly partitions a collection
     */
    public static <T> void assertPredicatePartitions(
            Predicate<T> predicate,
            Collection<T> shouldPass,
            Collection<T> shouldFail) {
        
        // Check all expected passes
        for (T item : shouldPass) {
            if (!predicate.test(item)) {
                throw new AssertionError(
                    "Predicate should accept " + item + " but rejected it");
            }
        }
        
        // Check all expected failures
        for (T item : shouldFail) {
            if (predicate.test(item)) {
                throw new AssertionError(
                    "Predicate should reject " + item + " but accepted it");
            }
        }
    }
    
    // Example usage
    public static void main(String[] args) {
        // Test a parsing function
        Function<String, Integer> parsePositiveInt = s -> {
            int value = Integer.parseInt(s);
            if (value <= 0) throw new IllegalArgumentException("Must be positive");
            return value;
        };
        
        assertFunctionBehavior(parsePositiveInt, Map.of(
            "1", 1,
            "42", 42,
            "100", 100
        ));
        
        // Test a predicate
        Predicate<String> isEmail = s -> s.contains("@") && s.contains(".");
        
        assertPredicatePartitions(
            isEmail,
            Arrays.asList("user@example.com", "test@domain.co.uk"),
            Arrays.asList("not-an-email", "missing-at.com", "@missing-domain")
        );
        
        System.out.println("All assertions passed!");
    }
}
```

### 4. Systematic Debugging Approach

Follow a structured approach when debugging complex stream operations:

```java
public class SystematicDebuggingApproach {
    
    /**
     * Step-by-step debugging framework
     */
    public static class StreamDebugger<T> {
        private final List<T> originalData;
        private List<T> currentState;
        private int stepNumber = 0;
        
        public StreamDebugger(List<T> data) {
            this.originalData = new ArrayList<>(data);
            this.currentState = new ArrayList<>(data);
        }
        
        public StreamDebugger<T> debugStep(String description, 
                                           Function<Stream<T>, Stream<T>> operation) {
            stepNumber++;
            System.out.println("\n=== Step " + stepNumber + ": " + description + " ===");
            System.out.println("Before: " + currentState);
            
            Stream<T> result = operation.apply(currentState.stream());
            currentState = result.collect(Collectors.toList());
            
            System.out.println("After: " + currentState);
            System.out.println("Elements removed: " + 
                (stepNumber == 1 ? 0 : previousSize - currentState.size()));
            
            return this;
        }
        
        private int previousSize = 0;
        
        public List<T> getResult() {
            return currentState;
        }
    }
    
    // Example: Debug a complex stream operation
    public static void main(String[] args) {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        StreamDebugger<Integer> debugger = new StreamDebugger<>(numbers);
        
        List<Integer> result = debugger
            .debugStep("Filter even numbers", 
                stream -> stream.filter(n -> n % 2 == 0))
            .debugStep("Square each number", 
                stream -> stream.map(n -> n * n))
            .debugStep("Keep only values > 20", 
                stream -> stream.filter(n -> n > 20))
            .getResult();
        
        System.out.println("\nFinal result: " + result);
    }
}
```

**Systematic Debugging Steps**:
1. **Isolate the Problem**: Comment out operations until the error disappears
2. **Add Peek Statements**: Observe data flow at each stage
3. **Check Assumptions**: Verify data types and null values
4. **Test with Minimal Data**: Use a small dataset that reproduces the issue
5. **Extract and Test**: Extract complex lambdas and test separately

### 5. Common Debugging Patterns

#### Pattern 1: Binary Search Debugging
When you have a long stream pipeline and don't know where the problem is:

```java
// Original (broken) pipeline
stream
    .operation1()
    .operation2()
    .operation3()
    .operation4()
    .operation5()
    .collect();

// Binary search approach:
// 1. Comment out the second half
stream
    .operation1()
    .operation2()
    .operation3()
    // .operation4()
    // .operation5()
    .collect();

// If it works, problem is in operation4 or operation5
// If it fails, problem is in operation1, operation2, or operation3
// Continue subdividing until you find the issue
```

#### Pattern 2: Progressive Building
Build your stream pipeline incrementally:

```java
// Start simple
List<String> step1 = data.stream()
    .collect(Collectors.toList());
System.out.println("Step 1: " + step1);

// Add one operation
List<String> step2 = data.stream()
    .filter(s -> s.length() > 5)
    .collect(Collectors.toList());
System.out.println("Step 2: " + step2);

// Continue adding operations one at a time
// This helps identify exactly where things go wrong
```

#### Pattern 3: Parallel to Sequential
If a parallel stream is behaving unexpectedly:

```java
// Temporarily switch to sequential for debugging
List<Result> results = data.parallelStream()
    .sequential() // ADD THIS FOR DEBUGGING
    .map(complexOperation)
    .filter(complexFilter)
    .collect(Collectors.toList());

// Once working correctly, remove .sequential()
// This helps identify race conditions and thread-safety issues
```
