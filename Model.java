import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Random;

public class Model extends Observable implements SudokuModel {

    private final int[][] board    = new int[SIZE][SIZE];
    private final int[][] initial  = new int[SIZE][SIZE];
    private final int[][] solution = new int[SIZE][SIZE];

    private final List<String> puzzles = new ArrayList<>();
    private final Random rng = new Random();

    private boolean validationFeedback = true;
    private boolean hintEnabled = true;
    private boolean randomPuzzle = true;
    private int fixedIndex = 0;

    private Move lastMove = null;

    private static final class Move {
        final int row, col, prev;
        Move(int row, int col, int prev) { this.row = row; this.col = col; this.prev = prev; }
    }

    /*@ @requires path != null && file at path exists and contains 81-digit puzzle lines
      @ @ensures puzzles.size() > 0 && a puzzle is loaded into board, initial, solution
      @ @ensures invariant()
      @*/
    public Model(String path) {
        loadPuzzles(path);
        assert !puzzles.isEmpty() : "puzzle file empty";
        loadGame();
        assert invariant();
    }

    public boolean invariant() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] < 0 || board[r][c] > 9) return false;
                if (initial[r][c] != 0 && board[r][c] != initial[r][c]) return false;
            }
        }
        return true;
    }

    @Override public int getCell(int row, int col)    { return board[row][col]; }
    @Override public int getInitial(int row, int col) { return initial[row][col]; }
    @Override public boolean isPrefilled(int row, int col) { return initial[row][col] != 0; }

    @Override public boolean isValidationFeedback() { return validationFeedback; }
    @Override public boolean isHintEnabled()        { return hintEnabled; }
    @Override public boolean isRandomPuzzle()       { return randomPuzzle; }

    /*@ @requires 0 <= row < 9 && 0 <= col < 9 && 1 <= value <= 9
      @ @requires !isPrefilled(row, col)
      @ @ensures getCell(row, col) == value
      @ @ensures canUndo() == true
      @ @ensures invariant()
      @*/
    @Override
    public void setCell(int row, int col, int value) {
        assert inRange(row, col) : "row/col out of range";
        assert value >= 1 && value <= 9 : "value must be 1..9";
        assert !isPrefilled(row, col) : "cannot modify pre-filled cell";

        lastMove = new Move(row, col, board[row][col]);
        board[row][col] = value;

        assert board[row][col] == value;
        assert invariant();
        setChanged();
        notifyObservers();
    }

    /*@ @requires 0 <= row < 9 && 0 <= col < 9 && !isPrefilled(row, col)
      @ @ensures getCell(row, col) == 0
      @ @ensures invariant()
      @*/
    @Override
    public void clearCell(int row, int col) {
        assert inRange(row, col);
        assert !isPrefilled(row, col) : "cannot clear pre-filled cell";

        if (board[row][col] == 0) return;
        lastMove = new Move(row, col, board[row][col]);
        board[row][col] = 0;

        assert board[row][col] == 0;
        assert invariant();
        setChanged();
        notifyObservers();
    }

    /*@ @requires canUndo()
      @ @ensures !canUndo()
      @ @ensures invariant()
      @*/
    @Override
    public void undo() {
        assert canUndo() : "no move to undo";
        Move m = lastMove;
        board[m.row][m.col] = m.prev;
        lastMove = null;

        assert lastMove == null;
        assert invariant();
        setChanged();
        notifyObservers();
    }

    /*@ @requires canHint()
      @ @ensures one previously-empty cell now holds its solution value
      @ @ensures invariant()
      @*/
    @Override
    public void hint() {
        assert canHint() : "hint not available";
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == 0) {
                    lastMove = new Move(r, c, 0);
                    board[r][c] = solution[r][c];
                    assert board[r][c] == solution[r][c];
                    assert invariant();
                    setChanged();
                    notifyObservers();
                    return;
                }
            }
        }
    }

    /*@ @ensures for all r,c: getCell(r,c) == getInitial(r,c)
      @ @ensures !canUndo()
      @ @ensures invariant()
      @*/
    @Override
    public void reset() {
        copy(initial, board);
        lastMove = null;
        assert !canUndo();
        assert invariant();
        setChanged();
        notifyObservers();
    }

    /*@ @ensures a fresh puzzle is loaded into board, initial, solution
      @ @ensures !canUndo()
      @ @ensures invariant()
      @*/
    @Override
    public void newGame() {
        loadGame();
        assert !canUndo();
        assert invariant();
        setChanged();
        notifyObservers();
    }

    @Override public boolean canUndo() { return lastMove != null; }
    @Override public boolean canHint() { return hintEnabled && hasEmpty(); }

    /*@ @ensures result == true iff every cell matches the solution
      @*/
    @Override
    public boolean isComplete() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (board[r][c] != solution[r][c]) return false;
        return true;
    }

    /*@ @requires 0 <= row < 9 && 0 <= col < 9
      @ @ensures result == true iff validation flag is on, the cell is non-empty,
      @          and another cell in its row, column or 3x3 box holds the same value
      @*/
    @Override
    public boolean isInvalidCell(int row, int col) {
        if (!validationFeedback) return false;
        int v = board[row][col];
        if (v == 0) return false;
        for (int i = 0; i < SIZE; i++) {
            if (i != col && board[row][i] == v) return true;
            if (i != row && board[i][col] == v) return true;
        }
        int br = row - row % 3, bc = col - col % 3;
        for (int r = br; r < br + 3; r++)
            for (int c = bc; c < bc + 3; c++)
                if ((r != row || c != col) && board[r][c] == v) return true;
        return false;
    }

    @Override public void setValidationFeedback(boolean on) { validationFeedback = on; setChanged(); notifyObservers(); }
    @Override public void setHintEnabled(boolean on)        { hintEnabled = on;        setChanged(); notifyObservers(); }
    @Override public void setRandomPuzzle(boolean on)       { randomPuzzle = on;       setChanged(); notifyObservers(); }

    void seedFixedIndex(int idx) { this.fixedIndex = idx; }

    private void loadPuzzles(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 81) puzzles.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("cannot read puzzles from " + path, e);
        }
    }

    private void loadGame() {
        int idx = randomPuzzle ? rng.nextInt(puzzles.size()) : fixedIndex % puzzles.size();
        parsePuzzle(puzzles.get(idx), initial);
        copy(initial, board);
        copy(initial, solution);
        if (!solve(solution)) throw new IllegalStateException("puzzle has no solution");
        lastMove = null;
    }

    private static void parsePuzzle(String line, int[][] dst) {
        for (int i = 0; i < 81; i++) dst[i / SIZE][i % SIZE] = line.charAt(i) - '0';
    }

    private static void copy(int[][] src, int[][] dst) {
        for (int r = 0; r < SIZE; r++) System.arraycopy(src[r], 0, dst[r], 0, SIZE);
    }

    private boolean solve(int[][] g) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (g[r][c] == 0) {
                    for (int v = 1; v <= 9; v++) {
                        if (isLegal(g, r, c, v)) {
                            g[r][c] = v;
                            if (solve(g)) return true;
                            g[r][c] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isLegal(int[][] g, int row, int col, int v) {
        for (int i = 0; i < SIZE; i++)
            if (g[row][i] == v || g[i][col] == v) return false;
        int br = row - row % 3, bc = col - col % 3;
        for (int r = br; r < br + 3; r++)
            for (int c = bc; c < bc + 3; c++)
                if (g[r][c] == v) return false;
        return true;
    }

    private boolean hasEmpty() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (board[r][c] == 0) return true;
        return false;
    }

    private static boolean inRange(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }
}
