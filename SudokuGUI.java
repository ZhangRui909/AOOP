import javax.swing.SwingUtilities;

public class SudokuGUI {

    public static void main(String[] args) {
        String path = args.length > 0 ? args[0] : "puzzles.txt";
        SwingUtilities.invokeLater(() -> {
            Model model = new Model(path);
            Controller controller = new Controller(model);
            new View(model, controller);
        });
    }
}
