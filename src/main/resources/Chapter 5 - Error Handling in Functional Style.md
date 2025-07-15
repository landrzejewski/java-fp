## Chapter 5 - Error Handling in Functional Style.md

One of the biggest challenges when adopting functional programming in Java is dealing with checked exceptions. Java's lambda expressions and functional interfaces don't play well with checked exceptions, forcing developers to write verbose try-catch blocks or wrap exceptions. This article explores elegant solutions to this problem using functional patterns like Try Monad, Either Type, and Validation Accumulation.

## The Problem with Checked Exceptions in Lambdas

### Understanding the Challenge

Java's functional interfaces like `Function`, `Consumer`, and `Supplier` don't declare checked exceptions in their method signatures. This creates a fundamental incompatibility when trying to use methods that throw checked exceptions within lambdas.

```java
import java.util.*;
import java.util.stream.Collectors;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CheckedExceptionProblem {
    public static void main(String[] args) {
        List<String> urls = Arrays.asList(
            "https://example.com",
            "https://google.com",
            "https://github.com"
        );
        
        // This won't compile - MalformedURLException is checked
        // List<URL> urlObjects = urls.stream()
        //     .map(URL::new)  // Compile error!
        //     .collect(Collectors.toList());
        
        // Traditional solution - verbose and ugly
        List<URL> urlObjects = urls.stream()
            .map(urlString -> {
                try {
                    return new URL(urlString);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());
        
        // Similar problem with file operations
        List<String> filePaths = Arrays.asList("file1.txt", "file2.txt");
        
        // This won't compile either
        // List<String> contents = filePaths.stream()
        //     .map(path -> Files.readString(Paths.get(path)))  // Compile error!
        //     .collect(Collectors.toList());
    }
}
```

### Basic Wrapper Approaches

Before diving into monadic solutions, let's look at basic wrapper approaches:

```java
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ExceptionWrappers {
    // Functional interfaces that can throw checked exceptions
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }
    
    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }
    
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
    
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
    
    // Wrapper methods
    public static <T, R> Function<T, R> wrap(ThrowingFunction<T, R> throwingFunction) {
        return t -> {
            try {
                return throwingFunction.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
    
    public static <T> Consumer<T> wrap(ThrowingConsumer<T> throwingConsumer) {
        return t -> {
            try {
                throwingConsumer.accept(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
    
    public static <T> Supplier<T> wrap(ThrowingSupplier<T> throwingSupplier) {
        return () -> {
            try {
                return throwingSupplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
    
    // Usage example
    public static void main(String[] args) {
        List<String> urls = Arrays.asList(
            "https://example.com",
            "https://google.com",
            "invalid-url"
        );
        
        // Using wrapper - cleaner but still throws RuntimeException
        List<URL> urlObjects = urls.stream()
            .map(wrap(URL::new))
            .collect(Collectors.toList());
        
        // With custom exception handling
        Function<String, URL> safeUrlCreator = url -> {
            try {
                return new URL(url);
            } catch (Exception e) {
                System.err.println("Failed to create URL: " + url);
                return null;
            }
        };
        
        List<URL> validUrls = urls.stream()
            .map(safeUrlCreator)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        System.out.println("Valid URLs: " + validUrls.size());
    }
}
```

## The Try Monad Pattern

The Try monad provides a functional way to handle operations that might throw exceptions, encapsulating the result as either a success or failure.

### Basic Try Implementation

```java
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class Try<T> {
    // Factory methods
    public static <T> Try<T> of(ThrowingSupplier<T> supplier) {
        Objects.requireNonNull(supplier, "Supplier cannot be null");
        try {
            return new Success<>(supplier.get());
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }
    
    public static <T> Try<T> success(T value) {
        return new Success<>(value);
    }
    
    public static <T> Try<T> failure(Exception e) {
        return new Failure<>(e);
    }
    
    // Abstract methods
    public abstract boolean isSuccess();
    public abstract boolean isFailure();
    public abstract T get() throws Exception;
    public abstract T getOrElse(T defaultValue);
    public abstract T getOrElseGet(Supplier<? extends T> supplier);
    public abstract <X extends Exception> T getOrElseThrow(Supplier<? extends X> exceptionSupplier) throws X;
    public abstract Optional<T> toOptional();
    public abstract <U> Try<U> map(Function<? super T, ? extends U> mapper);
    public abstract <U> Try<U> flatMap(Function<? super T, Try<U>> mapper);
    public abstract Try<T> filter(Predicate<? super T> predicate);
    public abstract Try<T> recover(Function<? super Exception, ? extends T> recoveryFunction);
    public abstract Try<T> recoverWith(Function<? super Exception, Try<T>> recoveryFunction);
    public abstract Try<T> onSuccess(Consumer<? super T> action);
    public abstract Try<T> onFailure(Consumer<Exception> action);
    
    // Functional interface for throwing suppliers
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
    
    // Success implementation
    private static final class Success<T> extends Try<T> {
        private final T value;
        
        private Success(T value) {
            this.value = value;
        }
        
        @Override
        public boolean isSuccess() {
            return true;
        }
        
        @Override
        public boolean isFailure() {
            return false;
        }
        
        @Override
        public T get() {
            return value;
        }
        
        @Override
        public T getOrElse(T defaultValue) {
            return value;
        }
        
        @Override
        public T getOrElseGet(Supplier<? extends T> supplier) {
            return value;
        }
        
        @Override
        public <X extends Exception> T getOrElseThrow(Supplier<? extends X> exceptionSupplier) {
            return value;
        }
        
        @Override
        public Optional<T> toOptional() {
            return Optional.ofNullable(value);
        }
        
        @Override
        public <U> Try<U> map(Function<? super T, ? extends U> mapper) {
            Objects.requireNonNull(mapper, "Mapper cannot be null");
            return Try.of(() -> mapper.apply(value));
        }
        
        @Override
        public <U> Try<U> flatMap(Function<? super T, Try<U>> mapper) {
            Objects.requireNonNull(mapper, "Mapper cannot be null");
            try {
                return mapper.apply(value);
            } catch (Exception e) {
                return new Failure<>(e);
            }
        }
        
        @Override
        public Try<T> filter(Predicate<? super T> predicate) {
            Objects.requireNonNull(predicate, "Predicate cannot be null");
            try {
                return predicate.test(value) ? this : 
                    new Failure<>(new NoSuchElementException("Predicate does not match"));
            } catch (Exception e) {
                return new Failure<>(e);
            }
        }
        
        @Override
        public Try<T> recover(Function<? super Exception, ? extends T> recoveryFunction) {
            return this;
        }
        
        @Override
        public Try<T> recoverWith(Function<? super Exception, Try<T>> recoveryFunction) {
            return this;
        }
        
        @Override
        public Try<T> onSuccess(Consumer<? super T> action) {
            Objects.requireNonNull(action, "Action cannot be null");
            try {
                action.accept(value);
            } catch (Exception e) {
                // Log but don't change the Try state
                e.printStackTrace();
            }
            return this;
        }
        
        @Override
        public Try<T> onFailure(Consumer<Exception> action) {
            return this;
        }
        
        @Override
        public String toString() {
            return "Success(" + value + ")";
        }
    }
    
    // Failure implementation
    private static final class Failure<T> extends Try<T> {
        private final Exception exception;
        
        private Failure(Exception exception) {
            this.exception = exception;
        }
        
        @Override
        public boolean isSuccess() {
            return false;
        }
        
        @Override
        public boolean isFailure() {
            return true;
        }
        
        @Override
        public T get() throws Exception {
            throw exception;
        }
        
        @Override
        public T getOrElse(T defaultValue) {
            return defaultValue;
        }
        
        @Override
        public T getOrElseGet(Supplier<? extends T> supplier) {
            Objects.requireNonNull(supplier, "Supplier cannot be null");
            return supplier.get();
        }
        
        @Override
        public <X extends Exception> T getOrElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            Objects.requireNonNull(exceptionSupplier, "Exception supplier cannot be null");
            throw exceptionSupplier.get();
        }
        
        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }
        
        @Override
        public <U> Try<U> map(Function<? super T, ? extends U> mapper) {
            return new Failure<>(exception);
        }
        
        @Override
        public <U> Try<U> flatMap(Function<? super T, Try<U>> mapper) {
            return new Failure<>(exception);
        }
        
        @Override
        public Try<T> filter(Predicate<? super T> predicate) {
            return this;
        }
        
        @Override
        public Try<T> recover(Function<? super Exception, ? extends T> recoveryFunction) {
            Objects.requireNonNull(recoveryFunction, "Recovery function cannot be null");
            return Try.of(() -> recoveryFunction.apply(exception));
        }
        
        @Override
        public Try<T> recoverWith(Function<? super Exception, Try<T>> recoveryFunction) {
            Objects.requireNonNull(recoveryFunction, "Recovery function cannot be null");
            try {
                return recoveryFunction.apply(exception);
            } catch (Exception e) {
                return new Failure<>(e);
            }
        }
        
        @Override
        public Try<T> onSuccess(Consumer<? super T> action) {
            return this;
        }
        
        @Override
        public Try<T> onFailure(Consumer<Exception> action) {
            Objects.requireNonNull(action, "Action cannot be null");
            try {
                action.accept(exception);
            } catch (Exception e) {
                // Log but don't change the Try state
                e.printStackTrace();
            }
            return this;
        }
        
        @Override
        public String toString() {
            return "Failure(" + exception + ")";
        }
    }
}
```

### Try Monad Usage Examples

```java
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TryMonadExamples {
    public static void main(String[] args) {
        // Example 1: URL parsing
        List<String> urls = Arrays.asList(
            "https://example.com",
            "https://google.com",
            "invalid-url",
            "https://github.com"
        );
        
        List<URL> validUrls = urls.stream()
            .map(url -> Try.of(() -> new URL(url)))
            .filter(Try::isSuccess)
            .map(tryUrl -> tryUrl.getOrElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        System.out.println("Valid URLs: " + validUrls);
        
        // Example 2: Chaining operations
        String filePath = "test.txt";
        Try<List<String>> fileLines = Try.of(() -> Files.readAllLines(Paths.get(filePath)))
            .map(lines -> lines.stream()
                .filter(line -> !line.trim().isEmpty())
                .collect(Collectors.toList()))
            .filter(lines -> !lines.isEmpty())
            .recover(e -> {
                System.err.println("Failed to read file: " + e.getMessage());
                return Arrays.asList("Default", "Content");
            });
        
        fileLines.onSuccess(lines -> System.out.println("Lines: " + lines))
                .onFailure(e -> System.err.println("Error: " + e));
        
        // Example 3: Complex transformations
        Try<Integer> result = Try.of(() -> "123")
            .map(Integer::parseInt)
            .filter(n -> n > 0)
            .map(n -> n * 2)
            .flatMap(n -> Try.of(() -> 100 / n))
            .recover(e -> {
                if (e instanceof ArithmeticException) {
                    return 0;
                }
                throw new RuntimeException(e);
            });
        
        System.out.println("Result: " + result.getOrElse(-1));
        
        // Example 4: Resource management
        Try<String> fileContent = readFile("config.properties")
            .map(content -> content.toUpperCase())
            .recover(e -> "DEFAULT_CONFIG");
        
        System.out.println("Config: " + fileContent.getOrElse("NO_CONFIG"));
    }
    
    private static Try<String> readFile(String path) {
        return Try.of(() -> new String(Files.readAllBytes(Paths.get(path))));
    }
    
    // Example 5: Combining multiple Try operations
    public static void combiningTryOperations() {
        Try<String> firstName = Try.of(() -> "John");
        Try<String> lastName = Try.of(() -> "Doe");
        Try<Integer> age = Try.of(() -> Integer.parseInt("25"));
        
        // Combine using flatMap
        Try<String> fullInfo = firstName.flatMap(fn ->
            lastName.flatMap(ln ->
                age.map(a -> fn + " " + ln + ", age: " + a)
            )
        );
        
        fullInfo.onSuccess(System.out::println);
        
        // Using Try.sequence pattern
        List<Try<String>> tryList = Arrays.asList(
            Try.of(() -> "A"),
            Try.of(() -> "B"),
            Try.failure(new Exception("Failed")),
            Try.of(() -> "D")
        );
        
        Try<List<String>> sequenced = sequence(tryList);
        System.out.println("Sequenced: " + sequenced);
    }
    
    // Helper method to sequence a list of Try
    private static <T> Try<List<T>> sequence(List<Try<T>> tryList) {
        return tryList.stream()
            .reduce(
                Try.success(new ArrayList<>()),
                (acc, tryElement) -> acc.flatMap(list ->
                    tryElement.map(element -> {
                        List<T> newList = new ArrayList<>(list);
                        newList.add(element);
                        return newList;
                    })
                ),
                (t1, t2) -> t1 // Combiner for parallel streams (not used here)
            );
    }
}
```

## The Either Type Pattern

The Either type represents a value of one of two possible types (a disjoint union). By convention, Left is used for failure and Right for success.

### Either Implementation

```java
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Consumer;

public abstract class Either<L, R> {
    // Factory methods
    public static <L, R> Either<L, R> left(L value) {
        return new Left<>(value);
    }
    
    public static <L, R> Either<L, R> right(R value) {
        return new Right<>(value);
    }
    
    // Abstract methods
    public abstract boolean isLeft();
    public abstract boolean isRight();
    public abstract L getLeft();
    public abstract R getRight();
    public abstract Optional<L> left();
    public abstract Optional<R> right();
    public abstract <T> Either<L, T> map(Function<? super R, ? extends T> mapper);
    public abstract <T> Either<L, T> flatMap(Function<? super R, Either<L, T>> mapper);
    public abstract <T> Either<T, R> mapLeft(Function<? super L, ? extends T> mapper);
    public abstract Either<L, R> onLeft(Consumer<? super L> action);
    public abstract Either<L, R> onRight(Consumer<? super R> action);
    public abstract <T> T fold(Function<? super L, ? extends T> leftMapper,
                               Function<? super R, ? extends T> rightMapper);
    public abstract Either<R, L> swap();
    public abstract R getOrElse(R defaultValue);
    public abstract Either<L, R> orElse(Either<L, R> alternative);
    
    // Left implementation
    private static final class Left<L, R> extends Either<L, R> {
        private final L value;
        
        private Left(L value) {
            this.value = value;
        }
        
        @Override
        public boolean isLeft() {
            return true;
        }
        
        @Override
        public boolean isRight() {
            return false;
        }
        
        @Override
        public L getLeft() {
            return value;
        }
        
        @Override
        public R getRight() {
            throw new NoSuchElementException("getRight() on Left");
        }
        
        @Override
        public Optional<L> left() {
            return Optional.of(value);
        }
        
        @Override
        public Optional<R> right() {
            return Optional.empty();
        }
        
        @Override
        public <T> Either<L, T> map(Function<? super R, ? extends T> mapper) {
            return new Left<>(value);
        }
        
        @Override
        public <T> Either<L, T> flatMap(Function<? super R, Either<L, T>> mapper) {
            return new Left<>(value);
        }
        
        @Override
        public <T> Either<T, R> mapLeft(Function<? super L, ? extends T> mapper) {
            return new Left<>(mapper.apply(value));
        }
        
        @Override
        public Either<L, R> onLeft(Consumer<? super L> action) {
            action.accept(value);
            return this;
        }
        
        @Override
        public Either<L, R> onRight(Consumer<? super R> action) {
            return this;
        }
        
        @Override
        public <T> T fold(Function<? super L, ? extends T> leftMapper,
                          Function<? super R, ? extends T> rightMapper) {
            return leftMapper.apply(value);
        }
        
        @Override
        public Either<R, L> swap() {
            return new Right<>(value);
        }
        
        @Override
        public R getOrElse(R defaultValue) {
            return defaultValue;
        }
        
        @Override
        public Either<L, R> orElse(Either<L, R> alternative) {
            return alternative;
        }
        
        @Override
        public String toString() {
            return "Left(" + value + ")";
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Left)) return false;
            Left<?, ?> other = (Left<?, ?>) obj;
            return Objects.equals(value, other.value);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash("Left", value);
        }
    }
    
    // Right implementation
    private static final class Right<L, R> extends Either<L, R> {
        private final R value;
        
        private Right(R value) {
            this.value = value;
        }
        
        @Override
        public boolean isLeft() {
            return false;
        }
        
        @Override
        public boolean isRight() {
            return true;
        }
        
        @Override
        public L getLeft() {
            throw new NoSuchElementException("getLeft() on Right");
        }
        
        @Override
        public R getRight() {
            return value;
        }
        
        @Override
        public Optional<L> left() {
            return Optional.empty();
        }
        
        @Override
        public Optional<R> right() {
            return Optional.of(value);
        }
        
        @Override
        public <T> Either<L, T> map(Function<? super R, ? extends T> mapper) {
            return new Right<>(mapper.apply(value));
        }
        
        @Override
        public <T> Either<L, T> flatMap(Function<? super R, Either<L, T>> mapper) {
            return mapper.apply(value);
        }
        
        @Override
        public <T> Either<T, R> mapLeft(Function<? super L, ? extends T> mapper) {
            return new Right<>(value);
        }
        
        @Override
        public Either<L, R> onLeft(Consumer<? super L> action) {
            return this;
        }
        
        @Override
        public Either<L, R> onRight(Consumer<? super R> action) {
            action.accept(value);
            return this;
        }
        
        @Override
        public <T> T fold(Function<? super L, ? extends T> leftMapper,
                          Function<? super R, ? extends T> rightMapper) {
            return rightMapper.apply(value);
        }
        
        @Override
        public Either<R, L> swap() {
            return new Left<>(value);
        }
        
        @Override
        public R getOrElse(R defaultValue) {
            return value;
        }
        
        @Override
        public Either<L, R> orElse(Either<L, R> alternative) {
            return this;
        }
        
        @Override
        public String toString() {
            return "Right(" + value + ")";
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Right)) return false;
            Right<?, ?> other = (Right<?, ?>) obj;
            return Objects.equals(value, other.value);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash("Right", value);
        }
    }
    
    // Utility methods
    public static <L, R> Either<L, R> fromOptional(Optional<R> optional, L leftValue) {
        return optional.map(Either::<L, R>right).orElse(Either.left(leftValue));
    }
    
    public static <L, R> Either<L, R> fromTry(Try<R> tryValue, Function<Exception, L> errorMapper) {
        return tryValue.isSuccess() 
            ? Either.right(tryValue.getOrElse(null))
            : Either.left(errorMapper.apply(
                tryValue.onFailure(e -> {}).fold(e -> e, v -> new Exception("Unexpected success"))));
    }
}
```

### Either Usage Examples

```java
import java.util.*;
import java.util.stream.Collectors;

public class EitherExamples {
    // Error types
    static class ValidationError {
        private final String field;
        private final String message;
        
        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }
        
        @Override
        public String toString() {
            return field + ": " + message;
        }
    }
    
    static class User {
        private final String name;
        private final String email;
        private final int age;
        
        public User(String name, String email, int age) {
            this.name = name;
            this.email = email;
            this.age = age;
        }
        
        @Override
        public String toString() {
            return "User{name='" + name + "', email='" + email + "', age=" + age + '}';
        }
    }
    
    public static void main(String[] args) {
        // Example 1: Basic validation
        Either<ValidationError, String> nameValidation = validateName("John");
        Either<ValidationError, String> emailValidation = validateEmail("john@example.com");
        Either<ValidationError, Integer> ageValidation = validateAge(25);
        
        // Combining validations
        Either<ValidationError, User> userValidation = 
            nameValidation.flatMap(name ->
                emailValidation.flatMap(email ->
                    ageValidation.map(age -> new User(name, email, age))
                )
            );
        
        userValidation
            .onRight(user -> System.out.println("Valid user: " + user))
            .onLeft(error -> System.out.println("Validation error: " + error));
        
        // Example 2: Error handling in stream processing
        List<String> userInputs = Arrays.asList(
            "john,john@example.com,25",
            "jane,invalid-email,30",
            "bob,bob@example.com,17",
            "alice,alice@example.com,35"
        );
        
        List<Either<ValidationError, User>> results = userInputs.stream()
            .map(EitherExamples::parseUser)
            .collect(Collectors.toList());
        
        System.out.println("\nProcessing results:");
        results.forEach(result -> 
            System.out.println(result.fold(
                error -> "Error: " + error,
                user -> "Success: " + user
            ))
        );
        
        // Example 3: Collecting only successful results
        List<User> validUsers = results.stream()
            .filter(Either::isRight)
            .map(Either::getRight)
            .collect(Collectors.toList());
        
        System.out.println("\nValid users: " + validUsers);
        
        // Example 4: Transforming errors
        List<String> errorMessages = results.stream()
            .filter(Either::isLeft)
            .map(either -> either.mapLeft(error -> error.toString()))
            .map(Either::getLeft)
            .collect(Collectors.toList());
        
        System.out.println("Error messages: " + errorMessages);
        
        // Example 5: Railway-oriented programming
        Either<String, Integer> computation = 
            compute(10)
                .flatMap(EitherExamples::doubleIfPositive)
                .flatMap(EitherExamples::divideByTwo)
                .map(n -> n + 5);
        
        System.out.println("\nComputation result: " + 
            computation.fold(
                error -> "Failed: " + error,
                value -> "Success: " + value
            ));
    }
    
    private static Either<ValidationError, String> validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Either.left(new ValidationError("name", "Name cannot be empty"));
        }
        if (name.length() < 2) {
            return Either.left(new ValidationError("name", "Name must be at least 2 characters"));
        }
        return Either.right(name);
    }
    
    private static Either<ValidationError, String> validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            return Either.left(new ValidationError("email", "Invalid email format"));
        }
        return Either.right(email);
    }
    
    private static Either<ValidationError, Integer> validateAge(int age) {
        if (age < 18) {
            return Either.left(new ValidationError("age", "Must be at least 18 years old"));
        }
        if (age > 150) {
            return Either.left(new ValidationError("age", "Invalid age"));
        }
        return Either.right(age);
    }
    
    private static Either<ValidationError, User> parseUser(String input) {
        String[] parts = input.split(",");
        if (parts.length != 3) {
            return Either.left(new ValidationError("input", "Invalid format"));
        }
        
        try {
            String name = parts[0].trim();
            String email = parts[1].trim();
            int age = Integer.parseInt(parts[2].trim());
            
            return validateName(name)
                .flatMap(n -> validateEmail(email)
                    .flatMap(e -> validateAge(age)
                        .map(a -> new User(n, e, a))));
        } catch (NumberFormatException e) {
            return Either.left(new ValidationError("age", "Age must be a number"));
        }
    }
    
    private static Either<String, Integer> compute(int value) {
        if (value < 0) {
            return Either.left("Value must be non-negative");
        }
        return Either.right(value);
    }
    
    private static Either<String, Integer> doubleIfPositive(int value) {
        if (value <= 0) {
            return Either.left("Value must be positive for doubling");
        }
        return Either.right(value * 2);
    }
    
    private static Either<String, Integer> divideByTwo(int value) {
        return Either.right(value / 2);
    }
}
```

## Validation Accumulation Pattern

While Either fails fast on the first error, the Validation pattern accumulates all errors, which is useful for form validation and similar scenarios.

### Validation Implementation

```java
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class Validation<E, T> {
    // Factory methods
    public static <E, T> Validation<E, T> valid(T value) {
        return new Valid<>(value);
    }
    
    public static <E, T> Validation<E, T> invalid(E error) {
        return new Invalid<>(Collections.singletonList(error));
    }
    
    public static <E, T> Validation<E, T> invalid(List<E> errors) {
        return new Invalid<>(new ArrayList<>(errors));
    }
    
    // Abstract methods
    public abstract boolean isValid();
    public abstract boolean isInvalid();
    public abstract T getValue();
    public abstract List<E> getErrors();
    public abstract <U> Validation<E, U> map(Function<? super T, ? extends U> mapper);
    public abstract <U> Validation<E, U> flatMap(Function<? super T, Validation<E, U>> mapper);
    public abstract Validation<E, T> combine(Validation<E, T> other);
    public abstract <U, R> Validation<E, R> combine(Validation<E, U> other, 
                                                    Function<T, Function<U, R>> combiner);
    
    // Valid implementation
    private static final class Valid<E, T> extends Validation<E, T> {
        private final T value;
        
        private Valid(T value) {
            this.value = value;
        }
        
        @Override
        public boolean isValid() {
            return true;
        }
        
        @Override
        public boolean isInvalid() {
            return false;
        }
        
        @Override
        public T getValue() {
            return value;
        }
        
        @Override
        public List<E> getErrors() {
            return Collections.emptyList();
        }
        
        @Override
        public <U> Validation<E, U> map(Function<? super T, ? extends U> mapper) {
            return new Valid<>(mapper.apply(value));
        }
        
        @Override
        public <U> Validation<E, U> flatMap(Function<? super T, Validation<E, U>> mapper) {
            return mapper.apply(value);
        }
        
        @Override
        public Validation<E, T> combine(Validation<E, T> other) {
            return other.isValid() ? this : other;
        }
        
        @Override
        public <U, R> Validation<E, R> combine(Validation<E, U> other, 
                                               Function<T, Function<U, R>> combiner) {
            return other.isValid() 
                ? new Valid<>(combiner.apply(value).apply(other.getValue()))
                : new Invalid<>(other.getErrors());
        }
        
        @Override
        public String toString() {
            return "Valid(" + value + ")";
        }
    }
    
    // Invalid implementation
    private static final class Invalid<E, T> extends Validation<E, T> {
        private final List<E> errors;
        
        private Invalid(List<E> errors) {
            this.errors = errors;
        }
        
        @Override
        public boolean isValid() {
            return false;
        }
        
        @Override
        public boolean isInvalid() {
            return true;
        }
        
        @Override
        public T getValue() {
            throw new NoSuchElementException("getValue() on Invalid");
        }
        
        @Override
        public List<E> getErrors() {
            return new ArrayList<>(errors);
        }
        
        @Override
        public <U> Validation<E, U> map(Function<? super T, ? extends U> mapper) {
            return new Invalid<>(errors);
        }
        
        @Override
        public <U> Validation<E, U> flatMap(Function<? super T, Validation<E, U>> mapper) {
            return new Invalid<>(errors);
        }
        
        @Override
        public Validation<E, T> combine(Validation<E, T> other) {
            if (other.isInvalid()) {
                List<E> allErrors = new ArrayList<>(errors);
                allErrors.addAll(other.getErrors());
                return new Invalid<>(allErrors);
            }
            return this;
        }
        
        @Override
        public <U, R> Validation<E, R> combine(Validation<E, U> other, 
                                               Function<T, Function<U, R>> combiner) {
            List<E> allErrors = new ArrayList<>(errors);
            if (other.isInvalid()) {
                allErrors.addAll(other.getErrors());
            }
            return new Invalid<>(allErrors);
        }
        
        @Override
        public String toString() {
            return "Invalid(" + errors + ")";
        }
    }
    
    // Applicative methods for combining multiple validations
    public static <E, T1, T2, R> Validation<E, R> map2(
            Validation<E, T1> v1,
            Validation<E, T2> v2,
            Function<T1, Function<T2, R>> f) {
        return v1.combine(v2, f);
    }
    
    public static <E, T1, T2, T3, R> Validation<E, R> map3(
            Validation<E, T1> v1,
            Validation<E, T2> v2,
            Validation<E, T3> v3,
            Function<T1, Function<T2, Function<T3, R>>> f) {
        return map2(v1, v2, t1 -> t2 -> (T3 t3) -> f.apply(t1).apply(t2).apply(t3))
            .combine(v3, r -> t3 -> r.apply(t3));
    }
    
    // Sequence a list of validations
    public static <E, T> Validation<E, List<T>> sequence(List<Validation<E, T>> validations) {
        return validations.stream()
            .reduce(
                valid(new ArrayList<>()),
                (acc, validation) -> acc.combine(validation, list -> item -> {
                    List<T> newList = new ArrayList<>(list);
                    newList.add(item);
                    return newList;
                }),
                (v1, v2) -> v1.combine(v2, list1 -> list2 -> {
                    List<T> combined = new ArrayList<>(list1);
                    combined.addAll(list2);
                    return combined;
                })
            );
    }
}
```

### Validation Accumulation Examples

```java
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

public class ValidationAccumulationExamples {
    // Domain classes
    static class RegistrationForm {
        private final String username;
        private final String email;
        private final String password;
        private final String confirmPassword;
        private final int age;
        private final boolean termsAccepted;
        
        public RegistrationForm(String username, String email, String password, 
                              String confirmPassword, int age, boolean termsAccepted) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.confirmPassword = confirmPassword;
            this.age = age;
            this.termsAccepted = termsAccepted;
        }
        
        @Override
        public String toString() {
            return "RegistrationForm{username='" + username + "', email='" + email + 
                   "', age=" + age + ", termsAccepted=" + termsAccepted + '}';
        }
    }
    
    static class ValidatedUser {
        private final String username;
        private final String email;
        private final String passwordHash;
        private final int age;
        
        public ValidatedUser(String username, String email, String passwordHash, int age) {
            this.username = username;
            this.email = email;
            this.passwordHash = passwordHash;
            this.age = age;
        }
        
        @Override
        public String toString() {
            return "ValidatedUser{username='" + username + "', email='" + email + 
                   "', age=" + age + '}';
        }
    }
    
    public static void main(String[] args) {
        // Example 1: Form validation with error accumulation
        RegistrationForm form1 = new RegistrationForm(
            "john_doe", "john@example.com", "password123", "password123", 25, true
        );
        
        RegistrationForm form2 = new RegistrationForm(
            "j", "invalid-email", "pass", "password", 16, false
        );
        
        System.out.println("=== Form Validation Examples ===");
        validateRegistration(form1);
        validateRegistration(form2);
        
        // Example 2: Batch processing with validation
        List<Map<String, String>> rawData = Arrays.asList(
            Map.of("name", "Alice", "email", "alice@example.com", "age", "30"),
            Map.of("name", "B", "email", "bob@", "age", "25"),
            Map.of("name", "Charlie", "email", "charlie@example.com", "age", "seventeen"),
            Map.of("name", "Diana", "email", "diana@example.com", "age", "28")
        );
        
        System.out.println("\n=== Batch Processing ===");
        List<Validation<String, ValidatedUser>> results = rawData.stream()
            .map(ValidationAccumulationExamples::validateUserData)
            .collect(Collectors.toList());
        
        results.forEach(validation -> {
            if (validation.isValid()) {
                System.out.println("Valid: " + validation.getValue());
            } else {
                System.out.println("Invalid: " + validation.getErrors());
            }
        });
        
        // Example 3: Complex object validation
        System.out.println("\n=== Complex Validation ===");
        validateComplexObject();
        
        // Example 4: Composing validators
        System.out.println("\n=== Validator Composition ===");
        demonstrateValidatorComposition();
    }
    
    private static void validateRegistration(RegistrationForm form) {
        Validation<String, String> usernameValidation = validateUsername(form.username);
        Validation<String, String> emailValidation = validateEmail(form.email);
        Validation<String, String> passwordValidation = validatePassword(form.password);
        Validation<String, Boolean> passwordMatchValidation = 
            validatePasswordMatch(form.password, form.confirmPassword);
        Validation<String, Integer> ageValidation = validateAge(form.age);
        Validation<String, Boolean> termsValidation = validateTermsAccepted(form.termsAccepted);
        
        // Combine all validations
        Validation<String, ValidatedUser> result = 
            Validation.map3(
                usernameValidation,
                emailValidation,
                passwordValidation.combine(passwordMatchValidation, p -> m -> p),
                username -> email -> password -> 
                    Validation.map2(
                        ageValidation,
                        termsValidation,
                        age -> terms -> new ValidatedUser(username, email, hashPassword(password), age)
                    )
            ).flatMap(f -> f);
        
        if (result.isValid()) {
            System.out.println("Registration successful: " + result.getValue());
        } else {
            System.out.println("Registration failed with errors:");
            result.getErrors().forEach(error -> System.out.println("  - " + error));
        }
    }
    
    private static Validation<String, String> validateUsername(String username) {
        List<String> errors = new ArrayList<>();
        
        if (username == null || username.trim().isEmpty()) {
            errors.add("Username is required");
        } else {
            if (username.length() < 3) {
                errors.add("Username must be at least 3 characters long");
            }
            if (username.length() > 20) {
                errors.add("Username must not exceed 20 characters");
            }
            if (!username.matches("^[a-zA-Z0-9_]+$")) {
                errors.add("Username can only contain letters, numbers, and underscores");
            }
        }
        
        return errors.isEmpty() ? Validation.valid(username) : Validation.invalid(errors);
    }
    
    private static Validation<String, String> validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Validation.invalid("Email is required");
        }
        
        Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
        if (!emailPattern.matcher(email).matches()) {
            return Validation.invalid("Invalid email format");
        }
        
        return Validation.valid(email);
    }
    
    private static Validation<String, String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.isEmpty()) {
            errors.add("Password is required");
        } else {
            if (password.length() < 8) {
                errors.add("Password must be at least 8 characters long");
            }
            if (!password.matches(".*[A-Z].*")) {
                errors.add("Password must contain at least one uppercase letter");
            }
            if (!password.matches(".*[a-z].*")) {
                errors.add("Password must contain at least one lowercase letter");
            }
            if (!password.matches(".*[0-9].*")) {
                errors.add("Password must contain at least one digit");
            }
        }
        
        return errors.isEmpty() ? Validation.valid(password) : Validation.invalid(errors);
    }
    
    private static Validation<String, Boolean> validatePasswordMatch(String password, String confirm) {
        return password.equals(confirm) 
            ? Validation.valid(true)
            : Validation.invalid("Passwords do not match");
    }
    
    private static Validation<String, Integer> validateAge(int age) {
        if (age < 18) {
            return Validation.invalid("Must be at least 18 years old");
        }
        if (age > 120) {
            return Validation.invalid("Invalid age");
        }
        return Validation.valid(age);
    }
    
    private static Validation<String, Boolean> validateTermsAccepted(boolean accepted) {
        return accepted 
            ? Validation.valid(true)
            : Validation.invalid("Terms and conditions must be accepted");
    }
    
    private static String hashPassword(String password) {
        // Simplified - in reality, use proper password hashing
        return "hashed_" + password;
    }
    
    private static Validation<String, ValidatedUser> validateUserData(Map<String, String> data) {
        Validation<String, String> nameValidation = 
            Optional.ofNullable(data.get("name"))
                .filter(n -> n.length() >= 2)
                .map(Validation::<String, String>valid)
                .orElse(Validation.invalid("Name must be at least 2 characters"));
        
        Validation<String, String> emailValidation = 
            Optional.ofNullable(data.get("email"))
                .filter(e -> e.contains("@") && e.contains("."))
                .map(Validation::<String, String>valid)
                .orElse(Validation.invalid("Invalid email format"));
        
        Validation<String, Integer> ageValidation = 
            Optional.ofNullable(data.get("age"))
                .map(a -> {
                    try {
                        return Validation.<String, Integer>valid(Integer.parseInt(a));
                    } catch (NumberFormatException e) {
                        return Validation.<String, Integer>invalid("Age must be a number");
                    }
                })
                .orElse(Validation.invalid("Age is required"));
        
        return Validation.map3(
            nameValidation,
            emailValidation,
            ageValidation,
            name -> email -> age -> new ValidatedUser(name, email, "default_hash", age)
        );
    }
    
    // Complex validation example
    static class Address {
        final String street;
        final String city;
        final String zipCode;
        
        Address(String street, String city, String zipCode) {
            this.street = street;
            this.city = city;
            this.zipCode = zipCode;
        }
    }
    
    static class Person {
        final String name;
        final int age;
        final Address address;
        
        Person(String name, int age, Address address) {
            this.name = name;
            this.age = age;
            this.address = address;
        }
        
        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + 
                   ", address=" + address.city + '}';
        }
    }
    
    private static void validateComplexObject() {
        // Validate nested object
        Validation<String, String> streetValidation = 
            Validation.valid("123 pl.training.Main St");
        Validation<String, String> cityValidation = 
            Validation.valid("New York");
        Validation<String, String> zipValidation = 
            validateZipCode("12345");
        
        Validation<String, Address> addressValidation = 
            Validation.map3(
                streetValidation,
                cityValidation,
                zipValidation,
                street -> city -> zip -> new Address(street, city, zip)
            );
        
        Validation<String, Person> personValidation = 
            Validation.map3(
                validateName("John Doe"),
                validateAge(30),
                addressValidation,
                name -> age -> address -> new Person(name, age, address)
            );
        
        if (personValidation.isValid()) {
            System.out.println("Valid person: " + personValidation.getValue());
        } else {
            System.out.println("Validation errors: " + personValidation.getErrors());
        }
    }
    
    private static Validation<String, String> validateName(String name) {
        return name != null && name.length() >= 2 
            ? Validation.valid(name)
            : Validation.invalid("Invalid name");
    }
    
    private static Validation<String, String> validateZipCode(String zip) {
        return zip != null && zip.matches("\\d{5}")
            ? Validation.valid(zip)
            : Validation.invalid("Zip code must be 5 digits");
    }
    
    // Validator composition
    @FunctionalInterface
    interface Validator<T> extends Function<T, Validation<String, T>> {}
    
    private static void demonstrateValidatorComposition() {
        // Create reusable validators
        Validator<String> notEmpty = value -> 
            value != null && !value.trim().isEmpty()
                ? Validation.valid(value)
                : Validation.invalid("Value cannot be empty");
        
        Validator<String> minLength = value -> 
            value.length() >= 3
                ? Validation.valid(value)
                : Validation.invalid("Value must be at least 3 characters");
        
        Validator<String> alphanumeric = value -> 
            value.matches("^[a-zA-Z0-9]+$")
                ? Validation.valid(value)
                : Validation.invalid("Value must be alphanumeric");
        
        // Compose validators
        Validator<String> usernameValidator = compose(
            notEmpty,
            minLength,
            alphanumeric
        );
        
        // Test composed validator
        System.out.println("Valid username: " + usernameValidator.apply("john123"));
        System.out.println("Invalid username: " + usernameValidator.apply("a!"));
    }
    
    @SafeVarargs
    private static <T> Validator<T> compose(Validator<T>... validators) {
        return value -> {
            List<Validation<String, T>> validations = Arrays.stream(validators)
                .map(v -> v.apply(value))
                .collect(Collectors.toList());
            
            List<String> allErrors = validations.stream()
                .filter(Validation::isInvalid)
                .flatMap(v -> v.getErrors().stream())
                .collect(Collectors.toList());
            
            return allErrors.isEmpty() 
                ? Validation.valid(value)
                : Validation.invalid(allErrors);
        };
    }
}
```
