package pl.training;

import java.util.*;
import java.util.function.*;

public class FunctionalProgramming {

    // Pure functions
    public static int abs(int value) {
        return value < 0 ? -value : value;
    }

    // Currying
    public static int add(int value, int otherValue) {
        return value + otherValue;
    }

    public static Function<Integer, Integer> add(int value) {
        return otherValue -> value + otherValue;
    }

    // Recursion
    public static int factorial(int number) {
        return loop(number, 1);
    }

    private static int loop(int currentNumber, int accumulator) {
        if (currentNumber <= 0) return accumulator;
        return loop(currentNumber - 1, currentNumber * accumulator);
    }

    public static int fibonacci(int elementIndex) {
        return fibLoop(elementIndex, 0, 1);
    }

    private static int fibLoop(int elementsLeft, int current, int next) {
        if (elementsLeft == 0) return current;
        return fibLoop(elementsLeft - 1, next, current + next);
    }

    // Higher-order functions
    public static String formatResult(int n, Function<Integer, Integer> f) {
        return "Result: " + f.apply(n);
    }

    // Polymorphic functions
    public static <E> int findFirst(E[] xs, Predicate<E> predicate) {
        return findFirstLoop(xs, predicate, 0);
    }

    private static <E> int findFirstLoop(E[] xs, Predicate<E> predicate, int index) {
        if (index == xs.length) return -1;
        if (predicate.test(xs[index])) return index;
        return findFirstLoop(xs, predicate, index + 1);
    }

    public static boolean isEven(int value) {
        return value % 2 == 0;
    }

    // Function composition utilities
    public static <A, B, C> Function<B, C> partial(A a, BiFunction<A, B, C> fn) {
        return b -> fn.apply(a, b);
    }

    public static <A, B, C> Function<A, Function<B, C>> curry(BiFunction<A, B, C> fn) {
        return a -> b -> fn.apply(a, b);
    }

    public static <A, B, C> BiFunction<A, B, C> uncurry(Function<A, Function<B, C>> fn) {
        return (a, b) -> fn.apply(a).apply(b);
    }

    public static <A, B, C> Function<A, C> compose(Function<B, C> f, Function<A, B> g) {
        return a -> f.apply(g.apply(a));
    }

    // Functional data structures - List
    public sealed interface List<A> permits Nil, Cons {
        boolean isEmpty();

        @SuppressWarnings("unchecked")
        static <A> List<A> of(A... xs) {
            List<A> result = nil();
            for (int i = xs.length - 1; i >= 0; i--) {
                result = new Cons<>(xs[i], result);
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        static <A> List<A> nil() {
            return (List<A>) Nil.INSTANCE;
        }

        static <A> List<A> empty() {
            return nil();
        }
    }

    public record Nil<A>() implements List<A> {
        private static final Nil<?> INSTANCE = new Nil<>();

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public String toString() {
            return "Nil";
        }
    }

    public record Cons<A>(A head, List<A> tail) implements List<A> {
        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public String toString() {
            return "Cons(" + head + ", " + tail + ")";
        }
    }

    // List operations
    public static int sum(List<Integer> xs) {
        return switch (xs) {
            case Nil<Integer> nil -> 0;
            case Cons<Integer> cons -> cons.head() + sum(cons.tail());
        };
    }

    public static <A> List<A> tail(List<A> xs) {
        return switch (xs) {
            case Cons<A> cons -> cons.tail();
            case Nil<A> nil -> List.nil();
        };
    }

    public static <A> List<A> setHead(List<A> xs, A x) {
        return switch (xs) {
            case Nil<A> nil -> List.nil();
            case Cons<A> cons -> new Cons<>(x, cons.tail());
        };
    }

    public static <A> List<A> prepend(List<A> xs, A x) {
        return switch (xs) {
            case Cons<A> cons -> new Cons<>(x, xs);
            case Nil<A> nil -> List.nil();
        };
    }

    public static <A> List<A> append(List<A> xs1, List<A> xs2) {
        return switch (xs1) {
            case Nil<A> nil -> xs2;
            case Cons<A> cons -> new Cons<>(cons.head(), append(cons.tail(), xs2));
        };
    }

    public static <A> List<A> drop(List<A> xs, int n) {
        if (n <= 0) return xs;
        return switch (xs) {
            case Cons<A> cons -> drop(cons.tail(), n - 1);
            case Nil<A> nil -> List.nil();
        };
    }

    public static <A> List<A> dropWhile(List<A> xs, Predicate<A> predicate) {
        return switch (xs) {
            case Cons<A> cons when predicate.test(cons.head()) -> dropWhile(cons.tail(), predicate);
            default -> xs;
        };
    }

    public static <A, B> B foldRight(List<A> xs, B value, BiFunction<A, B, B> f) {
        return switch (xs) {
            case Nil<A> nil -> value;
            case Cons<A> cons -> f.apply(cons.head(), foldRight(cons.tail(), value, f));
        };
    }

    public static <A, B> B foldLeft(List<A> xs, B value, BiFunction<B, A, B> f) {
        return switch (xs) {
            case Nil<A> nil -> value;
            case Cons<A> cons -> foldLeft(cons.tail(), f.apply(value, cons.head()), f);
        };
    }

    public static <A, B> List<B> map(List<A> xs, Function<A, B> f) {
        return foldRight(xs, List.<B>empty(), (a, bs) -> new Cons<>(f.apply(a), bs));
    }

    public static <A> List<A> filter(List<A> xs, Predicate<A> f) {
        return foldRight(xs, List.<A>empty(), (a, ls) ->
                f.test(a) ? new Cons<>(a, ls) : ls
        );
    }

    public static <A, B> List<B> flatMap(List<A> xa, Function<A, List<B>> f) {
        return foldRight(xa, List.<B>empty(), (a, lb) -> append(f.apply(a), lb));
    }

    public static <A> List<A> reverse(List<A> xs) {
        return foldLeft(xs, List.<A>empty(), (t, h) -> new Cons<>(h, t));
    }

    // Tree data structure
    public sealed interface Tree<A> permits Leaf, Branch {}

    public record Leaf<A>(A value) implements Tree<A> {}

    public record Branch<A>(Tree<A> left, Tree<A> right) implements Tree<A> {}

    public static <A> int numberOfNodes(Tree<A> tree) {
        return switch (tree) {
            case Leaf<A> leaf -> 1;
            case Branch<A> branch -> 1 + numberOfNodes(branch.left()) + numberOfNodes(branch.right());
        };
    }

    public static <A> int maxDepth(Tree<A> tree) {
        return switch (tree) {
            case Leaf<A> leaf -> 0;
            case Branch<A> branch -> 1 + Math.max(maxDepth(branch.left()), maxDepth(branch.right()));
        };
    }

    public static <A, B> Tree<B> map(Tree<A> tree, Function<A, B> f) {
        return switch (tree) {
            case Leaf<A> leaf -> new Leaf<>(f.apply(leaf.value()));
            case Branch<A> branch -> new Branch<>(map(branch.left(), f), map(branch.right(), f));
        };
    }

    // Option type
    public sealed interface Option<A> permits Some, None {
        boolean isPresent();

        A get();

        default <B> Option<B> map(Function<A, B> f) {
            return switch (this) {
                case Some<A> some -> new Some<>(f.apply(some.value()));
                case None<A> none -> none();
            };
        }

        default A getOrElse(Supplier<A> defaultValue) {
            return switch (this) {
                case Some<A> some -> some.value();
                case None<A> none -> defaultValue.get();
            };
        }

        default <B> Option<B> flatMap(Function<A, Option<B>> f) {
            return map(f).getOrElse(Option::none);
        }

        default Option<A> orElse(Supplier<Option<A>> ob) {
            return switch (this) {
                case Some<A> some -> this;
                case None<A> none -> ob.get();
            };
        }

        default Option<A> filter(Predicate<A> f) {
            return flatMap(a -> f.test(a) ? new Some<>(a) : none());
        }

        @SuppressWarnings("unchecked")
        static <A> Option<A> none() {
            return (Option<A>) None.INSTANCE;
        }

        static <A> Option<A> some(A value) {
            return new Some<>(value);
        }
    }

    public record Some<A>(A value) implements Option<A> {
        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public A get() {
            return value;
        }
    }

    public record None<A>() implements Option<A> {
        private static final None<?> INSTANCE = new None<>();

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public A get() {
            throw new NoSuchElementException("None.get");
        }
    }

    // Either type (using sealed classes and records)
    public sealed interface Either<E, A> permits Left, Right {
        boolean isRight();

        default <B> Either<E, B> map(Function<A, B> f) {
            return switch (this) {
                case Right<E, A> right -> new Right<>(f.apply(right.value()));
                case Left<E, A> left -> new Left<>(left.value());
            };
        }

        default Either<E, A> orElse(Supplier<Either<E, A>> f) {
            return switch (this) {
                case Left<E, A> left -> f.get();
                case Right<E, A> right -> this;
            };
        }
    }

    public record Left<E, A>(E value) implements Either<E, A> {
        @Override
        public boolean isRight() {
            return false;
        }
    }

    public record Right<E, A>(A value) implements Either<E, A> {
        @Override
        public boolean isRight() {
            return true;
        }
    }

    public static Either<String, Integer> safeDiv(int x, int y) {
        try {
            return new Right<>(x / y);
        } catch (Exception e) {
            return new Left<>("Division by zero");
        }
    }

    // Try type (for exception handling)
    public sealed interface Try<A> permits Success, Failure {
        boolean isSuccess();

        default <B> Try<B> map(Function<A, B> f) {
            return switch (this) {
                case Success<A> success -> Try.of(() -> f.apply(success.value()));
                case Failure<A> failure -> new Failure<>(failure.exception());
            };
        }

        default <B> Try<B> flatMap(Function<A, Try<B>> f) {
            return switch (this) {
                case Success<A> success -> {
                    try {
                        yield f.apply(success.value());
                    } catch (Exception e) {
                        yield new Failure<>(e);
                    }
                }
                case Failure<A> failure -> new Failure<>(failure.exception());
            };
        }

        default A getOrElse(Supplier<A> defaultValue) {
            return switch (this) {
                case Success<A> success -> success.value();
                case Failure<A> failure -> defaultValue.get();
            };
        }

        default Try<A> orElse(Supplier<Try<A>> alternative) {
            return switch (this) {
                case Success<A> success -> this;
                case Failure<A> failure -> alternative.get();
            };
        }

        default Try<A> recover(Function<Exception, A> f) {
            return switch (this) {
                case Success<A> success -> this;
                case Failure<A> failure -> new Success<>(f.apply(failure.exception()));
            };
        }

        default Try<A> recoverWith(Function<Exception, Try<A>> f) {
            return switch (this) {
                case Success<A> success -> this;
                case Failure<A> failure -> f.apply(failure.exception());
            };
        }

        default Either<Exception, A> toEither() {
            return switch (this) {
                case Success<A> success -> new Right<>(success.value());
                case Failure<A> failure -> new Left<>(failure.exception());
            };
        }

        static <A> Try<A> of(Supplier<A> supplier) {
            try {
                return new Success<>(supplier.get());
            } catch (Exception e) {
                return new Failure<>(e);
            }
        }

        static <A> Try<A> success(A value) {
            return new Success<>(value);
        }

        static <A> Try<A> failure(Exception exception) {
            return new Failure<>(exception);
        }
    }

    public record Success<A>(A value) implements Try<A> {
        @Override
        public boolean isSuccess() {
            return true;
        }
    }

    public record Failure<A>(Exception exception) implements Try<A> {
        @Override
        public boolean isSuccess() {
            return false;
        }
    }

    // Validation type (for accumulating errors)
    public sealed interface Validation<E, A> permits Valid, Invalid {
        boolean isValid();

        default <B> Validation<E, B> map(Function<A, B> f) {
            return switch (this) {
                case Valid<E, A> valid -> new Valid<>(f.apply(valid.value()));
                case Invalid<E, A> invalid -> new Invalid<>(invalid.errors());
            };
        }

        default <B> Validation<E, B> flatMap(Function<A, Validation<E, B>> f) {
            return switch (this) {
                case Valid<E, A> valid -> f.apply(valid.value());
                case Invalid<E, A> invalid -> new Invalid<>(invalid.errors());
            };
        }

        default A getOrElse(Supplier<A> defaultValue) {
            return switch (this) {
                case Valid<E, A> valid -> valid.value();
                case Invalid<E, A> invalid -> defaultValue.get();
            };
        }

        default <B, C> Validation<E, C> map2(Validation<E, B> vb, BiFunction<A, B, C> f) {
            return switch (this) {
                case Valid<E, A> va -> switch (vb) {
                    case Valid<E, B> vbValid -> new Valid<>(f.apply(va.value(), vbValid.value()));
                    case Invalid<E, B> vbInvalid -> new Invalid<>(vbInvalid.errors());
                };
                case Invalid<E, A> ia -> switch (vb) {
                    case Valid<E, B> vbValid -> new Invalid<>(ia.errors());
                    case Invalid<E, B> vbInvalid -> {
                        List<E> combined = append(ia.errors(), vbInvalid.errors());
                        yield new Invalid<>(combined);
                    }
                };
            };
        }

        default Either<List<E>, A> toEither() {
            return switch (this) {
                case Valid<E, A> valid -> new Right<>(valid.value());
                case Invalid<E, A> invalid -> new Left<>(invalid.errors());
            };
        }

        static <E, A> Validation<E, A> valid(A value) {
            return new Valid<>(value);
        }

        static <E, A> Validation<E, A> invalid(E error) {
            return new Invalid<>(List.of(error));
        }

        static <E, A> Validation<E, A> invalid(List<E> errors) {
            return new Invalid<>(errors);
        }
    }

    public record Valid<E, A>(A value) implements Validation<E, A> {
        @Override
        public boolean isValid() {
            return true;
        }
    }

    public record Invalid<E, A>(List<E> errors) implements Validation<E, A> {
        @Override
        public boolean isValid() {
            return false;
        }
    }

    // Stream (lazy list)
    public static abstract class Stream<A> {
        public abstract Option<Pair<A, Stream<A>>> uncons();

        public static <A> Stream<A> of(A... xs) {
            if (xs.length == 0) return empty();
            return cons(() -> xs[0], () -> {
                A[] tail = Arrays.copyOfRange(xs, 1, xs.length);
                return of(tail);
            });
        }

        public static <A> Stream<A> cons(Supplier<A> hd, Supplier<Stream<A>> tl) {
            return new Stream<A>() {
                private Option<Pair<A, Stream<A>>> cache = null;

                @Override
                public Option<Pair<A, Stream<A>>> uncons() {
                    if (cache == null) {
                        cache = Option.some(new Pair<>(hd.get(), tl.get()));
                    }
                    return cache;
                }
            };
        }

        public static <A> Stream<A> empty() {
            return new Stream<A>() {
                @Override
                public Option<Pair<A, Stream<A>>> uncons() {
                    return Option.none();
                }
            };
        }

        public Option<A> head() {
            return uncons().map(pair -> pair.first());
        }

        public List<A> toList() {
            return toListHelper(this, List.nil());
        }

        private static <A> List<A> toListHelper(Stream<A> stream, List<A> acc) {
            return switch (stream.uncons()) {
                case Some<Pair<A, Stream<A>>> some -> {
                    Pair<A, Stream<A>> pair = some.value();
                    yield toListHelper(pair.second(), new Cons<>(pair.first(), acc));
                }
                case None<Pair<A, Stream<A>>> none -> reverse(acc);
            };
        }

        public Stream<A> take(int n) {
            if (n <= 0) return empty();
            return uncons()
                    .map(pair -> cons(() -> pair.first(), () -> pair.second().take(n - 1)))
                    .getOrElse(Stream::empty);
        }

        public Stream<A> drop(int n) {
            Stream<A> xs = this;
            int remaining = n;
            while (remaining > 0 && xs.uncons().isPresent()) {
                xs = ((Some<Pair<A, Stream<A>>>) xs.uncons()).value().second();
                remaining--;
            }
            return xs;
        }

        public <B> Stream<B> map(Function<A, B> f) {
            return uncons()
                    .map(pair -> cons(() -> f.apply(pair.first()), () -> pair.second().map(f)))
                    .getOrElse(Stream::empty);
        }

        public Stream<A> filter(Predicate<A> p) {
            return uncons()
                    .map(pair -> {
                        if (p.test(pair.first())) {
                            return cons(() -> pair.first(), () -> pair.second().filter(p));
                        } else {
                            return pair.second().filter(p);
                        }
                    })
                    .getOrElse(Stream::empty);
        }
    }

    // Infinite streams
    public static Stream<Integer> ones() {
        return Stream.cons(() -> 1, FunctionalProgramming::ones);
    }

    public static <A> Stream<A> constant(A a) {
        return Stream.cons(() -> a, () -> constant(a));
    }

    public static Stream<Integer> from(int n) {
        return Stream.cons(() -> n, () -> from(n + 1));
    }

    public static Stream<Integer> fibonacci() {
        return fibonacciHelper(0, 1);
    }

    private static Stream<Integer> fibonacciHelper(int curr, int next) {
        return Stream.cons(() -> curr, () -> fibonacciHelper(next, curr + next));
    }

    // State
    public record State<S, A>(Function<S, Pair<A, S>> run) {
        public static <S, A> State<S, A> unit(A a) {
            return new State<>(s -> new Pair<>(a, s));
        }

        public <B> State<S, B> map(Function<A, B> f) {
            return flatMap(a -> unit(f.apply(a)));
        }

        public <B> State<S, B> flatMap(Function<A, State<S, B>> f) {
            return new State<>(s -> {
                Pair<A, S> pair = this.run.apply(s);
                return f.apply(pair.first()).run.apply(pair.second());
            });
        }
    }

    // Random number generation
    public interface Rnd {
        Pair<Integer, Rnd> nextInt();
    }

    public record SimpleRandom(long seed) implements Rnd {
        @Override
        public Pair<Integer, Rnd> nextInt() {
            long newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL;
            SimpleRandom nextRNG = new SimpleRandom(newSeed);
            int n = (int) (newSeed >>> 16);
            return new Pair<>(n, nextRNG);
        }
    }

    // Monoid
    public interface Semigroup<A> {
        A combine(A a1, A a2);
    }

    public interface Monoid<A> extends Semigroup<A> {
        A nil();
    }

    public static final Monoid<String> stringMonoid = new Monoid<>() {
        @Override
        public String combine(String a1, String a2) {
            return a1 + a2;
        }

        @Override
        public String nil() {
            return "";
        }
    };

    // IO Monad
    @FunctionalInterface
    public interface IO<A> {
        A run();

        default <B> IO<B> map(Function<A, B> f) {
            return () -> f.apply(this.run());
        }

        default <B> IO<B> flatMap(Function<A, IO<B>> f) {
            return () -> f.apply(this.run()).run();
        }

        default <B> IO<Pair<A, B>> combine(IO<B> io) {
            return () -> new Pair<>(this.run(), io.run());
        }
    }

    public static IO<String> read() {
        return () -> new Scanner(System.in).nextLine();
    }

    public static IO<Void> write(String text) {
        return () -> {
            System.out.println(text);
            return null;
        };
    }

    // Utility classes
    public record Pair<A, B>(A first, B second) {
        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }
    }

    // Example usage
    public static void main(String[] args) {

        // IO example
        var tempConverter = write("Enter temperature in degrees Fahrenheit: ")
                .flatMap(v -> read())
                .map(FunctionalProgramming::parseDouble)
                .map(FunctionalProgramming::toCelsius)
                .map(FunctionalProgramming::formatTemperature)
                .map(FunctionalProgramming::formatResult)
                .flatMap(FunctionalProgramming::write);

        // Uncomment to run the IO program
        tempConverter.run();
    }

    static Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    static Double toCelsius(Double value) {
        return (value - 32) * 5.0 / 9.0;
    }

    static String formatTemperature(double temperature) {
        return String.format("%.2f", temperature);
    }

    static String formatResult(String temperature) {
        return "Temperature is equal " + temperature + "°C";
    }

    // Validation helper functions
    static Validation<String, String> validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Validation.invalid("Name cannot be empty");
        }
        if (name.length() < 2) {
            return Validation.invalid("Name must be at least 2 characters");
        }
        return Validation.valid(name);
    }

    static Validation<String, Integer> validateAge(int age) {
        if (age < 0) {
            return Validation.invalid("Age cannot be negative");
        }
        if (age > 150) {
            return Validation.invalid("Age must be less than 150");
        }
        return Validation.valid(age);
    }

    static Validation<String, String> validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            return Validation.invalid("Email must contain @");
        }
        if (!email.contains(".")) {
            return Validation.invalid("Email must contain a domain");
        }
        return Validation.valid(email);
    }

    static <A> void printList(List<A> list) {
        if (list instanceof Nil) {
            System.out.println();
            return;
        }
        Cons<A> cons = (Cons<A>) list;
        System.out.print(cons.head());
        if (!(cons.tail() instanceof Nil)) {
            System.out.print(", ");
        }
        printList(cons.tail());
    }
}