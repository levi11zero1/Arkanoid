package game;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import function.ScoreManager;
import function.GameSession;
import levels.LevelManager;
import powerup.PowerUpManager;
import ui.UIManager;
import utils.AudioManager;

/**
 * GameController: xử lý logic điều khiển luồng trò chơi (level complete, game over, restart).
 */
public class GameController {
    private final GamePanel panel;
    private final IGameLoop gameLoop;
    private final LevelManager levelManager;
    private final PowerUpManager powerUpManager;
    private final ScoreManager scoreManager;
    private final GameSession gameSession;
    private final UIManager uiManager;

    public GameController(GamePanel panel,
                          IGameLoop gameLoop,
                          LevelManager levelManager,
                          PowerUpManager powerUpManager,
                          ScoreManager scoreManager,
                          GameSession gameSession,
                          UIManager uiManager) {
        this.panel = panel;
        this.gameLoop = gameLoop;
        this.levelManager = levelManager;
        this.powerUpManager = powerUpManager;
        this.scoreManager = scoreManager;
        this.gameSession = gameSession;
        this.uiManager = uiManager;
    }

    public void handleLevelComplete() {

        panel.resetAllPowerUps();
        // hoàn thành 1 màn
        panel.incrementLevelsCompleted();
        if (levelManager.isFinalLevel()) {
            if (gameLoop != null) gameLoop.stop();
            showGameComplete();
        } else {
            if (gameLoop != null) gameLoop.stop();
            showLevelComplete();
        }
    }

    public void handleGameOver() {
        panel.resetAllPowerUps();
        if (gameLoop != null) gameLoop.stop();

        try {
            AudioManager.playOnce("music/lose.wav", () -> {
                SwingUtilities.invokeLater(() -> {
                    if (scoreManager != null && gameSession != null)
                        scoreManager.submitIfNotSubmitted(gameSession);
                    if (panel.getEventsListener() != null) {
                        panel.getEventsListener().onGameOver();
                        return;
                    }
                    showGameOverDialogAndHandleChoice();
                });
            });
        } catch (Throwable t) {
            if (scoreManager != null && gameSession != null)
                scoreManager.submitIfNotSubmitted(gameSession);
            if (panel.getEventsListener() != null) {
                panel.getEventsListener().onGameOver();
                return;
            }
            showGameOverDialogAndHandleChoice();
        }
    }

    private void showGameOverDialogAndHandleChoice() {
        int choice = uiManager.showConfirm(panel, "Game Over",
                "Game Over! You reached Level " + levelManager.getCurrentLevel() +
                        "\n\nWould you like to play again?",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            restartGame();
        } else {
            System.exit(0);
        }
    }

    private void showLevelComplete() {
        int choice = uiManager.showConfirm(panel, "Level Complete",
                "Level " + levelManager.getCurrentLevel() + " Complete!\n\n" +
                        "Continue to Level " + (levelManager.getCurrentLevel() + 1) + "?",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            levelManager.advanceLevel();
            panel.initializeLevel();
            if (gameLoop != null) gameLoop.start();
        } else {
            System.exit(0);
        }
    }

    private void showGameComplete() {
        if (scoreManager != null && gameSession != null)
            scoreManager.submitIfNotSubmitted(gameSession);

        int choice = uiManager.showConfirm(panel, "Game Complete",
                "Congratulations! You completed all " + levelManager.getMaxLevels() +
                        " levels!\n\nWould you like to play again?",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            restartGame();
        } else {
            System.exit(0);
        }
    }

    public void restartGame() {
        levelManager.reset();
        panel.resetLivesForNewSession();
        panel.initializeLevel();
        if (gameLoop != null) gameLoop.start();
        panel.resetRunStatsForNewSession();
    }
}
