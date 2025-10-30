package game;

import entities.Ball;
import entities.Block;
import entities.Paddle;
import function.GameState;
import function.Pause;
// import function.SaveManager; // removed: save orchestration moved to SaveController
import function.ScoreManager;
import function.GameSession;
import java.awt.*;
import java.awt.event.*;
import input.InputHandler;
import entities.EntityManager;
import powerup.PowerUpManager;
import ui.UIManager;
import java.util.ArrayList;
import java.util.List; // Ảnh chụp trạng thái game để lưu/khôi phục
import javax.swing.*; // Điều khiển tạm dừng/tiếp tục
import levels.LevelBuilder;
import levels.LevelManager;
// powerup.PowerUp and ui.StyledButton imports removed (not used here)
import utils.GameConfig;

public class GamePanel extends JPanel implements KeyListener {
    private Ball ball;
    private Paddle paddle;
    private List<Block> blocks;
    // Game timing is delegated to IGameLoop
    private LevelManager levelManager;
    private GameEvents eventsListener;

    // Managers (skeleton wiring for incremental refactor)
    private IGameLoop gameLoop;
    private IRenderer renderer;
    private EntityManager entityManager;
    private InputHandler inputHandler;
    private PowerUpManager powerUpManager;
    private UIManager uiManager;
    private GameController gameController;

    // Ranking/session
    private ScoreManager scoreManager;
    private GameSession gameSession;

    // Power-up management delegated
    // active list, timer and random are now owned by PowerUpManager

    private boolean leftPressed = false;
    private boolean rightPressed = false;

    // Save button is created by UIManager; GamePanel does not keep a reference

    // ==== RUN STATS ====
    private String playerName = "Player";
    private long elapsedMsAccum = 0; // tích lũy thời gian chơi (không tính Pause vì timer dừng)
    private int levelsCompleted = 0; // số màn đã hoàn thành
    private int totalBlocksDestroyed = 0; // tổng số block phá được qua các màn
    private int lastDestroyedCountThisLevel = 0; // baseline để tính delta mỗi tick

    public GamePanel() {
        levelManager = new LevelManager();
        initializeLevel();

        // Khởi tạo các manager
        this.gameLoop = new GameLoop();
        this.renderer = new Renderer();
        this.entityManager = new EntityManager();
        this.inputHandler = new InputHandler();
    this.powerUpManager = new PowerUpManager();
    this.uiManager = new UIManager();
        this.scoreManager = new ScoreManager();
        this.gameSession = new GameSession(playerName, elapsedMsAccum, levelsCompleted, totalBlocksDestroyed);

        // wire game loop để tick
        this.gameLoop.setTickListener(delta -> onTick(delta));
        this.gameLoop.start();

    // create controller to coordinate high-level game flow
    this.gameController = new GameController(this, this.gameLoop, this.levelManager, this.powerUpManager, this.scoreManager, this.gameSession, this.uiManager);

        // cung cấp thực thể cho EntityManager
        this.entityManager.setEntities(ball, paddle, blocks);

        // start power-up spawning via manager
        powerUpManager.startSpawning(this);

        // Đăng ký Pause: dừng timer khi pause, chạy lại khi resume
        Pause.getInstance().setListener(new Pause.PauseListener() {
            @Override
            public void onPause() {
                if (gameLoop != null)
                    gameLoop.stop();
                powerUpManager.stopSpawning();
            }

            @Override
            public void onResume() {
                if (gameLoop != null)
                    gameLoop.start();
                powerUpManager.startSpawning(GamePanel.this);
            }
        });

        setFocusable(true);
        addKeyListener(this);
        setBackground(Color.BLACK);

        // Absolute layout to freely position overlay button
        setLayout(null);
        uiManager.createSaveButton(this, saved -> {
            if (!saved) return; // cancelled or failed
            Toolkit.getDefaultToolkit().beep();
            // Stop timers to prevent further gameplay
            if (gameLoop != null)
                gameLoop.stop();
            powerUpManager.stopSpawning();
            // Inform container to go back to menu
            if (eventsListener != null) {
                eventsListener.onGameOver();
            } else {
                // Fallback: close application if no listener
                System.exit(0);
            }
        });
    }

    public void initializeLevel() {
        ball = new Ball(GameConfig.SCREEN_WIDTH / 2, GameConfig.SCREEN_HEIGHT / 2);
        paddle = new Paddle(
                GameConfig.SCREEN_WIDTH / 2 - GameConfig.PADDLE_WIDTH / 2,
                GameConfig.SCREEN_HEIGHT - 100);

        // Tạo block
        blocks = LevelBuilder.createLevel(levelManager.getCurrentLevel());
        // baseline destroyed count for this level
        lastDestroyedCountThisLevel = countDestroyedDestructable();

        // Cho biết EntityManager về các thực thể mới
        if (entityManager != null)
            entityManager.setEntities(ball, paddle, blocks);
    }

    

    // ================== LƯU/LOAD (PHỤC VỤ NÚT "TIẾP TỤC" Ở MENU)
    // ==================
    // Tạo ảnh chụp trạng thái hiện tại để ghi xuống file save.
    // Bao gồm: level đang chơi, vị trí/tốc độ bóng, vị trí thanh đỡ, và danh sách
    // block còn lại.
    public GameState toGameState() {
        ArrayList<GameState.BlockState> bs = new ArrayList<>();
        for (Block b : blocks) {
            bs.add(new GameState.BlockState(b.getX(), b.getY(), b.getHitsRemaining(), b.isDestroyed()));
        }
        return new GameState(
                levelManager.getCurrentLevel(),
                ball.getPreciseX(), ball.getPreciseY(),
                ball.getVelocity().getDx(), ball.getVelocity().getDy(),
                paddle.getX(), paddle.getY(),
                bs);
    }

    // Áp dụng trạng thái đã lưu vào game panel này.
    // Gọi từ ArkanoidGame khi người chơi chọn một bản save trong menu "Tiếp tục".
    public void applyGameState(GameState state) {
        // 1) Khớp level hiện tại
        levelManager.setLevel(state.level);

        // 2) Dựng lại danh sách block từ file save
        ArrayList<Block> newBlocks = new ArrayList<>();
        for (GameState.BlockState b : state.blocks) {
            newBlocks.add(new Block(b.x, b.y, b.hitsRemaining, b.destroyed));
        }
        this.blocks = newBlocks;

        // 3) Khôi phục bóng và thanh đỡ
        ball.setPosition(state.ballX, state.ballY);
        ball.setVelocity(new utils.Velocity(state.ballDx, state.ballDy));
        this.paddle = new Paddle((int) Math.round(state.paddleX), state.paddleY);

        // 4) baseline destroyed count for this level (avoid recounting
        // already-destroyed blocks)
        lastDestroyedCountThisLevel = countDestroyedDestructable();

        // 5) Vẽ lại
        if (entityManager != null)
            entityManager.setEntities(ball, paddle, blocks);
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (g instanceof Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            try {
                renderer.render(g2d, ball, paddle, blocks, powerUpManager.snapshot(), levelManager);
            } catch (Throwable t) {
                // keep paint resilient during refactor
            }
        }
        // Render UI overlay using UIManager (non-invasive call)
        if (g instanceof Graphics2D) {
            try {
                uiManager.renderOverlay((Graphics2D) g);
            } catch (Throwable t) {
                // keep rendering resilient during incremental refactor
            }
        }
    }

    // Called by GameLoop every tick
    private void onTick(double deltaTime) {
        updateGame(deltaTime);
        checkGameState();
        // tích lũy thời gian chơi
        elapsedMsAccum += (long) (deltaTime * 1000);
        // cập nhật số block phá (delta so với lần đo trước)
        int curDestroyed = countDestroyedDestructable();
        if (curDestroyed > lastDestroyedCountThisLevel) {
            totalBlocksDestroyed += (curDestroyed - lastDestroyedCountThisLevel);
            lastDestroyedCountThisLevel = curDestroyed;
        }

        // keep session in sync
        if (gameSession != null) {
            gameSession.setElapsedMs(elapsedMsAccum);
            gameSession.setLevelsCompleted(levelsCompleted);
            gameSession.setTotalBlocksDestroyed(totalBlocksDestroyed);
        }

        // delegate power-up updates to manager
        powerUpManager.updateAll(getHeight(), paddle, ball);

        repaint();
    }

    private void updateGame(double deltaTime) {
        // Delegate entity updates to EntityManager
        if (entityManager != null) {
            entityManager.updateAll(deltaTime, getWidth(), getHeight(), leftPressed, rightPressed);
        } else {
            if (ball != null) {
                ball.move();
                ball.checkBounds(getWidth(), getHeight());
            }
            if (paddle != null) {
                paddle.update(leftPressed, rightPressed, getWidth(), deltaTime);
            }
        }

    }

    private void checkGameState() {
        boolean allBlocksDestroyed = blocks.stream().allMatch(Block::isDestroyed);
        if (allBlocksDestroyed) {
            if (gameController != null) gameController.handleLevelComplete(); else handleLevelComplete();
        }

        if (ball.getY() > getHeight()) {
            if (gameController != null) gameController.handleGameOver(); else handleGameOver();
        }
    }

    private void handleLevelComplete() {
        // Delegate to GameController if present
        if (gameController != null) {
            gameController.handleLevelComplete();
            return;
        }

        // Fallback behaviour (legacy)
        resetAllPowerUps();
        // hoàn thành 1 màn
        levelsCompleted++;
        if (levelManager.isFinalLevel()) {
            gameLoop.stop();
            showGameComplete();
        } else {
            gameLoop.stop();
            showLevelComplete();
        }
    }

    private void handleGameOver() {
        // Cho delegate lên GameController nếu có
        if (gameController != null) {
            gameController.handleGameOver();
            return;
        }

        // Fallback legacy behaviour
        resetAllPowerUps();
        if (gameLoop != null)
            gameLoop.stop();

        try {
            utils.MusicPlayer.playOnce("music/lose.wav", () -> {
                SwingUtilities.invokeLater(() -> {
                    if (scoreManager != null && gameSession != null)
                        scoreManager.submitIfNotSubmitted(gameSession);
                    if (eventsListener != null) {
                        eventsListener.onGameOver();
                        return;
                    }
                    showGameOverDialogAndHandleChoice();
                });
            });
        } catch (Throwable t) {
            if (scoreManager != null && gameSession != null)
                scoreManager.submitIfNotSubmitted(gameSession);
            if (eventsListener != null) {
                eventsListener.onGameOver();
                return;
            }
            showGameOverDialogAndHandleChoice();
        }
    }

    // Centralized dialog used when there's no external event listener to handle
    // game-over
    private void showGameOverDialogAndHandleChoice() {
        int choice = uiManager.showConfirm(this, "Game Over",
                "Game Over! You reached Level " + levelManager.getCurrentLevel() +
                        "\\n\\nWould you like to play again?",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            restartGame();
        } else {
            System.exit(0);
        }
    }

    private void showLevelComplete() {
        int choice = uiManager.showConfirm(this, "Level Complete",
                "Level " + levelManager.getCurrentLevel() + " Complete!\\n\\n" +
                        "Continue to Level " + (levelManager.getCurrentLevel() + 1) + "?",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            levelManager.advanceLevel();
            initializeLevel();
            gameLoop.start();
        } else {
            System.exit(0);
        }
    }

    private void showGameComplete() {
        // Cập nhật Ranking (thắng toàn bộ)
        if (scoreManager != null && gameSession != null)
            scoreManager.submitIfNotSubmitted(gameSession);
        int choice = uiManager.showConfirm(this, "Game Complete",
                "Congratulations! You completed all " + levelManager.getMaxLevels() +
                        " levels!\\n\\nWould you like to play again?",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            restartGame();
        } else {
            System.exit(0);
        }
    }

    private void restartGame() {
        // Cho delegate lên GameController nếu có
        if (gameController != null) {
            gameController.restartGame();
            return;
        }

        levelManager.reset();
        initializeLevel();
        if (gameLoop != null)
            gameLoop.start();
        // reset run stats for a new session (used in local replay flow)
        elapsedMsAccum = 0;
        levelsCompleted = 0;
        totalBlocksDestroyed = 0;
        if (gameSession != null) {
            gameSession.setElapsedMs(0);
            gameSession.setLevelsCompleted(0);
            gameSession.setTotalBlocksDestroyed(0);
            gameSession.resetSubmitted();
        }
    }

    private int countDestroyedDestructable() {
        int c = 0;
        for (Block b : blocks) {
            if (b.getHitsRemaining() != GameConfig.UNDESTRUCTABLE_BLOCK && b.isDestroyed())
                c++;
        }
        return c;
    }

    private void showLevelMap() {
        // Delegate to LevelBuilder which now provides a reusable preview dialog
        String input = uiManager.promptInput(this, "Xem Map Level", "Nhập số level (1-" + GameConfig.MAX_LEVELS + "):");
        if (input == null)
            return;
        try {
            int levelNum = Integer.parseInt(input.trim());
            if (levelNum < 1 || levelNum > GameConfig.MAX_LEVELS) {
                uiManager.showMessage(this, "Lỗi", "Level phải từ 1 đến " + GameConfig.MAX_LEVELS, JOptionPane.ERROR_MESSAGE);
                return;
            }
            levels.LevelPreview.show(this, levelNum);
        } catch (NumberFormatException ex) {
            uiManager.showMessage(this, "Lỗi", "Vui lòng nhập số hợp lệ!", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            inputHandler.keyPressed(e, this);
        } catch (Throwable ignore) {
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        try {
            inputHandler.keyReleased(e, this);
        } catch (Throwable ignore) {
        }
    }
    // PowerUp spawn/update logic moved to PowerUpManager

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // Cho phép ArkanoidGame đăng ký lắng nghe sự kiện trong game
    public void setEventsListener(GameEvents listener) {
        this.eventsListener = listener;
    }

    public GameEvents getEventsListener() {
        return this.eventsListener;
    }

    // Các phương thức hỗ trợ InputHandler
    public void setLeftPressed(boolean v) { this.leftPressed = v; }
    public void setRightPressed(boolean v) { this.rightPressed = v; }
    public void togglePauseAction() { Pause.getInstance().toggle(); }
    public void startIfNotRunning() { if (gameLoop != null && !gameLoop.isRunning()) gameLoop.start(); }
    public int confirm(String title, String message, int optionType) { return uiManager.showConfirm(this, title, message, optionType); }
    public void showLevelMapDialog() { showLevelMap(); }

    public void incrementLevelsCompleted() {
        this.levelsCompleted++;
        if (this.gameSession != null) {
            this.gameSession.setLevelsCompleted(this.levelsCompleted);
        }
    }

    public void resetRunStatsForNewSession() {
        this.elapsedMsAccum = 0;
        this.levelsCompleted = 0;
        this.totalBlocksDestroyed = 0;
        if (this.gameSession != null) {
            this.gameSession.setElapsedMs(0);
            this.gameSession.setLevelsCompleted(0);
            this.gameSession.setTotalBlocksDestroyed(0);
            this.gameSession.resetSubmitted();
        }
    }

    // Khởi tạo hoặc khôi phục thông tin người chơi và thống kê phiên chơi (khi solo
    // mới hoặc load save)
    public void setPlayerRunInfo(String playerName, long elapsedMs, int levelsCompleted, int totalBlocksDestroyed) {
        if (playerName != null && !playerName.isBlank())
            this.playerName = playerName.trim();
        this.elapsedMsAccum = Math.max(0, elapsedMs);
        this.levelsCompleted = Math.max(0, levelsCompleted);
        this.totalBlocksDestroyed = Math.max(0, totalBlocksDestroyed);
        if (this.gameSession != null) {
            this.gameSession.setPlayerName(this.playerName);
            this.gameSession.setElapsedMs(this.elapsedMsAccum);
            this.gameSession.setLevelsCompleted(this.levelsCompleted);
            this.gameSession.setTotalBlocksDestroyed(this.totalBlocksDestroyed);
            this.gameSession.resetSubmitted();
        }
    }

    public static class RunStats {
        public final String player;
        public final long elapsedMs;
        public final int levels;
        public final int blocks;

        public RunStats(String p, long e, int l, int b) {
            this.player = p;
            this.elapsedMs = e;
            this.levels = l;
            this.blocks = b;
        }
    }

    public RunStats getRunStats() {
        return new RunStats(playerName, elapsedMsAccum, levelsCompleted, totalBlocksDestroyed);
    }

    public interface GameEvents {
        void onGameOver();
    }

    public void resetAllPowerUps() {
        // 1️⃣ Reset kích thước bóng
        if (ball != null) {
            ball.resetSize();
        }

        // 2️⃣ Reset kích thước thanh đỡ
        if (paddle != null) {
            paddle.resetSize();
        }
        // 3️⃣ Delegate reset to PowerUpManager (clears list and stops spawning)
        if (powerUpManager != null) {
            powerUpManager.resetAll();
        }
    }

}
