## Chapter 4: Functional Design Patterns

Monads are a fundamental concept in functional programming that provide a way to structure programs generically. They represent computations defined as sequences of steps, where each step depends on the results of the previous ones.

### The Optional Monad

The Optional monad is Java's built-in implementation for handling nullable values safely. It encapsulates a value that may or may not be present, eliminating null pointer exceptions.

```java
import java.util.Optional;

public class OptionalExample {
    public static void main(String[] args) {
        // Creating Optional instances
        Optional<String> present = Optional.of("Hello");
        Optional<String> empty = Optional.empty();
        
        // Chaining operations with map and flatMap
        String result = present
            .map(String::toUpperCase)
            .flatMap(s -> Optional.of(s + " WORLD"))
            .filter(s -> s.length() > 5)
            .orElse("Default");
        
        System.out.println(result); // HELLO WORLD
        
        // Handling empty values
        String emptyResult = empty
            .map(String::toUpperCase)
            .orElse("Was empty");
        
        System.out.println(emptyResult); // Was empty
    }
}
```

### Custom Result Monad

A Result monad handles operations that might fail, providing a more functional alternative to exceptions.

```java
import java.util.function.Function;

sealed interface Result<T> {
    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(String error) implements Result<T> {}
    
    default <R> Result<R> map(Function<T, R> mapper) {
        return switch (this) {
            case Success<T>(var value) -> new Success<>(mapper.apply(value));
            case Failure<T>(var error) -> new Failure<>(error);
        };
    }
    
    default <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
        return switch (this) {
            case Success<T>(var value) -> mapper.apply(value);
            case Failure<T>(var error) -> new Failure<>(error);
        };
    }
}

public class ResultExample {
    public static Result<Integer> divide(int a, int b) {
        if (b == 0) {
            return new Result.Failure<>("Division by zero");
        }
        return new Result.Success<>(a / b);
    }
    
    public static void main(String[] args) {
        Result<Integer> result = divide(10, 2)
            .flatMap(x -> divide(x, 2))
            .map(x -> x * 10);
        
        switch (result) {
            case Result.Success<Integer>(var value) -> 
                System.out.println("Result: " + value); // Result: 25
            case Result.Failure<Integer>(var error) -> 
                System.out.println("Error: " + error);
        }
    }
}
```

### Try Monad for Exception Handling

The Try monad provides a functional way to handle exceptions, converting throwing operations into value-based error handling.

```java
import java.util.function.Function;

sealed interface Try<T> {
    record Success<T>(T value) implements Try<T> {}
    record Failure<T>(Exception exception) implements Try<T> {}
    
    static <T> Try<T> of(ThrowingSupplier<T> supplier) {
        try {
            return new Success<>(supplier.get());
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }
    
    default <R> Try<R> map(Function<T, R> mapper) {
        return switch (this) {
            case Success<T>(var value) -> Try.of(() -> mapper.apply(value));
            case Failure<T>(var exception) -> new Failure<>(exception);
        };
    }
    
    default <R> Try<R> flatMap(Function<T, Try<R>> mapper) {
        return switch (this) {
            case Success<T>(var value) -> mapper.apply(value);
            case Failure<T>(var exception) -> new Failure<>(exception);
        };
    }
    
    default T getOrElse(T defaultValue) {
        return switch (this) {
            case Success<T>(var value) -> value;
            case Failure<T> f -> defaultValue;
        };
    }
    
    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}

public class TryExample {
    public static void main(String[] args) {
        Try<Integer> result = Try.of(() -> Integer.parseInt("123"))
            .map(x -> x * 2)
            .flatMap(x -> Try.of(() -> 100 / x));
        
        System.out.println(result.getOrElse(0)); // 0 (because 100 / 246 = 0 in integer division)
        
        Try<Integer> failure = Try.of(() -> Integer.parseInt("not a number"))
            .map(x -> x * 2);
        
        System.out.println(failure.getOrElse(-1)); // -1
    }
}
```

## Function Composition and Currying

### Function Composition

Function composition allows you to combine simple functions to build more complex ones. It's the foundation of functional programming.

```java
import java.util.function.Function;

public class FunctionCompositionExample {
    public static void main(String[] args) {
        Function<String, String> trim = String::trim;
        Function<String, String> uppercase = String::toUpperCase;
        Function<String, Integer> length = String::length;
        
        // Compose functions using andThen
        Function<String, Integer> processString = trim
            .andThen(uppercase)
            .andThen(length);
        
        System.out.println(processString.apply("  hello  ")); // 5
        
        // Compose using compose (reverse order)
        Function<String, Integer> processString2 = length
            .compose(uppercase)
            .compose(trim);
        
        System.out.println(processString2.apply("  world  ")); // 5
        
        // Custom composition helper
        Function<String, String> combined = compose(trim, uppercase);
        System.out.println(combined.apply("  java  ")); // JAVA
    }
    
    public static <A, B, C> Function<A, C> compose(
            Function<A, B> f1, Function<B, C> f2) {
        return f1.andThen(f2);
    }
}
```

### Kleisli Composition

Kleisli composition allows you to compose functions that return monadic values (like Optional or Result).

```java
import java.util.Optional;
import java.util.function.Function;

public class KleisliCompositionExample {
    // Kleisli arrow: A -> M<B>
    static Function<String, Optional<Integer>> parseInt = s -> {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    };
    
    static Function<Integer, Optional<Double>> safeDivide(int divisor) {
        return x -> divisor == 0 ? Optional.empty() : Optional.of((double) x / divisor);
    }
    
    // Kleisli composition
    static <A, B, C> Function<A, Optional<C>> kleisliCompose(
            Function<A, Optional<B>> f,
            Function<B, Optional<C>> g) {
        return a -> f.apply(a).flatMap(g);
    }
    
    public static void main(String[] args) {
        Function<String, Optional<Double>> parseAndDivide = 
            kleisliCompose(parseInt, safeDivide(2));
        
        System.out.println(parseAndDivide.apply("10"));  // Optional[5.0]
        System.out.println(parseAndDivide.apply("abc")); // Optional.empty
    }
}
```

### Pipeline Builder

A Pipeline Builder provides a fluent API for composing operations in a readable, sequential manner.

```java
import java.util.function.Function;
import java.util.function.Predicate;

public class PipelineBuilder<T> {
    private Function<T, T> pipeline = Function.identity();
    
    public PipelineBuilder<T> pipe(Function<T, T> stage) {
        pipeline = pipeline.andThen(stage);
        return this;
    }
    
    public PipelineBuilder<T> pipeIf(Predicate<T> condition, Function<T, T> stage) {
        return pipe(t -> condition.test(t) ? stage.apply(t) : t);
    }
    
    public Function<T, T> build() {
        return pipeline;
    }
    
    public T execute(T input) {
        return pipeline.apply(input);
    }
    
    public static void main(String[] args) {
        var stringPipeline = new PipelineBuilder<String>()
            .pipe(String::trim)
            .pipe(String::toLowerCase)
            .pipeIf(s -> s.length() > 5, s -> s.substring(0, 5))
            .pipe(s -> "[" + s + "]")
            .build();
        
        System.out.println(stringPipeline.apply("  HELLO WORLD  ")); // [hello]
        System.out.println(stringPipeline.apply("  HI  "));          // [hi]
    }
}
```

### Currying Implementation

Currying transforms a function with multiple arguments into a sequence of functions, each taking a single argument.

```java
import java.util.function.Function;

public class CurryingExample {
    // Curry a 2-argument function
    public static <A, B, R> Function<A, Function<B, R>> curry(
            BiFunction<A, B, R> f) {
        return a -> b -> f.apply(a, b);
    }
    
    // Curry a 3-argument function
    public static <A, B, C, R> Function<A, Function<B, Function<C, R>>> curry3(
            TriFunction<A, B, C, R> f) {
        return a -> b -> c -> f.apply(a, b, c);
    }
    
    @FunctionalInterface
    interface BiFunction<A, B, R> {
        R apply(A a, B b);
    }
    
    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
    
    public static void main(String[] args) {
        // Example with 2 arguments
        BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
        Function<Integer, Function<Integer, Integer>> curriedAdd = curry(add);
        
        Function<Integer, Integer> add5 = curriedAdd.apply(5);
        System.out.println(add5.apply(3)); // 8
        
        // Example with 3 arguments
        TriFunction<String, String, String, String> concat3 = 
            (a, b, c) -> a + b + c;
        
        var curriedConcat = curry3(concat3);
        var greet = curriedConcat.apply("Hello, ");
        var greetName = greet.apply("John");
        System.out.println(greetName.apply("!")); // Hello, John!
    }
}
```

## Partial Application Techniques

### Generic Partial Application

Partial application creates a new function by fixing some arguments of an existing function.

```java
import java.util.function.Function;
import java.util.function.BiFunction;

public class PartialApplicationExample {
    // Partial application for BiFunction
    public static <A, B, R> Function<B, R> partial(
            BiFunction<A, B, R> f, A a) {
        return b -> f.apply(a, b);
    }
    
    // Generic partial application builder
    public static class Partial<A, B, R> {
        private final BiFunction<A, B, R> function;
        
        public Partial(BiFunction<A, B, R> function) {
            this.function = function;
        }
        
        public Function<B, R> withFirst(A a) {
            return b -> function.apply(a, b);
        }
        
        public Function<A, R> withSecond(B b) {
            return a -> function.apply(a, b);
        }
    }
    
    public static void main(String[] args) {
        BiFunction<String, Integer, String> repeat = (str, times) -> 
            str.repeat(times);
        
        // Partial application fixing the first argument
        Function<Integer, String> repeatHello = partial(repeat, "Hello");
        System.out.println(repeatHello.apply(3)); // HelloHelloHello
        
        // Using the Partial builder
        Partial<String, Integer, String> partialRepeat = new Partial<>(repeat);
        Function<Integer, String> repeatWorld = partialRepeat.withFirst("World");
        Function<String, String> repeatTwice = partialRepeat.withSecond(2);
        
        System.out.println(repeatWorld.apply(2)); // WorldWorld
        System.out.println(repeatTwice.apply("Java")); // JavaJava
    }
}
```

### Partial Application from Different Positions

This technique allows partial application from any position in a multi-argument function.

```java
import java.util.function.Function;

public class PositionalPartialApplication {
    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
    
    public static class PartialTriFunction<A, B, C, R> {
        private final TriFunction<A, B, C, R> function;
        
        public PartialTriFunction(TriFunction<A, B, C, R> function) {
            this.function = function;
        }
        
        // Fix first argument
        public BiFunction<B, C, R> fixFirst(A a) {
            return (b, c) -> function.apply(a, b, c);
        }
        
        // Fix second argument
        public BiFunction<A, C, R> fixSecond(B b) {
            return (a, c) -> function.apply(a, b, c);
        }
        
        // Fix third argument
        public BiFunction<A, B, R> fixThird(C c) {
            return (a, b) -> function.apply(a, b, c);
        }
        
        // Fix first two arguments
        public Function<C, R> fixFirstTwo(A a, B b) {
            return c -> function.apply(a, b, c);
        }
        
        // Fix last two arguments
        public Function<A, R> fixLastTwo(B b, C c) {
            return a -> function.apply(a, b, c);
        }
    }
    
    @FunctionalInterface
    interface BiFunction<A, B, R> {
        R apply(A a, B b);
    }
    
    public static void main(String[] args) {
        TriFunction<Integer, Integer, Integer, Integer> volume = 
            (length, width, height) -> length * width * height;
        
        var partial = new PartialTriFunction<>(volume);
        
        // Fix different positions
        var areaTimesHeight = partial.fixFirst(10);
        System.out.println(areaTimesHeight.apply(5, 3)); // 150
        
        var lengthTimesArea = partial.fixLastTwo(4, 2);
        System.out.println(lengthTimesArea.apply(10)); // 80
    }
}
```

## Builder Pattern with Lambdas

### FunctionalBuilder

A builder pattern that uses lambdas for configuration, providing a more functional approach to object construction.

```java
import java.util.function.Consumer;

public class FunctionalBuilderExample {
    public static class Person {
        private String name;
        private int age;
        private String email;
        
        private Person() {}
        
        public static Person build(Consumer<PersonBuilder> builderFunction) {
            PersonBuilder builder = new PersonBuilder();
            builderFunction.accept(builder);
            return builder.build();
        }
        
        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + ", email='" + email + "'}";
        }
    }
    
    public static class PersonBuilder {
        private final Person person = new Person();
        
        public PersonBuilder name(String name) {
            person.name = name;
            return this;
        }
        
        public PersonBuilder age(int age) {
            person.age = age;
            return this;
        }
        
        public PersonBuilder email(String email) {
            person.email = email;
            return this;
        }
        
        private Person build() {
            return person;
        }
    }
    
    public static void main(String[] args) {
        Person person = Person.build(builder -> builder
            .name("John Doe")
            .age(30)
            .email("john@example.com"));
        
        System.out.println(person);
        // Person{name='John Doe', age=30, email='john@example.com'}
        
        // Using method references
        Consumer<PersonBuilder> johnConfig = b -> b.name("John");
        Consumer<PersonBuilder> ageConfig = b -> b.age(25);
        Consumer<PersonBuilder> emailConfig = b -> b.email("john@test.com");
        
        Person john = Person.build(
            johnConfig.andThen(ageConfig).andThen(emailConfig)
        );
        
        System.out.println(john);
        // Person{name='John', age=25, email='john@test.com'}
    }
}
```

## Strategy Pattern Using Functional Interfaces

### Traditional Strategy with Functional Interfaces

The Strategy pattern using Java's functional interfaces provides a clean way to encapsulate algorithms.

```java
import java.util.function.Function;
import java.util.Map;

public class FunctionalStrategyExample {
    // Traditional strategy using functional interface
    @FunctionalInterface
    interface PricingStrategy {
        double calculatePrice(double basePrice, int quantity);
    }
    
    public static class PricingService {
        private final Map<String, PricingStrategy> strategies = Map.of(
            "standard", (price, qty) -> price * qty,
            "bulk", (price, qty) -> price * qty * (qty > 10 ? 0.9 : 1.0),
            "premium", (price, qty) -> price * qty * 1.2,
            "clearance", (price, qty) -> price * qty * 0.5
        );
        
        public double calculateTotal(String strategyName, double basePrice, int quantity) {
            PricingStrategy strategy = strategies.getOrDefault(strategyName, 
                strategies.get("standard"));
            return strategy.calculatePrice(basePrice, quantity);
        }
    }
    
    public static void main(String[] args) {
        PricingService service = new PricingService();
        
        System.out.println("Standard: " + service.calculateTotal("standard", 10, 5));      // 50.0
        System.out.println("Bulk: " + service.calculateTotal("bulk", 10, 15));            // 135.0
        System.out.println("Premium: " + service.calculateTotal("premium", 10, 5));       // 60.0
        System.out.println("Clearance: " + service.calculateTotal("clearance", 10, 5));   // 25.0
    }
}
```
