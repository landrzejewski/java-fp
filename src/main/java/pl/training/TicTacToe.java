package pl.training;

import java.util.Scanner;
import java.util.function.Function;

public class TicTacToe {

    public sealed interface Option<A> permits Some, None {
    }

    public record Some<A>(A value) implements Option<A> {
    }

    public record None<A>() implements Option<A> {
        @SuppressWarnings("rawtypes")
        private static final None INSTANCE = new None<>();
    }

    public static <A> boolean isPresent(Option<A> option) {
        return option instanceof Some;
    }

    public static <A> A get(Option<A> option) {
        return switch (option) {
            case Some<A> some -> some.value();
            case None<A> _ -> throw new java.util.NoSuchElementException("None.get");
        };
    }

    public static <A> Option<A> some(A value) {
        return new Some<>(value);
    }

    @SuppressWarnings("unchecked")
    public static <A> Option<A> none() {
        return (Option<A>) None.INSTANCE;
    }

    @FunctionalInterface
    public interface IO<A> {
        A run();
    }

    public static <A, B> IO<B> mapIO(IO<A> io, Function<A, B> function) {
        return () -> function.apply(io.run());
    }

    public static <A, B> IO<B> flatMapIO(IO<A> io, Function<A, IO<B>> function) {
        return () -> function.apply(io.run()).run();
    }

    public static IO<String> readLine() {
        return () -> new Scanner(System.in).nextLine();
    }

    public static IO<Void> writeLine(String text) {
        return () -> {
            System.out.println(text);
            return null;
        };
    }

    public sealed interface Player permits X, O {
        char symbol();
    }

    public record X() implements Player {
        @Override
        public char symbol() {
            return 'X';
        }

        @Override
        public String toString() {
            return "X";
        }
    }

    public record O() implements Player {
        @Override
        public char symbol() {
            return 'O';
        }

        @Override
        public String toString() {
            return "O";
        }
    }

    public sealed interface Cell permits Empty, Occupied {
    }

    public record Empty() implements Cell {
        @Override
        public String toString() {
            return " ";
        }
    }

    public record Occupied(Player player) implements Cell {
        @Override
        public String toString() {
            return player.toString();
        }
    }

    public record Position(int row, int col) {
    }

    public record Board(Cell[][] cells) {
    }

    public sealed interface GameState permits InProgress, Finished {
    }

    public record InProgress(Board board, Player currentPlayer) implements GameState {
    }

    public record Finished(Board board, GameResult result) implements GameState {
    }

    public sealed interface GameResult permits Winner, Draw {
    }

    public record Winner(Player player) implements GameResult {
        @Override
        public String toString() {
            return "Player " + player + " wins!";
        }
    }

    public record Draw() implements GameResult {
        @Override
        public String toString() {
            return "It's a draw!";
        }
    }

    public record Game(GameState state) {
    }

    // ============= PURE FUNCTIONS =============

    public static Player opponent(Player player) {
        return switch (player) {
            case X ignored -> new O();
            case O ignored -> new X();
        };
    }

    public static boolean isEmpty(Cell cell) {
        return cell instanceof Empty;
    }

    public static boolean isValidPosition(Position position) {
        return position.row() >= 0 && position.row() < 3 && position.col() >= 0 && position.col() < 3;
    }

    public static Board emptyBoard() {
        var cells = new Cell[3][3];
        fillCells(cells, 0, 0);
        return new Board(cells);
    }

    private static void fillCells(Cell[][] cells, int row, int column) {
        if (row == 3) return; // base case: finished all rows
        cells[row][column] = new Empty();
        // Move to next column or next row
        if (column < 2) {
            fillCells(cells, row, column + 1);
        } else {
            fillCells(cells, row + 1, 0);
        }
    }

    public static Cell getCell(Board board, Position position) {
        return board.cells()[position.row()][position.col()];
    }

    public static Board makeMove(Board board, Position position, Player player) {
        if (!isValidPosition(position) || !isEmpty(getCell(board, position))) {
            return board;
        }

        var newCells = new Cell[3][3];
        copyAndModify(board.cells(), newCells, position, player, 0, 0);
        return new Board(newCells);
    }

    private static void copyAndModify(Cell[][] oldCells, Cell[][] newCells, Position position, Player player, int row, int col) {
        if (row == 3) return; // base case: done filling all rows

        if (row == position.row() && col == position.col()) {
            newCells[row][col] = new Occupied(player);
        } else {
            newCells[row][col] = oldCells[row][col];
        }

        // Move recursively
        if (col < 2) {
            copyAndModify(oldCells, newCells, position, player, row, col + 1);
        } else {
            copyAndModify(oldCells, newCells, position, player, row + 1, 0);
        }
    }

    public static boolean isBoardFull(Board board) {
        return isBoardFullRecursive(board.cells(), 0, 0);
    }

    private static boolean isBoardFullRecursive(Cell[][] cells, int row, int col) {
        if (row == 3) {
            return true; // base case: finished all rows → board is full
        }

        if (isEmpty(cells[row][col])) {
            return false; // found an empty cell → board is not full
        }

        // Move to next cell
        if (col < 2) {
            return isBoardFullRecursive(cells, row, col + 1);
        } else {
            return isBoardFullRecursive(cells, row + 1, 0);
        }
    }

    public static Option<Player> findWinner(Board board) {
        Cell[][] cells = board.cells();

        // Check rows recursively
        Option<Player> rowWinner = checkRows(cells, 0);
        if (isPresent(rowWinner)) return rowWinner;

        // Check columns recursively
        Option<Player> columnWinner = checkColumns(cells, 0);
        if (isPresent(columnWinner)) return columnWinner;

        // Check diagonals
        return checkDiagonals(cells);
    }

    private static Option<Player> checkRows(Cell[][] cells, int row) {
        if (row == 3) return none(); // base case: all rows checked

        if (!isEmpty(cells[row][0]) &&
                cells[row][0].equals(cells[row][1]) &&
                cells[row][1].equals(cells[row][2])) {
            return some(((Occupied) cells[row][0]).player());
        }

        return checkRows(cells, row + 1); // recursive step
    }

    private static Option<Player> checkColumns(Cell[][] cells, int column) {
        if (column == 3) return none(); // base case: all columns checked

        if (!isEmpty(cells[0][column]) &&
                cells[0][column].equals(cells[1][column]) &&
                cells[1][column].equals(cells[2][column])) {
            return some(((Occupied) cells[0][column]).player());
        }

        return checkColumns(cells, column + 1); // recursive step
    }

    private static Option<Player> checkDiagonals(Cell[][] cells) {
        if (!isEmpty(cells[0][0]) &&
                cells[0][0].equals(cells[1][1]) &&
                cells[1][1].equals(cells[2][2])) {
            return some(((Occupied) cells[0][0]).player());
        }

        if (!isEmpty(cells[0][2]) &&
                cells[0][2].equals(cells[1][1]) &&
                cells[1][1].equals(cells[2][0])) {
            return some(((Occupied) cells[0][2]).player());
        }

        return none();
    }
    public static String toString(Board board) {
        StringBuilder text = new StringBuilder("\n");
        appendRows(board.cells(), text, 0);
        return text.toString();
    }

    private static void appendRows(Cell[][] cells, StringBuilder text, int row) {
        if (row == 3) return; // base case: done all rows

        text.append(" ");
        appendColumns(cells, text, row, 0);
        text.append("\n");

        if (row < 2) text.append("---|---|---\n");

        appendRows(cells, text, row + 1); // move to next row
    }

    private static void appendColumns(Cell[][] cells, StringBuilder text, int row, int col) {
        if (col == 3) return; // base case: end of row

        text.append(cells[row][col].toString());
        if (col < 2) text.append(" | ");

        appendColumns(cells, text, row, col + 1); // move to next column
    }

    public static Game newGame() {
        return new Game(new InProgress(emptyBoard(), new X()));
    }

    public static Game makeMoveInGame(Game game, Position position) {
        return switch (game.state()) {
            case InProgress inProgress -> {
                var newBoard = makeMove(inProgress.board(), position, inProgress.currentPlayer());

                // Check if move was valid
                if (newBoard.equals(inProgress.board())) {
                    yield game; // Invalid move, return same game
                }

                // Check for winner
                var winner = findWinner(newBoard);
                if (isPresent(winner)) {
                    yield new Game(new Finished(newBoard, new Winner(get(winner))));
                }

                // Check for draw
                if (isBoardFull(newBoard)) {
                    yield new Game(new Finished(newBoard, new Draw()));
                }

                // Continue game with next player
                yield new Game(new InProgress(newBoard, opponent(inProgress.currentPlayer())));
            }
            case Finished _ -> game; // Game is over
        };
    }

    public static boolean isGameFinished(Game game) {
        return game.state() instanceof Finished;
    }

    public static String toString(Game game) {
        return switch (game.state()) {
            case InProgress inProgress -> toString(inProgress.board()) + "\nCurrent player: " + inProgress.currentPlayer() + "\n";
            case Finished finished -> toString(finished.board()) + "\n" + finished.result() + "\n";
        };
    }

    public static Option<Position> parsePosition(String input) {
        try {
            var parts = input.trim().split("\\s+");
            if (parts.length != 2) {
                return none();
            }
            var row = Integer.parseInt(parts[0]);
            var column = Integer.parseInt(parts[1]);
            var position = new Position(row - 1, column - 1);
            return isValidPosition(position) ? some(position) : none();
        } catch (NumberFormatException exception) {
            return none();
        }
    }

    public static IO<Game> gameLoop(Game game) {
        if (isGameFinished(game)) {
            return mapIO(writeLine(toString(game)), _ -> game);
        }

        return flatMapIO(
                writeLine(toString(game)),
                _ -> flatMapIO(
                        writeLine("Enter row and column (1-3): "),
                        _ -> flatMapIO(
                                readLine(),
                                input -> {
                                    var optionalPosition = parsePosition(input);
                                    if (isPresent(optionalPosition)) {
                                        var newGame = makeMoveInGame(game, get(optionalPosition));
                                        if (newGame.equals(game)) {
                                            return flatMapIO(
                                                    writeLine("Invalid move! Try again."),
                                                    _ -> gameLoop(game)
                                            );
                                        }
                                        return gameLoop(newGame);
                                    } else {
                                        return flatMapIO(
                                                writeLine("Invalid input! Enter two numbers (1-3)."),
                                                _ -> gameLoop(game)
                                        );
                                    }
                                }
                        )
                )
        );
    }

    public static IO<Void> playGame() {
        return flatMapIO(
                writeLine("=== Tic Tac Toe ==="), _ -> mapIO(gameLoop(newGame()), _ -> null)
        );
    }

    static void main() {
        playGame().run();
    }
}