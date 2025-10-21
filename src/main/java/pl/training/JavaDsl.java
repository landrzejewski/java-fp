package pl.training;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class JavaDsl {

    // ============================================================================
    // EXAMPLE 1: Simple HTML Builder DSL
    // ============================================================================

    /**
     * HTML Builder DSL using method chaining and lambda consumers
     */
    public static class HtmlBuilder {
        private final StringBuilder html = new StringBuilder();
        private int indentLevel = 0;

        private void indent() {
            html.append("  ".repeat(indentLevel));
        }

        public HtmlBuilder tag(String name, Consumer<HtmlBuilder> content) {
            indent();
            html.append("<").append(name).append(">\n");
            indentLevel++;
            content.accept(this);
            indentLevel--;
            indent();
            html.append("</").append(name).append(">\n");
            return this;
        }

        public HtmlBuilder selfClosingTag(String name) {
            indent();
            html.append("<").append(name).append(" />\n");
            return this;
        }

        public HtmlBuilder text(String content) {
            indent();
            html.append(content).append("\n");
            return this;
        }

        public String build() {
            return html.toString();
        }
    }

    // Usage example for HTML Builder
    public static void htmlBuilderExample() {
        System.out.println("=== HTML Builder DSL ===");

        HtmlBuilder builder = new HtmlBuilder();
        String html = builder
                .tag("html", root -> root
                        .tag("head", head -> head
                                .tag("title", title -> title.text("My Page")))
                        .tag("body", body -> body
                                .tag("h1", h1 -> h1.text("Welcome"))
                                .tag("p", p -> p.text("This is a paragraph"))
                                .selfClosingTag("br")
                                .tag("div", div -> div
                                        .tag("span", span -> span.text("Nested content")))))
                .build();

        System.out.println(html);
    }

    // ============================================================================
    // EXAMPLE 2: SQL Query Builder DSL
    // ============================================================================

    /**
     * SQL Query Builder using fluent interface
     */
    public static class QueryBuilder {
        private String table;
        private final List<String> columns = new ArrayList<>();
        private final List<String> conditions = new ArrayList<>();
        private final List<String> orderBy = new ArrayList<>();
        private Integer limitValue;

        public QueryBuilder from(String table) {
            this.table = table;
            return this;
        }

        public QueryBuilder select(String... columns) {
            this.columns.addAll(Arrays.asList(columns));
            return this;
        }

        public QueryBuilder where(String condition) {
            this.conditions.add(condition);
            return this;
        }

        public QueryBuilder and(String condition) {
            if (!conditions.isEmpty()) {
                this.conditions.add("AND " + condition);
            }
            return this;
        }

        public QueryBuilder or(String condition) {
            if (!conditions.isEmpty()) {
                this.conditions.add("OR " + condition);
            }
            return this;
        }

        public QueryBuilder orderBy(String column) {
            this.orderBy.add(column);
            return this;
        }

        public QueryBuilder limit(int limit) {
            this.limitValue = limit;
            return this;
        }

        public String build() {
            StringBuilder query = new StringBuilder("SELECT ");

            if (columns.isEmpty()) {
                query.append("*");
            } else {
                query.append(String.join(", ", columns));
            }

            query.append(" FROM ").append(table);

            if (!conditions.isEmpty()) {
                query.append(" WHERE ").append(String.join(" ", conditions));
            }

            if (!orderBy.isEmpty()) {
                query.append(" ORDER BY ").append(String.join(", ", orderBy));
            }

            if (limitValue != null) {
                query.append(" LIMIT ").append(limitValue);
            }

            return query.toString();
        }
    }

    // Usage example for Query Builder
    public static void queryBuilderExample() {
        System.out.println("\n=== SQL Query Builder DSL ===");

        String query = new QueryBuilder()
                .select("id", "name", "email")
                .from("users")
                .where("age > 18")
                .and("status = 'active'")
                .or("role = 'admin'")
                .orderBy("name")
                .limit(10)
                .build();

        System.out.println(query);
    }

    // ============================================================================
    // EXAMPLE 3: Test Specification DSL
    // ============================================================================

    /**
     * Test specification DSL using nested builders
     */
    public static class TestSpec {
        private final String description;
        private final List<TestCase> testCases = new ArrayList<>();

        private TestSpec(String description) {
            this.description = description;
        }

        public static TestSpec describe(String description, Consumer<TestSpec> spec) {
            TestSpec testSpec = new TestSpec(description);
            spec.accept(testSpec);
            return testSpec;
        }

        public TestSpec it(String description, Runnable test) {
            testCases.add(new TestCase(description, test));
            return this;
        }

        public void run() {
            System.out.println("\nTest Suite: " + description);
            int passed = 0;
            int failed = 0;

            for (TestCase testCase : testCases) {
                try {
                    testCase.test.run();
                    System.out.println("  ✓ " + testCase.description);
                    passed++;
                } catch (AssertionError e) {
                    System.out.println("  ✗ " + testCase.description + " - " + e.getMessage());
                    failed++;
                }
            }

            System.out.println("\nResults: " + passed + " passed, " + failed + " failed");
        }

        private static class TestCase {
            final String description;
            final Runnable test;

            TestCase(String description, Runnable test) {
                this.description = description;
                this.test = test;
            }
        }
    }

    // Helper assertion methods
    public static class Assert {
        public static void assertEquals(Object expected, Object actual) {
            if (!expected.equals(actual)) {
                throw new AssertionError("Expected: " + expected + ", but got: " + actual);
            }
        }

        public static void assertTrue(boolean condition) {
            if (!condition) {
                throw new AssertionError("Expected true but got false");
            }
        }
    }

    // Usage example for Test Spec
    public static void testSpecExample() {
        System.out.println("\n=== Test Specification DSL ===");

        TestSpec.describe("Calculator", spec -> {
            spec.it("should add two numbers", () -> {
                int result = 2 + 2;
                Assert.assertEquals(4, result);
            });

            spec.it("should multiply two numbers", () -> {
                int result = 3 * 4;
                Assert.assertEquals(12, result);
            });

            spec.it("should fail intentionally", () -> {
                Assert.assertEquals(5, 2 + 2);
            });
        }).run();
    }

    // ============================================================================
    // EXAMPLE 4: Configuration DSL
    // ============================================================================

    /**
     * Configuration DSL using builder pattern with nested contexts
     */
    public static class ConfigBuilder {
        private final Map<String, Object> config = new HashMap<>();

        public static ConfigBuilder create() {
            return new ConfigBuilder();
        }

        public ConfigBuilder database(Consumer<DatabaseConfig> dbConfig) {
            DatabaseConfig db = new DatabaseConfig();
            dbConfig.accept(db);
            config.put("database", db.build());
            return this;
        }

        public ConfigBuilder server(Consumer<ServerConfig> serverConfig) {
            ServerConfig server = new ServerConfig();
            serverConfig.accept(server);
            config.put("server", server.build());
            return this;
        }

        public Map<String, Object> build() {
            return new HashMap<>(config);
        }

        public static class DatabaseConfig {
            private final Map<String, Object> settings = new HashMap<>();

            public DatabaseConfig host(String host) {
                settings.put("host", host);
                return this;
            }

            public DatabaseConfig port(int port) {
                settings.put("port", port);
                return this;
            }

            public DatabaseConfig username(String username) {
                settings.put("username", username);
                return this;
            }

            public DatabaseConfig password(String password) {
                settings.put("password", password);
                return this;
            }

            Map<String, Object> build() {
                return new HashMap<>(settings);
            }
        }

        public static class ServerConfig {
            private final Map<String, Object> settings = new HashMap<>();

            public ServerConfig port(int port) {
                settings.put("port", port);
                return this;
            }

            public ServerConfig ssl(boolean enabled) {
                settings.put("ssl", enabled);
                return this;
            }

            public ServerConfig maxConnections(int max) {
                settings.put("maxConnections", max);
                return this;
            }

            Map<String, Object> build() {
                return new HashMap<>(settings);
            }
        }
    }

    // Usage example for Config Builder
    public static void configBuilderExample() {
        System.out.println("\n=== Configuration DSL ===");

        Map<String, Object> config = ConfigBuilder.create()
                .database(db -> db
                        .host("localhost")
                        .port(5432)
                        .username("admin")
                        .password("secret"))
                .server(server -> server
                        .port(8080)
                        .ssl(true)
                        .maxConnections(100))
                .build();

        System.out.println("Configuration: " + config);
    }

    // ============================================================================
    // EXAMPLE 5: Route Definition DSL (REST API style)
    // ============================================================================

    /**
     * REST API Route DSL
     */
    public static class Router {
        private final List<Route> routes = new ArrayList<>();

        public static Router create() {
            return new Router();
        }

        public Router get(String path, Function<Request, Response> handler) {
            routes.add(new Route("GET", path, handler));
            return this;
        }

        public Router post(String path, Function<Request, Response> handler) {
            routes.add(new Route("POST", path, handler));
            return this;
        }

        public Router put(String path, Function<Request, Response> handler) {
            routes.add(new Route("PUT", path, handler));
            return this;
        }

        public Router delete(String path, Function<Request, Response> handler) {
            routes.add(new Route("DELETE", path, handler));
            return this;
        }

        public Response handle(String method, String path, Request request) {
            return routes.stream()
                    .filter(route -> route.method.equals(method) && route.path.equals(path))
                    .findFirst()
                    .map(route -> route.handler.apply(request))
                    .orElse(new Response(404, "Not Found"));
        }

        public void printRoutes() {
            System.out.println("\nRegistered Routes:");
            routes.forEach(route ->
                    System.out.println("  " + route.method + " " + route.path));
        }

        static class Route {
            final String method;
            final String path;
            final Function<Request, Response> handler;

            Route(String method, String path, Function<Request, Response> handler) {
                this.method = method;
                this.path = path;
                this.handler = handler;
            }
        }

        public static class Request {
            private final Map<String, String> params = new HashMap<>();
            private String body;

            public Request param(String key, String value) {
                params.put(key, value);
                return this;
            }

            public Request body(String body) {
                this.body = body;
                return this;
            }

            public String getParam(String key) {
                return params.get(key);
            }

            public String getBody() {
                return body;
            }
        }

        public static class Response {
            final int status;
            final String body;

            public Response(int status, String body) {
                this.status = status;
                this.body = body;
            }

            @Override
            public String toString() {
                return "Response{status=" + status + ", body='" + body + "'}";
            }
        }
    }

    // Usage example for Router
    public static void routerExample() {
        System.out.println("\n=== REST API Route DSL ===");

        Router router = Router.create()
                .get("/users", req ->
                        new Router.Response(200, "List of users"))
                .get("/users/:id", req ->
                        new Router.Response(200, "User with id: " + req.getParam("id")))
                .post("/users", req ->
                        new Router.Response(201, "User created: " + req.getBody()))
                .put("/users/:id", req ->
                        new Router.Response(200, "User updated: " + req.getParam("id")))
                .delete("/users/:id", req ->
                        new Router.Response(204, "User deleted"));

        router.printRoutes();

        // Simulate requests
        System.out.println("\nSimulating requests:");
        Router.Request getRequest = new Router.Request().param("id", "123");
        System.out.println(router.handle("GET", "/users/:id", getRequest));

        Router.Request postRequest = new Router.Request().body("{\"name\":\"John\"}");
        System.out.println(router.handle("POST", "/users", postRequest));
    }

    // ============================================================================
    // EXAMPLE 6: Validation DSL
    // ============================================================================

    /**
     * Validation DSL using functional composition
     */
    public static class Validator<T> {
        private final List<ValidationRule<T>> rules = new ArrayList<>();

        public static <T> Validator<T> of(Class<T> clazz) {
            return new Validator<>();
        }

        public Validator<T> field(String fieldName,
                                  Function<T, Object> extractor,
                                  Predicate<Object> predicate,
                                  String message) {
            rules.add(new ValidationRule<>(fieldName, extractor, predicate, message));
            return this;
        }

        public ValidationResult validate(T object) {
            List<String> errors = new ArrayList<>();

            for (ValidationRule<T> rule : rules) {
                Object value = rule.extractor.apply(object);
                if (!rule.predicate.test(value)) {
                    errors.add(rule.fieldName + ": " + rule.message);
                }
            }

            return new ValidationResult(errors.isEmpty(), errors);
        }

        static class ValidationRule<T> {
            final String fieldName;
            final Function<T, Object> extractor;
            final Predicate<Object> predicate;
            final String message;

            ValidationRule(String fieldName, Function<T, Object> extractor,
                           Predicate<Object> predicate, String message) {
                this.fieldName = fieldName;
                this.extractor = extractor;
                this.predicate = predicate;
                this.message = message;
            }
        }

        public static class ValidationResult {
            final boolean valid;
            final List<String> errors;

            ValidationResult(boolean valid, List<String> errors) {
                this.valid = valid;
                this.errors = errors;
            }

            public boolean isValid() {
                return valid;
            }

            public List<String> getErrors() {
                return errors;
            }

            @Override
            public String toString() {
                if (valid) {
                    return "Validation passed";
                }
                return "Validation failed:\n  " + String.join("\n  ", errors);
            }
        }
    }

    // Example domain object
    static class User {
        String name;
        String email;
        int age;

        User(String name, String email, int age) {
            this.name = name;
            this.email = email;
            this.age = age;
        }
    }

    // Usage example for Validator
    public static void validatorExample() {
        System.out.println("\n=== Validation DSL ===");

        Validator<User> userValidator = Validator.of(User.class)
                .field("name", u -> u.name,
                        name -> name != null && !name.toString().isEmpty(),
                        "must not be empty")
                .field("email", u -> u.email,
                        email -> email != null && email.toString().contains("@"),
                        "must be a valid email")
                .field("age", u -> u.age,
                        age -> (Integer) age >= 18,
                        "must be at least 18");

        // Valid user
        User validUser = new User("John Doe", "john@example.com", 25);
        System.out.println("Valid user: " + userValidator.validate(validUser));

        // Invalid user
        User invalidUser = new User("", "invalid-email", 15);
        System.out.println("\nInvalid user: " + userValidator.validate(invalidUser));
    }

    // ============================================================================
    // EXAMPLE 7: Pipeline DSL
    // ============================================================================

    /**
     * Data transformation pipeline DSL
     */
    public static class Pipeline<T> {
        private final List<Function<T, T>> transformations = new ArrayList<>();

        public static <T> Pipeline<T> start() {
            return new Pipeline<>();
        }

        public Pipeline<T> then(Function<T, T> transformation) {
            transformations.add(transformation);
            return this;
        }

        public Pipeline<T> filter(Predicate<T> predicate, T defaultValue) {
            transformations.add(value ->
                    predicate.test(value) ? value : defaultValue);
            return this;
        }

        public <R> Pipeline<R> map(Function<T, R> mapper) {
            Pipeline<R> newPipeline = new Pipeline<>();
            newPipeline.transformations.add(value -> {
                T current = (T) value;
                for (Function<T, T> transform : transformations) {
                    current = transform.apply(current);
                }
                return (R) mapper.apply(current);
            });
            return newPipeline;
        }

        public T execute(T input) {
            T result = input;
            for (Function<T, T> transformation : transformations) {
                result = transformation.apply(result);
            }
            return result;
        }
    }

    // Usage example for Pipeline
    public static void pipelineExample() {
        System.out.println("\n=== Pipeline DSL ===");

        Pipeline<String> textPipeline = Pipeline.<String>start()
                .then(String::trim)
                .then(String::toLowerCase)
                .then(s -> s.replace(" ", "_"))
                .filter(s -> s.length() > 3, "invalid");

        String result1 = textPipeline.execute("  Hello World  ");
        String result2 = textPipeline.execute("  Hi  ");

        System.out.println("Result 1: " + result1);
        System.out.println("Result 2: " + result2);

        // Number pipeline
        Pipeline<Integer> mathPipeline = Pipeline.<Integer>start()
                .then(n -> n * 2)
                .then(n -> n + 10)
                .filter(n -> n < 100, 0);

        System.out.println("Math result 1: " + mathPipeline.execute(5));
        System.out.println("Math result 2: " + mathPipeline.execute(50));
    }

    // ============================================================================
    // MAIN - Run all examples
    // ============================================================================

    public static void main(String[] args) {
        htmlBuilderExample();
        queryBuilderExample();
        testSpecExample();
        configBuilderExample();
        routerExample();
        validatorExample();
        pipelineExample();

        System.out.println("\n" + "=".repeat(60));
        System.out.println("DSL Tutorial completed!");
        System.out.println("=".repeat(60));
    }
}