package pl.training.exercises;

import java.util.*;
import java.util.stream.*;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;

class Book {
    private final String isbn;
    private final String title;
    private final String author;
    private final int year;
    private final double price;
    private final String genre;

    public Book(String isbn, String title, String author, int year, double price, String genre) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.year = year;
        this.price = price;
        this.genre = genre;
    }

    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getYear() { return year; }
    public double getPrice() { return price; }
    public String getGenre() { return genre; }

    @Override
    public String toString() {
        return String.format("%s by %s (%d) - $%.2f", title, author, year, price);
    }
}

public class LibraryManagementSystem {

    // 1. Find books published after 2020
    public List<Book> findBooksAfterYear(List<Book> books, int year) {
        return books.stream()
                .filter(book -> book.getYear() > year)
                .collect(toList());
    }

    // 2. Get unique authors sorted alphabetically
    public List<String> getUniqueAuthorsSorted(List<Book> books) {
        return books.stream()
                .map(Book::getAuthor)
                .distinct()
                .sorted()
                .collect(toList());
    }

    // 3. Calculate average price by genre
    public Map<String, Double> getAveragePriceByGenre(List<Book> books) {
        return books.stream()
                .collect(groupingBy(
                        Book::getGenre,
                        averagingDouble(Book::getPrice)
                ));
    }

    // 4. Find most expensive book
    public Optional<Book> findMostExpensiveBook(List<Book> books) {
        return books.stream()
                .max(comparing(Book::getPrice));
    }

    // 5. Create comma-separated string of titles
    public String getAllTitlesAsString(List<Book> books) {
        return books.stream()
                .map(Book::getTitle)
                .collect(joining(", "));
    }

    // 6. Group titles by genre for books after threshold year
    public Map<String, List<String>> getTitlesByGenreAfterYear(List<Book> books, int year) {
        return books.stream()
                .filter(book -> book.getYear() > year)
                .collect(groupingBy(
                        Book::getGenre,
                        mapping(Book::getTitle, toList())
                ));
    }

    // 7. Complex processing - authors with average price > $25
    public Map<String, Double> getAuthorsWithHighAveragePrice(List<Book> books) {
        return books.stream()
                .filter(book -> book.getPrice() >= 10 && book.getPrice() <= 50)
                .collect(groupingBy(Book::getAuthor, averagingDouble(Book::getPrice)))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 25)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static void main(String[] args) {
        // Sample data
        List<Book> books = Arrays.asList(
                new Book("978-0134685991", "Effective Java", "Joshua Bloch", 2018, 45.99, "Programming"),
                new Book("978-1617291999", "Spring in Action", "Craig Walls", 2022, 49.99, "Programming"),
                new Book("978-0596009205", "Head First Java", "Kathy Sierra", 2005, 35.99, "Programming"),
                new Book("978-0132350884", "Clean Code", "Robert Martin", 2008, 40.50, "Programming"),
                new Book("978-0134494166", "Clean Architecture", "Robert Martin", 2017, 35.00, "Programming"),
                new Book("978-1491950357", "Building Microservices", "Sam Newman", 2021, 55.99, "Architecture"),
                new Book("978-1492078739", "Fundamentals of Software Architecture", "Mark Richards", 2022, 59.99, "Architecture"),
                new Book("978-0735619678", "Code Complete", "Steve McConnell", 2004, 38.99, "Programming"),
                new Book("978-1617295119", "Microservices Patterns", "Chris Richardson", 2018, 49.99, "Architecture"),
                new Book("978-0134757599", "Refactoring", "Martin Fowler", 2019, 47.99, "Programming")
        );

        LibraryManagementSystem library = new LibraryManagementSystem();

        // Test all methods
        System.out.println("=== Books published after 2020 ===");
        library.findBooksAfterYear(books, 2020).forEach(System.out::println);

        System.out.println("\n=== Unique authors (sorted) ===");
        library.getUniqueAuthorsSorted(books).forEach(System.out::println);

        System.out.println("\n=== Average price by genre ===");
        library.getAveragePriceByGenre(books).forEach((genre, avgPrice) ->
                System.out.printf("%s: $%.2f%n", genre, avgPrice));

        System.out.println("\n=== Most expensive book ===");
        library.findMostExpensiveBook(books)
                .ifPresent(book -> System.out.println(book));

        System.out.println("\n=== All titles ===");
        System.out.println(library.getAllTitlesAsString(books));

        System.out.println("\n=== Titles by genre (after 2018) ===");
        library.getTitlesByGenreAfterYear(books, 2018).forEach((genre, titles) -> {
            System.out.println(genre + ":");
            titles.forEach(title -> System.out.println("  - " + title));
        });

        System.out.println("\n=== Authors with high average price ===");
        library.getAuthorsWithHighAveragePrice(books).forEach((author, avgPrice) ->
                System.out.printf("%s: $%.2f%n", author, avgPrice));

        // Additional complex query example
        System.out.println("\n=== Complex Query: Programming books stats ===");
        books.stream()
                .filter(book -> "Programming".equals(book.getGenre()))
                .collect(Collectors.teeing(
                        counting(),
                        averagingDouble(Book::getPrice),
                        (count, avg) -> String.format("Count: %d, Average Price: $%.2f", count, avg)
                ))
                .lines()
                .forEach(System.out::println);
    }
}