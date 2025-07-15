package pl.training.project;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
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

    public Money subtract(Money other) {
        if (!currency.equals(other.currency)) throw new IllegalArgumentException("Currency mismatch");
        return new Money(amount.subtract(other.amount), currency);
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
            case Result.Failure<Map<String, Integer>, List<String>> failure ->
                    new Result.Failure<>(String.join(", ", failure.error()));
        };
    }
}

@FunctionalInterface
interface DiscountRule {
    Optional<Discount> apply(Order order, Customer customer, Map<String, Product> products);
}

record Discount(String code, BigDecimal percentage, String description) {
}

class PricingService {

    private final List<DiscountRule> discountRules;

    public PricingService(List<DiscountRule> discountRules) {
        this.discountRules = new ArrayList<>(discountRules);
    }

    /**
     * Task 2.1: Combine multiple discount rules into one
     * <p>
     * Requirements:
     * - Apply all rules and return the BEST (highest percentage) discount
     * - If no rules apply, return a rule that always returns Optional.empty()
     * - The combined rule should itself be a DiscountRule
     * <p>
     * Hints:
     * - Use Stream.of(rules) to process all rules
     * - Use Optional.empty() as the identity
     * - Compare discounts by percentage
     */
    public static DiscountRule combineRules(List<DiscountRule> rules) {
        return (order, customer, products) ->
                rules.stream()
                        .map(rule -> rule.apply(order, customer, products))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .max(comparing(Discount::percentage))
                        .or(Optional::empty);
    }

    /**
     * Task 2.2a: Bulk discount rule
     * <p>
     * Requirements:
     * - Give 10% discount if total item count >= 10
     * - Discount code: "BULK10"
     * - Description: "10% off for 10+ items"
     */
    public static final DiscountRule bulkDiscount = (order, customer, products) ->
            order.items().stream()
                    .mapToInt(OrderItem::quantity)
                    .sum() >= 10
                    ? Optional.of(new Discount("BULK10", new BigDecimal("0.10"), "10% off for 10+ items"))
                    : Optional.empty();


    /**
     * Task 2.2b: VIP customer discount rule
     * <p>
     * Requirements:
     * - Give 15% discount to VIP customers only
     * - Discount code: "VIP15"
     * - Description: "15% VIP customer discount"
     */
    public static final DiscountRule vipDiscount = (order, customer, products) ->
            customer.type() == CustomerType.VIP
                    ? Optional.of(new Discount("VIP15", new BigDecimal("0.10"), "15% VIP customer discount"))
                    : Optional.empty();

    /**
     * Task 2.2c: Category-based discount rule factory
     * <p>
     * Requirements:
     * - Return a rule that gives specified discount if order contains ANY item from category
     * - This is a higher-order function (returns a function)
     * - Discount code: "CAT_" + categoryId
     * <p>
     * Hints:
     * - The returned lambda captures categoryId and percentage
     * - Check if any order item's product belongs to the category
     */
    public static DiscountRule categoryDiscount(String categoryId, BigDecimal percentage) {
        return (order, customer, products) -> {
            boolean hasCategory = order.items().stream()
                    .map(item -> products.get(item.productId()))
                    .anyMatch(product -> product.category().id().equals(categoryId));

            return hasCategory
                    ? Optional.of(new Discount("CAT_" + categoryId, percentage, "category discount"))
                    : Optional.empty();
        };
    }

    /**
     * Task 2.3: Calculate complete order pricing
     * <p>
     * Requirements:
     * 1. Calculate subtotal (sum of item price * quantity)
     * 2. Find and apply best discount from all rules
     * 3. Calculate tax based on product categories
     * 4. Return Success with OrderPricing or Failure if products not found
     * <p>
     * Calculation flow:
     * - Subtotal = Σ(product.price * item.quantity)
     * - Discount = best discount from rules
     * - After discount = Subtotal * (1 - discount%)
     * - Tax = Σ(item_total * category.taxRate) calculated per item
     * - Total = After discount + Tax
     * <p>
     * Hints:
     * - Validate all products exist first
     * - Use Money type's add() and multiply() methods
     * - Handle empty discount (Optional.empty())
     */

    private Money calculateTotal(OrderItem item, Map<String, Product> products) {
        var product = products.get(item.productId());
        return product.price().multiply(BigDecimal.valueOf(item.quantity()));
    }

    public Result<OrderPricing, String> calculatePricing(
            Order order,
            Customer customer,
            Map<String, Product> products) {

        var allProductsExists = order.items().stream()
                .allMatch(item -> products.containsKey(item.productId()));

        if (!allProductsExists) {
            return new Result.Failure<>("Product found");
        }

        var subtotal = order.items().stream()
                .map(item -> calculateTotal(item, products))
                .reduce(new Money(BigDecimal.ZERO, Currency.getInstance("PLN")), Money::add);

        var bestDiscount = combineRules(discountRules).apply(order, customer, products);

        var discount = bestDiscount
                .map(Discount::percentage)
                .map(subtotal::multiply)
                .orElse(new Money(BigDecimal.ZERO, Currency.getInstance("PLN")));

        var total = subtotal.subtract(discount);

        Function<OrderItem, Money> toTax = item -> {
            var product = products.get(item.productId());
            var itemTotal = calculateTotal(item, products);
            var taxValue = BigDecimal.valueOf(product.category().taxRate());
            return itemTotal.multiply(taxValue);
        };

        var totalTax = order.items().stream()
                .map(toTax)
                .reduce(new Money(BigDecimal.ZERO, Currency.getInstance("PLN")), Money::add);

        return new Result.Success<>(new OrderPricing(
            subtotal,
            bestDiscount,
            discount,
            totalTax,
            total
        ));
    }
}

record OrderPricing(
        Money subtotal,
        Optional<Discount> appliedDiscount,
        Money discountAmount,
        Money taxAmount,
        Money total
) {
}

public class Project {
}
