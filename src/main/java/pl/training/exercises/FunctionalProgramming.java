package pl.training.exercises;

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

    // Functional data structures

    // Immutable List
    public static abstract class List<A> {
        public abstract boolean isEmpty();

        public static <A> List<A> of(A... xs) {
            List<A> result = nil();
            for (int i = xs.length - 1; i >= 0; i--) {
                result = new Cons<>(xs[i], result);
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        public static <A> List<A> nil() {
            return (List<A>) Nil.INSTANCE;
        }

        public static <A> List<A> empty() {
            return nil();
        }
    }

    public static class Nil<A> extends List<A> {
        private static final Nil<?> INSTANCE = new Nil<>();

        private Nil() {}

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public String toString() {
            return "Nil";
        }
    }

    public static class Cons<A> extends List<A> {
        public final A head;
        public final List<A> tail;

        public Cons(A head, List<A> tail) {
            this.head = head;
            this.tail = tail;
        }

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
        if (xs instanceof Nil) return 0;
        Cons<Integer> cons = (Cons<Integer>) xs;
        return cons.head + sum(cons.tail);
    }

    public static double product(List<Double> xs) {
        if (xs instanceof Nil) return 1.0;
        Cons<Double> cons = (Cons<Double>) xs;
        return cons.head * product(cons.tail);
    }

    public static <A> List<A> tail(List<A> xs) {
        if (xs instanceof Cons) {
            return ((Cons<A>) xs).tail;
        }
        return List.nil();
    }

    public static <A> List<A> setHead(List<A> xs, A x) {
        if (xs instanceof Nil) return List.nil();
        Cons<A> cons = (Cons<A>) xs;
        return new Cons<>(x, cons.tail);
    }

    public static <A> List<A> prepend(List<A> xs, A x) {
        if (xs instanceof Cons) return new Cons<>(x, xs);
        return List.nil();
    }

    public static <A> List<A> append(List<A> xs1, List<A> xs2) {
        if (xs1 instanceof Nil) return xs2;
        Cons<A> cons = (Cons<A>) xs1;
        return new Cons<>(cons.head, append(cons.tail, xs2));
    }

    public static <A> List<A> drop(List<A> xs, int n) {
        if (n <= 0) return xs;
        if (xs instanceof Cons) {
            return drop(((Cons<A>) xs).tail, n - 1);
        }
        return List.nil();
    }

    public static <A> List<A> dropWhile(List<A> xs, Predicate<A> predicate) {
        if (xs instanceof Cons) {
            Cons<A> cons = (Cons<A>) xs;
            if (predicate.test(cons.head)) {
                return dropWhile(cons.tail, predicate);
            }
        }
        return xs;
    }

    public static <A, B> B foldRight(List<A> xs, B value, BiFunction<A, B, B> f) {
        if (xs instanceof Nil) return value;
        Cons<A> cons = (Cons<A>) xs;
        return f.apply(cons.head, foldRight(cons.tail, value, f));
    }

    public static <A, B> B foldLeft(List<A> xs, B value, BiFunction<B, A, B> f) {
        if (xs instanceof Nil) return value;
        Cons<A> cons = (Cons<A>) xs;
        return foldLeft(cons.tail, f.apply(value, cons.head), f);
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
    public static abstract class Tree<A> {}

    public static class Leaf<A> extends Tree<A> {
        public final A value;

        public Leaf(A value) {
            this.value = value;
        }
    }

    public static class Branch<A> extends Tree<A> {
        public final Tree<A> left;
        public final Tree<A> right;

        public Branch(Tree<A> left, Tree<A> right) {
            this.left = left;
            this.right = right;
        }
    }

    public static <A> int numberOfNodes(Tree<A> tree) {
        if (tree instanceof Leaf) return 1;
        Branch<A> branch = (Branch<A>) tree;
        return 1 + numberOfNodes(branch.left) + numberOfNodes(branch.right);
    }

    public static <A> int maxDepth(Tree<A> tree) {
        if (tree instanceof Leaf) return 0;
        Branch<A> branch = (Branch<A>) tree;
        return 1 + Math.max(maxDepth(branch.left), maxDepth(branch.right));
    }

    public static <A, B> Tree<B> map(Tree<A> tree, Function<A, B> f) {
        if (tree instanceof Leaf) {
            return new Leaf<>(f.apply(((Leaf<A>) tree).value));
        }
        Branch<A> branch = (Branch<A>) tree;
        return new Branch<>(map(branch.left, f), map(branch.right, f));
    }

    // Option type
    public static abstract class Option<A> {
        public abstract boolean isPresent();

        public abstract A get();

        public <B> Option<B> map(Function<A, B> f) {
            if (this instanceof Some) {
                return new Some<>(f.apply(((Some<A>) this).value));
            }
            return none();
        }

        public A getOrElse(Supplier<A> defaultValue) {
            if (this instanceof Some) {
                return ((Some<A>) this).value;
            }
            return defaultValue.get();
        }

        public <B> Option<B> flatMap(Function<A, Option<B>> f) {
            return map(f).getOrElse(Option::none);
        }

        public Option<A> orElse(Supplier<Option<A>> ob) {
            if (this instanceof Some) {
                return this;
            }
            return ob.get();
        }

        public Option<A> filter(Predicate<A> f) {
            return flatMap(a -> f.test(a) ? new Some<>(a) : none());
        }

        @SuppressWarnings("unchecked")
        public static <A> Option<A> none() {
            return (Option<A>) None.INSTANCE;
        }

        public static <A> Option<A> some(A value) {
            return new Some<>(value);
        }
    }

    public static class Some<A> extends Option<A> {
        public final A value;

        public Some(A value) {
            this.value = value;
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public A get() {
            return value;
        }
    }

    public static class None<A> extends Option<A> {
        private static final None<?> INSTANCE = new None<>();

        private None() {}

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public A get() {
            throw new NoSuchElementException("None.get");
        }
    }

    // Either type
    public static abstract class Either<E, A> {
        public abstract boolean isRight();

        public <B> Either<E, B> map(Function<A, B> f) {
            if (this instanceof Right) {
                return new Right<>(f.apply(((Right<E, A>) this).value));
            }
            return (Left<E, B>) this;
        }

        public Either<E, A> orElse(Supplier<Either<E, A>> f) {
            if (this instanceof Left) {
                return f.get();
            }
            return this;
        }
    }

    public static class Left<E, A> extends Either<E, A> {
        public final E value;

        public Left(E value) {
            this.value = value;
        }

        @Override
        public boolean isRight() {
            return false;
        }
    }

    public static class Right<E, A> extends Either<E, A> {
        public final A value;

        public Right(A value) {
            this.value = value;
        }

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
            return uncons().map(pair -> pair.first);
        }

        public List<A> toList() {
            return toListHelper(this, List.nil());
        }

        private static <A> List<A> toListHelper(Stream<A> stream, List<A> acc) {
            Option<Pair<A, Stream<A>>> unconsed = stream.uncons();
            if (unconsed instanceof Some) {
                Pair<A, Stream<A>> pair = ((Some<Pair<A, Stream<A>>>) unconsed).value;
                return toListHelper(pair.second, new Cons<>(pair.first, acc));
            }
            return reverse(acc);
        }

        public Stream<A> take(int n) {
            if (n <= 0) return empty();
            return uncons()
                    .map(pair -> cons(() -> pair.first, () -> pair.second.take(n - 1)))
                    .getOrElse(Stream::empty);
        }

        public Stream<A> drop(int n) {
            Stream<A> xs = this;
            while (n > 0 && xs.uncons().isPresent()) {
                xs = ((Some<Pair<A, Stream<A>>>) xs.uncons()).value.second;
                n--;
            }
            return xs;
        }

        public <B> Stream<B> map(Function<A, B> f) {
            return uncons()
                    .map(pair -> cons(() -> f.apply(pair.first), () -> pair.second.map(f)))
                    .getOrElse(Stream::empty);
        }

        public Stream<A> filter(Predicate<A> p) {
            return uncons()
                    .map(pair -> {
                        if (p.test(pair.first)) {
                            return cons(() -> pair.first, () -> pair.second.filter(p));
                        } else {
                            return pair.second.filter(p);
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
    public static class State<S, A> {
        public final Function<S, Pair<A, S>> run;

        public State(Function<S, Pair<A, S>> run) {
            this.run = run;
        }

        public static <S, A> State<S, A> unit(A a) {
            return new State<>(s -> new Pair<>(a, s));
        }

        public <B> State<S, B> map(Function<A, B> f) {
            return flatMap(a -> unit(f.apply(a)));
        }

        public <B> State<S, B> flatMap(Function<A, State<S, B>> f) {
            return new State<>(s -> {
                Pair<A, S> pair = this.run.apply(s);
                return f.apply(pair.first).run.apply(pair.second);
            });
        }
    }

    // Random number generation
    public interface Rnd {
        Pair<Integer, Rnd> nextInt();
    }

    public static class SimpleRandom implements Rnd {
        private final long seed;

        public SimpleRandom(long seed) {
            this.seed = seed;
        }

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

    public static final Monoid<String> stringMonoid = new Monoid<String>() {
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
    public static class Pair<A, B> {
        public final A first;
        public final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }
    }

    // Example usage
    public static void main(String[] args) {
        // Pure functions example
        System.out.println("abs(-5) = " + abs(-5));

        // Currying example
        Function<Integer, Integer> add3 = add(3);
        System.out.println("add3(2) = " + add3.apply(2));

        // Recursion example
        System.out.println("factorial(5) = " + factorial(5));
        System.out.println("fibonacci(10) = " + fibonacci(10));

        // List example
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);
        System.out.println("sum = " + sum(numbers));

        List<Integer> doubled = map(numbers, x -> x * 2);
        System.out.println("doubled = " + doubled);

        // Option example
        Option<Integer> some = Option.some(42);
        Option<Integer> none = Option.none();

        System.out.println("some.map(x -> x * 2) = " + some.map(x -> x * 2).getOrElse(() -> 0));
        System.out.println("none.map(x -> x * 2) = " + none.map(x -> x * 2).getOrElse(() -> 0));

        // Either example
        Either<String, Integer> result = safeDiv(10, 2);
        System.out.println("10 / 2 = " + result.map(x -> x.toString()).orElse(() -> new Right<>("Error")).isRight());

        // Stream example
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5);
        System.out.println("stream.take(3).toList() = " + stream.take(3).toList());

        // Infinite stream example
        System.out.println("ones().take(5).toList() = " + ones().take(5).toList());
        System.out.println("fibonacci().take(10).toList() = " + fibonacci().take(10).toList());

        // IO example
        IO<Void> program = write("Enter temperature in degrees Fahrenheit: ")
                .flatMap(v -> read())
                .map(Double::parseDouble)
                .map(temp -> (temp - 32) * 5.0 / 9.0)
                .map(celsius -> String.format("%.2f", celsius))
                .map(temp -> "Temperature is equal " + temp + "°C")
                .flatMap(FunctionalProgramming::write);

        // Uncomment to run the IO program
        // program.run();
    }
}