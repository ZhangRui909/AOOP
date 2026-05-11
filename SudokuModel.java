import java.util.Observer;

public interface SudokuModel {

    int SIZE = 9;

    int getCell(int row, int col);
    int getInitial(int row, int col);
    boolean isPrefilled(int row, int col);

    void setCell(int row, int col, int value);
    void clearCell(int row, int col);

    void undo();
    void hint();
    void reset();
    void newGame();

    boolean canUndo();
    boolean canHint();
    boolean isComplete();
    boolean isInvalidCell(int row, int col);

    boolean isValidationFeedback();
    boolean isHintEnabled();
    boolean isRandomPuzzle();
    void setValidationFeedback(boolean on);
    void setHintEnabled(boolean on);
    void setRandomPuzzle(boolean on);

    void addObserver(Observer o);
}
