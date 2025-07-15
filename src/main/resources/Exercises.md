Exercise 1: Basic Functional Interfaces and Lambda Expressions
Create a simple task management system using functional interfaces. Implement the following:

Create a Task class with fields: id (int), description (String), priority (int, 1-5), and completed (boolean).
Implement the following operations using appropriate built-in functional interfaces:

Filter tasks by priority (only tasks with priority >= 3)
Transform task descriptions to uppercase
Count completed tasks
Create a summary string for each task in format: "Task #[id]: [description] (Priority: [priority])"


Create a TaskProcessor functional interface that takes a list of tasks and returns a processed result of any type.
Implement a method that chains multiple operations: filter high-priority tasks, transform their descriptions to uppercase, and return only incomplete tasks.





Exercise 2: Custom Functional Interfaces with Composition
Create a validation framework for user registration with the following requirements:

Design a Validator<T> functional interface with:

A validate method that returns a ValidationResult (containing boolean valid and List<String> errors)
Default methods and, or, and negate for combining validators


Create a User class with fields: username, email, age, and password.
Implement specific validators:

Username: 3-20 characters, alphanumeric only
Email: contains '@' and '.'
Age: between 18 and 120
Password: minimum 8 characters, contains at least one digit and one uppercase letter


Create a ValidatorBuilder<T> that allows fluent chaining of validators and can produce a combined validator.
Implement a method that validates a list of users and returns a map of users to their validation errors.




Exercise 3: Advanced Patterns - Pipeline with Memoization
Create a data processing pipeline for analyzing text documents:

Design a Pipeline<T, R> functional interface that can:

Process input of type T and return result of type R
Chain with other pipelines
Handle errors gracefully with a Try<T> wrapper (similar to Result pattern)


Implement a MemoizedPipeline<T, R> that caches results for expensive operations with:

Time-based expiration
Size-limited cache (LRU eviction)


Create specific pipeline stages:

Text normalizer (lowercase, trim, remove extra spaces)
Word frequency counter (returns Map<String, Integer>)
Sentiment analyzer (mock implementation returning positive/negative/neutral)
Statistics calculator (word count, average word length, unique words)


Implement a PipelineExecutor that can:

Execute pipelines asynchronously
Retry failed operations with exponential backoff
Collect metrics (execution time, cache hits/misses)


Create a method that processes multiple documents in parallel and aggregates the results.



Exercise 4: Basic Stream Operations and Collectors
Create a library management system that processes book data using streams.

Create a Book class with fields: isbn (String), title (String), author (String), year (int), price (double), and genre (String).
Implement the following operations using streams:

Find all books published after 2020
Get a list of unique authors (sorted alphabetically)
Calculate the average price of books in each genre
Find the most expensive book
Create a comma-separated string of all book titles


Create a method that takes a list of books and a year threshold, then returns a Map where:

Keys are genres
Values are lists of book titles in that genre published after the threshold year


Implement a method that processes books in multiple steps:

Filter books priced between $10 and $50
Group by author
For each author, calculate their average book price
Return only authors with average price > $25




Exercise 5: Custom Stream Processing with Error Handling
Create a data processing pipeline for analyzing CSV data with error handling.

Design a DataRecord class representing a row with fields: id (String), timestamp (String), value (String), category (String).
Create a DataProcessor that:

Reads records from multiple sources (simulate with Lists)
Validates records (id not null, timestamp parseable, value is numeric)
Transforms valid records (parse timestamp to LocalDateTime, value to Double)
Handles errors gracefully without stopping the stream


Implement a custom collector that:

Groups records by category
For each category, calculates min, max, average, and count
Returns a custom CategoryStatistics object


Create a method that:

Processes records in parallel
Filters out records older than 30 days
Detects anomalies (values that are 2 standard deviations from the mean)
Returns both normal and anomalous records in separate lists


Implement retry logic for records that fail validation, with up to 3 attempts and exponential backoff.




Exercise 6: Advanced Stream Patterns with Custom Spliterator
Create a real-time event processing system with custom stream sources.

Design an Event class hierarchy:

Base Event class with id, timestamp, type
Subclasses: UserEvent, SystemEvent, SecurityEvent


Implement a custom EventStreamSpliterator that:

Generates events from multiple sources (simulated)
Supports parallel splitting for concurrent processing
Implements backpressure when consumer is slow
Maintains event ordering within each source


Create an EventProcessor with these features:

Window-based aggregation (e.g., events per 5-second window)
Complex event detection (patterns across multiple events)
State management for session tracking
Metrics collection (events/second, processing latency)


Implement stream composition patterns:

Merge multiple event streams with priority
Fork stream into multiple processing pipelines
Join events based on correlation IDs
Implement circuit breaker pattern for downstream failures


Create a monitoring system that:

Tracks stream health metrics
Detects and reports processing bottlenecks
Provides real-time statistics
Supports dynamic reconfiguration of processing rules