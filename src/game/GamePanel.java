package game;

import entities.Ball;
import entities.Block;
import entities.EntityManager;
import entities.Paddle;
import function.GameSession;
import function.GameState;
import function.LifeManager;
import function.Pause;
import function.ScoreManager;
import input.InputHandler;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import levels.LevelBackgrounds;
import levels.LevelBuilder;
import levels.LevelManager;
import powerup.PowerUp;
import powerup.PowerUpManager;
import ui.UIManager;
import utils.GameConfig;

public class GamePanel extends JPanel implements KeyListener {
    private Ball ball;
    private Paddle paddle;
    private List<Block> blocks;
    private LevelManager levelManager;
    private GameEvents eventsListener;

    // Managers
    private IGameLoop gameLoop;
    private IRenderer renderer;
    private EntityManager entityManager;
    private InputHandler inputHandler;
    private PowerUpManager powerUpManager;
    private UIManager uiManager;
    private GameController gameController;
    private PaddleCloneManager paddleCloneManager;

    // Ranking/session
    private ScoreManager scoreManager;
    private GameSession gameSession;

    private Image levelBackground;
    // Overlay text khi hoàn thành level (hiển thị 3s rồi chuyển tiếp)
    private volatile boolean showingLevelCompleteOverlay = false;
    private String levelCompleteText = null;

    // Power-up management delegated
    // active list, timer and random are now owned by PowerUpManager

    private boolean leftPressed = false;
    private boolean rightPressed = false;

    private int lives;

    // ==== RUN STATS ====
    private String playerName = "Player";
    private long elapsedMsAccum = 0; // tích lũy thời gian chơi (không tính Pause vì timer dừng)
    private int levelsCompleted = 0; // số màn đã hoàn thành
    private int totalBlocksDestroyed = 0; // tổng số block phá được qua các màn
    private int lastDestroyedCountThisLevel = 0;
    private boolean gameStarted = false;

    public GamePanel() {
        levelManager = new LevelManager();
        lives = LifeManager.resetLives();
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
    this.paddleCloneManager = new PaddleCloneManager();

        // wire game loop để tick
        this.gameLoop.setTickListener(delta -> onTick(delta));
        this.gameLoop.start();

        // tạo game controller
        this.gameController = new GameController(this, this.gameLoop, this.levelManager, this.powerUpManager,
                this.scoreManager, this.gameSession, this.uiManager);

        // cung cấp thực thể cho EntityManager
        this.entityManager.setEntities(ball, paddle, blocks);

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
        setLayout(null);

        uiManager.createSaveButton(this, saved -> {
            if (!saved)
                return;
            Toolkit.getDefaultToolkit().beep();

            if (gameLoop != null)
                gameLoop.stop();
            powerUpManager.stopSpawning();

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
                GameConfig.SCREEN_WIDTH / 2 - GameConfig.DEFAULT_PADDLE_WIDTH / 2,
                GameConfig.SCREEN_HEIGHT - GameConfig.PADDLE_BOTTOM_MARGIN - GameConfig.PADDLE_EXTRA_RAISE_PIXELS);
        ball.attachToPaddle(paddle);

        // Tạo block
        int currentLevel = levelManager.getCurrentLevel();
        blocks = LevelBuilder.createLevel(currentLevel);
        levelBackground = LevelBackgrounds.getForLevel(currentLevel);
        lastDestroyedCountThisLevel = countDestroyedDestructable();

        // Cho biết EntityManager về các thực thể mới
        if (entityManager != null)
            entityManager.setEntities(ball, paddle, blocks);

        if (powerUpManager != null) {
            powerUpManager.resetAll();
            powerUpManager.stopSpawning();
        }
        if (paddleCloneManager != null) {
            paddleCloneManager.reset();
        }
        gameStarted = false;
    }

    // Chuyển trạng thái hiện tại của game panel này thành một GameState để lưu.
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
                bs,
                ball != null && ball.isAttachedToPaddle());
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

        lives = LifeManager.loadLives();

        // 3) Khôi phục bóng và thanh đỡ
        ball.setPosition(state.ballX, state.ballY);
        ball.setVelocity(new utils.Velocity(state.ballDx, state.ballDy));
        this.paddle = new Paddle((int) Math.round(state.paddleX), state.paddleY);

        if (state.ballAttached) {
            ball.attachToPaddle(this.paddle);
        } else {
            ball.detachFromPaddle();
        }

    // 4) Cập nhật lại nền theo level hiện tại
        this.levelBackground = LevelBackgrounds.getForLevel(levelManager.getCurrentLevel());

        // 5) baseline destroyed count for this level (avoid recounting
        // already-destroyed blocks)
        lastDestroyedCountThisLevel = countDestroyedDestructable();

        // 6) Reset clone manager theo state mới và vẽ lại
        if (paddleCloneManager != null) paddleCloneManager.reset();
        if (entityManager != null)
            entityManager.setEntities(ball, paddle, blocks);
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (levelBackground != null) {
            g.drawImage(levelBackground, 0, 0, getWidth(), getHeight(), this);
        }

        if (g instanceof Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            try {
                List<Ball> renderBalls;
                if (entityManager != null) {
                    renderBalls = entityManager.getBalls();
                } else if (ball != null) {
                    renderBalls = java.util.Collections.singletonList(ball);
                } else {
                    renderBalls = java.util.Collections.emptyList();
                }
                renderer.render(g2d, renderBalls, paddle, blocks, powerUpManager.snapshot(), levelManager);
                // vẽ paddle clones (nếu có)
                if (paddleCloneManager != null) {
                    try { paddleCloneManager.render(g2d); } catch (Throwable ignored) {}
                }
            } catch (Throwable t) {

            }
        }

        if (g instanceof Graphics2D) {
            try {
                uiManager.renderOverlay((Graphics2D) g, this);
            } catch (Throwable t) {

            }
        }

        // Vẽ overlay text khi hoàn thành level
        if (showingLevelCompleteOverlay && g instanceof Graphics2D) {
            try {
                Graphics2D g2 = (Graphics2D) g;
                // nền mờ
                Composite old = g2.getComposite();
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRect(0, 0, getWidth(), getHeight());

                String text = levelCompleteText != null ? levelCompleteText
                        : ("Level " + levelManager.getCurrentLevel() + " Complete!");
                Font font = new Font("SansSerif", Font.BOLD, Math.max(28, getWidth() / 16));
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics(font);
                int tx = (getWidth() - fm.stringWidth(text)) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                // shadow
                g2.setColor(new Color(0, 0, 0, 200));
                g2.drawString(text, tx + 3, ty + 3);
                // main
                g2.setColor(new Color(255, 215, 64));
                g2.drawString(text, tx, ty);
                g2.setComposite(old);
            } catch (Throwable ignored) {
            }
        }

        for (PowerUp p : powerUpManager.snapshot()) {
            p.draw(g);
        }

    }

    private void onTick(double deltaTime) {
        updateGame(deltaTime);
        checkGameState();
        // tích lũy thời gian chơi
        elapsedMsAccum += (long) (deltaTime * 1000);
        // cập nhật số block phá (delta so với lần đo trước)
        int curDestroyed = countDestroyedDestructable();
        if (curDestroyed > lastDestroyedCountThisLevel) {
            int newly = (curDestroyed - lastDestroyedCountThisLevel);
            totalBlocksDestroyed += newly;
            lastDestroyedCountThisLevel = curDestroyed;
        }

        if (gameSession != null) {
            gameSession.setElapsedMs(elapsedMsAccum);
            gameSession.setLevelsCompleted(levelsCompleted);
            gameSession.setTotalBlocksDestroyed(totalBlocksDestroyed);
        }

        powerUpManager.updateAll(getHeight(), paddle, entityManager);

        repaint();
    }

    private void updateGame(double deltaTime) {
        if (entityManager != null) {
            entityManager.updateAll(deltaTime, getWidth(), getHeight(), leftPressed, rightPressed);
            entityManager.removeOutOfBoundsBalls(getHeight());
            Ball currentPrimary = entityManager.getPrimaryBall();
            if (currentPrimary != null) {
                ball = currentPrimary;
            }
            if (entityManager.getActiveBallCount() == 0) {
                handleBallLost();
            }

            for (Block block : blocks) {
                if (block.isDestroyed() && !block.isPowerUpSpawned()) {
                    block.setPowerUpSpawned(true);
                    double spawnChance = 0.25; // 25% tỉ lệ rơi power-up
                    if (Math.random() < spawnChance) {
                        PowerUp.Type type = getRandomAvailablePowerUpType();
                        if (type != null && powerUpManager != null) {
                            int spawnX = block.getX() + GameConfig.BLOCK_WIDTH / 2 - 10;
                            int spawnY = block.getY() + GameConfig.BLOCK_HEIGHT / 2;
                            powerUpManager.spawnPowerUp(type, spawnX, spawnY);
                            block.setPowerUpSpawned(true);
                        }
                    }
                }
            }
            // cập nhật và va chạm với paddle clones
            if (paddleCloneManager != null) {
                try {
                    paddleCloneManager.update(paddle);
                    paddleCloneManager.handleCollisions(entityManager.getBalls());
                } catch (Throwable ignored) {}
            }
        } else {
            if (ball != null) {
                if (ball.isAttachedToPaddle()) {
                    ball.centerOnPaddle(paddle);
                } else {
                    ball.move();
                    ball.checkBounds(getWidth(), getHeight());
                }
            }
            if (paddle != null) {
                paddle.update(leftPressed, rightPressed, getWidth(), deltaTime);
            }
            if (ball != null && ball.isAttachedToPaddle()) {
                ball.centerOnPaddle(paddle);
            }
            // cập nhật clones ở nhánh legacy
            if (paddleCloneManager != null) {
                try { paddleCloneManager.update(paddle); } catch (Throwable ignored) {}
            }
        }

    }

    private void checkGameState() {
        boolean allDestructiblesGone = blocks.stream()
                .filter(b -> b.getHitsRemaining() != GameConfig.UNDESTRUCTABLE_BLOCK)
                .allMatch(Block::isDestroyed);
        if (allDestructiblesGone) {
            gameController.handleLevelComplete();
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
            gameController.restartGame();
        } else {
            System.exit(0);
        }
    }

    /**
     * Hiển thị overlay text "Bạn đã hoàn thành Level X" trong delayMs milliseconds,
     * sau đó tự động advance level và resume game.
     */
    public void showLevelCompleteOverlayAndAdvance(int delayMs) {
        if (showingLevelCompleteOverlay)
            return;
        showingLevelCompleteOverlay = true;
        levelCompleteText = "Bạn đã hoàn thành Level " + levelManager.getCurrentLevel() + "!";
        repaint();

        Timer t = new Timer(delayMs, ev -> {
            try {
                levelManager.advanceLevel();
                initializeLevel();
                if (powerUpManager != null)
                    powerUpManager.startSpawning(this);
                if (gameLoop != null)
                    gameLoop.start();
            } catch (Throwable ignored) {
            } finally {
                showingLevelCompleteOverlay = false;
                levelCompleteText = null;
                repaint();
                ((Timer) ev.getSource()).stop();
            }
        });
        t.setRepeats(false);
        t.start();
    }

    private int countDestroyedDestructable() {
        int c = 0;
        for (Block b : blocks) {
            if (b.getHitsRemaining() != GameConfig.UNDESTRUCTABLE_BLOCK && b.isDestroyed())
                c++;
        }
        return c;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            inputHandler.keyPressed(e, this);
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (entityManager != null) {
                    entityManager.queueLaunch();
                } else if (ball != null && ball.isAttachedToPaddle()) {
                    ball.detachFromPaddle();
                }
            }
        } catch (Throwable ignore) {
        }

        // 🟢 Khi người chơi nhấn SPACE để bắt đầu game
        if (e.getKeyCode() == KeyEvent.VK_SPACE && !gameStarted) {
            gameStarted = true;
            if (powerUpManager != null) {
                powerUpManager.startSpawning(this); // Bắt đầu spawn Power-Up
            }
            if (gameLoop != null && !gameLoop.isRunning()) {
                gameLoop.start(); // Chạy game loop
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        try {
            inputHandler.keyReleased(e, this);
        } catch (Throwable ignore) {
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public void setEventsListener(GameEvents listener) {
        this.eventsListener = listener;
    }

    public GameEvents getEventsListener() {
        return this.eventsListener;
    }

    public void setLeftPressed(boolean v) {
        this.leftPressed = v;
    }

    public void setRightPressed(boolean v) {
        this.rightPressed = v;
    }

    public void togglePauseAction() {
        Pause.getInstance().toggle();
    }

    public void startIfNotRunning() {
        if (gameLoop != null && !gameLoop.isRunning())
            gameLoop.start();
    }

    // playSkillMusic() removed (feature deprecated)

    public int confirm(String title, String message, int optionType) {
        return uiManager.showConfirm(this, title, message, optionType);
    }

    public void incrementLevelsCompleted() {
        this.levelsCompleted++;
        if (this.gameSession != null) {
            this.gameSession.setLevelsCompleted(this.levelsCompleted);
        }
    }

    // Toggle phân thân paddle (gọi từ InputHandler qua phím R)
    public void togglePaddleClones() {
        if (paddleCloneManager != null) {
            paddleCloneManager.toggle(paddle);
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

    private void handleBallLost() {
        if (ball == null || paddle == null) {
            return;
        }

        if (entityManager != null) {
            entityManager.cancelQueuedLaunch();
        }

        ball.attachToPaddle(paddle);
        ball.centerOnPaddle(paddle);

        if (entityManager != null) {
            entityManager.resetToSingleBall(ball);
        }

        lives = LifeManager.decrementLife();

        if (lives > 0) {
            return;
        }
        gameController.handleGameOver();
    }

    public void resetLivesForNewSession() {
        lives = LifeManager.resetLives();
    }

    public int getLives() {
        return lives;
    }

    public void resetAllPowerUps() {
        // 1️⃣ Reset kích thước bóng
        if (ball != null) {
            ball.resetSize();
            ball.resetSpeed();
        }

        // 2️⃣ Reset kích thước thanh đỡ
        if (paddle != null) {
            paddle.resetSize();
        }
        // 3️⃣ reset tất cả Power-Up đang active
        if (powerUpManager != null) {
            powerUpManager.resetAll();
        }
    }

    private PowerUp.Type getRandomAvailablePowerUpType() {
        PowerUp.Type[] all = PowerUp.Type.values();
        List<PowerUp.Type> available = new ArrayList<>();

        for (PowerUp.Type t : all) {
            if (!powerUpManager.isActive(t)) {
                available.add(t);
            }
        }

        if (available.isEmpty())
            return null;
        return available.get(new java.util.Random().nextInt(available.size()));
    }

}
