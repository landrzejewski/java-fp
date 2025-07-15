package pl.training;

import pl.training.exercises.FunctionalProgramming;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
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
