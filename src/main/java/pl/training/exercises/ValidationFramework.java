package pl.training.exercises;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class ValidationResults {
    private final boolean valid;
    private final List<String> errors;

    private ValidationResults(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public static ValidationResults valid() {
        return new ValidationResults(true, Collections.emptyList());
    }

    public static ValidationResults invalid(String... errors) {
        return new ValidationResults(false, Arrays.asList(errors));
    }

    public static ValidationResults invalid(List<String> errors) {
        return new ValidationResults(false, new ArrayList<>(errors));
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public ValidationResults merge(ValidationResults other) {
        if (this.valid && other.valid) {
            return valid();
        }
        List<String> allErrors = new ArrayList<>(this.errors);
        allErrors.addAll(other.errors);
        return invalid(allErrors);
    }
}

@FunctionalInterface
interface Validator<T> {
    ValidationResults validate(T value);

    default Validator<T> and(Validator<T> other) {
        return value -> {
            ValidationResults result1 = this.validate(value);
            ValidationResults result2 = other.validate(value);
            return result1.merge(result2);
        };
    }

    default Validator<T> or(Validator<T> other) {
        return value -> {
            ValidationResults result1 = this.validate(value);
            if (result1.isValid()) return result1;
            return other.validate(value);
        };
    }

    default Validator<T> negate() {
        return value -> {
            ValidationResults result = this.validate(value);
            return result.isValid()
                    ? ValidationResults.invalid("Validation should have failed")
                    : ValidationResults.valid();
        };
    }
}

class User {
    private final String username;
    private final String email;
    private final int age;
    private final String password;

    public User(String username, String email, int age, String password) {
        this.username = username;
        this.email = email;
        this.age = age;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public int getAge() {
        return age;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return String.format("User{username='%s', email='%s', age=%d}",
                username, email, age);
    }
}

class ValidatorBuilder<T> {
    private Validator<T> validator = value -> ValidationResults.valid();

    public ValidatorBuilder<T> add(Validator<T> newValidator) {
        this.validator = this.validator.and(newValidator);
        return this;
    }

    public ValidatorBuilder<T> addIf(boolean condition, Validator<T> newValidator) {
        if (condition) {
            this.validator = this.validator.and(newValidator);
        }
        return this;
    }

    public Validator<T> build() {
        return validator;
    }
}

class UserValidators {
    private static final Pattern ALPHANUMERIC = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_UPPERCASE = Pattern.compile(".*[A-Z].*");

    public static final Validator<User> USERNAME = user -> {
        String username = user.getUsername();
        if (username == null || username.isEmpty()) {
            return ValidationResults.invalid("Username is required");
        }
        if (username.length() < 3 || username.length() > 20) {
            return ValidationResults.invalid("Username must be 3-20 characters");
        }
        if (!ALPHANUMERIC.matcher(username).matches()) {
            return ValidationResults.invalid("Username must be alphanumeric");
        }
        return ValidationResults.valid();
    };

    public static final Validator<User> EMAIL = user -> {
        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            return ValidationResults.invalid("Email is required");
        }
        if (!email.contains("@") || !email.contains(".")) {
            return ValidationResults.invalid("Email must contain @ and .");
        }
        if (email.indexOf("@") > email.lastIndexOf(".")) {
            return ValidationResults.invalid("Invalid email format");
        }
        return ValidationResults.valid();
    };

    public static final Validator<User> AGE = user -> {
        int age = user.getAge();
        if (age < 18) {
            return ValidationResults.invalid("Must be at least 18 years old");
        }
        if (age > 120) {
            return ValidationResults.invalid("Age cannot exceed 120");
        }
        return ValidationResults.valid();
    };

    public static final Validator<User> PASSWORD = user -> {
        String password = user.getPassword();
        List<String> errors = new ArrayList<>();

        if (password == null || password.length() < 8) {
            errors.add("Password must be at least 8 characters");
        } else {
            if (!HAS_DIGIT.matcher(password).matches()) {
                errors.add("Password must contain at least one digit");
            }
            if (!HAS_UPPERCASE.matcher(password).matches()) {
                errors.add("Password must contain at least one uppercase letter");
            }
        }

        return errors.isEmpty()
                ? ValidationResults.valid()
                : ValidationResults.invalid(errors);
    };
}

public class ValidationFramework {

    public static void main(String[] args) {
        // Create test users
        var users = Arrays.asList(
                new User("john123", "john@email.com", 25, "Password1"),
                new User("ab", "invalid-email", 17, "weak"),
                new User("alice_99", "alice@test.com", 30, "SecurePass123"),
                new User("bob2023", "bob@example.com", 130, "MyPassword2"),
                new User("charlie", "charlie@mail.org", 22, "nodigits")
        );

        // Build composite validator
        Validator<User> userValidator = new ValidatorBuilder<User>()
                .add(UserValidators.USERNAME)
                .add(UserValidators.EMAIL)
                .add(UserValidators.AGE)
                .add(UserValidators.PASSWORD)
                .build();

        // Validate all users and collect results
        Map<User, List<String>> validationResults = users.stream()
                .collect(Collectors.toMap(
                        user -> user,
                        user -> userValidator.validate(user).getErrors()
                ));

        // Display results
        System.out.println("Validation results:");
        System.out.println("==================");
        validationResults.forEach((user, errors) -> {
            System.out.println("\n" + user);
            if (errors.isEmpty()) {
                System.out.println("  ✓ Valid user");
            } else {
                System.out.println("  ✗ Validation errors:");
                errors.forEach(error -> System.out.println("    - " + error));
            }
        });

        // Example of conditional validation
        boolean strictMode = true;
        Validator<User> conditionalValidator = new ValidatorBuilder<User>()
                .add(UserValidators.USERNAME)
                .add(UserValidators.EMAIL)
                .addIf(strictMode, UserValidators.AGE)
                .addIf(strictMode, UserValidators.PASSWORD)
                .build();

        // Custom validator using composition
        Validator<User> adminValidator = UserValidators.USERNAME
                .and(user -> user.getUsername().startsWith("admin")
                        ? ValidationResults.valid()
                        : ValidationResults.invalid("Admin username must start with 'admin'"));

        User adminUser = new User("admin123", "admin@company.com", 25, "AdminPass1");
        ValidationResults adminResult = adminValidator.validate(adminUser);
        System.out.println("\nAdmin validation: " + (adminResult.isValid() ? "PASS" : "FAIL: " + adminResult.getErrors()));
    }

}