# Sudoku Solver with Backtracking

A Java Swing application that allows users to play Sudoku at different difficulty levels, check their solution, request hints, undo moves, and auto-solve puzzles using a backtracking algorithm.

## Features

- **Difficulty Levels**: Easy, Medium, Hard.
- **Interactive Board**: Users can fill in numbers by typing in editable cells.
- **Validation**: 
  - "Check" button verifies if the current solution is correct.
  - Prevents invalid inputs (only digits 1–9 are allowed).
- **Undo Functionality**: Revert the last move.
- **Hint System**: Reveals the correct number in the next empty cell.
- **Auto Solve**: Automatically solves the Sudoku using backtracking.
- **Timer**: Tracks the time taken to complete the puzzle.
- **Reset**: Resets the puzzle to its initial state.

## Technologies Used

- **Java** (JDK 8 or higher recommended)
- **Swing** for the graphical user interface
- **Backtracking Algorithm** for solving Sudoku puzzles

## How to Run

1. Clone the repository:

   ```bash
   git clone https://github.com/nishanthreddy16/SudokuSolver.git
