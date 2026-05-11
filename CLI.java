import java.util.Scanner;

public class CLI {

    private static final int N = SudokuModel.SIZE;
    private final SudokuModel model;
    private final Scanner in = new Scanner(System.in);

    public CLI(SudokuModel model) {
        this.model = model;
    }

    public static void main(String[] args) {
        String path = args.length > 0 ? args[0] : "puzzles.txt";
        new CLI(new Model(path)).run();
    }

    public void run() {
        printHelp();
        render();
        while (true) {
            System.out.print("> ");
            if (!in.hasNextLine()) break;
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;
            if (handle(line)) break;
            render();
            if (model.isComplete()) {
                System.out.println("Puzzle solved.");
                break;
            }
        }
    }

    private boolean handle(String line) {
        String[] parts = line.split("\\s+");
        switch (parts[0].toLowerCase()) {
            case "set":   doSet(parts); return false;
            case "clear": doClear(parts); return false;
            case "undo":  doUndo(); return false;
            case "hint":  doHint(); return false;
            case "reset": model.reset(); return false;
            case "new":   model.newGame(); return false;
            case "help":  printHelp(); return false;
            case "quit":
            case "exit":  return true;
            default: System.out.println("unknown command, type 'help'"); return false;
        }
    }

    private void doSet(String[] p) {
        if (p.length != 4) { System.out.println("usage: set <row> <col> <value>  (1-9)"); return; }
        try {
            int r = Integer.parseInt(p[1]) - 1;
            int c = Integer.parseInt(p[2]) - 1;
            int v = Integer.parseInt(p[3]);
            if (!inRange(r, c) || v < 1 || v > 9) { System.out.println("out of range"); return; }
            if (model.isPrefilled(r, c)) { System.out.println("cell is pre-filled, cannot modify"); return; }
            model.setCell(r, c, v);
            if (model.isValidationFeedback() && model.isInvalidCell(r, c))
                System.out.println("note: that move creates a duplicate");
        } catch (NumberFormatException e) { System.out.println("numbers only"); }
    }

    private void doClear(String[] p) {
        if (p.length != 3) { System.out.println("usage: clear <row> <col>"); return; }
        try {
            int r = Integer.parseInt(p[1]) - 1;
            int c = Integer.parseInt(p[2]) - 1;
            if (!inRange(r, c)) { System.out.println("out of range"); return; }
            if (model.isPrefilled(r, c)) { System.out.println("cell is pre-filled"); return; }
            model.clearCell(r, c);
        } catch (NumberFormatException e) { System.out.println("numbers only"); }
    }

    private void doUndo() {
        if (!model.canUndo()) { System.out.println("nothing to undo"); return; }
        model.undo();
    }

    private void doHint() {
        if (!model.canHint()) { System.out.println("hint disabled or board full"); return; }
        model.hint();
    }

    private void render() {
        System.out.println();
        System.out.println("    1 2 3   4 5 6   7 8 9");
        System.out.println("  +-------+-------+-------+");
        for (int r = 0; r < N; r++) {
            StringBuilder sb = new StringBuilder();
            sb.append(r + 1).append(' ').append('|');
            for (int c = 0; c < N; c++) {
                int v = model.getCell(r, c);
                char ch = v == 0 ? '.' : (char) ('0' + v);
                if (model.isInvalidCell(r, c)) sb.append('*').append(ch);
                else sb.append(' ').append(ch);
                if (c % 3 == 2) sb.append(" |");
            }
            System.out.println(sb);
            if (r % 3 == 2) System.out.println("  +-------+-------+-------+");
        }
    }

    private void printHelp() {
        System.out.println("Sudoku CLI -- commands:");
        System.out.println("  set <row> <col> <value>   place 1-9 at row/col (1-9 each)");
        System.out.println("  clear <row> <col>         erase a cell");
        System.out.println("  undo                      revert last move");
        System.out.println("  hint                      reveal one cell");
        System.out.println("  reset                     restore initial puzzle");
        System.out.println("  new                       load another puzzle");
        System.out.println("  help                      show this");
        System.out.println("  quit                      exit");
        System.out.println("Cells with '*' before them are invalid (validation feedback).");
    }

    private static boolean inRange(int r, int c) {
        return r >= 0 && r < N && c >= 0 && c < N;
    }
}
