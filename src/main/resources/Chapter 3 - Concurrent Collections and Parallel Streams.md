## Chapter 3: Concurrent Collections and Parallel Streams in Java

Java provides several concurrent collections in the `java.util.concurrent` package. Each is designed for specific concurrent access patterns and use cases.

### ConcurrentHashMap

A thread-safe hash table that allows concurrent reads and writes. It uses segment-based locking to allow multiple threads to modify different parts of the map simultaneously. Supports atomic operations like `compute()`, `merge()`, and `putIfAbsent()`.

**When to use**: When you need a thread-safe map with high concurrency and frequent updates.

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("key", 1);
map.compute("key", (k, v) -> v + 1);  // Atomic increment
```

### ConcurrentLinkedQueue

An unbounded, thread-safe queue that uses non-blocking algorithms. Multiple threads can add and remove elements concurrently without locking.

**When to use**: For producer-consumer scenarios where you don't need blocking behavior.

```java
ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
queue.offer("item");
String item = queue.poll();
```

### BlockingQueue Interface

Extends Queue with blocking operations. Threads wait when trying to add to a full queue or remove from an empty queue.

**Implementations**:
- **ArrayBlockingQueue**: Bounded queue backed by an array
- **LinkedBlockingQueue**: Optionally bounded queue backed by linked nodes
- **PriorityBlockingQueue**: Unbounded priority queue
- **SynchronousQueue**: Zero-capacity queue for direct handoffs between threads
- **DelayQueue**: Queue where elements become available after a delay

**When to use**: For producer-consumer patterns where threads should wait for availability.

```java
BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
queue.put("item");  // Blocks if full
String item = queue.take();  // Blocks if empty
```

### ConcurrentLinkedDeque

Thread-safe double-ended queue allowing concurrent insertion and removal at both ends.

**When to use**: When you need concurrent access to both ends of a queue, useful for work-stealing algorithms.

```java
ConcurrentLinkedDeque<String> deque = new ConcurrentLinkedDeque<>();
deque.addFirst("first");
deque.addLast("last");
```

### ConcurrentSkipListMap & ConcurrentSkipListSet

Thread-safe sorted map and set implementations based on skip lists. Provide concurrent access with O(log n) average time complexity for most operations.

**When to use**: When you need a concurrent sorted collection.

```java
ConcurrentSkipListMap<Integer, String> sortedMap = new ConcurrentSkipListMap<>();
sortedMap.put(3, "three");
sortedMap.put(1, "one");  // Automatically sorted by key
```

### CopyOnWriteArrayList & CopyOnWriteArraySet

Thread-safe collections that create a new copy of the underlying array on every modification. Reads are lock-free and always see a consistent snapshot.

**When to use**: When reads vastly outnumber writes, such as maintaining a list of listeners or observers.

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("item");  // Creates a new internal array
// Iteration is safe even if list is modified concurrently
for (String item : list) {
    System.out.println(item);
}
```

## Parallel Streams

Parallel streams divide the provided task into many subtasks and process them in different threads, utilizing multiple cores of the processor. They use the Fork/Join framework under the hood to manage the parallel execution.

Key characteristics:
- Automatically splits data into chunks
- Processes chunks concurrently
- Combines results transparently
- Uses common ForkJoinPool by default

## Creating Parallel Streams

There are two main ways to create parallel streams:

### Method 1: Using parallelStream()
```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
Stream<Integer> parallelStream = numbers.parallelStream();
```

### Method 2: Converting Sequential Stream to Parallel
```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
Stream<Integer> parallelStream = numbers.stream().parallel();
```

## How Parallel Streams Work

Parallel streams use the Fork/Join framework, which:
1. **Splits** the data source into smaller chunks
2. **Processes** each chunk in a separate thread
3. **Combines** the results from all threads

The default thread pool used is `ForkJoinPool.commonPool()`, which has a parallelism level equal to the number of available processors minus one.

```java
// Check available processors
int processors = Runtime.getRuntime().availableProcessors();
System.out.println("Available processors: " + processors);

// Check ForkJoinPool parallelism
int parallelism = ForkJoinPool.commonPool().getParallelism();
System.out.println("Default parallelism: " + parallelism);
```

## Usage Examples

### Example 1: Sum of Numbers
```java
// Sequential
long sequentialSum = IntStream.rangeClosed(1, 1000000)
    .sum();

// Parallel
long parallelSum = IntStream.rangeClosed(1, 1000000)
    .parallel()
    .sum();
```

### Example 2: Filtering and Collecting
```java
List<String> names = Arrays.asList("John", "Jane", "Jack", "Jill", "James", "Jenny");

// Find names starting with 'J' and having length > 4
List<String> filteredNames = names.parallelStream()
    .filter(name -> name.startsWith("J"))
    .filter(name -> name.length() > 4)
    .collect(Collectors.toList());
```

### Example 3: Map and Reduce Operations
```java
List<Integer> numbers = IntStream.rangeClosed(1, 100)
    .boxed()
    .collect(Collectors.toList());

// Calculate sum of squares
int sumOfSquares = numbers.parallelStream()
    .map(n -> n * n)
    .reduce(0, Integer::sum);
```

### Example 4: Custom Thread Pool
```java
ForkJoinPool customThreadPool = new ForkJoinPool(4);
        try {
        List<Integer> numbers = IntStream.rangeClosed(1, 100)
        .boxed()
        .collect(Collectors.toList());

        long sum = customThreadPool.submit(() ->
        numbers.parallelStream()
        .mapToInt(Integer::intValue)
        .sum()
        ).get();

        System.out.println("Sum: " + sum);
        } catch (Exception e) {
        e.printStackTrace();
        } finally {
        customThreadPool.shutdown();
        }
```

## Performance Considerations

### Benchmarking Example
```java
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class ParallelStreamBenchmark {
    public static void main(String[] args) {
        int size = 10_000_000;
        
        // Sequential processing
        long startSeq = System.nanoTime();
        long sumSeq = IntStream.rangeClosed(1, size)
            .mapToLong(i -> i * i)
            .sum();
        long endSeq = System.nanoTime();
        
        // Parallel processing
        long startPar = System.nanoTime();
        long sumPar = IntStream.rangeClosed(1, size)
            .parallel()
            .mapToLong(i -> i * i)
            .sum();
        long endPar = System.nanoTime();
        
        System.out.println("Sequential time: " + 
            TimeUnit.NANOSECONDS.toMillis(endSeq - startSeq) + " ms");
        System.out.println("Parallel time: " + 
            TimeUnit.NANOSECONDS.toMillis(endPar - startPar) + " ms");
    }
}
```

### Factors Affecting Performance
1. **Data size**: Small datasets may not benefit from parallelization
2. **Operation complexity**: Simple operations might not justify overhead
3. **Data source**: Some sources split better than others
4. **Hardware**: Number of available cores matters

## When to Use Parallel Streams

### Good Use Cases
- Large datasets (thousands of elements or more)
- CPU-intensive operations
- Independent operations (no shared state)
- Operations that can be easily split

### Example: Image Processing
```java
public List<BufferedImage> processImages(List<BufferedImage> images) {
    return images.parallelStream()
        .map(image -> applyFilter(image))
        .map(image -> resize(image))
        .collect(Collectors.toList());
}
```

### When NOT to Use
- Small datasets
- I/O bound operations
- Operations requiring ordering
- When using shared mutable state

## Common Pitfalls

### Pitfall 1: Shared Mutable State
```java
// WRONG - Race condition!
List<Integer> results = new ArrayList<>();
IntStream.rangeClosed(1, 1000)
    .parallel()
    .forEach(i -> results.add(i)); // Not thread-safe!

// CORRECT - Use thread-safe collection
List<Integer> results = IntStream.rangeClosed(1, 1000)
    .parallel()
    .boxed()
    .collect(Collectors.toList());
```

### Pitfall 2: Order Dependency
```java
// Order might not be preserved
List<String> letters = Arrays.asList("a", "b", "c", "d", "e");
letters.parallelStream()
    .forEach(System.out::print); // Output order unpredictable

// Use forEachOrdered to maintain order
letters.parallelStream()
    .forEachOrdered(System.out::print); // Output: abcde
```

### Pitfall 3: Blocking Operations
```java
// Avoid blocking operations in parallel streams
list.parallelStream()
    .map(item -> {
        // Bad: Blocking I/O operation
        return readFromDatabase(item);
    })
    .collect(Collectors.toList());
```
