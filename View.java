import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.Observable;
import java.util.Observer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

public class View extends JFrame implements Observer {

    private static final int N = SudokuModel.SIZE;

    private static final Color BG_PREFILLED = new Color(232, 240, 254);
    private static final Color BG_EDITABLE  = Color.WHITE;
    private static final Color BG_INVALID   = new Color(255, 205, 210);
    private static final Color BG_SELECTED  = new Color(187, 222, 251);
    private static final Color FG_PREFILLED = new Color(33, 33, 33);
    private static final Color FG_USER      = new Color(25, 118, 210);

    private static final Font  CELL_FONT_PREFILLED = new Font("SansSerif", Font.BOLD, 22);
    private static final Font  CELL_FONT_USER      = new Font("SansSerif", Font.PLAIN, 22);

    private final SudokuModel model;
    private final Controller controller;

    private final JButton[][] cells = new JButton[N][N];
    private JButton eraseBtn, undoBtn, hintBtn, resetBtn, newBtn;
    private final JButton[] numpad = new JButton[9];
    private JCheckBox validationBox, hintBox, randomBox;

    private int selRow = 0, selCol = 0;
    private boolean justCompleted = false;

    public View(SudokuModel model, Controller controller) {
        super("Sudoku");
        this.model = model;
        this.controller = controller;
        model.addObserver(this);

        buildUI();
        installKeyboardHandler();
        controller.setView(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        update(null, null);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(buildGrid(),    BorderLayout.CENTER);
        root.add(buildSidebar(), BorderLayout.EAST);
        root.add(buildFlags(),   BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildGrid() {
        JPanel grid = new JPanel(new GridLayout(N, N));
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(52, 52));
                b.setFocusPainted(false);
                b.setBorder(cellBorder(r, c));
                final int row = r, col = c;
                b.addActionListener(e -> { selRow = row; selCol = col; refresh(); });
                cells[r][c] = b;
                grid.add(b);
            }
        }
        return grid;
    }

    private Border cellBorder(int r, int c) {
        int top    = (r == 0)     ? 3 : (r % 3 == 0 ? 2 : 1);
        int left   = (c == 0)     ? 3 : (c % 3 == 0 ? 2 : 1);
        int bottom = (r == N - 1) ? 3 : 1;
        int right  = (c == N - 1) ? 3 : 1;
        return BorderFactory.createMatteBorder(top, left, bottom, right, Color.DARK_GRAY);
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

        JPanel pad = new JPanel(new GridLayout(3, 3, 4, 4));
        for (int i = 0; i < 9; i++) {
            final int v = i + 1;
            JButton b = new JButton(String.valueOf(v));
            b.setPreferredSize(new Dimension(46, 46));
            b.setFocusable(false);
            b.addActionListener(e -> controller.inputDigit(selRow, selCol, v));
            numpad[i] = b;
            pad.add(b);
        }

        eraseBtn = control("Erase", e -> controller.erase(selRow, selCol));
        undoBtn  = control("Undo",  e -> controller.undo());
        hintBtn  = control("Hint",  e -> controller.hint());
        resetBtn = control("Reset", e -> controller.reset());
        newBtn   = control("New Game", e -> controller.newGame());

        side.add(pad);
        side.add(Box.createVerticalStrut(8));
        side.add(eraseBtn);
        side.add(undoBtn);
        side.add(hintBtn);
        side.add(Box.createVerticalStrut(8));
        side.add(resetBtn);
        side.add(newBtn);
        return side;
    }

    private JButton control(String text, java.awt.event.ActionListener l) {
        JButton b = new JButton(text);
        b.setMaximumSize(new Dimension(160, 36));
        b.setAlignmentX(JButton.CENTER_ALIGNMENT);
        b.setFocusable(false);
        b.addActionListener(l);
        return b;
    }

    private JPanel buildFlags() {
        JPanel flags = new JPanel();
        validationBox = flagBox("Validation feedback", model.isValidationFeedback(),
                                e -> controller.setValidationFeedback(validationBox.isSelected()));
        hintBox       = flagBox("Hint enabled",        model.isHintEnabled(),
                                e -> controller.setHintEnabled(hintBox.isSelected()));
        randomBox     = flagBox("Random puzzle",       model.isRandomPuzzle(),
                                e -> controller.setRandomPuzzle(randomBox.isSelected()));
        flags.add(validationBox);
        flags.add(hintBox);
        flags.add(randomBox);
        return flags;
    }

    private JCheckBox flagBox(String text, boolean selected, java.awt.event.ActionListener l) {
        JCheckBox cb = new JCheckBox(text, selected);
        cb.setFocusable(false);
        cb.addActionListener(l);
        return cb;
    }

    private void installKeyboardHandler() {
        KeyEventDispatcher dispatcher = e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) return false;
            return handleKey(e);
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
    }

    private boolean handleKey(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
            controller.inputDigit(selRow, selCol, code - KeyEvent.VK_0);
            return true;
        }
        if (code >= KeyEvent.VK_NUMPAD1 && code <= KeyEvent.VK_NUMPAD9) {
            controller.inputDigit(selRow, selCol, code - KeyEvent.VK_NUMPAD0);
            return true;
        }
        switch (code) {
            case KeyEvent.VK_BACK_SPACE:
            case KeyEvent.VK_DELETE: controller.erase(selRow, selCol); return true;
            case KeyEvent.VK_LEFT:   move(0, -1); return true;
            case KeyEvent.VK_RIGHT:  move(0,  1); return true;
            case KeyEvent.VK_UP:     move(-1, 0); return true;
            case KeyEvent.VK_DOWN:   move(1,  0); return true;
            default: return false;
        }
    }

    private void move(int dr, int dc) {
        selRow = Math.max(0, Math.min(N - 1, selRow + dr));
        selCol = Math.max(0, Math.min(N - 1, selCol + dc));
        refresh();
    }

    @Override
    public void update(Observable o, Object arg) {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                JButton b = cells[r][c];
                int v = model.getCell(r, c);
                b.setText(v == 0 ? "" : String.valueOf(v));
                boolean prefilled = model.isPrefilled(r, c);
                b.setFont(prefilled ? CELL_FONT_PREFILLED : CELL_FONT_USER);
                b.setForeground(prefilled ? FG_PREFILLED : FG_USER);
                Color bg = prefilled ? BG_PREFILLED : BG_EDITABLE;
                if (model.isInvalidCell(r, c)) bg = BG_INVALID;
                if (r == selRow && c == selCol) bg = BG_SELECTED;
                b.setBackground(bg);
                b.setOpaque(true);
            }
        }
        validationBox.setSelected(model.isValidationFeedback());
        hintBox.setSelected(model.isHintEnabled());
        randomBox.setSelected(model.isRandomPuzzle());
        controller.refreshButtons();

        if (model.isComplete() && !justCompleted) {
            justCompleted = true;
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this,
                    "Puzzle solved.",
                    "Congratulations",
                    JOptionPane.INFORMATION_MESSAGE));
        } else if (!model.isComplete()) {
            justCompleted = false;
        }
    }

    private void refresh() {
        update(null, null);
    }

    public void setUndoEnabled(boolean on)    { undoBtn.setEnabled(on); }
    public void setHintEnabled(boolean on)    { hintBtn.setEnabled(on); }
    public void setEraseEnabled(boolean on)   { eraseBtn.setEnabled(on); }
    public void setNumpadEnabled(boolean on)  { for (JButton b : numpad) b.setEnabled(on); }
}
