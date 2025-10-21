## The Streams API

### 2.1 Introduction to Streams

Streams represent a sequence of elements supporting sequential and parallel aggregate operations. They provide a functional approach to processing collections of objects. Streams are not data structures; instead, they are wrappers around data sources that allow us to operate on the data using a pipeline of operations.

Key characteristics of streams:
- **Functional in nature**: Streams support operations expressed as lambda expressions
- **Lazy evaluation**: Intermediate operations are not executed until a terminal operation is invoked
- **Possibly unbounded**: While collections have a finite size, streams need not
- **Consumable**: The elements of a stream are only visited once during the life of a stream
- **No storage**: Streams don't store elements; they convey elements from a source through a pipeline of operations

**Example: Basic Stream Pipeline**

This example demonstrates the fundamental stream pipeline pattern: source → intermediate operations → terminal operation. We create a stream from a list, filter elements based on a condition, and count the results.

```java
import java.util.stream.Stream;
import java.util.List;

List<String> names = List.of("Alice", "Bob", "Charlie", "David");

// Creating and using a stream
long count = names.stream()                    // Source: create stream from list
    .filter(name -> name.length() > 4)         // Intermediate: keep names longer than 4 chars
    .count();                                  // Terminal: count remaining elements

System.out.println("Names longer than 4 characters: " + count); // 2

// The same list can be used to create multiple streams
names.stream()
    .filter(name -> name.startsWith("C"))
    .forEach(System.out::println);             // Prints: Charlie
```

### 2.2 Creating Streams

There are numerous ways to create streams in Java, each suited to different scenarios:

#### From Collections

The most common way to create streams is from existing collections. Any Collection (List, Set, Queue) can be converted to a stream using the `stream()` method.

```java
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

List<String> list = List.of("a", "b", "c");
Stream<String> streamFromList = list.stream();

Set<Integer> set = Set.of(1, 2, 3);
Stream<Integer> streamFromSet = set.stream();

// Usage example: Convert list to uppercase
List<String> upperCaseList = list.stream()
    .map(String::toUpperCase)
    .collect(Collectors.toList());
System.out.println(upperCaseList);  // [A, B, C]
```

#### From Arrays

Arrays can be converted to streams using `Arrays.stream()` or `Stream.of()`. The `Arrays.stream()` method also supports creating streams from array slices.

```java
import java.util.Arrays;
import java.util.stream.Stream;

String[] array = {"x", "y", "z"};
Stream<String> streamFromArray = Arrays.stream(array);

// Or using Stream.of()
Stream<String> streamFromArrayOf = Stream.of(array);

// Create stream from array slice
int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
IntStream slice = Arrays.stream(numbers, 2, 7);  // Elements from index 2 to 6
slice.forEach(System.out::print);  // Prints: 34567
```

#### From Individual Values

`Stream.of()` creates a stream directly from individual values. This is useful when you have a fixed set of values to process.

```java
Stream<String> streamOfValues = Stream.of("one", "two", "three");
Stream<Integer> streamOfNumbers = Stream.of(1, 2, 3, 4, 5);

// Usage example: Calculate sum of specific values
int sum = Stream.of(10, 20, 30, 40, 50)
    .reduce(0, Integer::sum);
System.out.println("Sum: " + sum);  // Sum: 150

// Process mixed types
Stream.of("Java", 8, "Stream", "API")
    .forEach(item -> System.out.println(item.getClass().getSimpleName() + ": " + item));
```

#### Using Stream Builders

Stream builders provide a mutable builder pattern for constructing streams. This is useful when you need to conditionally add elements to a stream.

```java
// Basic builder usage
Stream<String> streamFromBuilder = Stream.<String>builder()
    .add("first")
    .add("second")
    .add("third")
    .build();

// Practical example: Building a stream conditionally
Stream.Builder<String> builder = Stream.builder();
builder.add("always");

if (someCondition) {
    builder.add("conditional1");
}

if (anotherCondition) {
    builder.add("conditional2");
}

Stream<String> conditionalStream = builder.build();
conditionalStream.forEach(System.out::println);
```

#### Infinite Streams

Streams can be infinite, generated on-demand using `Stream.generate()` or `Stream.iterate()`. Always use `limit()` or another short-circuiting operation to avoid infinite processing.

```java
// Generate infinite stream of random numbers
Stream<Double> randomNumbers = Stream.generate(Math::random);

// Generate infinite sequence using iteration
Stream<Integer> infiniteSequence = Stream.iterate(0, n -> n + 2);

// Use limit() to make infinite streams finite
Stream.iterate(0, n -> n + 2)
    .limit(10)
    .forEach(System.out::println); // 0, 2, 4, 6, 8, 10, 12, 14, 16, 18

// Practical example: Generate first 10 Fibonacci numbers
Stream.iterate(new int[]{0, 1}, arr -> new int[]{arr[1], arr[0] + arr[1]})
    .limit(10)
    .map(arr -> arr[0])
    .forEach(System.out::println);  // 0, 1, 1, 2, 3, 5, 8, 13, 21, 34

// Generate random passwords
Stream.generate(() -> (char)('a' + Math.random() * 26))
    .limit(8)
    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
    .toString();  // Random 8-character string
```

### 2.3 Intermediate Operations

Intermediate operations return a new stream and are always lazy. They are not executed until a terminal operation is invoked on the stream pipeline.

#### filter()

The `filter()` operation selects elements that match a given predicate. It's one of the most commonly used stream operations for data filtering.

```java
import java.util.List;
import java.util.stream.Collectors;

List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

// Filter even numbers
List<Integer> evenNumbers = numbers.stream()
    .filter(n -> n % 2 == 0)
    .collect(Collectors.toList());

System.out.println(evenNumbers); // [2, 4, 6, 8, 10]

// Multiple filters can be chained
List<Integer> result = numbers.stream()
    .filter(n -> n % 2 == 0)           // Keep even numbers
    .filter(n -> n > 5)                // Keep numbers greater than 5
    .collect(Collectors.toList());     // Result: [6, 8, 10]

// Complex filtering with objects
class Person {
    String name;
    int age;
    Person(String name, int age) { this.name = name; this.age = age; }
}

List<Person> people = List.of(
    new Person("Alice", 25),
    new Person("Bob", 17),
    new Person("Charlie", 30)
);

List<String> adults = people.stream()
    .filter(p -> p.age >= 18)
    .map(p -> p.name)
    .collect(Collectors.toList());  // [Alice, Charlie]
```

#### map()

The `map()` operation transforms each element of the stream using a provided function. It's a one-to-one transformation where each input element produces exactly one output element.

```java
import java.util.List;
import java.util.stream.Collectors;

List<String> words = List.of("hello", "world", "java", "stream");

// Transform strings to their lengths
List<Integer> lengths = words.stream()
    .map(String::length)                      // String -> Integer
    .collect(Collectors.toList());

System.out.println(lengths); // [5, 5, 4, 6]

// Chaining map operations
List<String> upperCaseWords = words.stream()
    .map(String::toUpperCase)                 // Transform to uppercase
    .filter(s -> s.length() > 4)              // Filter after transformation
    .collect(Collectors.toList());

System.out.println(upperCaseWords); // [HELLO, WORLD, STREAM]

// Complex object transformation
class Product {
    String name;
    double price;
    Product(String name, double price) { this.name = name; this.price = price; }
}

List<Product> products = List.of(
    new Product("Laptop", 999.99),
    new Product("Mouse", 29.99)
);

// Extract and transform product information
List<String> priceLabels = products.stream()
    .map(p -> p.name + ": $" + p.price)      // Product -> String
    .collect(Collectors.toList());            // [Laptop: $999.99, Mouse: $29.99]
```

#### flatMap()

The `flatMap()` operation transforms each element into a stream and then flattens all the streams into a single stream. It's used for one-to-many transformations.

```java
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Flattening nested lists
List<List<Integer>> nestedNumbers = List.of(
    List.of(1, 2, 3),
    List.of(4, 5),
    List.of(6, 7, 8, 9)
);

List<Integer> flattenedNumbers = nestedNumbers.stream()
    .flatMap(List::stream)                    // List<Integer> -> Stream<Integer>
    .collect(Collectors.toList());

System.out.println(flattenedNumbers); // [1, 2, 3, 4, 5, 6, 7, 8, 9]

// Splitting sentences into words
List<String> sentences = List.of("Hello World", "Java Streams", "Functional Programming");

List<String> words = sentences.stream()
    .flatMap(sentence -> Arrays.stream(sentence.split(" ")))  // String -> Stream<String>
    .collect(Collectors.toList());

System.out.println(words); // [Hello, World, Java, Streams, Functional, Programming]

// Practical example: Finding all unique characters in strings
List<String> strings = List.of("abc", "def", "ghi");
List<Character> uniqueChars = strings.stream()
    .flatMap(s -> s.chars().mapToObj(c -> (char) c))         // String -> Stream<Character>
    .distinct()
    .collect(Collectors.toList());  // [a, b, c, d, e, f, g, h, i]

// Working with Optional values
List<Optional<String>> optionals = List.of(
    Optional.of("Hello"),
    Optional.empty(),
    Optional.of("World")
);

List<String> values = optionals.stream()
    .flatMap(Optional::stream)        // Java 9+ - flattens Optional to its value
    .collect(Collectors.toList());    // [Hello, World]
```

#### distinct()

The `distinct()` operation removes duplicate elements from the stream. It uses the `equals()` method to determine duplicates.

```java
import java.util.List;
import java.util.stream.Collectors;

List<Integer> numbersWithDuplicates = List.of(1, 2, 2, 3, 3, 3, 4, 5, 5);

// Remove duplicates
List<Integer> uniqueNumbers = numbersWithDuplicates.stream()
    .distinct()
    .collect(Collectors.toList());

System.out.println(uniqueNumbers); // [1, 2, 3, 4, 5]

// Practical example: Finding unique words in text
String text = "the quick brown fox jumps over the lazy dog the fox";
List<String> uniqueWords = Arrays.stream(text.split(" "))
    .distinct()
    .collect(Collectors.toList());
// Result: [the, quick, brown, fox, jumps, over, lazy, dog]

// With custom objects (requires proper equals/hashCode)
class Employee {
    String name;
    String department;
    
    Employee(String name, String department) {
        this.name = name;
        this.department = department;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee)) return false;
        Employee e = (Employee) o;
        return name.equals(e.name) && department.equals(e.department);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, department);
    }
}
```

#### sorted()

The `sorted()` operation sorts stream elements either in natural order or using a custom comparator. Sorting is a stateful operation that requires processing all elements.

```java
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

List<String> unsortedWords = List.of("zebra", "apple", "mango", "banana");

// Natural ordering (alphabetical for strings)
List<String> sortedWords = unsortedWords.stream()
    .sorted()
    .collect(Collectors.toList());

System.out.println(sortedWords); // [apple, banana, mango, zebra]

// Custom comparator - sort by length
List<String> sortedByLength = unsortedWords.stream()
    .sorted(Comparator.comparing(String::length))
    .collect(Collectors.toList());

System.out.println(sortedByLength); // [apple, mango, zebra, banana]

// Multiple sorting criteria
class Student {
    String name;
    int grade;
    int age;
    
    Student(String name, int grade, int age) {
        this.name = name;
        this.grade = grade;
        this.age = age;
    }
}

List<Student> students = List.of(
    new Student("Alice", 90, 20),
    new Student("Bob", 90, 19),
    new Student("Charlie", 85, 21)
);

// Sort by grade (descending), then by age (ascending)
List<Student> sorted = students.stream()
    .sorted(Comparator.comparing(Student::getGrade).reversed()
            .thenComparing(Student::getAge))
    .collect(Collectors.toList());

// Reverse order
List<String> reverseOrder = unsortedWords.stream()
    .sorted(Comparator.reverseOrder())
    .collect(Collectors.toList());  // [zebra, mango, banana, apple]
```

### 2.4 Terminal Operations

Terminal operations produce a result or a side effect and mark the end of the stream pipeline. Once a terminal operation is performed, the stream is consumed and cannot be used again.

#### collect()

The `collect()` operation is the most versatile terminal operation, allowing you to gather stream elements into various data structures using Collectors.

```java
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

List<String> names = List.of("Alice", "Bob", "Charlie", "David");

// Collect to List
List<String> namesList = names.stream()
    .filter(name -> name.startsWith("A") || name.startsWith("B"))
    .collect(Collectors.toList());
System.out.println(namesList);  // [Alice, Bob]

// Collect to Set (removes duplicates)
Set<String> namesSet = names.stream()
    .collect(Collectors.toSet());
System.out.println(namesSet);  // [Bob, Alice, Charlie, David] (order not guaranteed)

// Collect to Map
Map<String, Integer> nameLengthMap = names.stream()
    .collect(Collectors.toMap(
        Function.identity(),  // key mapper: name -> name
        String::length        // value mapper: name -> length
    ));
System.out.println(nameLengthMap); // {Bob=3, Alice=5, Charlie=7, David=5}

// Joining strings
String joined = names.stream()
    .collect(Collectors.joining(", ", "Names: [", "]"));
System.out.println(joined);  // Names: [Alice, Bob, Charlie, David]

// Grouping by criteria
Map<Integer, List<String>> groupedByLength = names.stream()
    .collect(Collectors.groupingBy(String::length));
System.out.println(groupedByLength);  // {3=[Bob], 5=[Alice, David], 7=[Charlie]}
```

#### forEach()

The `forEach()` operation performs an action for each element in the stream. It's a terminal operation that doesn't return a value and is mainly used for side effects.

```java
import java.util.List;
import java.util.ArrayList;

List<Integer> numbers = List.of(1, 2, 3, 4, 5);

// Print each number
numbers.stream()
    .forEach(System.out::println);  // Prints 1, 2, 3, 4, 5 on separate lines

// Perform multiple actions
numbers.stream()
    .map(n -> n * n)
    .forEach(n -> {
        System.out.print("Square: " + n);
        System.out.println(" (Even: " + (n % 2 == 0) + ")");
    });
// Output:
// Square: 1 (Even: false)
// Square: 4 (Even: true)
// Square: 9 (Even: false)
// Square: 16 (Even: true)
// Square: 25 (Even: false)

// Note: forEach doesn't guarantee order in parallel streams
numbers.parallelStream()
    .forEach(System.out::println);  // May print in any order

// Use forEachOrdered to maintain order in parallel streams
numbers.parallelStream()
    .forEachOrdered(System.out::println);  // Always prints 1, 2, 3, 4, 5

// Common anti-pattern: Don't use forEach to populate collections
// Bad:
List<Integer> squares = new ArrayList<>();
numbers.stream().forEach(n -> squares.add(n * n));

// Good: Use collect instead
List<Integer> squares = numbers.stream()
    .map(n -> n * n)
    .collect(Collectors.toList());
```

#### reduce()

The `reduce()` operation combines stream elements into a single result using an associative accumulation function. It comes in three forms: with identity, without identity, and with combiner.

```java
import java.util.List;
import java.util.Optional;

List<Integer> numbers = List.of(1, 2, 3, 4, 5);

// Form 1: With identity value (always returns a value)
int sum = numbers.stream()
    .reduce(0, (a, b) -> a + b);  // 0 is the identity/initial value

System.out.println("Sum: " + sum); // 15

// Using method reference
int product = numbers.stream()
    .reduce(1, (a, b) -> a * b);  // 1 is the identity for multiplication
System.out.println("Product: " + product); // 120

// Form 2: Without identity (returns Optional)
Optional<Integer> max = numbers.stream()
    .reduce(Integer::max);  // No identity value

max.ifPresent(m -> System.out.println("Max: " + m)); // Max: 5

// Form 3: With identity and combiner (useful for parallel streams)
String sentence = Stream.of("Hello", "World", "from", "Java")
    .parallel()
    .reduce("",                           // identity
            (s1, s2) -> s1 + " " + s2,   // accumulator
            (s1, s2) -> s1 + s2)          // combiner for parallel
    .trim();

System.out.println(sentence); // Hello World from Java

// Practical example: Finding the longest string
List<String> words = List.of("Java", "Stream", "API", "Programming");
Optional<String> longest = words.stream()
    .reduce((s1, s2) -> s1.length() > s2.length() ? s1 : s2);
longest.ifPresent(System.out::println);  // Programming
```

#### Other Terminal Operations

Streams provide several specialized terminal operations for common tasks like counting, finding elements, and testing conditions.

```java
import java.util.List;
import java.util.Optional;

List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

// count() - Returns the number of elements
long count = numbers.stream()
    .filter(n -> n > 5)
    .count();
System.out.println("Count: " + count); // 5

// findFirst() - Returns the first element (deterministic)
Optional<Integer> first = numbers.stream()
    .filter(n -> n > 5)
    .findFirst();
first.ifPresent(n -> System.out.println("First > 5: " + n)); // Always 6

// findAny() - Returns any element (non-deterministic in parallel)
Optional<Integer> any = numbers.parallelStream()
    .filter(n -> n % 3 == 0)
    .findAny();  // May return 3, 6, or 9
any.ifPresent(n -> System.out.println("Any divisible by 3: " + n));

// Short-circuiting match operations
// anyMatch() - True if any element matches
boolean hasEven = numbers.stream()
    .anyMatch(n -> n % 2 == 0);  // Stops at first match
System.out.println("Has even numbers: " + hasEven); // true

// allMatch() - True if all elements match
boolean allPositive = numbers.stream()
    .allMatch(n -> n > 0);  // Stops at first non-match
System.out.println("All positive: " + allPositive); // true

// noneMatch() - True if no elements match
boolean noneNegative = numbers.stream()
    .noneMatch(n -> n < 0);  // Stops at first match
System.out.println("None negative: " + noneNegative); // true

// Practical examples
List<String> emails = List.of("user@example.com", "admin@site.org", "test@domain.com");

// Check if any email is from a specific domain
boolean hasGmailUser = emails.stream()
    .anyMatch(email -> email.endsWith("@gmail.com")); // false

// Find first admin email
Optional<String> adminEmail = emails.stream()
    .filter(email -> email.startsWith("admin"))
    .findFirst();
```

### 2.5 Working with Optional

The Optional class is a container object that may or may not contain a non-null value. It helps avoid null pointer exceptions and makes the code more expressive about the possibility of absence. Streams often return Optional values from operations like `findFirst()` and `reduce()`.

**Creating and Using Optional**

```java
import java.util.Optional;

// Creating Optional objects
Optional<String> empty = Optional.empty();                    // Empty optional
Optional<String> present = Optional.of("Hello");             // Must be non-null
Optional<String> nullable = Optional.ofNullable(null);       // Can be null

// Checking if value is present
if (present.isPresent()) {
    System.out.println("Value: " + present.get());  // Avoid using get() directly
}

// Better approach using ifPresent
present.ifPresent(value -> System.out.println("Value: " + value));

// Providing default values
String value1 = empty.orElse("Default");                     // Direct default value
String value2 = empty.orElseGet(() -> "Generated Default");  // Lazy default value

// Throwing exception if empty
try {
    String value3 = empty.orElseThrow(() -> new IllegalStateException("No value present"));
} catch (IllegalStateException e) {
    System.out.println("Exception caught: " + e.getMessage());
}
```

**Transforming Optional Values**

```java
// map() - Transform the value if present
Optional<Integer> length = present.map(String::length);
length.ifPresent(len -> System.out.println("Length: " + len)); // Length: 5

// flatMap() - Transform to another Optional
Optional<String> upperCase = present.flatMap(s -> 
    s.isEmpty() ? Optional.empty() : Optional.of(s.toUpperCase())
);

// filter() - Keep value only if it matches predicate
Optional<String> filtered = present.filter(s -> s.startsWith("H"));

// Chaining operations
Optional<String> result = Optional.of("  hello  ")
    .map(String::trim)
    .map(String::toUpperCase)
    .filter(s -> s.length() > 3);

System.out.println(result.orElse("Not found")); // HELLO
```

**Practical Optional Usage with Streams**

```java
// Finding user by ID
class User {
    String id;
    String name;
    User(String id, String name) { this.id = id; this.name = name; }
}

List<User> users = List.of(
    new User("1", "Alice"),
    new User("2", "Bob")
);

Optional<User> user = users.stream()
    .filter(u -> u.id.equals("2"))
    .findFirst();

// Chain operations safely
String userName = user
    .map(u -> u.name)
    .map(String::toUpperCase)
    .orElse("Unknown User");

System.out.println(userName);  // BOB
```

### 2.6 Primitive Streams

Java provides specialized stream types for primitives to avoid the overhead of boxing and unboxing. These streams offer additional operations specific to numeric values.

#### IntStream

IntStream is a specialized stream for int values that avoids boxing overhead and provides numeric-specific operations like `sum()`, `average()`, and `summaryStatistics()`.

**Creating IntStreams**

```java
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import java.util.OptionalInt;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

// Range methods - very useful for loops
IntStream range = IntStream.range(1, 10);          // 1 to 9 (exclusive end)
IntStream rangeClosed = IntStream.rangeClosed(1, 10); // 1 to 10 (inclusive end)

// From explicit values
IntStream of = IntStream.of(1, 2, 3, 4, 5);

// From arrays
int[] numbers = {1, 2, 3, 4, 5};
IntStream fromArray = IntStream.of(numbers);

// Generating infinite streams
IntStream evenNumbers = IntStream.iterate(0, n -> n + 2);  // 0, 2, 4, 6...
IntStream randomInts = IntStream.generate(() -> (int)(Math.random() * 100));

// Always limit infinite streams
evenNumbers.limit(10).forEach(System.out::println);
```

**Numeric Operations**

```java
// Specialized terminal operations for numeric streams
int sum = IntStream.rangeClosed(1, 100).sum();              // 5050
OptionalInt max = IntStream.of(3, 1, 4, 1, 5).max();       // 5
OptionalInt min = IntStream.of(3, 1, 4, 1, 5).min();       // 1
double average = IntStream.rangeClosed(1, 10).average().orElse(0); // 5.5

// Getting all statistics at once
IntSummaryStatistics stats = IntStream.rangeClosed(1, 100)
    .summaryStatistics();
System.out.println("Count: " + stats.getCount());     // 100
System.out.println("Sum: " + stats.getSum());         // 5050
System.out.println("Min: " + stats.getMin());         // 1
System.out.println("Max: " + stats.getMax());         // 100
System.out.println("Average: " + stats.getAverage()); // 50.5
```

**Stream Conversions**

```java
// Boxing: IntStream -> Stream<Integer>
Stream<Integer> boxed = IntStream.rangeClosed(1, 5).boxed();

// To other primitive streams
LongStream longs = IntStream.rangeClosed(1, 5).asLongStream();
DoubleStream doubles = IntStream.rangeClosed(1, 5).asDoubleStream();

// mapToObj: IntStream -> Stream<T>
Stream<String> strings = IntStream.rangeClosed(1, 5)
    .mapToObj(i -> "Number: " + i);
```

**Practical Example: Prime Numbers**

```java
class PrimeExample {
    static boolean isPrime(int n) {
        return n > 1 && IntStream.rangeClosed(2, (int) Math.sqrt(n))
            .noneMatch(divisor -> n % divisor == 0);
    }
    
    public static void main(String[] args) {
        // Find all primes up to 100
        List<Integer> primes = IntStream.rangeClosed(2, 100)
            .filter(PrimeExample::isPrime)
            .boxed()
            .collect(Collectors.toList());
        
        System.out.println("Primes: " + primes);
        System.out.println("Count: " + primes.size());
    }
}
```

#### LongStream

LongStream provides similar functionality to IntStream but for long values. It's useful for working with large numbers, timestamps, and IDs.

```java
import java.util.stream.LongStream;
import java.util.OptionalLong;
import java.util.LongSummaryStatistics;
import java.util.concurrent.ThreadLocalRandom;

// Creating LongStreams
LongStream range = LongStream.range(1L, 1_000_000L);         // Exclusive end
LongStream rangeClosed = LongStream.rangeClosed(1L, 10L);   // Inclusive end
LongStream of = LongStream.of(1L, 2L, 3L, 4L, 5L);

// Practical Examples
class LongStreamExample {
    // Calculate factorial (watch for overflow with large numbers!)
    static long factorial(int n) {
        return LongStream.rangeClosed(1, n)
            .reduce(1, (a, b) -> a * b);
    }
    
    // Generate unique IDs
    static LongStream generateIds(long start, int count) {
        return LongStream.range(start, start + count);
    }
    
    public static void main(String[] args) {
        // Working with timestamps
        LongStream timestamps = LongStream.generate(System::currentTimeMillis)
            .limit(10);
        
        // Calculate time differences
        long start = System.currentTimeMillis();
        // ... some operation ...
        long elapsed = System.currentTimeMillis() - start;
        
        // Parallel computation for large datasets
        long sum = LongStream.rangeClosed(1, 1_000_000)
            .parallel()
            .sum();
        System.out.println("Sum: " + sum);  // 500000500000
        
        // Random number generation with bounds
        LongStream randomLongs = ThreadLocalRandom.current()
            .longs(100, 0, 1000);  // 100 random longs between 0 and 999
        
        // Statistics
        LongSummaryStatistics stats = LongStream.rangeClosed(1, 1000)
            .summaryStatistics();
        System.out.println("Average: " + stats.getAverage());  // 500.5
        
        // Example: Processing file sizes
        long[] fileSizes = {1024L, 2048L, 512L, 4096L};
        long totalSize = LongStream.of(fileSizes).sum();
        System.out.println("Total size: " + totalSize + " bytes");
    }
}
```

#### DoubleStream

DoubleStream is specialized for floating-point operations and is commonly used for mathematical calculations, statistics, and scientific computing.

```java
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.OptionalDouble;
import java.util.DoubleSummaryStatistics;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleUnaryOperator;

// Creating DoubleStreams
DoubleStream of = DoubleStream.of(1.5, 2.3, 3.7, 4.2, 5.9);
DoubleStream random = DoubleStream.generate(Math::random).limit(10);  // 10 random values [0, 1)

// Mathematical operations
double[] values = {1.0, 2.0, 3.0, 4.0, 5.0};
double sum = DoubleStream.of(values).sum();                          // 15.0
double product = DoubleStream.of(values).reduce(1.0, (a, b) -> a * b); // 120.0

// Statistical operations
OptionalDouble average = DoubleStream.of(values).average();          // 3.0
OptionalDouble stdDev = calculateStandardDeviation(values);          // Custom calculation

// Generating distributions
DoubleStream gaussian = new Random().doubles().limit(1000);          // Gaussian distribution
DoubleStream uniform = DoubleStream.generate(() ->                   // Uniform [0, 100)
    ThreadLocalRandom.current().nextDouble(0, 100));
```

**Mathematical Transformations and Calculations**

```java
// Generate sine wave values
DoubleStream sineWave = DoubleStream.iterate(0, x -> x + 0.1)
    .limit(100)
    .map(Math::sin);

// Example: Scientific calculations
class MathExample {
    // Numerical integration using trapezoid rule
    static double integrate(DoubleUnaryOperator f, double a, double b, int n) {
        double h = (b - a) / n;
        return h * (0.5 * f.applyAsDouble(a) + 
            IntStream.range(1, n)
                .mapToDouble(i -> f.applyAsDouble(a + i * h))
                .sum() + 
            0.5 * f.applyAsDouble(b));
    }
    
    // Calculate standard deviation
    static double standardDeviation(double[] values) {
        DoubleSummaryStatistics stats = DoubleStream.of(values).summaryStatistics();
        double mean = stats.getAverage();
        
        double variance = DoubleStream.of(values)
            .map(x -> Math.pow(x - mean, 2))
            .average()
            .orElse(0.0);
            
        return Math.sqrt(variance);
    }
    
    public static void main(String[] args) {
        // Integrate x^2 from 0 to 1
        double result = integrate(x -> x * x, 0, 1, 1000);
        System.out.println("Integral of x^2 from 0 to 1: " + result);  // ≈ 0.333
        
        // Temperature conversions
        double[] celsius = {0, 20, 37, 100};
        DoubleStream fahrenheit = DoubleStream.of(celsius)
            .map(c -> c * 9.0/5.0 + 32);
        fahrenheit.forEach(f -> System.out.println(f + "°F"));
    }
}

// Get comprehensive statistics
DoubleSummaryStatistics stats = DoubleStream.of(values)
    .summaryStatistics();
System.out.println("Count: " + stats.getCount());
System.out.println("Average: " + stats.getAverage());
System.out.println("Min: " + stats.getMin());
System.out.println("Max: " + stats.getMax());
```

#### Converting Between Stream Types

Java provides methods to convert between object streams and primitive streams, which is essential for performance optimization and proper type handling.

```java
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.DoubleStream;
import java.util.Arrays;

// Object stream to primitive stream (unboxing)
Stream<String> strings = Stream.of("1", "2", "3", "4", "5");
IntStream lengths = strings.mapToInt(String::length);  // String -> int

DoubleStream parsed = Stream.of("1.5", "2.3", "3.7")
    .mapToDouble(Double::parseDouble);  // String -> double

// Primitive to object stream (boxing)
Stream<Integer> boxedInts = IntStream.rangeClosed(1, 10).boxed();
Stream<String> numberStrings = IntStream.rangeClosed(1, 10)
    .mapToObj(i -> "Number: " + i);  // int -> String

// Between primitive streams
IntStream ints = IntStream.rangeClosed(1, 10);
LongStream longs = ints.asLongStream();  // int -> long
DoubleStream doubles = IntStream.rangeClosed(1, 10).asDoubleStream();  // int -> double

// FlatMapping with primitive streams
IntStream flatMapped = Stream.of("1,2,3", "4,5,6")
    .flatMapToInt(s -> Arrays.stream(s.split(","))
        .mapToInt(Integer::parseInt));  // Results in: 1, 2, 3, 4, 5, 6
```

**Practical Conversion Examples**

```java
// Example 1: Calculate total length of all strings
List<String> words = List.of("Hello", "World", "Java");
int totalLength = words.stream()
    .mapToInt(String::length)  // Convert to IntStream for sum()
    .sum();  // 14

// Example 2: Parse and process numeric strings
List<String> prices = List.of("19.99", "29.99", "39.99");
double total = prices.stream()
    .mapToDouble(Double::parseDouble)
    .sum();
System.out.println("Total: $" + total);  // Total: $89.97

// Example 3: Convert character codes to characters
IntStream asciiCodes = IntStream.of(72, 101, 108, 108, 111);
String word = asciiCodes
    .mapToObj(code -> String.valueOf((char) code))
    .collect(Collectors.joining());  // "Hello"

// Example 4: Working with indices
List<String> items = List.of("A", "B", "C", "D");
IntStream.range(0, items.size())
    .mapToObj(i -> i + ": " + items.get(i))
    .forEach(System.out::println);  // 0: A, 1: B, 2: C, 3: D
```

### 2.7 Collectors and Custom Collectors

The Collectors class provides a rich set of reduction operations, and you can create custom collectors for specific needs.

#### Built-in Collectors

The Collectors class provides numerous pre-built collectors for common reduction operations. Understanding these collectors is essential for effective stream processing.

**Collection Collectors**

```java
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.*;

List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "David", "Eve");

// Basic collection collectors
List<String> list = names.stream().collect(toList());         // ArrayList
Set<String> set = names.stream().collect(toSet());           // HashSet (no duplicates)
LinkedList<String> linkedList = names.stream()
    .collect(toCollection(LinkedList::new));                  // Specific collection type

// String joining - very useful for creating formatted strings
String joined = names.stream().collect(joining());            // AliceBobCharlieDavidEve
String delimited = names.stream().collect(joining(", "));     // Alice, Bob, Charlie, David, Eve
String wrapped = names.stream()
    .collect(joining(", ", "[", "]"));                       // [Alice, Bob, Charlie, David, Eve]
```

**Numeric Collectors**

```java
// Counting and summing
long count = names.stream().collect(counting());              // 5
int totalLength = names.stream()
    .collect(summingInt(String::length));                     // 23
double averageLength = names.stream()
    .collect(averagingDouble(String::length));                // 4.6

// Min/Max with comparator
Optional<String> longest = names.stream()
    .collect(maxBy(Comparator.comparing(String::length)));    // Charlie
Optional<String> shortest = names.stream()
    .collect(minBy(Comparator.comparing(String::length)));    // Bob or Eve

// Get all statistics at once
IntSummaryStatistics lengthStats = names.stream()
    .collect(summarizingInt(String::length));
System.out.println("Count: " + lengthStats.getCount());      // 5
System.out.println("Average: " + lengthStats.getAverage());  // 4.6
System.out.println("Min: " + lengthStats.getMin());          // 3
System.out.println("Max: " + lengthStats.getMax());          // 7
```

**Map Collectors**

```java
// Creating maps from streams
Map<String, Integer> nameLengths = names.stream()
    .collect(toMap(
        Function.identity(),     // Key: the name itself
        String::length          // Value: length of the name
    ));
// Result: {Bob=3, Alice=5, Charlie=7, David=5, Eve=3}

// Handling duplicate keys
List<String> items = Arrays.asList("apple", "banana", "apple", "cherry");
Map<String, Integer> itemCounts = items.stream()
    .collect(toMap(
        Function.identity(),
        s -> 1,                                    // Initial count
        (existing, replacement) -> existing + 1    // Merge function for duplicates
    ));
// Result: {apple=2, banana=1, cherry=1}

// Concurrent collections for parallel streams
ConcurrentMap<String, Integer> concurrent = names.parallelStream()
    .collect(toConcurrentMap(
        Function.identity(),
        String::length
    ));
```

#### Grouping and Partitioning

Grouping and partitioning are powerful collectors that organize stream elements into maps based on classification functions.

```java
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.*;

// Sample data
class Person {
    private String name;
    private int age;
    private String city;
    private double salary;
    
    public Person(String name, int age, String city, double salary) {
        this.name = name;
        this.age = age;
        this.city = city;
        this.salary = salary;
    }
    
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getCity() { return city; }
    public double getSalary() { return salary; }
}

List<Person> people = Arrays.asList(
    new Person("Alice", 25, "New York", 50000),
    new Person("Bob", 30, "London", 60000),
    new Person("Charlie", 25, "New York", 55000),
    new Person("David", 35, "London", 70000),
    new Person("Eve", 30, "Paris", 65000)
);

**Grouping Operations**

```java
// Simple grouping - creates Map<K, List<T>>
Map<String, List<Person>> byCity = people.stream()
    .collect(groupingBy(Person::getCity));
// Result: {New York=[Alice, Charlie], London=[Bob, David], Paris=[Eve]}

// Grouping with counting
Map<String, Long> countByCity = people.stream()
    .collect(groupingBy(Person::getCity, counting()));
// Result: {New York=2, London=2, Paris=1}

// Grouping with average calculation
Map<String, Double> avgSalaryByCity = people.stream()
    .collect(groupingBy(
        Person::getCity,
        averagingDouble(Person::getSalary)
    ));
// Result: {New York=52500.0, London=65000.0, Paris=65000.0}

// Multi-level grouping
Map<String, Map<Integer, List<Person>>> byCityAndAge = people.stream()
    .collect(groupingBy(
        Person::getCity,
        groupingBy(Person::getAge)
    ));
// Result: {New York={25=[Alice, Charlie]}, London={30=[Bob], 35=[David]}, Paris={30=[Eve]}}

// Grouping with transformation
Map<String, Set<String>> namesByCity = people.stream()
    .collect(groupingBy(
        Person::getCity,
        mapping(Person::getName, toSet())  // Transform Person to name
    ));
// Result: {New York=[Alice, Charlie], London=[Bob, David], Paris=[Eve]}
```

**Partitioning Operations**

```java
// Partitioning splits into exactly two groups: true and false
Map<Boolean, List<Person>> partitionedBySalary = people.stream()
    .collect(partitioningBy(p -> p.getSalary() > 60000));
// Result: {false=[Alice, Charlie], true=[Bob, David, Eve]}

// Partitioning with downstream collector
Map<Boolean, Map<String, List<Person>>> complexPartition = people.stream()
    .collect(partitioningBy(
        p -> p.getAge() > 30,                    // Partition by age
        groupingBy(Person::getCity)              // Then group by city
    ));
// Result: {false={New York=[Alice, Charlie], London=[Bob], Paris=[Eve]}, 
//          true={London=[David]}}

// Concurrent grouping for parallel streams
ConcurrentMap<String, List<Person>> concurrentGrouping = people.parallelStream()
    .collect(groupingByConcurrent(Person::getCity));
```

#### Custom Collectors

When built-in collectors don't meet your needs, you can create custom collectors using `Collector.of()`. A collector consists of four components: supplier, accumulator, combiner, and finisher.

```java
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collector;

// Custom collector implementations
class CustomCollectors {
    
    /**
     * Collector that calculates the median of numeric values
     */
    public static <T> Collector<T, ?, Double> toMedian(ToDoubleFunction<T> mapper) {
        return Collector.of(
            () -> new ArrayList<Double>(),                         // Supplier: creates accumulator
            (list, item) -> list.add(mapper.applyAsDouble(item)), // Accumulator: adds elements
            (list1, list2) -> {                                   // Combiner: merges partial results
                list1.addAll(list2); 
                return list1; 
            },
            list -> {                                             // Finisher: produces final result
                Collections.sort(list);
                int size = list.size();
                if (size == 0) return 0.0;
                if (size % 2 == 0) {
                    return (list.get(size/2 - 1) + list.get(size/2)) / 2.0;
                } else {
                    return list.get(size/2);
                }
            }
        );
    }
    
    /**
     * Collector that keeps only the top N elements according to a comparator
     */
    public static <T> Collector<T, ?, List<T>> toTopN(int n, Comparator<? super T> comparator) {
        return Collector.of(
            () -> new TreeSet<>(comparator.reversed()),  // TreeSet maintains sorted order
            (set, elem) -> {
                set.add(elem);
                if (set.size() > n) {
                    set.remove(set.last());              // Remove smallest when exceeding n
                }
            },
            (set1, set2) -> {                           // Merge two sets
                set1.addAll(set2);
                while (set1.size() > n) {
                    set1.remove(set1.last());
                }
                return set1;
            },
            set -> new ArrayList<>(set),                // Convert to list
            Collector.Characteristics.UNORDERED         // Result order doesn't match encounter order
        );
    }
    
    /**
     * Collector that creates a frequency map
     */
    public static <T> Collector<T, ?, Map<T, Long>> toFrequencyMap() {
        return Collector.of(
            HashMap::new,                               // Create empty map
            (map, item) -> map.merge(item, 1L, Long::sum), // Increment count
            (map1, map2) -> {                          // Merge two frequency maps
                map2.forEach((key, value) -> map1.merge(key, value, Long::sum));
                return map1;
            }
        );
    }
}
```

**Using Custom Collectors**

```java
// Calculate median salary
double median = people.stream()
    .collect(CustomCollectors.toMedian(Person::getSalary));
System.out.println("Median salary: $" + median);  // $60000.0

// Get top 3 earners
List<Person> top3Earners = people.stream()
    .collect(CustomCollectors.toTopN(3, Comparator.comparing(Person::getSalary)));
top3Earners.forEach(p -> System.out.println(p.getName() + ": $" + p.getSalary()));
// David: $70000, Eve: $65000, Bob: $60000

// Count frequency of cities
Map<String, Long> cityFrequency = people.stream()
    .map(Person::getCity)
    .collect(CustomCollectors.toFrequencyMap());
System.out.println(cityFrequency);  // {New York=2, London=2, Paris=1}

// Practical example: Word frequency in text
String text = "the quick brown fox jumps over the lazy dog";
Map<String, Long> wordFreq = Arrays.stream(text.split(" "))
    .collect(CustomCollectors.toFrequencyMap());
// {the=2, quick=1, brown=1, fox=1, jumps=1, over=1, lazy=1, dog=1}
```

#### Advanced Collector Composition

Collectors can be composed to create complex reduction operations. The `collectingAndThen` collector is particularly useful for post-processing collection results.

```java
import java.util.stream.Collector;
import static java.util.stream.Collectors.*;

// Composing collectors to join names by city
Collector<Person, ?, Map<String, String>> composedCollector = 
    groupingBy(
        Person::getCity,
        collectingAndThen(
            mapping(Person::getName, toList()),       // First: collect names to list
            names -> String.join(", ", names)         // Then: join with comma
        )
    );

Map<String, String> namesByCity = people.stream()
    .collect(composedCollector);
// Result: {New York=Alice, Charlie, London=Bob, David, Paris=Eve}

// Another example: Get the highest earner per city
Map<String, Optional<Person>> topEarnerByCity = people.stream()
    .collect(groupingBy(
        Person::getCity,
        maxBy(Comparator.comparing(Person::getSalary))
    ));

// Post-process to get just the names
Map<String, String> topEarnerNameByCity = people.stream()
    .collect(groupingBy(
        Person::getCity,
        collectingAndThen(
            maxBy(Comparator.comparing(Person::getSalary)),
            opt -> opt.map(Person::getName).orElse("No one")
        )
    ));
// Result: {New York=Charlie, London=David, Paris=Eve}
```

**Note on Java Version Compatibility**

```java
// Java 12+ teeing collector - combines two collectors
// Pair<Double, Long> avgAndCount = people.stream()
//     .collect(teeing(
//         averagingDouble(Person::getSalary),
//         counting(),
//         (avg, count) -> new Pair<>(avg, count)
//     ));

// Java 9+ filtering collector
// Map<String, List<Person>> filtered = people.stream()
//     .collect(groupingBy(
//         Person::getCity,
//         filtering(p -> p.getSalary() > 60000, toList())
//     ));

// Alternative for older Java versions:
Map<String, List<Person>> highEarnersByCity = people.stream()
    .filter(p -> p.getSalary() > 60000)
    .collect(groupingBy(Person::getCity));
```

### 2.8 Advanced Stream Operations and Patterns

#### Stream Debugging and Monitoring

When working with complex stream pipelines, debugging and monitoring become essential. The `peek()` operation and custom monitoring utilities help track stream processing.

```java
import java.util.List;
import java.util.function.Function;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// Peek for debugging
List<String> result = Stream.of("a", "b", "c", "d")
    .peek(s -> System.out.println("Processing: " + s))
    .map(String::toUpperCase)
    .peek(s -> System.out.println("After map: " + s))
    .filter(s -> !s.equals("B"))
    .peek(s -> System.out.println("After filter: " + s))
    .collect(Collectors.toList());

// Custom stream monitor
class StreamMonitor {
    private final AtomicLong count = new AtomicLong();
    private final AtomicLong processTime = new AtomicLong();
    
    public <T> Function<T, T> monitor(String operation) {
        return item -> {
            long start = System.nanoTime();
            count.incrementAndGet();
            System.out.printf("%s processing item %d: %s%n", 
                operation, count.get(), item);
            processTime.addAndGet(System.nanoTime() - start);
            return item;
        };
    }
    
    public void printStats() {
        System.out.printf("Processed %d items in %d ms%n",
            count.get(), processTime.get() / 1_000_000);
    }
}

/**
 * Using the stream monitor to track processing
 */
public class MonitoringExample {
    public static void main(String[] args) {
        StreamMonitor monitor = new StreamMonitor();
        
        // Monitor a complex pipeline
        List<Integer> result = IntStream.rangeClosed(1, 100)
            .boxed()
            .map(monitor.monitor("Input"))          // Monitor input values
            .map(n -> n * n)
            .map(monitor.monitor("After square"))   // Monitor after squaring
            .filter(n -> n % 2 == 0)
            .map(monitor.monitor("After filter"))   // Monitor what passed filter
            .collect(Collectors.toList());
        
        monitor.printStats();
        System.out.println("Result size: " + result.size());
        
        // Example: Debugging performance issues
        List<String> urls = List.of("url1", "url2", "url3");
        StreamMonitor apiMonitor = new StreamMonitor();
        
        List<String> responses = urls.stream()
            .map(apiMonitor.monitor("Fetching"))
            .map(url -> fetchUrl(url))  // Simulated API call
            .map(apiMonitor.monitor("Processing response"))
            .filter(response -> response != null)
            .collect(Collectors.toList());
        
        apiMonitor.printStats();  // Shows timing for each operation
    }
    
    private static String fetchUrl(String url) {
        // Simulate API call with delay
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        return "Response from " + url;
    }
}
```

#### Stream Caching and Reuse

Streams are consumed after a terminal operation and cannot be reused. The caching pattern allows you to process the same data multiple times without recreating the stream.

```java
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

// Caching stream results
class CachedStream<T> {
    private final Supplier<Stream<T>> streamSupplier;
    private List<T> cache;
    
    public CachedStream(Supplier<Stream<T>> streamSupplier) {
        this.streamSupplier = streamSupplier;
    }
    
    public Stream<T> stream() {
        if (cache == null) {
            cache = streamSupplier.get().collect(Collectors.toList());
        }
        return cache.stream();
    }
}

/**
 * Practical examples of stream caching
 */
class CacheExample {
    public static void main(String[] args) {
        // Example 1: Cache expensive file reading operation
        CachedStream<String> cachedFile = new CachedStream<>(() -> {
            try {
                System.out.println("Reading file (expensive operation)...");
                return Files.lines(Paths.get("data.txt"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty());
            } catch (IOException e) {
                return Stream.empty();
            }
        });
        
        // File is read only once, then cached
        long lineCount = cachedFile.stream().count();
        System.out.println("Lines: " + lineCount);
        
        // Reuse cached data - no file reading
        List<String> longLines = cachedFile.stream()
            .filter(line -> line.length() > 50)
            .collect(Collectors.toList());
        System.out.println("Long lines: " + longLines.size());
        
        // Example 2: Cache computation results
        CachedStream<Integer> cachedComputation = new CachedStream<>(() -> {
            System.out.println("Performing expensive computation...");
            return IntStream.range(1, 1000)
                .map(n -> n * n * n)  // Cube each number
                .filter(n -> n % 2 == 0)
                .boxed();
        });
        
        // Multiple analyses on same computed data
        double average = cachedComputation.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0);
        
        int max = cachedComputation.stream()
            .max(Integer::compareTo)
            .orElse(0);
        
        System.out.println("Average: " + average + ", Max: " + max);
    }
}
```

#### Error Handling in Streams

Streams don't handle checked exceptions well by default. These patterns show how to handle errors gracefully in stream pipelines.

```java
import java.util.function.Function;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.List;

// Wrapper for checked exceptions
@FunctionalInterface
interface CheckedFunction<T, R> {
    R apply(T t) throws Exception;
    
    static <T, R> Function<T, R> wrap(CheckedFunction<T, R> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}

/**
 * Error handling patterns in streams
 */
public class ErrorHandlingExample {
    public static void main(String[] args) {
        // Example 1: Handling parsing errors with Optional
        List<String> numberStrings = List.of("1", "2", "abc", "4", "5.5", "6");
        
        // Approach 1: Collect all results including failures
        List<Optional<Integer>> results = numberStrings.stream()
            .map(s -> {
                try {
                    return Optional.of(Integer.parseInt(s));
                } catch (NumberFormatException e) {
                    System.err.println("Failed to parse: " + s);
                    return Optional.<Integer>empty();
                }
            })
            .collect(Collectors.toList());
        
        // Extract only valid numbers
        List<Integer> validNumbers = results.stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        System.out.println("Valid numbers: " + validNumbers);  // [1, 2, 4, 6]
        
        // Approach 2: Direct filtering
        List<Integer> numbers = numberStrings.stream()
            .filter(s -> s.matches("\\d+"))  // Pre-filter valid format
            .map(Integer::parseInt)
            .collect(Collectors.toList());
        
        // Example 2: Using CheckedFunction for file operations
        List<String> filePaths = List.of("config.txt", "data.txt", "missing.txt");
        
        // This will throw RuntimeException if any file is missing
        try {
            List<String> contents = filePaths.stream()
                .map(CheckedFunction.wrap(path -> 
                    Files.readString(Paths.get(path))))
                .collect(Collectors.toList());
        } catch (RuntimeException e) {
            System.err.println("Error reading files: " + e.getCause());
        }
        
        // Example 3: Collecting errors and successes separately
        Map<Boolean, List<String>> partitioned = numberStrings.stream()
            .collect(Collectors.partitioningBy(s -> s.matches("\\d+")));
        
        List<String> valid = partitioned.get(true);
        List<String> invalid = partitioned.get(false);
        System.out.println("Invalid inputs: " + invalid);  // [abc, 5.5]
    }
}

// Example 4: Result wrapper pattern
class Result<T> {
    private final T value;
    private final Exception error;
    
    private Result(T value, Exception error) {
        this.value = value;
        this.error = error;
    }
    
    static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }
    
    static <T> Result<T> failure(Exception error) {
        return new Result<>(null, error);
    }
    
    boolean isSuccess() { return error == null; }
    T getValue() { return value; }
    Exception getError() { return error; }
}

// Usage of Result pattern
List<Result<Integer>> parseResults = numberStrings.stream()
    .map(s -> {
        try {
            return Result.success(Integer.parseInt(s));
        } catch (Exception e) {
            return Result.<Integer>failure(e);
        }
    })
    .collect(Collectors.toList());

// Process successes
parseResults.stream()
    .filter(Result::isSuccess)
    .map(Result::getValue)
    .forEach(System.out::println);

// Log failures
parseResults.stream()
    .filter(r -> !r.isSuccess())
    .forEach(r -> System.err.println("Error: " + r.getError().getMessage()));
```