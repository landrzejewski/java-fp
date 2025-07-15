package pl.training.project;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

record Product(String id, String name, Category category, Money price, int stock) {}
record Category(String id, String name, double taxRate) {}
record Money(BigDecimal amount, Currency currency) {
    // Helper method for calculations
    public Money multiply(BigDecimal factor) {
        return new Money(amount.multiply(factor), currency);
    }
    public Money add(Money other) {
        if (!currency.equals(other.currency)) throw new IllegalArgumentException("Currency mismatch");
        return new Money(amount.add(other.amount), currency);
    }
}
record Customer(String id, String name, CustomerType type, List<Address> addresses) {}
record Address(String street, String city, String country, String zipCode) {}
enum CustomerType { REGULAR, PREMIUM, VIP }

record OrderItem(String productId, int quantity) {}
record Order(String id, String customerId, List<OrderItem> items,
             Instant createdAt, PaymentMethod paymentMethod) {}
enum PaymentMethod { CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER }

// Result type for functional error handling - USE THIS INSTEAD OF EXCEPTIONS
sealed interface Result<T, E> {
    record Success<T, E>(T value) implements Result<T, E> {}
    record Failure<T, E>(E error) implements Result<T, E> {}

    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success(var value) -> new Success<>(mapper.apply(value));
            case Failure(var error) -> new Failure<>(error);
        };
    }

    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return switch (this) {
            case Success(var value) -> mapper.apply(value);
            case Failure(var error) -> new Failure<>(error);
        };
    }

    // Helper methods
    default T getOrElse(T defaultValue) {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var error) -> defaultValue;
        };
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }
}

// Validation types for accumulating errors

record ValidationError(String field, String message) {}

 record Validated<T>(T value, List<ValidationError> errors) {
    public boolean isValid() { return errors.isEmpty(); }
    public Optional<T> toOptional() { return isValid() ? Optional.of(value) : Optional.empty(); }

    // Combine with another validation
    public Validated<T> combine(Validated<T> other) {
        List<ValidationError> combinedErrors = new ArrayList<>(errors);
        combinedErrors.addAll(other.errors);
        return new Validated<>(value, combinedErrors);
    }
}

class InventoryService {
    private final Map<String, Product> products;

    public InventoryService(Map<String, Product> products) {
        this.products = new HashMap<>(products);
    }

    /**
     * Task 1.1: Find products by category with sufficient stock
     *
     * Requirements:
     * - Filter products that belong to the specified category
     * - Only include products with stock >= minStock
     * - Return as a Stream for further processing
     *
     * Hints:
     * - Use products.values().stream()
     * - Chain filter operations
     * - Compare product.category().id() with categoryId
     */
    public Stream<Product> findAvailableProductsByCategory(String categoryId, int minStock) {
        // TODO: Implement using streams
        // Example: findAvailableProductsByCategory("electronics", 10)
        //          should return all electronics with 10+ items in stock
        return Stream.empty();
    }

    /**
     * Task 1.2: Calculate total inventory value grouped by category
     *
     * Requirements:
     * - Group all products by their category
     * - For each category, sum: (product.price * product.stock)
     * - Handle the Money type correctly (use Money.multiply and Money.add)
     *
     * Hints:
     * - Use Collectors.groupingBy for grouping
     * - Use Collectors.reducing for summing Money values
     * - Remember: Money has a currency, ensure all products in a category use same currency
     */
    public Map<Category, Money> calculateInventoryValueByCategory() {
        // TODO: Implement using streams and collectors
        // Example result: {Category[Electronics] -> Money[1000 USD],
        //                  Category[Books] -> Money[500 USD]}
        return Map.of();
    }

    /**
     * Task 1.3: Check if all order items are available in stock
     *
     * Requirements:
     * - For each OrderItem, check if product exists and has sufficient stock
     * - Return Success with a map of productId -> availableQuantity if ALL items available
     * - Return Failure with list of unavailable items if ANY item is not available
     * - Use the Result pattern, not exceptions
     *
     * Hints:
     * - First collect all availability checks
     * - Use Stream.allMatch or partition results
     * - Build appropriate Success or Failure
     */
    public Result<Map<String, Integer>, List<String>> checkAvailability(List<OrderItem> items) {
        // TODO: Implement availability checking
        // Example Success: Result.Success({productA -> 50, productB -> 30})
        // Example Failure: Result.Failure(["productC: requested 10 but only 5 available"])
        return null;
    }

    /**
     * Task 1.4: Reserve items (create new InventoryService with updated stock)
     *
     * Requirements:
     * - DO NOT modify the current InventoryService
     * - Create a new InventoryService with reduced stock levels
     * - Validate all items are available before reserving
     * - Return Success with new InventoryService or Failure with error message
     * - This demonstrates immutable updates
     *
     * Hints:
     * - First use checkAvailability to validate
     * - Create new Product instances with updated stock
     * - Create new InventoryService with updated products map
     */
    public Result<InventoryService, String> reserveItems(List<OrderItem> items) {
        // TODO: Implement functional state update
        // This method shows how to handle "updates" in functional programming
        return null;
    }
}

public class Project {
}
