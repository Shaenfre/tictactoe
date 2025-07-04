import java.util.*;
import java.util.stream.Collectors;

public class TicTacToe {
    // Letter
    sealed interface Letter permits X, O {}
    record X() implements Letter {}
    record O() implements Letter {}

    // Value
    sealed interface Value permits Unspecified, LetterValue {}
    record Unspecified() implements Value {}
    record LetterValue(Letter letter) implements Value {}

    // Position enums
    enum OneThroughThree { One, Two, Three }

    record Position(OneThroughThree row, OneThroughThree column) {}

    // Row and Board
    record Row(Value v1, Value v2, Value v3) {}
    record Board(Row r1, Row r2, Row r3) {}

    static final Value U = new Unspecified();

    static final Board emptyBoard = new Board(
        new Row(U, U, U),
        new Row(U, U, U),
        new Row(U, U, U)
    );

    record Move(Position at, Letter place) {}

    static Value select(Board board, Position pos) {
        Row row = switch (pos.row()) {
            case One -> board.r1();
            case Two -> board.r2();
            case Three -> board.r3();
        };
        return switch (pos.column()) {
            case One -> row.v1();
            case Two -> row.v2();
            case Three -> row.v3();
        };
    }

    static Board set(Value value, Board board, Position pos) {
        Row r1 = board.r1(), r2 = board.r2(), r3 = board.r3();

        Row newRow = switch (pos.row()) {
            case One -> updateRow(r1, pos.column(), value);
            case Two -> updateRow(r2, pos.column(), value);
            case Three -> updateRow(r3, pos.column(), value);
        };

        return switch (pos.row()) {
            case One -> new Board(newRow, r2, r3);
            case Two -> new Board(r1, newRow, r3);
            case Three -> new Board(r1, r2, newRow);
        };
    }

    static Row updateRow(Row row, OneThroughThree col, Value val) {
        return switch (col) {
            case One -> new Row(val, row.v2(), row.v3());
            case Two -> new Row(row.v1(), val, row.v3());
            case Three -> new Row(row.v1(), row.v2(), val);
        };
    }

    static Board modify(java.util.function.Function<Value, Value> f, Board board, Position pos) {
        return set(f.apply(select(board, pos)), board, pos);
    }

    static Board placePieceIfCan(Letter piece, Board board, Position pos) {
        return modify(v -> v instanceof Unspecified ? new LetterValue(piece) : v, board, pos);
    }

    static Optional<Board> makeMove(Board board, Move move) {
        return select(board, move.at()) instanceof Unspecified
            ? Optional.of(placePieceIfCan(move.place(), board, move.at()))
            : Optional.empty();
    }

    static final List<List<Position>> waysToWin = List.of(
        List.of(new Position(OneThroughThree.One, OneThroughThree.One),
                new Position(OneThroughThree.One, OneThroughThree.Two),
                new Position(OneThroughThree.One, OneThroughThree.Three)),
        List.of(new Position(OneThroughThree.Two, OneThroughThree.One),
                new Position(OneThroughThree.Two, OneThroughThree.Two),
                new Position(OneThroughThree.Two, OneThroughThree.Three)),
        List.of(new Position(OneThroughThree.Three, OneThroughThree.One),
                new Position(OneThroughThree.Three, OneThroughThree.Two),
                new Position(OneThroughThree.Three, OneThroughThree.Three)),
        List.of(new Position(OneThroughThree.One, OneThroughThree.One),
                new Position(OneThroughThree.Two, OneThroughThree.One),
                new Position(OneThroughThree.Three, OneThroughThree.One)),
        List.of(new Position(OneThroughThree.One, OneThroughThree.Two),
                new Position(OneThroughThree.Two, OneThroughThree.Two),
                new Position(OneThroughThree.Three, OneThroughThree.Two)),
        List.of(new Position(OneThroughThree.One, OneThroughThree.Three),
                new Position(OneThroughThree.Two, OneThroughThree.Three),
                new Position(OneThroughThree.Three, OneThroughThree.Three)),
        List.of(new Position(OneThroughThree.One, OneThroughThree.One),
                new Position(OneThroughThree.Two, OneThroughThree.Two),
                new Position(OneThroughThree.Three, OneThroughThree.Three)),
        List.of(new Position(OneThroughThree.One, OneThroughThree.Three),
                new Position(OneThroughThree.Two, OneThroughThree.Two),
                new Position(OneThroughThree.Three, OneThroughThree.One))
    );

    static final List<Position> allPositions = Arrays.stream(OneThroughThree.values())
        .flatMap(r -> Arrays.stream(OneThroughThree.values())
        .map(c -> new Position(r, c))).collect(Collectors.toList());

    static Optional<Letter> winner(Board board) {
        for (var path : waysToWin) {
            List<Value> line = path.stream().map(p -> select(board, p)).toList();
            if (line.stream().allMatch(v -> v instanceof LetterValue lv && lv.letter() instanceof X))
                return Optional.of(new X());
            if (line.stream().allMatch(v -> v instanceof LetterValue lv && lv.letter() instanceof O))
                return Optional.of(new O());
        }
        return Optional.empty();
    }

    static boolean slotsRemaining(Board board) {
        return allPositions.stream().anyMatch(pos -> select(board, pos) instanceof Unspecified);
    }

    sealed interface Outcome permits Outcome.NoneYet, Outcome.Draw, Outcome.Winner {
        record NoneYet() implements Outcome {}
        record Draw() implements Outcome {}
        record Winner(Letter letter) implements Outcome {}
    }

    static Outcome outcome(Board board) {
        return winner(board)
            .map(Outcome.Winner::new)
            .orElse(slotsRemaining(board) ? new Outcome.NoneYet() : new Outcome.Draw());
    }

    static String renderValue(Value v) {
        return switch (v) {
            case Unspecified _ -> " ";
            case LetterValue lv -> lv.letter() instanceof X ? "X" : "O";
        };
    }

    static String render(Board board) {
        return renderRow(board.r1()) + "\n-----\n" +
               renderRow(board.r2()) + "\n-----\n" +
               renderRow(board.r3());
    }

    static String renderRow(Row row) {
        return String.join("|",
            renderValue(row.v1()), renderValue(row.v2()), renderValue(row.v3()));
    }

    static Letter otherPlayer(Letter l) {
        return l instanceof X ? new O() : new X();
    }

    record GameState(Board board, Letter whoseTurn) {}

    static final GameState initialGameState = new GameState(emptyBoard, new X());

    static Optional<OneThroughThree> parseOneThroughThree(String s) {
        return switch (s) {
            case "1" -> Optional.of(OneThroughThree.One);
            case "2" -> Optional.of(OneThroughThree.Two);
            case "3" -> Optional.of(OneThroughThree.Three);
            default -> Optional.empty();
        };
    }

    static Optional<Position> parseMove(String raw) {
        String[] parts = raw.trim().split("\\s+");
        if (parts.length != 2) return Optional.empty();
        var rowOpt = parseOneThroughThree(parts[0]);
        var colOpt = parseOneThroughThree(parts[1]);
        return rowOpt.flatMap(r -> colOpt.map(c -> new Position(r, c)));
    }

    static Move readMoveIo(Letter letter, Scanner scanner) {
        while (true) {
            String input = scanner.nextLine();
            Optional<Position> posOpt = parseMove(input);
            if (posOpt.isPresent()) {
                return new Move(posOpt.get(), letter);
            }
            System.out.println("Bad move! Please input row and column numbers");
        }
    }

    static Board nextMoveIo(Board board, Letter letter, Scanner scanner) {
        while (true) {
            Move move = readMoveIo(letter, scanner);
            Optional<Board> newBoard = makeMove(board, move);
            if (newBoard.isPresent()) {
                return newBoard.get();
            }
            System.out.println("Bad move! Position is occupied.");
        }
    }

    static void playIo(GameState state, Scanner scanner) {
        while (true) {
            System.out.printf("%s turn%n", state.whoseTurn() instanceof X ? "X" : "O");
            System.out.println(render(state.board()));
            System.out.println();

            Board newBoard = nextMoveIo(state.board(), state.whoseTurn(), scanner);
            System.out.println();

            Outcome result = outcome(newBoard);
            switch (result) {
                case Outcome.Winner w -> {
                    System.out.printf("%s wins!!!%n", w.letter() instanceof X ? "X" : "O");
                    System.out.println(render(newBoard));
                    return;
                }
                case Outcome.Draw _ -> {
                    System.out.println("It's a draw!");
                    return;
                }
                case Outcome.NoneYet _ -> {
                    state = new GameState(newBoard, otherPlayer(state.whoseTurn()));
                }
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        playIo(initialGameState, scanner);
    }
}
