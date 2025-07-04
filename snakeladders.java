import java.util.*;
import java.util.stream.Collectors;

public class SnakeAndLadders {
    // Dice roll outcome
    record Dice(int value) {}

    // Square type: Normal, Snake, Ladder
    sealed interface Square permits Normal, Snake, Ladder {}
    record Normal(int position) implements Square {}
    record Snake(int start, int end) implements Square {}
    record Ladder(int start, int end) implements Square {}

    // Board representation
    record Board(Map<Integer, Square> squares, int finalSquare) {}

    // Player state
    record Player(String name, int position) {}

    // Game state
    record GameState(Board board, List<Player> players, int currentPlayerIndex) {}

    // Outcome
    sealed interface Outcome permits Ongoing, Win {}
    record Ongoing(GameState state) implements Outcome {}
    record Win(Player winner) implements Outcome {}

    // Build a standard 10x10 board with snakes and ladders
    static Board createStandardBoard() {
        int size = 100;
        Map<Integer, Square> map = new HashMap<>();
        for (int i = 1; i <= size; i++) {
            map.put(i, new Normal(i));
        }
        // Snakes
        map.put(16, new Snake(16, 6));
        map.put(47, new Snake(47, 26));
        map.put(49, new Snake(49, 11));
        map.put(56, new Snake(56, 53));
        map.put(62, new Snake(62, 19));
        map.put(64, new Snake(64, 60));
        map.put(87, new Snake(87, 24));
        map.put(93, new Snake(93, 73));
        map.put(95, new Snake(95, 75));
        map.put(98, new Snake(98, 78));
        // Ladders
        map.put(1, new Ladder(1, 38));
        map.put(4, new Ladder(4, 14));
        map.put(9, new Ladder(9, 31));
        map.put(21, new Ladder(21, 42));
        map.put(28, new Ladder(28, 84));
        map.put(36, new Ladder(36, 44));
        map.put(51, new Ladder(51, 67));
        map.put(71, new Ladder(71, 91));
        map.put(80, new Ladder(80, 100));
        return new Board(map, size);
    }

    // Roll a die: 1-6
    static Dice rollDie() {
        return new Dice(new Random().nextInt(6) + 1);
    }

    // Apply a move for the current player
    static GameState applyMove(GameState state, Dice dice) {
        Board board = state.board();
        List<Player> players = new ArrayList<>(state.players());
        int idx = state.currentPlayerIndex();
        Player cur = players.get(idx);
        int rawPos = cur.position() + dice.value();
        int finalPos = Math.min(rawPos, board.finalSquare()); // cannot exceed final

        // Check square type
        Square sq = board.squares().getOrDefault(finalPos, new Normal(finalPos));
        int dest = switch (sq) {
            case Snake s -> s.end();
            case Ladder l -> l.end();
            case Normal n -> n.position();
        };

        players.set(idx, new Player(cur.name(), dest));

        // Next player index
        int nextIdx = (idx + 1) % players.size();
        
        return new GameState(board, players, nextIdx);
    }

    // Check outcome
    static Outcome checkOutcome(GameState state) {
        for (Player p : state.players()) {
            if (p.position() == state.board().finalSquare()) {
                return new Win(p);
            }
        }
        return new Ongoing(state);
    }

    // Game loop
    static void play(List<String> playerNames) {
        Board board = createStandardBoard();
        List<Player> players = playerNames.stream().map(n -> new Player(n, 0)).collect(Collectors.toList());
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
            System.out.printf("%s moves to %d\n", updated.name(), updated.position());
            System.out.println("--------------------------------");
        }
    }

    public static void main(String[] args) {
        play(List.of("Alice", "Bob"));
    }
}
