import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ModelTest {

    private static final String PUZZLE =
            "001700509573024106800501002700295018009400305652800007465080071000159004908007053";

    private Path puzzleFile;
    private Model model;

    @Before
    public void setUp() throws IOException {
        puzzleFile = Files.createTempFile("puzzles", ".txt");
        Files.write(puzzleFile, PUZZLE.getBytes(StandardCharsets.UTF_8));
        model = new Model(puzzleFile.toString());
        model.setRandomPuzzle(false);
        model.seedFixedIndex(0);
        model.newGame();
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(puzzleFile);
    }

    /*
     * Scenario 1 -- Completion detection.
     * Pre: a valid puzzle is loaded with some empty editable cells.
     * Post: after each editable cell is set to its solution value, isComplete() is true
     *       and the model invariant holds at every intermediate step.
     */
    @Test
    public void completionDetectedWhenAllCellsFilledCorrectly() {
        int[][] solution = solveAndReset(model);
        for (int r = 0; r < SudokuModel.SIZE; r++) {
            for (int c = 0; c < SudokuModel.SIZE; c++) {
                if (!model.isPrefilled(r, c)) {
                    model.setCell(r, c, solution[r][c]);
                    assertTrue("invariant must hold after every move", model.invariant());
                }
            }
        }
        assertTrue("board should be detected complete", model.isComplete());
    }

    /*
     * Scenario 2 -- Validation feedback flag.
     * Pre: pick an editable empty cell and a value that already exists elsewhere in its row.
     * Post: with the flag on, isInvalidCell() reports both cells as invalid; with the flag off,
     *       it reports neither. The set value is preserved either way (temporary invalid states
     *       are allowed and do not corrupt the board).
     */
    @Test
    public void validationFeedbackFlagControlsDuplicateReporting() {
        int row = 0;
        int existingCol = findExistingValueInRow(row);
        int existingValue = model.getCell(row, existingCol);
        int targetCol = findEditableEmptyCellInRow(row, existingCol);

        model.setValidationFeedback(true);
        model.setCell(row, targetCol, existingValue);

        assertEquals(existingValue, model.getCell(row, targetCol));
        assertTrue(model.isInvalidCell(row, targetCol));
        assertTrue(model.isInvalidCell(row, existingCol));

        model.setValidationFeedback(false);
        assertFalse(model.isInvalidCell(row, targetCol));
        assertFalse(model.isInvalidCell(row, existingCol));
    }

    /*
     * Scenario 3 -- Undo and reset.
     * Pre: pick an editable empty cell.
     * Post: setCell + undo restores the cell to empty and disables further undo;
     *       reset wipes any number of subsequent edits and returns the board to its initial state.
     */
    @Test
    public void undoRevertsLastMoveAndResetClearsAllUserInput() {
        int[][] initialBoard = boardSnapshot(model);

        int[] cell = findFirstEditableEmpty();
        int row = cell[0], col = cell[1];

        model.setCell(row, col, 5);
        assertEquals(5, model.getCell(row, col));
        assertTrue(model.canUndo());

        model.undo();
        assertEquals(0, model.getCell(row, col));
        assertFalse(model.canUndo());
        assertArrayEquals(initialBoard, boardSnapshot(model));

        model.setCell(row, col, 5);
        int[] another = findEditableEmptySkipping(row, col);
        model.setCell(another[0], another[1], 5);
        model.reset();
        assertArrayEquals(initialBoard, boardSnapshot(model));
        assertFalse(model.canUndo());
    }

    private static int[][] boardSnapshot(SudokuModel m) {
        int[][] b = new int[SudokuModel.SIZE][SudokuModel.SIZE];
        for (int r = 0; r < SudokuModel.SIZE; r++)
            for (int c = 0; c < SudokuModel.SIZE; c++)
                b[r][c] = m.getCell(r, c);
        return b;
    }

    private static int[][] solveAndReset(Model m) {
        while (m.canHint()) m.hint();
        int[][] solved = boardSnapshot(m);
        m.reset();
        return solved;
    }

    private int findExistingValueInRow(int row) {
        for (int c = 0; c < SudokuModel.SIZE; c++)
            if (model.getCell(row, c) != 0) return c;
        throw new IllegalStateException("row has no values");
    }

    private int findEditableEmptyCellInRow(int row, int avoidCol) {
        for (int c = 0; c < SudokuModel.SIZE; c++)
            if (c != avoidCol && !model.isPrefilled(row, c) && model.getCell(row, c) == 0) return c;
        throw new IllegalStateException("no editable empty cell in row");
    }

    private int[] findFirstEditableEmpty() {
        for (int r = 0; r < SudokuModel.SIZE; r++)
            for (int c = 0; c < SudokuModel.SIZE; c++)
                if (!model.isPrefilled(r, c) && model.getCell(r, c) == 0) return new int[]{r, c};
        throw new IllegalStateException("no editable empty cell");
    }

    private int[] findEditableEmptySkipping(int skipRow, int skipCol) {
        for (int r = 0; r < SudokuModel.SIZE; r++)
            for (int c = 0; c < SudokuModel.SIZE; c++)
                if (!model.isPrefilled(r, c) && model.getCell(r, c) == 0
                        && (r != skipRow || c != skipCol)) return new int[]{r, c};
        throw new IllegalStateException("no other editable empty cell");
    }
}
