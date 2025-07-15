package pl.training.exercises;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

// Base Event class
abstract class Event {
    private final String id;
    private final Instant timestamp;
    private final String type;

    protected Event(String id, Instant timestamp, String type) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return String.format("%s[id=%s, time=%s]", type, id, timestamp);
    }
}

// Event subclasses
class UserEvent extends Event {
    private final String userId;
    private final String action;

    public UserEvent(String id, Instant timestamp, String userId, String action) {
        super(id, timestamp, "USER");
        this.userId = userId;
        this.action = action;
    }

    public String getUserId() { return userId; }
    public String getAction() { return action; }
}

class SystemEvent extends Event {
    private final String component;
    private final String severity;

    public SystemEvent(String id, Instant timestamp, String component, String severity) {
        super(id, timestamp, "SYSTEM");
        this.component = component;
        this.severity = severity;
    }

    public String getComponent() { return component; }
    public String getSeverity() { return severity; }
}

class SecurityEvent extends Event {
    private final String threat;
    private final int riskLevel;

    public SecurityEvent(String id, Instant timestamp, String threat, int riskLevel) {
        super(id, timestamp, "SECURITY");
        this.threat = threat;
        this.riskLevel = riskLevel;
    }

    public String getThreat() { return threat; }
    public int getRiskLevel() { return riskLevel; }
}

// Event source interface
interface EventSource {
    Optional<Event> nextEvent();
    String getSourceId();
    int getPriority();
}

// Mock event source implementation
class MockEventSource implements EventSource {
    private final String sourceId;
    private final int priority;
    private final Random random = new Random();
    private final AtomicLong eventCounter = new AtomicLong();
    private volatile boolean active = true;

    public MockEventSource(String sourceId, int priority) {
        this.sourceId = sourceId;
        this.priority = priority;
    }

    @Override
    public Optional<Event> nextEvent() {
        if (!active || random.nextDouble() > 0.8) {
            return Optional.empty();
        }

        try {
            Thread.sleep(random.nextInt(50)); // Simulate processing delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }

        String eventId = sourceId + "-" + eventCounter.incrementAndGet();
        Instant timestamp = Instant.now();

        switch (random.nextInt(3)) {
            case 0:
                return Optional.of(new UserEvent(
                        eventId, timestamp,
                        "user" + random.nextInt(100),
                        random.nextBoolean() ? "LOGIN" : "LOGOUT"
                ));
            case 1:
                return Optional.of(new SystemEvent(
                        eventId, timestamp,
                        "component" + random.nextInt(10),
                        random.nextBoolean() ? "INFO" : "ERROR"
                ));
            case 2:
                return Optional.of(new SecurityEvent(
                        eventId, timestamp,
                        "threat" + random.nextInt(5),
                        random.nextInt(10) + 1
                ));
            default:
                return Optional.empty();
        }
    }

    @Override
    public String getSourceId() { return sourceId; }

    @Override
    public int getPriority() { return priority; }

    public void stop() { active = false; }
}

// Custom Spliterator for event streams
class EventStreamSpliterator implements Spliterator<Event> {
    private final List<EventSource> sources;
    private final BlockingQueue<Event> buffer;
    private final int maxBufferSize;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public EventStreamSpliterator(List<EventSource> sources, int maxBufferSize) {
        this.sources = new ArrayList<>(sources);
        this.maxBufferSize = maxBufferSize;
        this.buffer = new LinkedBlockingQueue<>(maxBufferSize);

        // Start background thread to fill buffer
        startBufferFiller();
    }

    private void startBufferFiller() {
        Thread fillerThread = new Thread(() -> {
            while (active.get()) {
                for (EventSource source : sources) {
                    source.nextEvent().ifPresent(event -> {
                        try {
                            buffer.offer(event, 100, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }
        });
        fillerThread.setDaemon(true);
        fillerThread.start();
    }

    @Override
    public boolean tryAdvance(Consumer<? super Event> action) {
        try {
            Event event = buffer.poll(100, TimeUnit.MILLISECONDS);
            if (event != null) {
                action.accept(event);
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Override
    public Spliterator<Event> trySplit() {
        if (sources.size() <= 1) {
            return null;
        }

        int splitPoint = sources.size() / 2;
        List<EventSource> newSources = new ArrayList<>(sources.subList(splitPoint, sources.size()));
        sources.subList(splitPoint, sources.size()).clear();

        return new EventStreamSpliterator(newSources, maxBufferSize);
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE; // Unknown size for continuous stream
    }

    @Override
    public int characteristics() {
        return ORDERED | NONNULL;
    }

    public void stop() {
        active.set(false);
    }
}

// Window-based aggregation
class TimeWindow {
    private final Instant start;
    private final Duration duration;
    private final List<Event> events = new CopyOnWriteArrayList<>();

    public TimeWindow(Instant start, Duration duration) {
        this.start = start;
        this.duration = duration;
    }

    public boolean contains(Instant timestamp) {
        Instant end = start.plus(duration);
        return !timestamp.isBefore(start) && timestamp.isBefore(end);
    }

    public void addEvent(Event event) {
        events.add(event);
    }

    public List<Event> getEvents() {
        return new ArrayList<>(events);
    }

    public Instant getStart() { return start; }
    public Instant getEnd() { return start.plus(duration); }
}

// Stream metrics
class StreamMetrics {
    private final AtomicLong eventCount = new AtomicLong();
    private final AtomicLong processingTime = new AtomicLong();
    private final Map<String, AtomicLong> eventTypeCount = new ConcurrentHashMap<>();
    private final Instant startTime = Instant.now();

    public void recordEvent(Event event, long processingNanos) {
        eventCount.incrementAndGet();
        processingTime.addAndGet(processingNanos);
        eventTypeCount.computeIfAbsent(event.getType(), k -> new AtomicLong())
                .incrementAndGet();
    }

    public double getEventsPerSecond() {
        long elapsed = Duration.between(startTime, Instant.now()).toMillis();
        return elapsed > 0 ? (eventCount.get() * 1000.0) / elapsed : 0;
    }

    public double getAverageProcessingTime() {
        long count = eventCount.get();
        return count > 0 ? processingTime.get() / (double) count / 1_000_000 : 0; // Convert to ms
    }

    public Map<String, Long> getEventTypeCounts() {
        return eventTypeCount.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                ));
    }

    @Override
    public String toString() {
        return String.format("Metrics{events=%d, rate=%.2f/s, avgTime=%.2fms, types=%s}",
                eventCount.get(), getEventsPerSecond(), getAverageProcessingTime(),
                getEventTypeCounts());
    }
}

// Main event processor
public class EventProcessor {
    private final StreamMetrics metrics = new StreamMetrics();
    private final Map<String, TimeWindow> activeWindows = new ConcurrentHashMap<>();
    private final Duration windowDuration = Duration.ofSeconds(5);
    private final CircuitBreaker circuitBreaker = new CircuitBreaker(5, Duration.ofSeconds(30));

    // Circuit breaker pattern
    static class CircuitBreaker {
        private final int threshold;
        private final Duration timeout;
        private final AtomicInteger failureCount = new AtomicInteger();
        private final AtomicReference<Instant> lastFailure = new AtomicReference<>();
        private volatile boolean open = false;

        public CircuitBreaker(int threshold, Duration timeout) {
            this.threshold = threshold;
            this.timeout = timeout;
        }

        public boolean isOpen() {
            if (open) {
                Instant last = lastFailure.get();
                if (last != null && Duration.between(last, Instant.now()).compareTo(timeout) > 0) {
                    reset();
                }
            }
            return open;
        }

        public void recordFailure() {
            lastFailure.set(Instant.now());
            if (failureCount.incrementAndGet() >= threshold) {
                open = true;
            }
        }

        public void reset() {
            failureCount.set(0);
            open = false;
        }
    }

    // Process event stream with various patterns
    public void processEventStream(List<EventSource> sources) {
        // Create custom spliterator
        EventStreamSpliterator spliterator = new EventStreamSpliterator(sources, 1000);

        // Create parallel stream
        Stream<Event> eventStream = StreamSupport.stream(spliterator, true);

        // Fork into multiple processing pipelines
        ExecutorService executor = Executors.newFixedThreadPool(4);

        try {
            eventStream
                    .peek(event -> {
                        long start = System.nanoTime();
                        processEvent(event);
                        metrics.recordEvent(event, System.nanoTime() - start);
                    })
                    .forEach(event -> {
                        // Window-based aggregation
                        assignToWindow(event);

                        // Complex event detection
                        detectComplexPatterns(event);

                        // Fork to different processors based on type
                        switch (event.getType()) {
                            case "USER":
                                processUserEvent((UserEvent) event);
                                break;
                            case "SYSTEM":
                                processSystemEvent((SystemEvent) event);
                                break;
                            case "SECURITY":
                                processSecurityEvent((SecurityEvent) event);
                                break;
                        }
                    });
        } finally {
            spliterator.stop();
            executor.shutdown();
        }
    }

    private void processEvent(Event event) {
        if (circuitBreaker.isOpen()) {
            System.err.println("Circuit breaker open, dropping event: " + event.getId());
            return;
        }

        try {
            // Simulate processing
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void assignToWindow(Event event) {
        // Get the timestamp and round down to the nearest 5-second window
        Instant timestamp = event.getTimestamp();
        long epochSecond = timestamp.getEpochSecond();
        long windowStartEpoch = (epochSecond / 5) * 5;
        Instant windowStart = Instant.ofEpochSecond(windowStartEpoch);

        String windowKey = windowStart.toString();
        TimeWindow window = activeWindows.computeIfAbsent(windowKey,
                k -> new TimeWindow(windowStart, windowDuration));

        window.addEvent(event);

        // Clean old windows
        activeWindows.entrySet().removeIf(entry ->
                Duration.between(entry.getValue().getEnd(), Instant.now()).toSeconds() > 60);
    }

    private void detectComplexPatterns(Event event) {
        // Example: Detect rapid login/logout pattern
        if (event instanceof UserEvent) {
            UserEvent userEvent = (UserEvent) event;
            // Complex pattern detection logic here
        }
    }

    private void processUserEvent(UserEvent event) {
        // User-specific processing
    }

    private void processSystemEvent(SystemEvent event) {
        // System event processing
        if ("ERROR".equals(event.getSeverity())) {
            circuitBreaker.recordFailure();
        }
    }

    private void processSecurityEvent(SecurityEvent event) {
        // Security event processing
        if (event.getRiskLevel() > 7) {
            System.err.println("HIGH RISK SECURITY EVENT: " + event);
        }
    }

    // Monitoring system
    public void startMonitoring() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("\n=== Stream Health Report ===");
            System.out.println(metrics);
            System.out.println("Active windows: " + activeWindows.size());
            System.out.println("Circuit breaker: " + (circuitBreaker.isOpen() ? "OPEN" : "CLOSED"));

            // Check for bottlenecks
            if (metrics.getAverageProcessingTime() > 10) {
                System.err.println("WARNING: High processing time detected!");
            }

            if (metrics.getEventsPerSecond() < 10) {
                System.err.println("WARNING: Low throughput detected!");
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws InterruptedException {
        // Create event sources with different priorities
        List<EventSource> sources = Arrays.asList(
                new MockEventSource("source1", 1),
                new MockEventSource("source2", 2),
                new MockEventSource("source3", 3)
        );

        EventProcessor processor = new EventProcessor();

        // Start monitoring
        processor.startMonitoring();

        // Process events in separate thread
        Thread processingThread = new Thread(() ->
                processor.processEventStream(sources)
        );
        processingThread.start();

        // Run for 30 seconds
        Thread.sleep(30000);

        // Stop sources
        sources.forEach(source -> {
            if (source instanceof MockEventSource) {
                ((MockEventSource) source).stop();
            }
        });

        processingThread.join();

        // Final report
        System.out.println("\n=== Final Report ===");
        System.out.println(processor.metrics);
        System.out.println("Total windows processed: " + processor.activeWindows.size());
    }
}