package main

import (
    "bufio"
    "errors"
    "fmt"
    "math/rand"
    "os"
    "time"
)

// BoardPos wraps an int [1..100]
type BoardPos struct {
    Index int
}

func NewBoardPos(i int) (BoardPos, error) {
    if i < 1 || i > 100 {
        return BoardPos{}, fmt.Errorf("BoardPos out of bounds: %d", i)
    }
    return BoardPos{i}, nil
}

// DieRoll wraps an int [1..6]
type DieRoll struct {
    Value int
}

func NewDieRoll(v int) (DieRoll, error) {
    if v < 1 || v > 6 {
        return DieRoll{}, fmt.Errorf("DieRoll must be 1–6: %d", v)
    }
    return DieRoll{v}, nil
}

func RollDie() DieRoll {
    v := rand.Intn(6) + 1
    dr, _ := NewDieRoll(v)
    return dr
}

// Square sum type
type Square interface {
    Dest() BoardPos
}

type Normal struct{ Pos BoardPos }
func (n Normal) Dest() BoardPos      { return n.Pos }

type Snake struct{ From, To BoardPos }
func (s Snake) Dest() BoardPos       { return s.To }

type Ladder struct{ From, To BoardPos }
func (l Ladder) Dest() BoardPos      { return l.To }

// Board holds the map and final square
type Board struct {
    Squares    map[int]Square
    FinalSquare BoardPos
}

func CreateStandardBoard() Board {
    squares := make(map[int]Square, 100)
    for i := 1; i <= 100; i++ {
        pos, _ := NewBoardPos(i)
        squares[i] = Normal{pos}
    }
    add := func(from, to int, sq Square) {
        squares[from] = sq
    }
    // Snakes
    add(16, 6, Snake{mustBP(16), mustBP(6)})
    add(47, 26, Snake{mustBP(47), mustBP(26)})
    // ... (other snakes) ...
    add(98, 78, Snake{mustBP(98), mustBP(78)})
    // Ladders
    add(1, 38, Ladder{mustBP(1), mustBP(38)})
    add(4, 14, Ladder{mustBP(4), mustBP(14)})
    // ... (other ladders) ...
    add(80, 100, Ladder{mustBP(80), mustBP(100)})

    final, _ := NewBoardPos(100)
    return Board{Squares: squares, FinalSquare: final}
}

func mustBP(i int) BoardPos {
    bp, err := NewBoardPos(i)
    if err != nil {
        panic(err)
    }
    return bp
}

// Player
type Player struct {
    Name     string
    Position BoardPos
}

// GameState
type GameState struct {
    Board              Board
    Players            []Player
    CurrentPlayerIndex int
}

// Outcome sum type
type Outcome interface{}

type Ongoing struct{ State GameState }
type Win struct{ Winner Player }

func applyMove(gs GameState, dr DieRoll) GameState {
    b := gs.Board
    ps := append([]Player(nil), gs.Players...) // copy
    idx := gs.CurrentPlayerIndex
    cur := ps[idx]

    raw := cur.Position.Index + dr.Value
    var newPos BoardPos
    if raw > b.FinalSquare.Index {
        newPos = b.FinalSquare
    } else {
        newPos = mustBP(raw)
    }

    square := b.Squares[newPos.Index]
    dest := square.Dest()
    ps[idx].Position = dest

    next := (idx + 1) % len(ps)
    return GameState{b, ps, next}
}

func checkOutcome(gs GameState) Outcome {
    for _, p := range gs.Players {
        if p.Position == gs.Board.FinalSquare {
            return Win{p}
        }
    }
    return Ongoing{gs}
}

func play(names []string) {
    rand.Seed(time.Now().UnixNano())
    board := CreateStandardBoard()
    players := make([]Player, len(names))
    start, _ := NewBoardPos(1)
    for i, n := range names {
        players[i] = Player{Name: n, Position: start}
    }
    state := GameState{board, players, 0}
    reader := bufio.NewReader(os.Stdin)

    for {
        if res := checkOutcome(state); win, ok := res.(Win); ok {
            fmt.Printf("%s wins the game!\n", win.Winner.Name)
            return
        }
        cur := state.Players[state.CurrentPlayerIndex]
        fmt.Printf("%s's turn. Press Enter to roll...\n", cur.Name)
        reader.ReadString('\n')
        roll := RollDie()
        fmt.Printf("Rolled: %d\n", roll.Value)
        state = applyMove(state, roll)
        prev := (state.CurrentPlayerIndex + len(state.Players) - 1) % len(state.Players)
        moved := state.Players[prev]
        fmt.Printf("%s moves to %d\n", moved.Name, moved.Position.Index)
        fmt.Println("--------------------------------")
    }
}

func main() {
    play([]string{"Alice", "Bob"})
}
