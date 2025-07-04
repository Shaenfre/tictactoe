import java.util.*;
import java.util.stream.Collectors;

public class SnakeAndLadders {
    // Value object for board position, validated between 1 and 100
    public static record BoardPos(int index) {
        public BoardPos {
            if (index < 1 || index > 100) {
                throw new IllegalArgumentException("BoardPos out of bounds: " + index);
            }
        }
    }

    // Dice roll outcome
    public static record Dice(int value) {}

    // Square type: Normal, Snake, Ladder
    public sealed interface Square permits Normal, Snake, Ladder {}
    public static record Normal(BoardPos position) implements Square {}
    public static record Snake(BoardPos start, BoardPos end) implements Square {}
    public static record Ladder(BoardPos start, BoardPos end) implements Square {}

    // Board representation
    public static record Board(Map<BoardPos, Square> squares, BoardPos finalSquare) {}

    // Player state
    public static record Player(String name, BoardPos position) {}

    // Game state
    public static record GameState(Board board, List<Player> players, int currentPlayerIndex) {}

    // Outcome
    public sealed interface Outcome permits Ongoing, Win {}
    public static record Ongoing(GameState state) implements Outcome {}
    public static record Win(Player winner) implements Outcome {}

    // Build a standard 10x10 board with snakes and ladders
    public static Board createStandardBoard() {
        int size = 100;
        Map<BoardPos, Square> map = new HashMap<>();
        for (int i = 1; i <= size; i++) {
            BoardPos pos = new BoardPos(i);
            map.put(pos, new Normal(pos));
        }
        // Snakes
        map.put(new BoardPos(16), new Snake(new BoardPos(16), new BoardPos(6)));
        map.put(new BoardPos(47), new Snake(new BoardPos(47), new BoardPos(26)));
        map.put(new BoardPos(49), new Snake(new BoardPos(49), new BoardPos(11)));
        map.put(new BoardPos(56), new Snake(new BoardPos(56), new BoardPos(53)));
        map.put(new BoardPos(62), new Snake(new BoardPos(62), new BoardPos(19)));
        map.put(new BoardPos(64), new Snake(new BoardPos(64), new BoardPos(60)));
        map.put(new BoardPos(87), new Snake(new BoardPos(87), new BoardPos(24)));
        map.put(new BoardPos(93), new Snake(new BoardPos(93), new BoardPos(73)));
        map.put(new BoardPos(95), new Snake(new BoardPos(95), new BoardPos(75)));
        map.put(new BoardPos(98), new Snake(new BoardPos(98), new BoardPos(78)));
        // Ladders
        map.put(new BoardPos(1), new Ladder(new BoardPos(1), new BoardPos(38)));
        map.put(new BoardPos(4), new Ladder(new BoardPos(4), new BoardPos(14)));
        map.put(new BoardPos(9), new Ladder(new BoardPos(9), new BoardPos(31)));
        map.put(new BoardPos(21), new Ladder(new BoardPos(21), new BoardPos(42)));
        map.put(new BoardPos(28), new Ladder(new BoardPos(28), new BoardPos(84)));
        map.put(new BoardPos(36), new Ladder(new BoardPos(36), new BoardPos(44)));
        map.put(new BoardPos(51), new Ladder(new BoardPos(51), new BoardPos(67)));
        map.put(new BoardPos(71), new Ladder(new BoardPos(71), new BoardPos(91)));
        map.put(new BoardPos(80), new Ladder(new BoardPos(80), new BoardPos(100)));
        return new Board(map, new BoardPos(size));
    }

    // Roll a die: 1-6
    public static Dice rollDie() {
        return new Dice(new Random().nextInt(6) + 1);
    }

    // Apply a move for the current player
    public static GameState applyMove(GameState state, Dice dice) {
        Board board = state.board();
        List<Player> players = new ArrayList<>(state.players());
        int idx = state.currentPlayerIndex();
        Player cur = players.get(idx);

        // Calculate raw new position and cap at final square
        BoardPos rawPos = new BoardPos(cur.position().index() + dice.value());
        BoardPos finalPos = rawPos.index() > board.finalSquare().index()
                             ? board.finalSquare()
                             : rawPos;

        // Check square type and get destination
        Square sq = board.squares().getOrDefault(finalPos, new Normal(finalPos));
        BoardPos dest = switch (sq) {
            case Snake s -> s.end();
            case Ladder l -> l.end();
            case Normal n -> n.position();
        };

        players.set(idx, new Player(cur.name(), dest));
        int nextIdx = (idx + 1) % players.size();
        return new GameState(board, players, nextIdx);
    }

    // Check outcome
    public static Outcome checkOutcome(GameState state) {
        for (Player p : state.players()) {
            if (p.position().equals(state.board().finalSquare())) {
                return new Win(p);
            }
        }
        return new Ongoing(state);
    }

    // Game loop
    public static void play(List<String> playerNames) {
        Board board = createStandardBoard();
        List<Player> players = playerNames.stream()
            .map(n -> new Player(n, new BoardPos(0 + 1))) // start at 1? or define start position 1?
            .collect(Collectors.toList());
        // If start is square 1 as ladder, use new BoardPos(1). To start off-board, consider BoardPos(0) if allowed.
        GameState state = new GameState(board, players, 0);
        Scanner scanner = new Scanner(System.in);

        while (true) {
            Outcome res = checkOutcome(state);
            if (res instanceof Win w) {
                System.out.printf("%s wins the game!\n", w.winner().name());
                break;
            }
            Player cur = state.players().get(state.currentPlayerIndex());
            System.out.printf("%s's turn. Press Enter to roll the die...\n", cur.name());
            scanner.nextLine();
            Dice dice = rollDie();
            System.out.printf("Rolled: %d\n", dice.value());
            state = applyMove(state, dice);
            Player updated = state.players().get((state.currentPlayerIndex() + players.size() - 1) % players.size());
            System.out.printf("%s moves to %d\n", updated.name(), updated.position().index());
            System.out.println("--------------------------------");
        }
    }

    public static void main(String[] args) {
        play(List.of("Alice", "Bob"));
    }
}
