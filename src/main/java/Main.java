import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class Main {

    public static void main(String[] args) {
        var numbers = Arrays.asList(20, 5, 3, 2);
        int seq = numbers.stream().reduce((a, b) -> a - b)
                .orElse(0);
        System.out.println(seq);

        int seq2 = numbers.stream()
                .parallel()
                .reduce((a, b) -> a - b)
                .orElse(0);
        System.out.println(seq2);
    }

}
