package pl.training;

import java.util.*;
import java.util.List;
import java.util.function.*;

import static pl.training.FunctionalProgramming.*;

public class ParserCombinators {

    // Parser is a function from String to Result
    @FunctionalInterface
    public interface Parser<T> extends Function<String, Result<T>> {
    }

    // Result type for parser results
    public sealed interface Result<T> permits Success, Failure {

        default <U> Result<U> map(Function<T, U> f) {
            return switch (this) {
                case Failure<T> failure -> new Failure<>(failure.expected(), failure.remainder());
                case Success<T> success -> new Success<>(f.apply(success.value()), success.remainder());
            };
        }

        default <U> Result<U> flatMap(BiFunction<T, String, Result<U>> f) {
            return switch (this) {
                case Failure<T> failure -> new Failure<>(failure.expected(), failure.remainder());
                case Success<T> success -> f.apply(success.value(), success.remainder());
            };
        }

        default Result<T> mapExpected(Function<String, String> f) {
            return switch (this) {
                case Success<T> success -> success;
                case Failure<T> failure -> new Failure<>(f.apply(failure.expected()), failure.remainder());
            };
        }
    }

    public record Success<T>(T value, String remainder) implements Result<T> {
    }

    public record Failure<T>(String expected, String remainder) implements Result<T> {
    }

    // Basic parsers

    /**
     * Parses a specific prefix string
     */
    public static Parser<String> prefix(String prefixValue) {
        return input -> {
            if (input.startsWith(prefixValue)) {
                return new Success<>(prefixValue, input.substring(prefixValue.length()));
            } else {
                return new Failure<>("Expected: \"" + prefixValue + "\" prefix", input);
            }
        };
    }

    /**
     * Parses an integer value
     */
    public static Parser<Integer> intParser() {
        return input -> {
            int i = 0;
            while (i < input.length() && Character.isDigit(input.charAt(i))) {
                i++;
            }
            if (i > 0) {
                String match = input.substring(0, i);
                return new Success<>(Integer.parseInt(match), input.substring(i));
            } else {
                return new Failure<>("Expected: integer value", input);
            }
        };
    }

    /**
     * Parses one or more whitespace characters
     */
    public static Parser<String> whitespace() {
        return input -> {
            int i = 0;
            while (i < input.length() && Character.isWhitespace(input.charAt(i))) {
                i++;
            }
            if (i > 0) {
                return new Success<>(input.substring(0, i), input.substring(i));
            } else {
                return new Failure<>("Expected: one or more whitespace", input);
            }
        };
    }

    /**
     * Parses characters matching a predicate
     */
    public static Parser<String> prefixWhile(Predicate<Character> predicate) {
        return input -> {
            int i = 0;
            while (i < input.length() && predicate.test(input.charAt(i))) {
                i++;
            }
            if (i > 0) {
                return new Success<>(input.substring(0, i), input.substring(i));
            } else {
                return new Failure<>("Expected: prefix matching predicate", input);
            }
        };
    }

    // Parser combinators

    /**
     * Sequences two parsers - applies first, then second
     */
    public static <T1, T2> Parser<Pair<T1, T2>> sequence(Parser<T1> firstParser, Parser<T2> secondParser) {
        return input -> firstParser.apply(input).flatMap((firstResult, remainder) ->
                secondParser.apply(remainder).map(secondResult -> new Pair<>(firstResult, secondResult))
        );
    }

    /**
     * Infix version of sequence using helper class
     */
    public static <T1, T2> Parser<Pair<T1, T2>> then(Parser<T1> first, Parser<T2> second) {
        return sequence(first, second);
    }

    /**
     * Tries first parser, if it fails tries second parser
     */
    public static <T> Parser<T> oneOf(Parser<T> firstParser, Parser<T> secondParser) {
        return input -> {
            Result<T> firstResult = firstParser.apply(input);
            return switch (firstResult) {
                case Success<T> success -> success;
                case Failure<T> failure -> secondParser.apply(input)
                        .mapExpected(expected -> failure.expected() + ", " + expected);
            };
        };
    }

    /**
     * Infix version of oneOf
     */
    public static <T> Parser<T> or(Parser<T> first, Parser<T> second) {
        return oneOf(first, second);
    }

    /**
     * Maps the result of a parser
     */
    public static <T, U> Parser<U> map(Parser<T> parser, Function<T, U> f) {
        return input -> parser.apply(input).map(f);
    }

    /**
     * Skips the left parser result, keeps the right
     */
    public static <X, T> Parser<T> skipLeft(Parser<X> left, Parser<T> right) {
        return map(sequence(left, right), pair -> pair.second());
    }

    /**
     * Skips the right parser result, keeps the left
     */
    public static <T, Y> Parser<T> skipRight(Parser<T> left, Parser<Y> right) {
        return map(sequence(left, right), pair -> pair.first());
    }

    /**
     * Applies parser zero or more times
     */
    public static <T> Parser<List<T>> many(Parser<T> parser) {
        return input -> {
            Result<T> result = parser.apply(input);
            return switch (result) {
                case Failure<T> failure -> new Success<>(new ArrayList<>(), input);
                case Success<T> success -> {
                    Result<List<T>> rest = many(parser).apply(success.remainder());
                    yield rest.map(list -> {
                        List<T> newList = new ArrayList<>();
                        newList.add(success.value());
                        newList.addAll(list);
                        return newList;
                    });
                }
            };
        };
    }

    /**
     * Parses values separated by a separator parser
     */
    public static <T, X> Parser<List<T>> separatedBy(Parser<T> parser, Parser<X> separatorParser) {
        return input -> {
            Function<String, Result<List<T>>> parseRest = new Function<>() {
                @Override
                public Result<List<T>> apply(String tail) {
                    Result<X> separatorResult = separatorParser.apply(tail);
                    return switch (separatorResult) {
                        case Failure<X> failure -> new Success<>(new ArrayList<>(), tail);
                        case Success<X> sepSuccess -> {
                            Result<T> itemResult = parser.apply(sepSuccess.remainder());
                            yield switch (itemResult) {
                                case Failure<T> itemFailure -> itemFailure.map(x -> new ArrayList<T>());
                                case Success<T> itemSuccess -> apply(itemSuccess.remainder()).map(list -> {
                                    List<T> newList = new ArrayList<>();
                                    newList.add(itemSuccess.value());
                                    newList.addAll(list);
                                    return newList;
                                });
                            };
                        }
                    };
                }
            };

            Result<T> firstResult = parser.apply(input);
            return switch (firstResult) {
                case Failure<T> failure -> new Success<>(new ArrayList<>(), input);
                case Success<T> success -> parseRest.apply(success.remainder()).map(list -> {
                    List<T> newList = new ArrayList<>();
                    newList.add(success.value());
                    newList.addAll(list);
                    return newList;
                });
            };
        };
    }

    // Utility functions for building parsers

    /**
     * Helper to create parsers that parse letters
     */
    public static Parser<String> letters() {
        return prefixWhile(Character::isLetter);
    }

    /**
     * Helper to create parsers that parse digits
     */
    public static Parser<String> digits() {
        return prefixWhile(Character::isDigit);
    }

    // Example: Parsing a simple variable assignment like "let x = [1, 2, 3]"
    public record VariableAssignment(String variableName, List<Integer> values) {

        @Override
        public String toString() {
            return "VariableAssignment{" +
                    "variableName='" + variableName + '\'' +
                    ", values=" + values +
                    '}';
        }
    }

    /**
     * Parser for variable assignments: "let variableName = [1, 2, 3]"
     */
    public static Parser<VariableAssignment> variableAssignmentParser() {
        Parser<String> manySpaces = input ->
                many(whitespace()).apply(input).map(list -> String.join("", list));

        // "let" followed by whitespace
        Parser<Pair<String, String>> letKeyword = then(prefix("let"), whitespace());

        // Variable name (letters) followed by optional spaces
        Parser<String> variableName = skipRight(letters(), manySpaces);

        // "=" followed by optional spaces
        Parser<String> assignment = skipRight(prefix("="), manySpaces);

        // Numbers separated by comma and optional spaces
        Parser<List<Integer>> numbers = separatedBy(
                intParser(),
                then(prefix(","), manySpaces)
        );

        // Array: "[" numbers "]"
        Parser<List<Integer>> array = skipRight(
                skipLeft(prefix("["), numbers),
                prefix("]")
        );

        // Complete parser
        Parser<Pair<String, List<Integer>>> parser = then(
                skipRight(skipLeft(letKeyword, variableName), assignment),
                array
        );

        return map(parser, pair -> new VariableAssignment(pair.first(), pair.second()));
    }

    public static void main(String[] args) {
        System.out.println("=== Parser Combinators Examples ===\n");

        // 1. Prefix parser
        System.out.println("1. Prefix Parser:");
        Parser<String> prefixParser = prefix("*  ");
        System.out.println(prefixParser.apply("*  jan"));  // Success
        System.out.println(prefixParser.apply("jan"));     // Failure
        System.out.println();

        // 2. Integer parser
        System.out.println("2. Integer Parser:");
        Parser<Integer> intP = intParser();
        System.out.println(intP.apply("1234jan"));  // Success(1234, "jan")
        System.out.println(intP.apply("jan"));      // Failure
        System.out.println();

        // 3. Whitespace parser
        System.out.println("3. Whitespace Parser:");
        Parser<String> wsParser = whitespace();
        System.out.println(wsParser.apply("   jan"));  // Success
        System.out.println(wsParser.apply("jan"));     // Failure
        System.out.println();

        // 4. Sequence parser
        System.out.println("4. Sequence Parser:");
        Parser<Pair<String, Integer>> seqParser = sequence(prefix("-"), intParser());
        System.out.println(seqParser.apply("-123jan"));  // Success(("-", 123), "jan")
        System.out.println(seqParser.apply("123jan"));   // Failure
        System.out.println();

        // 5. OneOf parser
        System.out.println("5. OneOf Parser:");
        Parser<String> oneOfParser = oneOf(prefix("a"), prefix("b"));
        System.out.println(oneOfParser.apply("ab"));  // Success("a", "b")
        System.out.println(oneOfParser.apply("bc"));  // Success("b", "c")
        System.out.println(oneOfParser.apply("cd"));  // Failure
        System.out.println();

        // 6. Map parser
        System.out.println("6. Map Parser:");
        Parser<Boolean> evenParser = map(intParser(), n -> n % 2 == 0);
        System.out.println(evenParser.apply("11"));  // Success(false, "")
        System.out.println(evenParser.apply("12"));  // Success(true, "")
        System.out.println();

        // 7. Skip left
        System.out.println("7. Skip Left:");
        Parser<String> skipLeftParser = skipLeft(intParser(), prefix("a"));
        System.out.println(skipLeftParser.apply("1a"));  // Success("a", "")
        System.out.println();

        // 8. Skip right
        System.out.println("8. Skip Right:");
        Parser<String> skipRightParser = skipRight(prefix("a"), intParser());
        System.out.println(skipRightParser.apply("a1"));  // Success("a", "")
        System.out.println();

        // 9. Many parser
        System.out.println("9. Many Parser:");
        Parser<List<String>> manyParser = many(prefix("a"));
        System.out.println(manyParser.apply("aaa"));  // Success([a, a, a], "")
        System.out.println(manyParser.apply("aaab")); // Success([a, a, a], "b")
        System.out.println();

        // 10. SeparatedBy parser
        System.out.println("10. SeparatedBy Parser:");
        Parser<List<Integer>> separatedParser = separatedBy(intParser(), prefix(","));
        System.out.println(separatedParser.apply("1,2,3"));  // Success([1, 2, 3], "")
        System.out.println(separatedParser.apply("1,2"));    // Success([1, 2], "")
        System.out.println(separatedParser.apply("a"));      // Success([], "a")
        System.out.println();

        // 11. Complex example - Variable assignment
        System.out.println("11. Variable Assignment Parser:");
        String text = "let  ab = [1, 2, 3,  4]";
        Parser<VariableAssignment> varParser = variableAssignmentParser();
        Result<VariableAssignment> result = varParser.apply(text);

        switch (result) {
            case Success<VariableAssignment> success -> System.out.println("Parsed: " + success.value());
            case Failure<VariableAssignment> failure -> System.out.println("Failed: " + failure.expected());
        }
        System.out.println();

        // 12. JSON-like array parser
        System.out.println("12. JSON-like Array Parser:");
        Parser<List<Integer>> jsonArrayParser = skipRight(
                skipLeft(prefix("["), separatedBy(intParser(), sequence(prefix(","), whitespace()))),
                prefix("]")
        );
        System.out.println(jsonArrayParser.apply("[1, 2, 3]"));
        System.out.println();

        // 13. Key-value pair parser
        System.out.println("13. Key-Value Parser:");
        Parser<Pair<String, String>> kvParser = sequence(
                skipRight(letters(), prefix(":")),
                letters()
        );
        System.out.println(kvParser.apply("firstName:Jan"));
        System.out.println();
    }
}