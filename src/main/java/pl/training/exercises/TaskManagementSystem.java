package pl.training.exercises;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

class Task {

    private final int id;
    private final String description;
    private final int priority;
    private boolean completed;

    public Task(int id, String description, int priority, boolean completed) {
        this.id = id;
        this.description = description;
        this.priority = priority;
        this.completed = completed;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Task withDescription(String newDescription) {
        return new Task(this.id, newDescription, this.priority, this.completed);
    }

    @Override
    public String toString() {
        return String.format("Task #%d: %s (Priority: %d) [%s]",
                id, description, priority, completed ? "DONE" : "TODO");
    }
}

@FunctionalInterface
interface TaskProcessor<R> {
    R process(List<Task> tasks);

    default <V> TaskProcessor<V> andThen(Function<R, V> after) {
        return tasks -> after.apply(this.process(tasks));
    }
}

public class TaskManagementSystem {

    public static void main(String[] args) {
        var tasks = Arrays.asList(
                new Task(1, "implement login feature", 5, false),
                new Task(2, "write unit tests", 4, true),
                new Task(3, "update documentation", 2, false),
                new Task(4, "fix bug in payment", 5, false),
                new Task(5, "refactor database layer", 3, true)
        );

        // 1. Filter tasks by priority (>= 3)
        Predicate<Task> highPriority = task -> task.getPriority() >= 3;
        var highPriorityTasks = tasks.stream()
                .filter(highPriority)
                .toList();
        System.out.println("High priority tasks: " + highPriorityTasks.size());

        // 2. Transform descriptions to uppercase
        Function<Task, Task> toUpperCase = task -> task.withDescription(task.getDescription().toUpperCase());
        var upperCaseTasks = tasks.stream()
                .map(toUpperCase)
                .toList();
        upperCaseTasks.forEach(System.out::println);

        // 3. Count completed tasks
        Predicate<Task> isCompleted = Task::isCompleted;
        var completedCount = tasks.stream()
                .filter(isCompleted)
                .count();
        System.out.println("Completed tasks: " + completedCount);

        // 4. Create summary strings
        Function<Task, String> taskSummary = task -> String.format("Task #%d: %s (Priority: %d)",
                        task.getId(), task.getDescription(), task.getPriority());
        var summaries = tasks.stream()
                .map(taskSummary)
                .toList();
        System.out.println("\nTask summaries:");
        summaries.forEach(System.out::println);

        // 5. TaskProcessor implementations
        TaskProcessor<List<Task>> highPriorityProcessor = taskList -> taskList.stream()
                .filter(highPriority)
                .collect(Collectors.toList());

        TaskProcessor<Long> completedCounter = taskList -> taskList.stream()
                .filter(Task::isCompleted)
                .count();

        TaskProcessor<Map<Integer, List<Task>>> groupByPriority = taskList -> taskList.stream()
                .collect(Collectors.groupingBy(Task::getPriority));

        // 6. Chain operations
        Function<List<Task>, List<Task>> chainedOperation = taskList -> taskList.stream()
                .filter(highPriority)                    // Filter high priority
                .map(toUpperCase)                        // Transform descriptions
                .filter(isCompleted.negate())            // Keep only incomplete
                .collect(Collectors.toList());

        var processedTasks = chainedOperation.apply(tasks);
        System.out.println("\nChained operation results:");
        processedTasks.forEach(System.out::println);

        // Using TaskProcessor with composition
        TaskProcessor<String> complexProcessor = highPriorityProcessor
                .andThen(list -> list.stream()
                        .filter(isCompleted.negate())
                        .count())
                .andThen(count -> "Incomplete high-priority tasks: " + count);

        System.out.println("\n" + complexProcessor.process(tasks));
    }

}