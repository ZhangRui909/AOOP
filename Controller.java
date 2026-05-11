public class Controller {

    private final SudokuModel model;
    private View view;

    public Controller(SudokuModel model) {
        this.model = model;
    }

    public void setView(View view) {
        this.view = view;
    }

    public void inputDigit(int row, int col, int value) {
        if (model.isPrefilled(row, col)) return;
        if (value < 1 || value > 9) return;
        model.setCell(row, col, value);
    }

    public void erase(int row, int col) {
        if (model.isPrefilled(row, col)) return;
        if (model.getCell(row, col) == 0) return;
        model.clearCell(row, col);
    }

    public void undo()    { if (model.canUndo()) model.undo(); }
    public void hint()    { if (model.canHint()) model.hint(); }
    public void reset()   { model.reset(); }
    public void newGame() { model.newGame(); }

    public void setValidationFeedback(boolean on) { model.setValidationFeedback(on); }
    public void setHintEnabled(boolean on)        { model.setHintEnabled(on); }
    public void setRandomPuzzle(boolean on)       { model.setRandomPuzzle(on); }

    public void refreshButtons() {
        if (view == null) return;
        boolean playable = !model.isComplete();
        view.setUndoEnabled(model.canUndo() && playable);
        view.setHintEnabled(model.canHint() && playable);
        view.setNumpadEnabled(playable);
        view.setEraseEnabled(playable);
    }
}
