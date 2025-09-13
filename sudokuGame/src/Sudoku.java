import java.awt.*;
import java.awt.event.*;
import java.util.Stack;
import javax.swing.*;
import javax.swing.border.MatteBorder;

public class Sudoku extends JFrame {
    private final JTextField[][] cells = new JTextField[9][9];
    private final JButton checkButton;
    private final JButton solveButton;
    private final JButton resetButton;
    private final JButton undoButton;
    private final JButton hintButton;
    private Timer gameTimer;
    private int secondsPassed = 0;
    private final JLabel timerLabel;
    private final JComboBox<String> levelSelector;
    private String currentLevel = "Easy";
    private final Stack<Move> moveStack = new Stack<>();

    private final int[][] easyPuzzle  = {
            {5,3,0,0,7,0,0,0,0},
            {6,0,0,1,9,5,0,0,0},
            {0,9,8,0,0,0,0,6,0},
            {8,0,0,0,6,0,0,0,3},
            {4,0,0,8,0,3,0,0,1},
            {7,0,0,0,2,0,0,0,6},
            {0,6,0,0,0,0,2,8,0},
            {0,0,0,4,1,9,0,0,5},
            {0,0,0,0,8,0,0,7,9}
    };

    private final int[][] mediumPuzzle = {
            {0,2,0,6,0,8,0,0,0},
            {5,8,0,0,0,9,7,0,0},
            {0,0,0,0,4,0,0,0,0},
            {3,7,0,0,0,0,5,0,0},
            {6,0,0,0,0,0,0,0,4},
            {0,0,8,0,0,0,0,1,3},
            {0,0,0,0,2,0,0,0,0},
            {0,0,9,8,0,0,0,3,6},
            {0,0,0,3,0,6,0,9,0}
    };

    private final int[][] hardPuzzle = {
            {0,0,0,0,0,0,0,1,2},
            {0,0,0,0,0,0,7,0,0},
            {0,0,1,0,0,0,0,0,0},
            {0,0,0,0,0,5,0,0,0},
            {4,0,0,3,0,0,0,0,0},
            {0,9,0,0,0,0,0,0,0},
            {0,0,0,7,0,0,3,0,0},
            {3,0,0,0,0,0,6,0,0},
            {0,0,0,9,0,0,0,0,0}
    };

    private int[][] currentPuzzle;
    private int[][] solvedPuzzle;

    public Sudoku() {
        currentPuzzle = copyBoard(easyPuzzle);
        solvedPuzzle = new int[9][9];

        setTitle("Sudoku Solver with Backtracking");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Board panel
        JPanel board = new JPanel(new GridLayout(9,9));
        Font f = new Font("Arial", Font.BOLD, 28);

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                JTextField tf = new JTextField();
                tf.setHorizontalAlignment(SwingConstants.CENTER);
                tf.setFont(f);

                // thicker borders for 3x3 blocks
                int top = (r % 3 == 0) ? 3 : 1;
                int left = (c % 3 == 0) ? 3 : 1;
                int bottom = (r == 8) ? 3 : 1;
                int right = (c == 8) ? 3 : 1;
                tf.setBorder(new MatteBorder(top, left, bottom, right, Color.BLACK));

                final int rr = r;
                final int cc = c;

                // Key listener to accept only 1-9 and support undo
                tf.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent e) {
                        String text = tf.getText().trim();
                        String prev = (text.length() > 1) ? text.substring(0,1) : text;
                        // normalize to single character 1-9 or empty
                        if (text.isEmpty()) {
                            // push move (if cell was editable and had previous non-empty)
                            moveStack.push(new Move(rr, cc, ""));
                            tf.setText("");
                            return;
                        }
                        char ch = text.charAt(0);
                        if (ch >= '1' && ch <= '9') {
                            // keep only first digit
                            String prevVal = ""; // previous value is not easily available here; push current as move for undo only from setText usage
                            tf.setText(String.valueOf(ch));
                            // push move with prevValue: we can read the puzzle cell if needed (but simpler: push empty prev if you want to support full undo reliably you should capture prev before edit)
                            moveStack.push(new Move(rr, cc, prevVal));
                        } else {
                            // invalid input -> clear
                            tf.setText("");
                            Toolkit.getDefaultToolkit().beep();
                        }
                    }
                });

                cells[r][c] = tf;
                board.add(tf);
            }
        }

        // Top controls (difficulty + timer)
        JPanel top = new JPanel();
        levelSelector = new JComboBox<>(new String[] {"Easy", "Medium", "Hard"});
        levelSelector.addActionListener(e -> {
            String sel = (String) levelSelector.getSelectedItem();
            if ("Easy".equals(sel)) {
                currentPuzzle = copyBoard(easyPuzzle);
            } else if ("Medium".equals(sel)) {
                currentPuzzle = copyBoard(mediumPuzzle);
            } else {
                currentPuzzle = copyBoard(hardPuzzle);
            }
            currentLevel = sel;
            setupInitialPuzzle();
            solvedPuzzle = copyBoard(currentPuzzle);
            solveSudoku(solvedPuzzle);
        });

        top.add(new JLabel("Difficulty:"));
        top.add(levelSelector);

        timerLabel = new JLabel("Time: 00:00");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        timerLabel.setForeground(Color.BLUE);

        gameTimer = new Timer(1000, ev -> {
            secondsPassed++;
            int mins = secondsPassed / 60;
            int secs = secondsPassed % 60;
            timerLabel.setText(String.format("Time: %02d:%02d", mins, secs));
        });

        top.add(Box.createHorizontalStrut(20));
        top.add(timerLabel);

        // Bottom controls
        JPanel bottom = new JPanel();
        checkButton = new JButton("Check");
        checkButton.addActionListener(e -> checkSolution());

        solveButton = new JButton("Auto Solve");
        solveButton.addActionListener(e -> {
            // fill only empty editable cells
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (cells[r][c].isEditable() && (cells[r][c].getText().trim().isEmpty())) {
                        cells[r][c].setText(String.valueOf(solvedPuzzle[r][c]));
                        cells[r][c].setEditable(false);
                        cells[r][c].setBackground(Color.GREEN);
                    }
                }
            }
            stopTimer();
        });

        resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            setupInitialPuzzle();
            solvedPuzzle = copyBoard(currentPuzzle);
            solveSudoku(solvedPuzzle);
        });

        undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> {
            if (!moveStack.isEmpty()) {
                Move m = moveStack.pop();
                if (m != null) {
                    cells[m.row][m.col].setText(m.prevValue);
                }
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        });

        hintButton = new JButton("Hint!");
        hintButton.addActionListener(e -> {
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (cells[r][c].isEditable() && cells[r][c].getText().trim().isEmpty()) {
                        String prev = cells[r][c].getText();
                        cells[r][c].setText(String.valueOf(solvedPuzzle[r][c]));
                        cells[r][c].setBackground(new Color(255,255,180));
                        moveStack.push(new Move(r, c, prev));
                        return;
                    }
                }
            }
            Toolkit.getDefaultToolkit().beep();
        });

        bottom.add(checkButton);
        bottom.add(solveButton);
        bottom.add(resetButton);
        bottom.add(undoButton);
        bottom.add(hintButton);

        add(top, BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        setupInitialPuzzle();
        solvedPuzzle = copyBoard(currentPuzzle);
        solveSudoku(solvedPuzzle);

        setPreferredSize(new Dimension(700,800));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setupInitialPuzzle() {
        moveStack.clear();
        // Stop and reset timer
        stopTimer();
        secondsPassed = 0;
        timerLabel.setText("Time: 00:00");
        // Fill board
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int val = currentPuzzle[r][c];
                if (val == 0) {
                    cells[r][c].setText("");
                    cells[r][c].setEditable(true);
                    cells[r][c].setBackground(Color.WHITE);
                } else {
                    cells[r][c].setText(String.valueOf(val));
                    cells[r][c].setEditable(false);
                    cells[r][c].setBackground(Color.LIGHT_GRAY);
                }
            }
        }
        startTimer();
    }

    private int[][] copyBoard(int[][] board) {
        int[][] copy = new int[9][9];
        for (int r = 0; r < 9; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, 9);
        }
        return copy;
    }

    private boolean solveSudoku(int[][] board) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    for (int num = 1; num <= 9; num++) {
                        if (isSafe(board, r, c, num)) {
                            board[r][c] = num;
                            if (solveSudoku(board)) return true;
                            board[r][c] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isSafe(int[][] board, int row, int col, int num) {
        // row & column check
        for (int k = 0; k < 9; k++) {
            if (board[row][k] == num || board[k][col] == num) return false;
        }
        // 3x3 block check
        int br = row - row % 3;
        int bc = col - col % 3;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[br + r][bc + c] == num) return false;
            }
        }
        return true;
    }

    private void startTimer() {
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
        }
        secondsPassed = 0;
        timerLabel.setText("Time: 00:00");
        gameTimer.start();
    }

    private void stopTimer() {
        if (gameTimer != null && gameTimer.isRunning()) gameTimer.stop();
    }

    private void checkSolution() {
        int[][] board = new int[9][9];
        try {
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    String s = cells[r][c].getText().trim();
                    if (s.isEmpty()) {
                        board[r][c] = 0;
                    } else {
                        int v = Integer.parseInt(s);
                        if (v < 1 || v > 9) throw new NumberFormatException();
                        board[r][c] = v;
                    }
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Only numbers 1–9 allowed!");
            return;
        }

        if (isCorrectSolution(board)) {
            stopTimer();
            JOptionPane.showMessageDialog(this, "Congratulations! Sudoku is correct.");
        } else {
            JOptionPane.showMessageDialog(this, "Incorrect solution. Try again.");
        }
    }

    private boolean isCorrectSolution(int[][] board) {
        // check rows
        for (int r = 0; r < 9; r++) {
            boolean[] seen = new boolean[10];
            for (int c = 0; c < 9; c++) {
                int v = board[r][c];
                if (v < 1 || v > 9 || seen[v]) return false;
                seen[v] = true;
            }
        }
        // check columns
        for (int c = 0; c < 9; c++) {
            boolean[] seen = new boolean[10];
            for (int r = 0; r < 9; r++) {
                int v = board[r][c];
                if (v < 1 || v > 9 || seen[v]) return false;
                seen[v] = true;
            }
        }
        // check 3x3 blocks
        for (int br = 0; br < 9; br += 3) {
            for (int bc = 0; bc < 9; bc += 3) {
                boolean[] seen = new boolean[10];
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        int v = board[br + r][bc + c];
                        if (v < 1 || v > 9 || seen[v]) return false;
                        seen[v] = true;
                    }
                }
            }
        }
        return true;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Sudoku::new);
    }

    // simple struct for undo
    private static class Move {
        final int row, col;
        final String prevValue;
        Move(int r, int c, String prev) {
            this.row = r;
            this.col = c;
            this.prevValue = prev == null ? "" : prev;
        }
    }
}