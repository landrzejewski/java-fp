## Functional Interfaces and Lambda Expressions

### 1.1 Understanding Functional Interfaces

A functional interface is a core concept in Java's functional programming paradigm. It is an interface that contains exactly one abstract method, though it may contain any number of default or static methods. This single abstract method represents the functional contract that can be implemented using lambda expressions.

The key characteristics of functional interfaces include:
- They enable the use of lambda expressions and method references
- They can be annotated with `@FunctionalInterface` for compile-time checking
- They form the basis for Java's functional programming capabilities

```java
@FunctionalInterface
interface Calculator {
    int calculate(int a, int b);
    
    // Default method is allowed
    default void printResult(int result) {
        System.out.println("Result: " + result);
    }
}
```

**Explanation**: The `@FunctionalInterface` annotation is optional but recommended. It instructs the compiler to verify that the interface has exactly one abstract method. If you accidentally add a second abstract method, the compiler will generate an error, helping prevent mistakes.

**Usage Example**:
```java
public class CalculatorExample {
    public static void main(String[] args) {
        // Using lambda expression to implement Calculator
        Calculator addition = (a, b) -> a + b;
        Calculator multiplication = (a, b) -> a * b;
        
        // Using the functional interface
        int sum = addition.calculate(10, 5);
        addition.printResult(sum); // Output: Result: 15
        
        int product = multiplication.calculate(10, 5);
        multiplication.printResult(product); // Output: Result: 50
        
        // Passing functional interface as method parameter
        performCalculation(20, 10, (a, b) -> a - b); // Output: Result: 10
    }
    
    static void performCalculation(int x, int y, Calculator calc) {
        int result = calc.calculate(x, y);
        calc.printResult(result);
    }
}
```

### 1.2 Lambda Expressions

Lambda expressions provide a concise way to represent functional interfaces. They allow you to treat functionality as a method argument or store it in a variable. The syntax of a lambda expression consists of parameters, an arrow token (->), and a body.

Basic lambda syntax patterns:
```java
// No parameters
() -> System.out.println("Hello World!")

// Single parameter (parentheses optional)
x -> x * 2

// Multiple parameters
(x, y) -> x + y

// Multiple statements in body
(x, y) -> {
    int sum = x + y;
    return sum * 2;
}
```

**Explanation**: Lambda expressions have flexible syntax rules. For single parameters, parentheses are optional. For zero or multiple parameters, parentheses are required. If the body contains a single expression, braces and the `return` keyword can be omitted. For multiple statements, braces are required and `return` must be explicit for non-void methods.

**Complete Usage Example**:
```java
import java.util.function.*;

public class LambdaSyntaxDemo {
    public static void main(String[] args) {
        // No parameters - Runnable
        Runnable greeting = () -> System.out.println("Hello World!");
        greeting.run(); // Output: Hello World!
        
        // Single parameter - UnaryOperator
        UnaryOperator<Integer> doubler = x -> x * 2;
        System.out.println(doubler.apply(5)); // Output: 10
        
        // Multiple parameters - BiFunction
        BiFunction<Integer, Integer, Integer> adder = (x, y) -> x + y;
        System.out.println(adder.apply(3, 4)); // Output: 7
        
        // Multiple statements - Function with complex logic
        Function<String, Integer> wordScorer = (word) -> {
            int score = word.length();
            if (word.contains("java")) {
                score += 10;
            }
            return score * 2;
        };
        System.out.println(wordScorer.apply("java")); // Output: 28
        System.out.println(wordScorer.apply("hello")); // Output: 10
    }
}
```

Example of lambda expressions in action:
```java
// Traditional anonymous class
Calculator add = new Calculator() {
    @Override
    public int calculate(int a, int b) {
        return a + b;
    }
};

// Lambda expression equivalent
Calculator addLambda = (a, b) -> a + b;
Calculator multiplyLambda = (a, b) -> a * b;

// Using the lambdas
System.out.println(addLambda.calculate(5, 3));      // Output: 8
System.out.println(multiplyLambda.calculate(5, 3)); // Output: 15
```

**Explanation**: Lambda expressions dramatically reduce boilerplate code compared to anonymous inner classes. The compiler automatically infers the parameter types from the functional interface's method signature, and the return type from the expression. This example shows how a 6-line anonymous class can be replaced with a single-line lambda expression while maintaining the same functionality.

### 1.3 Type Inference with Lambdas

Type inference is a powerful feature in Java that allows the compiler to automatically determine types based on context, making lambda expressions more concise and readable. Understanding how type inference works with lambdas is crucial for writing idiomatic functional Java code.

#### Basic Type Inference

The Java compiler can infer types for lambda parameters when the target type is known:

```java
// Without type inference - explicit types
Function<String, Integer> lengthFunction1 = (String s) -> s.length();

// With type inference - compiler infers String type
Function<String, Integer> lengthFunction2 = s -> s.length();

// Multiple parameters - explicit types
BiFunction<Integer, Integer, Integer> add1 = (Integer a, Integer b) -> a + b;

// Multiple parameters - inferred types
BiFunction<Integer, Integer, Integer> add2 = (a, b) -> a + b;
```

**Explanation**: Type inference eliminates the need to explicitly declare parameter types in lambda expressions. The compiler examines the target functional interface's method signature and automatically determines the parameter types. This makes lambdas more concise without sacrificing type safety.

#### Target Type Context

The compiler uses the target type (the functional interface being implemented) to infer parameter types:

```java
// The compiler knows 'compare' expects two Integer parameters
Comparator<Integer> comp = (a, b) -> a - b;

// In method calls, the parameter type provides context
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
names.sort((s1, s2) -> s1.compareToIgnoreCase(s2));

// Generic method inference
public static <T> void process(T item, Consumer<T> processor) {
    processor.accept(item);
}

// Usage - compiler infers String for T
process("Hello", s -> System.out.println(s.toUpperCase()));
```

**Explanation**: The target type context allows the compiler to infer types in various scenarios. When passing a lambda to a method, the method's parameter type provides the context. With generic methods, the compiler can often infer both the generic type parameter and the lambda parameter types from the arguments provided.

**Complete Example with Multiple Contexts**:
```java
import java.util.*;
import java.util.function.*;

public class TargetTypeContextDemo {
    public static void main(String[] args) {
        // Context from variable declaration
        Predicate<String> isEmpty = s -> s.isEmpty();
        
        // Context from method parameter
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        numbers.removeIf(n -> n % 2 == 0); // Removes even numbers
        
        // Context from return type
        Supplier<String> supplier = createSupplier();
        System.out.println(supplier.get()); // Output: Generated String
        
        // Context from cast
        Object func = (Function<String, Integer>) s -> s.length();
        System.out.println(((Function<String, Integer>) func).apply("test")); // Output: 4
    }
    
    static Supplier<String> createSupplier() {
        // Return type provides context
        return () -> "Generated String";
    }
}
```

#### Type Inference in Method Chains

Type inference works seamlessly with method chaining in streams and other fluent APIs:

```java
List<String> words = Arrays.asList("apple", "banana", "cherry");

// Type inference throughout the chain
List<Integer> lengths = words.stream()
    .filter(s -> s.length() > 5)      // s inferred as String
    .map(s -> s.toUpperCase())        // s inferred as String
    .map(s -> s.length())             // s inferred as String, returns Integer
    .filter(len -> len < 10)          // len inferred as Integer
    .collect(Collectors.toList());

// Complex type inference with flatMap
Map<String, List<Integer>> data = new HashMap<>();
data.put("A", Arrays.asList(1, 2, 3));
data.put("B", Arrays.asList(4, 5, 6));

List<String> result = data.entrySet().stream()
    .flatMap(entry -> entry.getValue().stream()  // entry inferred as Map.Entry<String, List<Integer>>
        .map(num -> entry.getKey() + num))       // num inferred as Integer
    .collect(Collectors.toList());
```

**Explanation**: In stream operations, each intermediate operation knows its input and output types, allowing the compiler to infer types throughout the chain. The type inference flows through the pipeline, with each lambda parameter's type determined by the previous operation's output type. This enables writing complex data transformations without explicitly declaring any types.

**Practical Example - Data Processing Pipeline**:
```java
public class StreamTypeInferenceDemo {
    static class Person {
        String name;
        int age;
        List<String> hobbies;
        
        Person(String name, int age, List<String> hobbies) {
            this.name = name;
            this.age = age;
            this.hobbies = hobbies;
        }
    }
    
    public static void main(String[] args) {
        List<Person> people = Arrays.asList(
            new Person("Alice", 25, Arrays.asList("reading", "gaming")),
            new Person("Bob", 30, Arrays.asList("cooking", "hiking", "photography")),
            new Person("Charlie", 35, Arrays.asList("music"))
        );
        
        // Complex type inference chain
        Map<String, Long> hobbyFrequency = people.stream()
            .filter(p -> p.age >= 30)                    // p inferred as Person
            .flatMap(p -> p.hobbies.stream())            // p inferred as Person, returns Stream<String>
            .map(hobby -> hobby.toLowerCase())           // hobby inferred as String
            .collect(Collectors.groupingBy(
                h -> h,                                  // h inferred as String
                Collectors.counting()                    // Returns Long
            ));
        
        System.out.println(hobbyFrequency);
        // Output: {cooking=1, hiking=1, photography=1, music=1}
    }
}
```

#### Generic Type Inference

Lambda expressions can work with generic types, and the compiler infers type parameters:

```java
// Generic functional interface
@FunctionalInterface
interface Transformer<T, R> {
    R transform(T input);
}

// Method using generic functional interface
public static <T, R> List<R> transformList(List<T> list, Transformer<T, R> transformer) {
    return list.stream()
        .map(transformer::transform)
        .collect(Collectors.toList());
}

// Usage - types are inferred
List<String> strings = Arrays.asList("1", "2", "3");
List<Integer> integers = transformList(strings, s -> Integer.parseInt(s));

// Even more complex inference
Map<String, Integer> nameToAge = new HashMap<>();
nameToAge.put("Alice", 30);
nameToAge.put("Bob", 25);

// Compiler infers all generic types
Map<Integer, List<String>> ageToNames = nameToAge.entrySet().stream()
    .collect(Collectors.groupingBy(
        entry -> entry.getValue(),    // Key mapper - age
        Collectors.mapping(
            entry -> entry.getKey(),  // Value mapper - name
            Collectors.toList()
        )
    ));
```

#### Type Inference Limitations

While powerful, type inference has limitations that developers should understand:

```java
// Cannot infer type without context
// This won't compile:
// var lambda = x -> x * 2;  // Error: cannot infer type

// Must provide context:
Function<Integer, Integer> doubler = x -> x * 2;  // OK

// Or use explicit type:
var explicitDoubler = (Function<Integer, Integer>) x -> x * 2;  // OK

// Ambiguous overloaded methods
public void process(Consumer<String> consumer) { }
public void process(Function<String, String> function) { }

// This causes ambiguity:
// process(s -> s.toUpperCase());  // Error: ambiguous

// Must disambiguate:
process((Consumer<String>) s -> System.out.println(s));
process((Function<String, String>) s -> s.toUpperCase());
```

#### Diamond Operator and Type Inference

The diamond operator (<>) works with lambda expressions for cleaner code:

```java
// Without diamond operator
Map<String, Function<Integer, Integer>> operations1 = 
    new HashMap<String, Function<Integer, Integer>>();

// With diamond operator
Map<String, Function<Integer, Integer>> operations2 = new HashMap<>();

// Adding lambdas with inferred types
operations2.put("double", x -> x * 2);
operations2.put("square", x -> x * x);
operations2.put("negate", x -> -x);

// Using the operations
int result = operations2.get("square").apply(5); // 25
```

### 1.4 Built-In Functional Interfaces

Java provides a comprehensive set of pre-defined functional interfaces in the `java.util.function` package. These interfaces cover virtually all common use cases and are organized into several categories: core interfaces, primitive specializations, bi-argument variations, and operators.

#### Core Functional Interfaces

##### Predicate<T>

The Predicate interface represents a boolean-valued function of one argument. It's commonly used for filtering or matching operations.

```java
import java.util.function.Predicate;

Predicate<String> startsWithA = s -> s.startsWith("A");
Predicate<Integer> isEven = n -> n % 2 == 0;

// Using predicates
System.out.println(startsWithA.test("Apple"));  // true
System.out.println(startsWithA.test("Banana")); // false

// Combining predicates
Predicate<Integer> isPositive = n -> n > 0;
Predicate<Integer> isPositiveAndEven = isPositive.and(isEven);
Predicate<Integer> isNegativeOrOdd = isPositive.negate().or(isEven.negate());

// Static methods
Predicate<String> isNull = Predicate.isEqual(null);
Predicate<String> isEmpty = Predicate.isEqual("");
Predicate<String> notEmpty = isEmpty.negate();
```

**Explanation**: Predicate<T> is one of the most frequently used functional interfaces. Its `test()` method returns a boolean, making it perfect for filtering operations. The interface provides default methods `and()`, `or()`, and `negate()` for combining predicates, enabling complex boolean logic without writing verbose code.

**Practical Usage Example - Data Validation**:
```java
import java.util.*;
import java.util.function.Predicate;

public class PredicateValidationExample {
    static class User {
        String email;
        int age;
        String username;
        
        User(String email, int age, String username) {
            this.email = email;
            this.age = age;
            this.username = username;
        }
    }
    
    public static void main(String[] args) {
        // Define validation rules
        Predicate<User> hasValidEmail = user -> 
            user.email != null && user.email.contains("@");
        Predicate<User> isAdult = user -> user.age >= 18;
        Predicate<User> hasValidUsername = user -> 
            user.username != null && user.username.length() >= 3;
        
        // Combine all validations
        Predicate<User> isValidUser = hasValidEmail
            .and(isAdult)
            .and(hasValidUsername);
        
        List<User> users = Arrays.asList(
            new User("john@email.com", 25, "john123"),
            new User("invalid-email", 20, "ab"),
            new User("jane@email.com", 16, "jane456"),
            new User("bob@email.com", 30, "bob")
        );
        
        // Filter valid users
        List<User> validUsers = users.stream()
            .filter(isValidUser)
            .toList();
        
        System.out.println("Valid users count: " + validUsers.size()); // Output: 1
        
        // Find specific issues
        users.stream()
            .filter(isValidUser.negate())
            .forEach(user -> {
                if (!hasValidEmail.test(user)) 
                    System.out.println(user.username + " has invalid email");
                if (!isAdult.test(user)) 
                    System.out.println(user.username + " is underage");
                if (!hasValidUsername.test(user)) 
                    System.out.println(user.username + " has invalid username");
            });
    }
}
```

##### Consumer<T>

The Consumer interface represents an operation that accepts a single input argument and returns no result. It's typically used for side effects.

```java
import java.util.function.Consumer;

Consumer<String> printer = s -> System.out.println("Processing: " + s);
Consumer<String> logger = s -> System.err.println("[LOG] " + s);

// Chaining consumers
Consumer<String> combinedConsumer = printer.andThen(logger);

// Null-safe consumer
Consumer<String> safePrinter = s -> {
    if (s != null) {
        System.out.println(s);
    }
};
```

**Explanation**: Consumer<T> is used when you need to perform an action on an object without returning a result. Common use cases include printing, logging, updating state, or sending data. The `andThen()` method allows chaining multiple consumers to execute in sequence.

**Practical Usage Example - Event Processing**:
```java
import java.util.*;
import java.util.function.Consumer;
import java.time.LocalDateTime;

public class ConsumerEventExample {
    static class Event {
        String type;
        String data;
        LocalDateTime timestamp;
        
        Event(String type, String data) {
            this.type = type;
            this.data = data;
            this.timestamp = LocalDateTime.now();
        }
    }
    
    public static void main(String[] args) {
        // Define different event handlers
        Consumer<Event> logEvent = event -> 
            System.out.println(String.format("[%s] %s: %s", 
                event.timestamp, event.type, event.data));
        
        Consumer<Event> saveToDatabase = event -> {
            // Simulate database save
            System.out.println("Saving to DB: " + event.type);
        };
        
        Consumer<Event> sendNotification = event -> {
            if ("ERROR".equals(event.type)) {
                System.out.println("ALERT! Error occurred: " + event.data);
            }
        };
        
        // Create composite event processor
        Consumer<Event> processEvent = logEvent
            .andThen(saveToDatabase)
            .andThen(sendNotification);
        
        // Process different events
        List<Event> events = Arrays.asList(
            new Event("INFO", "Application started"),
            new Event("ERROR", "Connection failed"),
            new Event("WARNING", "Memory usage high")
        );
        
        events.forEach(processEvent);
        
        // Conditional processing
        Map<String, Consumer<Event>> eventHandlers = new HashMap<>();
        eventHandlers.put("ERROR", event -> System.err.println("Error handler: " + event.data));
        eventHandlers.put("INFO", event -> System.out.println("Info handler: " + event.data));
        
        events.forEach(event -> 
            eventHandlers.getOrDefault(event.type, e -> {}).accept(event)
        );
    }
}
```

##### Function<T, R>

The Function interface represents a function that accepts one argument and produces a result.

```java
import java.util.function.Function;

Function<String, Integer> stringLength = s -> s.length();
Function<Integer, String> intToString = i -> "Number: " + i;

// Function composition
Function<String, String> lengthDescription = 
    stringLength.andThen(intToString);

// compose() vs andThen()
Function<Integer, Integer> multiplyBy2 = x -> x * 2;
Function<Integer, Integer> add10 = x -> x + 10;

// andThen: first multiplyBy2, then add10
Function<Integer, Integer> multiplyThenAdd = multiplyBy2.andThen(add10);
System.out.println(multiplyThenAdd.apply(5)); // (5 * 2) + 10 = 20

// compose: first add10, then multiplyBy2
Function<Integer, Integer> addThenMultiply = multiplyBy2.compose(add10);
System.out.println(addThenMultiply.apply(5)); // (5 + 10) * 2 = 30

// Identity function
Function<String, String> identity = Function.identity();
```

##### Supplier<T>

The Supplier interface represents a supplier of results without any input.

```java
import java.util.function.Supplier;
import java.time.LocalDateTime;
import java.util.Random;

Supplier<String> timeSupplier = () -> "Current time: " + LocalDateTime.now();
Supplier<Double> randomSupplier = Math::random;

// Lazy initialization pattern
class ExpensiveObject {
    private static Supplier<ExpensiveObject> lazyInstance = () -> {
        ExpensiveObject obj = new ExpensiveObject();
        lazyInstance = () -> obj; // Memoize for future calls
        return obj;
    };
    
    public static ExpensiveObject getInstance() {
        return lazyInstance.get();
    }
}

// Supplier with state
Supplier<Integer> counter = new Supplier<>() {
    private int count = 0;
    @Override
    public Integer get() {
        return ++count;
    }
};
```

##### UnaryOperator<T>

The UnaryOperator interface represents an operation on a single operand that produces a result of the same type.

```java
import java.util.function.UnaryOperator;

UnaryOperator<String> toUpperCase = String::toUpperCase;
UnaryOperator<Integer> square = n -> n * n;

// Chaining operators
UnaryOperator<Integer> increment = n -> n + 1;
UnaryOperator<Integer> doubleIt = n -> n * 2;
UnaryOperator<Integer> incrementThenDouble = increment.andThen(doubleIt);

// Identity operator
UnaryOperator<String> noOp = UnaryOperator.identity();
```

#### Bi-Argument Functional Interfaces

##### BiPredicate<T, U>

BiPredicate represents a predicate (boolean-valued function) of two arguments.

```java
import java.util.function.BiPredicate;

BiPredicate<String, Integer> hasLength = (str, len) -> str.length() == len;
BiPredicate<Integer, Integer> isGreater = (a, b) -> a > b;

// Combining bi-predicates
BiPredicate<String, String> sameLength = 
    (s1, s2) -> s1.length() == s2.length();
BiPredicate<String, String> bothNonEmpty = 
    (s1, s2) -> !s1.isEmpty() && !s2.isEmpty();

BiPredicate<String, String> sameLengthAndNonEmpty = 
    sameLength.and(bothNonEmpty);

// Negation
BiPredicate<Integer, Integer> notEqual = isGreater.or(isGreater.negate());
```

##### BiConsumer<T, U>

BiConsumer represents an operation that accepts two input arguments and returns no result.

```java
import java.util.function.BiConsumer;
import java.util.Map;
import java.util.HashMap;

BiConsumer<String, Integer> printKeyValue = 
    (key, value) -> System.out.println(key + " = " + value);

Map<String, Integer> map = new HashMap<>();
BiConsumer<String, Integer> putInMap = map::put;

// Chaining bi-consumers
BiConsumer<String, Integer> processAndStore = 
    printKeyValue.andThen(putInMap);

// Processing map entries
map.put("A", 1);
map.put("B", 2);
map.forEach(printKeyValue);
```

##### BiFunction<T, U, R>

BiFunction represents a function that accepts two arguments and produces a result.

```java
import java.util.function.BiFunction;

BiFunction<String, String, String> concat = (s1, s2) -> s1 + s2;
BiFunction<Integer, Integer, Integer> multiply = (a, b) -> a * b;

// Complex calculations
BiFunction<Double, Double, Double> pythagoras = 
    (a, b) -> Math.sqrt(a * a + b * b);

// Using andThen with BiFunction
BiFunction<String, Integer, String> repeat = String::repeat;
Function<String, String> exclaim = s -> s + "!";
BiFunction<String, Integer, String> repeatAndExclaim = 
    repeat.andThen(exclaim);

System.out.println(repeatAndExclaim.apply("Hi", 3)); // "HiHiHi!"
```

##### BinaryOperator<T>

BinaryOperator represents an operation on two operands of the same type, producing a result of the same type.

```java
import java.util.function.BinaryOperator;

BinaryOperator<Integer> add = (a, b) -> a + b;
BinaryOperator<Integer> max = Integer::max;
BinaryOperator<String> concat = String::concat;

// Static utility methods
BinaryOperator<Integer> minBy = BinaryOperator.minBy(Integer::compare);
BinaryOperator<String> maxByLength = 
    BinaryOperator.maxBy((s1, s2) -> s1.length() - s2.length());

// Using in reduce operations
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
Integer sum = numbers.stream().reduce(0, add);
Integer maximum = numbers.stream().reduce(Integer.MIN_VALUE, max);
```

#### Primitive Specializations

Java provides primitive specializations to avoid boxing/unboxing overhead.

##### Int Specializations

```java
import java.util.function.*;

// IntPredicate
IntPredicate isEven = n -> n % 2 == 0;
IntPredicate isPositive = n -> n > 0;
IntPredicate isEvenAndPositive = isEven.and(isPositive);

// IntConsumer
IntConsumer printInt = System.out::println;
IntConsumer saveToArray = new IntConsumer() {
    private int[] array = new int[10];
    private int index = 0;
    
    @Override
    public void accept(int value) {
        if (index < array.length) {
            array[index++] = value;
        }
    }
};

// IntFunction<R>
IntFunction<String> intToString = String::valueOf;
IntFunction<Double> intToDouble = i -> i / 2.0;

// IntSupplier
IntSupplier randomInt = () -> ThreadLocalRandom.current().nextInt();
IntSupplier constantZero = () -> 0;

// IntUnaryOperator
IntUnaryOperator negate = n -> -n;
IntUnaryOperator abs = Math::abs;
IntUnaryOperator doubleValue = n -> n * 2;

// IntBinaryOperator
IntBinaryOperator sum = Integer::sum;
IntBinaryOperator difference = (a, b) -> a - b;
IntBinaryOperator multiply = (a, b) -> a * b;

// IntToDoubleFunction
IntToDoubleFunction sqrt = n -> Math.sqrt(n);
IntToDoubleFunction half = n -> n / 2.0;

// IntToLongFunction
IntToLongFunction toLong = n -> (long) n;
IntToLongFunction square = n -> (long) n * n;
```

##### Long Specializations

```java
// LongPredicate
LongPredicate isLarge = n -> n > 1_000_000_000L;
LongPredicate isNegative = n -> n < 0;

// LongConsumer
LongConsumer printLong = System.out::println;
LongConsumer accumulator = new LongConsumer() {
    private long sum = 0;
    
    @Override
    public void accept(long value) {
        sum += value;
        System.out.println("Running sum: " + sum);
    }
};

// LongFunction<R>
LongFunction<String> longToHex = Long::toHexString;
LongFunction<Date> epochToDate = Date::new;

// LongSupplier
LongSupplier currentTime = System::currentTimeMillis;
LongSupplier nanoTime = System::nanoTime;

// LongUnaryOperator
LongUnaryOperator increment = n -> n + 1;
LongUnaryOperator doubleIt = n -> n * 2;

// LongBinaryOperator
LongBinaryOperator gcd = (a, b) -> {
    while (b != 0) {
        long temp = b;
        b = a % b;
        a = temp;
    }
    return a;
};

// LongToIntFunction
LongToIntFunction hashCode = n -> Long.hashCode(n);

// LongToDoubleFunction
LongToDoubleFunction normalize = n -> n / 1_000_000_000.0;
```

##### Double Specializations

```java
// DoublePredicate
DoublePredicate isNaN = Double::isNaN;
DoublePredicate isFinite = Double::isFinite;
DoublePredicate isInRange = d -> d >= 0.0 && d <= 1.0;

// DoubleConsumer
DoubleConsumer printDouble = System.out::println;
DoubleConsumer statistics = new DoubleConsumer() {
    private double sum = 0;
    private int count = 0;
    
    @Override
    public void accept(double value) {
        sum += value;
        count++;
        System.out.printf("Avg: %.2f%n", sum / count);
    }
};

// DoubleFunction<R>
DoubleFunction<String> format = d -> String.format("%.2f", d);
DoubleFunction<Long> round = d -> Math.round(d);

// DoubleSupplier
DoubleSupplier random = Math::random;
DoubleSupplier pi = () -> Math.PI;

// DoubleUnaryOperator
DoubleUnaryOperator sqrt = Math::sqrt;
DoubleUnaryOperator reciprocal = d -> 1.0 / d;
DoubleUnaryOperator toDegrees = Math::toDegrees;

// DoubleBinaryOperator
DoubleBinaryOperator pow = Math::pow;
DoubleBinaryOperator hypot = Math::hypot;
DoubleBinaryOperator average = (a, b) -> (a + b) / 2.0;

// DoubleToIntFunction
DoubleToIntFunction floor = d -> (int) Math.floor(d);
DoubleToIntFunction ceil = d -> (int) Math.ceil(d);

// DoubleToLongFunction
DoubleToLongFunction roundToLong = Math::round;
```

### 1.5 Method References

Method references provide a more concise alternative to lambda expressions when the lambda simply calls an existing method. They use the :: operator and come in four varieties.

#### Static Method References

Reference to a static method of a class:
```java
// Lambda expression
Function<String, Integer> parser1 = s -> Integer.parseInt(s);

// Method reference
Function<String, Integer> parser2 = Integer::parseInt;

System.out.println(parser2.apply("123")); // 123
```

#### Instance Method References on a Type

Reference to an instance method of an arbitrary object of a particular type:
```java
// Lambda expression
Function<String, String> toUpper1 = s -> s.toUpperCase();

// Method reference
Function<String, String> toUpper2 = String::toUpperCase;

List<String> words = Arrays.asList("hello", "world");
words.stream()
     .map(String::toUpperCase)
     .forEach(System.out::println); // HELLO WORLD
```

#### Instance Method References on an Existing Object

Reference to an instance method of a specific object:
```java
String prefix = "Result: ";
Function<String, String> prefixer = prefix::concat;

System.out.println(prefixer.apply("Success")); // "Result: Success"
```

#### Constructor References

Reference to a constructor:
```java
// Lambda expression
Supplier<List<String>> listSupplier1 = () -> new ArrayList<>();

// Constructor reference
Supplier<List<String>> listSupplier2 = ArrayList::new;

Function<Integer, List<String>> listCreator = ArrayList::new;
List<String> myList = listCreator.apply(10); // Creates ArrayList with initial capacity 10
```

### 1.6 Variable Capture and Effectively Final

Lambda expressions can capture variables from their enclosing scope, but these variables must be final or effectively final (not modified after initialization).

```java
public void demonstrateCapture() {
    int multiplier = 3; // effectively final
    
    // This lambda captures the multiplier variable
    Function<Integer, Integer> multiply = x -> x * multiplier;
    
    System.out.println(multiply.apply(5)); // 15
    
    // This would cause a compilation error:
    // multiplier = 4; // Error: Variable used in lambda should be final or effectively final
}
```

### 1.7 Functional Interface Inheritance and Composition

Functional interfaces can be extended and composed to create more specialized or complex behaviors. This section explores how functional interfaces interact with Java's inheritance model and how they can be composed to create powerful abstractions.

#### Extending Functional Interfaces

Functional interfaces can extend other interfaces, including other functional interfaces, as long as they maintain the single abstract method constraint:

```java
// Base functional interface
@FunctionalInterface
interface Validator<T> {
    boolean validate(T value);
}

// Extended functional interface with additional default methods
@FunctionalInterface
interface StringValidator extends Validator<String> {
    
    // Inherits validate(String value) as the single abstract method
    
    default StringValidator and(StringValidator other) {
        return value -> this.validate(value) && other.validate(value);
    }
    
    default StringValidator or(StringValidator other) {
        return value -> this.validate(value) || other.validate(value);
    }
    
    default StringValidator negate() {
        return value -> !this.validate(value);
    }
}

// Usage example
public class ValidationExample {
    public static void main(String[] args) {
        StringValidator notEmpty = str -> str != null && !str.isEmpty();
        StringValidator maxLength10 = str -> str.length() <= 10;
        StringValidator containsDigit = str -> str.matches(".*\\d.*");
        
        // Composing validators
        StringValidator complexValidator = notEmpty
            .and(maxLength10)
            .and(containsDigit.negate());
        
        System.out.println(complexValidator.validate("Hello"));    // true
        System.out.println(complexValidator.validate("Hello123")); // false (contains digit)
        System.out.println(complexValidator.validate(""));         // false (empty)
    }
}
```

#### Interface Inheritance with Generic Bounds

Functional interfaces can use generic bounds to create type-safe hierarchies:

```java
// Base transformer interface
@FunctionalInterface
interface Transformer<T, R> {
    R transform(T input);
}

// Specialized number transformer
@FunctionalInterface
interface NumberTransformer<T extends Number, R extends Number> 
    extends Transformer<T, R> {
    
    // Additional default methods for number operations
    default NumberTransformer<T, R> withScaling(double factor) {
        return input -> {
            R result = this.transform(input);
            if (result instanceof Double) {
                return (R) Double.valueOf(result.doubleValue() * factor);
            }
            return result;
        };
    }
}

// Even more specialized integer transformer
@FunctionalInterface
interface IntegerTransformer extends NumberTransformer<Integer, Integer> {
    
    default IntegerTransformer compose(IntegerTransformer before) {
        return input -> this.transform(before.transform(input));
    }
}
```

#### Composition Patterns

Functional interfaces can be composed in various ways to create complex behaviors from simple building blocks:

```java
// Function composition utilities
public class FunctionComposition {
    
    // Generic composition for any functional interface
    public static <T> Predicate<T> compose(
            Predicate<T> first, 
            Predicate<T> second, 
            BinaryOperator<Boolean> combiner) {
        return t -> combiner.apply(first.test(t), second.test(t));
    }
    
    // Chaining multiple functions
    public static <T, R> Function<T, R> chain(
            Function<T, R> first,
            UnaryOperator<R>... operators) {
        Function<T, R> result = first;
        for (UnaryOperator<R> op : operators) {
            result = result.andThen(op);
        }
        return result;
    }
    
    // Example usage
    public static void main(String[] args) {
        // Predicate composition
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isPositive = n -> n > 0;
        
        Predicate<Integer> isEvenAndPositive = compose(
            isEven, isPositive, (a, b) -> a && b
        );
        
        System.out.println(isEvenAndPositive.test(4));  // true
        System.out.println(isEvenAndPositive.test(-2)); // false
        
        // Function chaining
        Function<String, Integer> stringToLength = String::length;
        UnaryOperator<Integer> doubleIt = n -> n * 2;
        UnaryOperator<Integer> addTen = n -> n + 10;
        
        Function<String, Integer> complexFunction = chain(
            stringToLength, doubleIt, addTen
        );
        
        System.out.println(complexFunction.apply("Hello")); // 20 (5 * 2 + 10)
    }
}
```

#### Builder Pattern with Functional Interfaces

Functional interfaces can be used to create fluent builder patterns:

```java
@FunctionalInterface
interface ConfigurationStep<T> {
    T configure(T target);
}

public class ServerBuilder {
    private String host = "localhost";
    private int port = 8080;
    private boolean secure = false;
    
    public ServerBuilder with(ConfigurationStep<ServerBuilder> step) {
        return step.configure(this);
    }
    
    // Builder methods
    public ServerBuilder host(String host) {
        this.host = host;
        return this;
    }
    
    public ServerBuilder port(int port) {
        this.port = port;
        return this;
    }
    
    public ServerBuilder secure(boolean secure) {
        this.secure = secure;
        return this;
    }
    
    public Server build() {
        return new Server(host, port, secure);
    }
}

class Server {
    private final String host;
    private final int port;
    private final boolean secure;
    
    public Server(String host, int port, boolean secure) {
        this.host = host;
        this.port = port;
        this.secure = secure;
    }
}

// Usage example for ServerBuilder
class ServerBuilderExample {
    public static void main(String[] args) {
        Server server = new ServerBuilder()
            .with(builder -> builder.host("example.com"))
            .with(builder -> builder.port(443))
            .with(builder -> builder.secure(true))
            .build();
            
        // Or with predefined configurations
        ConfigurationStep<ServerBuilder> productionConfig = builder ->
            builder.host("prod.example.com")
                   .port(443)
                   .secure(true);
                   
        Server prodServer = new ServerBuilder()
            .with(productionConfig)
            .build();
    }
}
```

#### Decorator Pattern with Functional Interfaces

Functional interfaces enable elegant implementations of the decorator pattern:

```java
@FunctionalInterface
interface RequestHandler {
    String handle(String request);
    
    // Decorator method
    default RequestHandler decorate(
            Function<String, String> preprocessor,
            Function<String, String> postprocessor) {
        return request -> {
            String preprocessed = preprocessor.apply(request);
            String result = this.handle(preprocessed);
            return postprocessor.apply(result);
        };
    }
    
    // Convenience decorators
    default RequestHandler withLogging() {
        return request -> {
            System.out.println("Handling request: " + request);
            String result = this.handle(request);
            System.out.println("Response: " + result);
            return result;
        };
    }
    
    default RequestHandler withTiming() {
        return request -> {
            long start = System.currentTimeMillis();
            String result = this.handle(request);
            long duration = System.currentTimeMillis() - start;
            System.out.println("Request processed in " + duration + "ms");
            return result;
        };
    }
}

// Usage
public class HandlerExample {
    public static void main(String[] args) {
        RequestHandler baseHandler = request -> 
            "Processed: " + request.toUpperCase();
        
        RequestHandler decoratedHandler = baseHandler
            .decorate(
                req -> req.trim(),           // preprocessor
                res -> "[" + res + "]"       // postprocessor
            )
            .withLogging()
            .withTiming();
        
        decoratedHandler.handle("  hello world  ");
        // Output:
        // Handling request:   hello world  
        // Response: [Processed: HELLO WORLD]
        // Request processed in Xms
    }
}
```

#### Combining Multiple Functional Interfaces

Complex behaviors can be created by combining multiple functional interfaces:

```java
public class MultiInterfaceComposition {
    
    // Combining Predicate and Function
    public static <T, R> Function<T, Optional<R>> conditionalTransform(
            Predicate<T> condition,
            Function<T, R> transformer) {
        return input -> condition.test(input) 
            ? Optional.of(transformer.apply(input))
            : Optional.empty();
    }
    
    // Combining Consumer and Supplier
    public static <T> void processWithFallback(
            Supplier<Optional<T>> supplier,
            Consumer<T> processor,
            Runnable fallback) {
        supplier.get().ifPresentOrElse(processor, fallback);
    }
    
    // Complex composition example
    public static void main(String[] args) {
        // Conditional transformation
        Function<String, Optional<Integer>> parsePositiveInt = 
            conditionalTransform(
                str -> str.matches("\\d+"),
                Integer::parseInt
            );
        
        System.out.println(parsePositiveInt.apply("123"));  // Optional[123]
        System.out.println(parsePositiveInt.apply("abc"));  // Optional.empty
        
        // Process with fallback
        processWithFallback(
            () -> Optional.ofNullable(System.getenv("CONFIG_PATH")),
            path -> System.out.println("Loading config from: " + path),
            () -> System.out.println("Using default configuration")
        );
    }
}
```

#### Type-Safe Event Handling with Functional Interfaces

Functional interfaces can create type-safe event handling systems:

```java
// Generic event handler
@FunctionalInterface
interface EventHandler<T> {
    void handle(T event);
}

// Specialized event handlers
@FunctionalInterface
interface MouseEventHandler extends EventHandler<MouseEvent> {
    default MouseEventHandler andThen(MouseEventHandler after) {
        return event -> {
            this.handle(event);
            after.handle(event);
        };
    }
}

// Event dispatcher
public class EventDispatcher<T> {
    private final List<EventHandler<T>> handlers = new ArrayList<>();
    
    public void register(EventHandler<T> handler) {
        handlers.add(handler);
    }
    
    public void dispatch(T event) {
        handlers.forEach(handler -> handler.handle(event));
    }
    
    // Conditional registration
    public void registerIf(Predicate<T> condition, EventHandler<T> handler) {
        register(event -> {
            if (condition.test(event)) {
                handler.handle(event);
            }
        });
    }
}

// Usage
class MouseEvent {
    final int x, y;
    final String type;
    
    public MouseEvent(int x, int y, String type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public String getType() { return type; }
}

public class EventExample {
    public static void main(String[] args) {
        EventDispatcher<MouseEvent> dispatcher = new EventDispatcher<>();
        
        // Register handlers
        dispatcher.register(event -> 
            System.out.println("Mouse event at: " + event.x + ", " + event.y));
            
        dispatcher.registerIf(
            event -> "click".equals(event.getType()),
            event -> System.out.println("Click detected!")
        );
        
        // Dispatch events
        dispatcher.dispatch(new MouseEvent(100, 200, "move"));
        dispatcher.dispatch(new MouseEvent(150, 250, "click"));
    }
}
```

#### Custom Functional Interface Design Patterns

Here are key patterns for designing effective custom functional interfaces:

##### 1. The Fluent Builder Pattern

**Explanation**: This pattern combines functional interfaces with the builder pattern to create a flexible, type-safe configuration API. Each build step is a function that transforms the target object. This approach allows for dynamic configuration steps and better composition compared to traditional builders.

```java
@FunctionalInterface
interface BuildStep<T, R> {
    R apply(T builder);
    
    default <S> BuildStep<T, S> andThen(Function<R, S> next) {
        return builder -> next.apply(this.apply(builder));
    }
}

public class FluentBuilder<T> {
    private final T target;
    
    public FluentBuilder(T target) {
        this.target = target;
    }
    
    public FluentBuilder<T> with(BuildStep<T, T> step) {
        return new FluentBuilder<>(step.apply(target));
    }
    
    public T build() {
        return target;
    }
}

// Usage
class Configuration {
    private String host;
    private int port;
    private boolean secure;
    
    // Setters return this for chaining
    public Configuration setHost(String host) {
        this.host = host;
        return this;
    }
    
    public Configuration setPort(int port) {
        this.port = port;
        return this;
    }
    
    public Configuration setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }
}

Configuration config = new FluentBuilder<>(new Configuration())
    .with(c -> c.setHost("localhost"))
    .with(c -> c.setPort(8080))
    .with(c -> c.setSecure(true))
    .build();
```

**Extended Usage Example - Dynamic Configuration with Validation**:
```java
public class AdvancedFluentBuilderExample {
    // Predefined configuration steps
    static final BuildStep<Configuration, Configuration> DEVELOPMENT = 
        c -> c.setHost("localhost").setPort(8080).setSecure(false);
    
    static final BuildStep<Configuration, Configuration> PRODUCTION = 
        c -> c.setHost("prod.example.com").setPort(443).setSecure(true);
    
    // Validation step
    static final BuildStep<Configuration, Configuration> VALIDATE = c -> {
        if (c.secure && c.port != 443) {
            throw new IllegalStateException("Secure connections must use port 443");
        }
        return c;
    };
    
    public static void main(String[] args) {
        // Use predefined configurations
        Configuration devConfig = new FluentBuilder<>(new Configuration())
            .with(DEVELOPMENT)
            .build();
        
        // Compose configurations dynamically
        Configuration customConfig = new FluentBuilder<>(new Configuration())
            .with(PRODUCTION)
            .with(c -> c.setHost("custom.example.com")) // Override host
            .with(VALIDATE)
            .build();
        
        // Conditional configuration
        boolean isDebug = true;
        Configuration conditionalConfig = new FluentBuilder<>(new Configuration())
            .with(isDebug ? DEVELOPMENT : PRODUCTION)
            .with(c -> isDebug ? c.setPort(8081) : c)
            .build();
    }
}
```

##### 2. The Strategy Pattern with Type Safety

```java
// Type-safe strategy pattern
interface ValidationStrategy<T> {
    ValidationResult validate(T input);
    
    default ValidationStrategy<T> and(ValidationStrategy<T> other) {
        return input -> {
            ValidationResult result1 = this.validate(input);
            if (!result1.isValid()) return result1;
            return other.validate(input);
        };
    }
    
    default ValidationStrategy<T> or(ValidationStrategy<T> other) {
        return input -> {
            ValidationResult result1 = this.validate(input);
            if (result1.isValid()) return result1;
            return other.validate(input);
        };
    }
}

class ValidationResult {
    private final boolean valid;
    private final String message;
    
    private ValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }
    
    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }
    
    public static ValidationResult invalid(String message) {
        return new ValidationResult(false, message);
    }
    
    public boolean isValid() { return valid; }
    public String getMessage() { return message; }
}

// Type-specific strategies
class StringValidations {
    public static ValidationStrategy<String> minLength(int min) {
        return str -> str.length() >= min 
            ? ValidationResult.valid() 
            : ValidationResult.invalid("String too short");
    }
    
    public static ValidationStrategy<String> maxLength(int max) {
        return str -> str.length() <= max 
            ? ValidationResult.valid() 
            : ValidationResult.invalid("String too long");
    }
    
    public static ValidationStrategy<String> pattern(String regex) {
        return str -> str.matches(regex) 
            ? ValidationResult.valid() 
            : ValidationResult.invalid("Pattern mismatch");
    }
}
```

##### 3. The Pipeline Pattern

**Explanation**: The Pipeline pattern enables composing a series of transformations into a single, reusable processing pipeline. This pattern is particularly useful for data processing workflows where you need to apply multiple transformations in sequence. The SafePipeline variant adds error handling capabilities, allowing pipelines to gracefully handle failures.

```java
@FunctionalInterface
interface Pipeline<T> {
    T execute(T input);
    
    default Pipeline<T> pipe(Pipeline<T> next) {
        return input -> next.execute(this.execute(input));
    }
    
    static <T> Pipeline<T> of(UnaryOperator<T> operator) {
        return operator::apply;
    }
}

// Specialized pipelines with error handling
@FunctionalInterface
interface SafePipeline<T> {
    Result<T> execute(T input);
    
    default SafePipeline<T> pipe(SafePipeline<T> next) {
        return input -> {
            Result<T> result = this.execute(input);
            return result.isSuccess() 
                ? next.execute(result.getValue())
                : result;
        };
    }
    
    default SafePipeline<T> recover(Function<Exception, T> recovery) {
        return input -> {
            Result<T> result = this.execute(input);
            if (result.isFailure()) {
                try {
                    return Result.success(recovery.apply(result.getError()));
                } catch (Exception e) {
                    return Result.failure(e);
                }
            }
            return result;
        };
    }
}

class Result<T> {
    private final T value;
    private final Exception error;
    
    private Result(T value, Exception error) {
        this.value = value;
        this.error = error;
    }
    
    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }
    
    public static <T> Result<T> failure(Exception error) {
        return new Result<>(null, error);
    }
    
    public boolean isSuccess() { return error == null; }
    public boolean isFailure() { return error != null; }
    public T getValue() { return value; }
    public Exception getError() { return error; }
}
```

**Comprehensive Pipeline Usage Example**:
```java
import java.util.function.UnaryOperator;

public class PipelineProcessingExample {
    public static void main(String[] args) {
        // Simple text processing pipeline
        Pipeline<String> textProcessor = Pipeline.of(String::trim)
            .pipe(Pipeline.of(String::toLowerCase))
            .pipe(Pipeline.of(s -> s.replaceAll("\\s+", " ")))
            .pipe(Pipeline.of(s -> s.substring(0, Math.min(s.length(), 50))));
        
        String result = textProcessor.execute("  HELLO   WORLD   FROM    JAVA  ");
        System.out.println(result); // Output: "hello world from java"
        
        // Numeric processing pipeline
        Pipeline<Double> mathPipeline = Pipeline.of((Double d) -> d * 2)
            .pipe(Pipeline.of(d -> d + 10))
            .pipe(Pipeline.of(Math::sqrt));
        
        System.out.println(mathPipeline.execute(3.0)); // sqrt((3*2)+10) = 4.0
        
        // Safe pipeline with error handling
        SafePipeline<String> safeParser = input -> {
            try {
                return Result.success(input.trim());
            } catch (Exception e) {
                return Result.failure(e);
            }
        };
        
        SafePipeline<String> safeUpperCase = input -> {
            try {
                if (input.isEmpty()) {
                    throw new IllegalArgumentException("Empty string");
                }
                return Result.success(input.toUpperCase());
            } catch (Exception e) {
                return Result.failure(e);
            }
        };
        
        SafePipeline<String> safeTextPipeline = safeParser
            .pipe(safeUpperCase)
            .recover(e -> "DEFAULT_VALUE");
        
        Result<String> safeResult1 = safeTextPipeline.execute("hello");
        System.out.println(safeResult1.getValue()); // "HELLO"
        
        Result<String> safeResult2 = safeTextPipeline.execute("  ");
        System.out.println(safeResult2.getValue()); // "DEFAULT_VALUE"
        
        // Complex data transformation pipeline
        class Order {
            String id;
            double amount;
            String status;
            
            Order(String id, double amount, String status) {
                this.id = id;
                this.amount = amount;
                this.status = status;
            }
        }
        
        Pipeline<Order> orderProcessor = Pipeline.<Order>of(order -> {
            // Apply discount
            if (order.amount > 100) {
                order.amount *= 0.9; // 10% discount
            }
            return order;
        })
        .pipe(Pipeline.of(order -> {
            // Add tax
            order.amount *= 1.08; // 8% tax
            return order;
        }))
        .pipe(Pipeline.of(order -> {
            // Update status
            order.status = "PROCESSED";
            return order;
        }));
        
        Order order = new Order("ORD-001", 150.0, "PENDING");
        orderProcessor.execute(order);
        System.out.printf("Order %s: $%.2f - %s%n", 
            order.id, order.amount, order.status);
        // Output: Order ORD-001: $145.80 - PROCESSED
    }
}
```

##### 4. The Memoization Pattern

**Explanation**: Memoization is an optimization technique that caches the results of expensive function calls. When the same inputs occur again, the cached result is returned instead of recomputing. This pattern is particularly useful for recursive algorithms, expensive calculations, or frequently called functions with the same parameters.

```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;
import java.util.function.Function;

@FunctionalInterface
interface MemoizableFunction<T, R> extends Function<T, R> {
    
    default MemoizableFunction<T, R> memoized() {
        Map<T, R> cache = new ConcurrentHashMap<>();
        return input -> cache.computeIfAbsent(input, this);
    }
    
    default MemoizableFunction<T, R> memoizedWithExpiry(Duration duration) {
        Map<T, TimedValue<R>> cache = new ConcurrentHashMap<>();
        return input -> {
            TimedValue<R> cached = cache.get(input);
            if (cached != null && !cached.isExpired(duration)) {
                return cached.getValue();
            }
            R result = this.apply(input);
            cache.put(input, new TimedValue<>(result));
            return result;
        };
    }
    
    class TimedValue<V> {
        private final V value;
        private final long timestamp;
        
        TimedValue(V value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        
        V getValue() { return value; }
        
        boolean isExpired(Duration duration) {
            return System.currentTimeMillis() - timestamp > duration.toMillis();
        }
    }
}

// Usage
MemoizableFunction<Integer, String> expensiveOperation = 
    n -> {
        // Simulate expensive computation
        try { Thread.sleep(1000); } catch (InterruptedException e) { }
        return "Result: " + n;
    };

MemoizableFunction<Integer, String> cached = expensiveOperation.memoized();
```

**Comprehensive Memoization Usage Example**:
```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoizationExample {
    // Track computation count for demonstration
    static AtomicInteger computationCount = new AtomicInteger(0);
    
    // Fibonacci with memoization
    static MemoizableFunction<Integer, Long> fibonacci = 
        MemoizableFunction.of(n -> {
            computationCount.incrementAndGet();
            if (n <= 1) return (long) n;
            // Recursive call using the memoized version
            return fibonacci.apply(n - 1) + fibonacci.apply(n - 2);
        }).memoized();
    
    // Expensive string processing
    static MemoizableFunction<String, String> expensiveStringProcess = 
        MemoizableFunction.of(input -> {
            System.out.println("Processing: " + input);
            // Simulate expensive operation
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            return input.toUpperCase().replaceAll("[AEIOU]", "*");
        });
    
    public static void main(String[] args) {
        // Fibonacci example
        System.out.println("Fibonacci Tests:");
        long start = System.currentTimeMillis();
        System.out.println("fib(40) = " + fibonacci.apply(40));
        System.out.println("Computations: " + computationCount.get());
        System.out.println("Time: " + (System.currentTimeMillis() - start) + "ms");
        
        // Second call is instant due to memoization
        computationCount.set(0);
        start = System.currentTimeMillis();
        System.out.println("fib(40) again = " + fibonacci.apply(40));
        System.out.println("Computations: " + computationCount.get());
        System.out.println("Time: " + (System.currentTimeMillis() - start) + "ms\n");
        
        // String processing with expiry
        System.out.println("String Processing with Expiry:");
        MemoizableFunction<String, String> cachedProcessor = 
            expensiveStringProcess.memoizedWithExpiry(Duration.ofSeconds(3));
        
        // First call - computes
        System.out.println(cachedProcessor.apply("Hello World"));
        
        // Second call within 3 seconds - uses cache
        System.out.println(cachedProcessor.apply("Hello World"));
        
        // Wait for cache to expire
        try { Thread.sleep(3500); } catch (InterruptedException e) {}
        
        // Third call after expiry - recomputes
        System.out.println(cachedProcessor.apply("Hello World"));
        
        // Memoization for API calls
        MemoizableFunction<String, String> apiCall = 
            MemoizableFunction.of(endpoint -> {
                System.out.println("Calling API: " + endpoint);
                // Simulate API call
                return "Response from " + endpoint;
            }).memoizedWithExpiry(Duration.ofMinutes(5));
        
        // Multiple calls to same endpoint use cache
        for (int i = 0; i < 3; i++) {
            System.out.println(apiCall.apply("/api/users"));
        }
    }
}
```

##### 5. The Retry Pattern

**Explanation**: The Retry pattern provides automatic retry logic for operations that may fail temporarily (like network calls, database operations, or external service interactions). It includes configurable retry attempts, delays between retries, and support for both synchronous and asynchronous execution. This pattern is essential for building resilient applications.

```java
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@FunctionalInterface
interface RetryableOperation<T> {
    T execute() throws Exception;
    
    default T executeWithRetry(int maxAttempts, Duration delay) throws Exception {
        Exception lastException = null;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                return execute();
            } catch (Exception e) {
                lastException = e;
                if (i < maxAttempts - 1) {
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                }
            }
        }
        throw new RuntimeException("Failed after " + maxAttempts + " attempts", lastException);
    }
    
    default CompletableFuture<T> executeAsync(Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
}
```

**Comprehensive Retry Pattern Usage Example**:
```java
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;

public class RetryPatternExample {
    static Random random = new Random();
    static AtomicInteger attemptCounter = new AtomicInteger(0);
    
    // Simulated unreliable service
    static class UnreliableService {
        static String fetchData() throws Exception {
            int attempt = attemptCounter.incrementAndGet();
            System.out.println("Attempt #" + attempt);
            
            // 70% chance of failure
            if (random.nextDouble() < 0.7) {
                throw new RuntimeException("Service temporarily unavailable");
            }
            return "Success! Data retrieved on attempt #" + attempt;
        }
        
        static String fetchDataWithCondition(String id) throws Exception {
            if (id.isEmpty()) {
                throw new IllegalArgumentException("ID cannot be empty");
            }
            // 50% chance of temporary failure
            if (random.nextDouble() < 0.5) {
                throw new IOException("Network timeout");
            }
            return "Data for ID: " + id;
        }
    }
    
    public static void main(String[] args) throws Exception {
        // Basic retry example
        RetryableOperation<String> fetchOperation = () -> UnreliableService.fetchData();
        
        try {
            attemptCounter.set(0);
            String result = fetchOperation.executeWithRetry(5, Duration.ofSeconds(1));
            System.out.println("Result: " + result);
        } catch (Exception e) {
            System.out.println("Failed after all retries: " + e.getMessage());
        }
        
        // Async retry example
        ExecutorService executor = Executors.newFixedThreadPool(2);
        RetryableOperation<String> asyncOperation = () -> {
            System.out.println("Async attempt on thread: " + 
                Thread.currentThread().getName());
            return UnreliableService.fetchData();
        };
        
        attemptCounter.set(0);
        CompletableFuture<String> future = asyncOperation.executeAsync(executor);
        
        future.thenAccept(result -> System.out.println("Async result: " + result))
              .exceptionally(ex -> {
                  System.out.println("Async failed: " + ex.getMessage());
                  return null;
              });
        
        // Advanced retry with exponential backoff
        RetryableOperation<String> advancedRetry = new RetryableOperation<>() {
            private int attempt = 0;
            
            @Override
            public String execute() throws Exception {
                attempt++;
                // Exponential backoff
                if (attempt > 1) {
                    long delay = (long) Math.pow(2, attempt - 1) * 1000;
                    System.out.println("Waiting " + delay + "ms before retry");
                    Thread.sleep(delay);
                }
                return UnreliableService.fetchDataWithCondition("user123");
            }
        };
        
        // Retry with different strategies for different exceptions
        RetryableOperation<String> smartRetry = () -> {
            try {
                return UnreliableService.fetchDataWithCondition("");
            } catch (IllegalArgumentException e) {
                // Don't retry for invalid arguments
                System.out.println("Invalid argument - not retrying");
                throw e;
            } catch (IOException e) {
                // Retry for IO exceptions
                System.out.println("IO error - will retry");
                throw e;
            }
        };
        
        // Custom retry logic wrapper
        class RetryConfig {
            int maxAttempts = 3;
            Duration initialDelay = Duration.ofMillis(100);
            double backoffMultiplier = 2.0;
            
            <T> T executeWithBackoff(RetryableOperation<T> operation) 
                    throws Exception {
                Exception lastException = null;
                Duration currentDelay = initialDelay;
                
                for (int i = 0; i < maxAttempts; i++) {
                    try {
                        return operation.execute();
                    } catch (Exception e) {
                        lastException = e;
                        if (i < maxAttempts - 1) {
                            System.out.println("Retry " + (i + 1) + 
                                " failed, waiting " + currentDelay.toMillis() + "ms");
                            Thread.sleep(currentDelay.toMillis());
                            currentDelay = Duration.ofMillis(
                                (long)(currentDelay.toMillis() * backoffMultiplier));
                        }
                    }
                }
                throw new RuntimeException("All retries exhausted", lastException);
            }
        }
        
        // Use custom retry configuration
        RetryConfig config = new RetryConfig();
        try {
            String result = config.executeWithBackoff(
                () -> UnreliableService.fetchData());
            System.out.println("Custom retry result: " + result);
        } catch (Exception e) {
            System.out.println("Custom retry failed: " + e.getMessage());
        }
        
        executor.shutdown();
    }
}
```