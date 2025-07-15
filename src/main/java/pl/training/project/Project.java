package pl.training.project;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toMap;

record Product(String id, String name, Category category, Money price, int stock) {

    public Product withStock(int stock) {
        return new Product(this.id, this.name, this.category, this.price, stock);
    }

}

record Category(String id, String name, double taxRate) {
}

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

record Customer(String id, String name, CustomerType type, List<Address> addresses) {
}

record Address(String street, String city, String country, String zipCode) {
}

enum CustomerType {REGULAR, PREMIUM, VIP}

record OrderItem(String productId, int quantity) {
}

record Order(String id, String customerId, List<OrderItem> items,
             Instant createdAt, PaymentMethod paymentMethod) {
}

enum PaymentMethod {CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER}

// Result type for functional error handling - USE THIS INSTEAD OF EXCEPTIONS
sealed interface Result<T, E> {
    record Success<T, E>(T value) implements Result<T, E> {
    }

    record Failure<T, E>(E error) implements Result<T, E> {
    }

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

record ValidationError(String field, String message) {
}

record Validated<T>(T value, List<ValidationError> errors) {
    public boolean isValid() {
        return errors.isEmpty();
    }

    public Optional<T> toOptional() {
        return isValid() ? Optional.of(value) : Optional.empty();
    }

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
     * <p>
     * Requirements:
     * - Filter products that belong to the specified category
     * - Only include products with stock >= minStock
     * - Return as a Stream for further processing
     * <p>
     * Hints:
     * - Use products.values().stream()
     * - Chain filter operations
     * - Compare product.category().id() with categoryId
     */

    private Predicate<Product> byCategoryId(String categoryId) {
        return product -> product.category().id().equals(categoryId);
    }

    private Predicate<Product> byMinStockValue(int minStockValue) {
        return product -> product.stock() >= minStockValue;
    }

    public Stream<Product> findAvailableProductsByCategory(String categoryId, int minStock) {
        return products.values().stream()
                .filter(byCategoryId(categoryId))
                .filter(byMinStockValue(minStock));
    }

    /**
     * Task 1.2: Calculate total inventory value grouped by category
     * <p>
     * Requirements:
     * - Group all products by their category
     * - For each category, sum: (product.price * product.stock)
     * - Handle the Money type correctly (use Money.multiply and Money.add)
     * <p>
     * Hints:
     * - Use Collectors.groupingBy for grouping
     * - Use Collectors.reducing for summing Money values
     * - Remember: Money has a currency, ensure all products in a category use same currency
     */
    public Map<Category, Money> calculateInventoryValueByCategory() {
        return products.values().stream()
                .collect(groupingBy(
                        Product::category,
                        reducing(
                                new Money(BigDecimal.ZERO, Currency.getInstance("PLN")),
                                product -> product.price().multiply(BigDecimal.valueOf(product.stock())),
                                Money::add
                        )));
    }

    /**
     * Task 1.3: Check if all order items are available in stock
     * <p>
     * Requirements:
     * - For each OrderItem, check if product exists and has sufficient stock
     * - Return Success with a map of productId -> availableQuantity if ALL items available
     * - Return Failure with list of unavailable items if ANY item is not available
     * - Use the Result pattern, not exceptions
     * <p>
     * Hints:
     * - First collect all availability checks
     * - Use Stream.allMatch or partition results
     * - Build appropriate Success or Failure
     */

    private Optional<Product> findProductById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    private Predicate<Map.Entry<OrderItem, Optional<Product>>> byAvailability() {
        return entry -> entry.getValue().isPresent() && entry.getValue().get().stock() >= entry.getKey().quantity();
    }

    private String toAvailabilityError(Map.Entry<OrderItem, Optional<Product>> entry) {
        var item = entry.getKey();
        var product = entry.getValue();
        return product.map(value -> item.productId() + ": requested " + item.quantity() + " but only " + value.stock() + " available")
                .orElseGet(() -> item.productId() + ": product not found");
    }

    public Result<Map<String, Integer>, List<String>> checkAvailability(List<OrderItem> items) {
        /*Map<String, Integer> availableQuantities = new HashMap<>();
        List<String> unavailableItems = new ArrayList<>();
        for (OrderItem item : items) {
            Product product = products.get(item.productId());
            if (product == null) {
                unavailableItems.add(item.productId() + ": product not found");
            } else if (product.stock() < item.quantity()) {
                unavailableItems.add(item.productId() + ": requested " + item.quantity() +
                        " but only " + product.stock() + " available");
            } else {
                availableQuantities.put(product.id(), item.quantity());
            }
        }

        return unavailableItems.isEmpty() ? new Result.Success<>(availableQuantities) : new Result.Failure<>(unavailableItems);*/

        var result = items.stream()
                .map(item -> Map.entry(item, findProductById(item.productId())))
                .collect(partitioningBy(byAvailability()));

        return result.get(false).isEmpty()
                ? new Result.Success<>(
                result.get(true).stream()
                        .map(Map.Entry::getKey)
                        .collect(toMap(OrderItem::productId, OrderItem::quantity))
        )
                : new Result.Failure<>(
                result.get(false).stream()
                        .map(this::toAvailabilityError)
                        .toList()
        );
    }

    /**
     * Task 1.4: Reserve items (create new InventoryService with updated stock)
     * <p>
     * Requirements:
     * - DO NOT modify the current InventoryService
     * - Create a new InventoryService with reduced stock levels
     * - Validate all items are available before reserving
     * - Return Success with new InventoryService or Failure with error message
     * - This demonstrates immutable updates
     * <p>
     * Hints:
     * - First use checkAvailability to validate
     * - Create new Product instances with updated stock
     * - Create new InventoryService with updated products map
     */
    private Product toProduct(Map.Entry<String, Product> entry, Map<String, Integer> availability) {
        var product = entry.getValue();
        var quantity = availability.get(product.id());
        return quantity == null ? product : product.withStock(product.stock() - quantity);
    }

    public Result<InventoryService, String> reserveItems(List<OrderItem> items) {
        return switch (checkAvailability(items)) {
            case Result.Success<Map<String, Integer>, List<String>> success -> {
                var updatedProducts = products.entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, entry -> toProduct(entry, success.value())));
                yield new Result.Success<>(new InventoryService(updatedProducts));
            }
            case Result.Failure<Map<String, Integer>, List<String>> failure -> new Result.Failure<>(String.join(", ", failure.error()));
        };
    }
}

public class Project {
}
