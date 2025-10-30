package game;

import entities.Ball;
import entities.Block;
import entities.Paddle;
import function.GameState;
import function.Pause;
import function.SaveManager;
import function.RankingManager;
import java.awt.*;
import java.awt.event.*;
import game.IGameLoop;
import game.GameLoop;
import game.IRenderer;
import game.Renderer;
import input.InputHandler;
import entities.EntityManager;
import powerup.PowerUpManager;
import ui.UIManager;
import utils.AudioManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;           // Ảnh chụp trạng thái game để lưu/khôi phục
import javax.swing.*;               // Điều khiển tạm dừng/tiếp tục
import levels.LevelBuilder;
import levels.LevelManager;
import powerup.PowerUp;
import ui.StyledButton;
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
    private AudioManager audioManager;

    // Power-up management delegated
    // active list, timer and random are now owned by PowerUpManager

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    // Quản lý va chạm tách riêng
    private final CollisionManager collisionManager = new CollisionManager();

    // Save button placed at top-right
    private StyledButton saveButton;

    // ==== RUN STATS ====
    private String playerName = "Player";
    private long elapsedMsAccum = 0; // tích lũy thời gian chơi (không tính Pause vì timer dừng)
    private int levelsCompleted = 0; // số màn đã hoàn thành
    private int totalBlocksDestroyed = 0; // tổng số block phá được qua các màn
    private int lastDestroyedCountThisLevel = 0; // baseline để tính delta mỗi tick
    private boolean rankingSubmitted = false; // tránh ghi 2 lần


    public GamePanel() {
        levelManager = new LevelManager();
        initializeLevel();

    // instantiate lightweight managers (non-invasive wiring)
    this.gameLoop = new GameLoop();
    this.renderer = new Renderer();
    this.entityManager = new EntityManager();
    this.inputHandler = new InputHandler();
    this.powerUpManager = new PowerUpManager();
    this.uiManager = new UIManager();
    this.audioManager = new AudioManager();

    // wire game loop tick listener and start loop
    this.gameLoop.setTickListener(delta -> onTick(delta));
    this.gameLoop.start();

        // Trong constructor GamePanel()
    // start power-up spawning via manager
    powerUpManager.startSpawning(this);

        // Đăng ký Pause: dừng timer khi pause, chạy lại khi resume
        Pause.getInstance().setListener(new Pause.PauseListener() {
            @Override public void onPause() {
                if (gameLoop != null) gameLoop.stop();
                powerUpManager.stopSpawning();
            }
            @Override public void onResume() {
                if (gameLoop != null) gameLoop.start();
                powerUpManager.startSpawning(GamePanel.this);
            }
        });

        setFocusable(true);
        addKeyListener(this);
        setBackground(Color.BLACK);

        // Absolute layout to freely position overlay button
        setLayout(null);
        initSaveButton();
    }


    private void initializeLevel() {
        ball = new Ball(GameConfig.SCREEN_WIDTH / 2, GameConfig.SCREEN_HEIGHT / 2);
        paddle = new Paddle(
            GameConfig.SCREEN_WIDTH / 2 - GameConfig.PADDLE_WIDTH / 2,
            GameConfig.SCREEN_HEIGHT - 100
        );

        // Tạo block
        blocks = LevelBuilder.createLevel(levelManager.getCurrentLevel());
        // baseline destroyed count for this level
        lastDestroyedCountThisLevel = countDestroyedDestructable();
    }

    private void initSaveButton() {
        saveButton = new StyledButton("Save");
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD, 16f));
        saveButton.setToolTipText("Lưu game");
        // Size and margins
        final int btnW = 90, btnH = 34;
        final int rightMargin = 100;    // giảm lề phải để sát mép hơn
        final int topMargin = 10;
        // Initial placement: prefer actual width if available, fallback to config
        int baseW = getWidth() > 0 ? getWidth() : GameConfig.SCREEN_WIDTH;
        int x = Math.max(0, baseW - btnW - rightMargin);
        saveButton.setBounds(x, topMargin, btnW, btnH);

        // Reposition on resize to keep at top-right
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int newX = getWidth() - btnW - rightMargin;
                saveButton.setLocation(Math.max(0, newX), topMargin);
            }
        });

        // Ensure correct position after first show
        SwingUtilities.invokeLater(() -> {
            int newX = getWidth() - btnW - rightMargin;
            saveButton.setLocation(Math.max(0, newX), topMargin);
        });

        // Click handler: prompt for save name, save, then return to menu (cannot continue playing)
        saveButton.addActionListener(ev -> {
            if (ev != null) { /* satisfy linter */ }
            saveButton.setEnabled(false);
            try {
                String name = JOptionPane.showInputDialog(
                    this,
                    "Nhập tên bản lưu:",
                    "Lưu game",
                    JOptionPane.PLAIN_MESSAGE
                );
                if (name == null) {
                    // user cancelled
                    return;
                }
                if (name.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Tên bản lưu không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                SaveManager.Metadata meta = new SaveManager.Metadata(playerName, elapsedMsAccum, levelsCompleted, totalBlocksDestroyed);
                SaveManager.save(toGameState(), name, meta);
                Toolkit.getDefaultToolkit().beep();
                // Stop timers to prevent further gameplay
                if (gameLoop != null) gameLoop.stop();
                powerUpManager.stopSpawning();
                // Inform container to go back to menu
                if (eventsListener != null) {
                    eventsListener.onGameOver();
                } else {
                    // Fallback: close application if no listener
                    System.exit(0);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lưu game thất bại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            } finally {
                // In case we didn't leave the screen (cancel/invalid), re-enable and refocus
                saveButton.setEnabled(true);
                SwingUtilities.invokeLater(this::requestFocusInWindow);
            }
        });

        add(saveButton);
    }

    // ================== LƯU/LOAD (PHỤC VỤ NÚT "TIẾP TỤC" Ở MENU) ==================
    // Tạo ảnh chụp trạng thái hiện tại để ghi xuống file save.
    // Bao gồm: level đang chơi, vị trí/tốc độ bóng, vị trí thanh đỡ, và danh sách block còn lại.
    private GameState toGameState() {
        ArrayList<GameState.BlockState> bs = new ArrayList<>();
        for (Block b : blocks) {
            bs.add(new GameState.BlockState(b.getX(), b.getY(), b.getHitsRemaining(), b.isDestroyed()));
        }
        return new GameState(
            levelManager.getCurrentLevel(),
            ball.getPreciseX(), ball.getPreciseY(),
            ball.getVelocity().getDx(), ball.getVelocity().getDy(),
            paddle.getX(), paddle.getY(),
            bs
        );
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

        // 4) baseline destroyed count for this level (avoid recounting already-destroyed blocks)
        lastDestroyedCountThisLevel = countDestroyedDestructable();

        // 5) Vẽ lại
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
        handleCollisions();
        checkGameState();
        // tích lũy thời gian chơi
        elapsedMsAccum += (long) (deltaTime * 1000);
        // cập nhật số block phá (delta so với lần đo trước)
        int curDestroyed = countDestroyedDestructable();
        if (curDestroyed > lastDestroyedCountThisLevel) {
            totalBlocksDestroyed += (curDestroyed - lastDestroyedCountThisLevel);
            lastDestroyedCountThisLevel = curDestroyed;
        }

        // delegate power-up updates to manager
        powerUpManager.updateAll(getHeight(), paddle, ball);

        repaint();
    }

    private void updateGame(double deltaTime) {
        ball.move();
        ball.checkBounds(getWidth(), getHeight());
        paddle.update(leftPressed, rightPressed, getWidth(), deltaTime);

        // Tick cooldown trong bộ xử lý va chạm
        collisionManager.tickCooldown();
    }

    private void handleCollisions() {
        collisionManager.handleCollisions(ball, paddle, blocks);
    }

    private void checkGameState() {
        boolean allBlocksDestroyed = blocks.stream().allMatch(Block::isDestroyed);
        if (allBlocksDestroyed) {
            handleLevelComplete();
        }

        if (ball.getY() > getHeight()) {
            handleGameOver();
        }
    }

    private void handleLevelComplete() {
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
        resetAllPowerUps();
        if (gameLoop != null) gameLoop.stop();

        // Try to play the lose sound and wait until it finishes before proceeding.
        // After the sound (or on fallback), update ranking and either notify the
        // registered `eventsListener` (menu integration) or show the default dialog.
        try {
            utils.MusicPlayer.playOnce("music/lose.wav", () -> {
                SwingUtilities.invokeLater(() -> {
                    submitRankingOnce();
                    if (eventsListener != null) {
                        eventsListener.onGameOver();
                        return;
                    }
                    showGameOverDialogAndHandleChoice();
                });
            });
        } catch (Throwable t) {
            // Fallback: if playback fails, still update ranking and proceed.
            submitRankingOnce();
            if (eventsListener != null) {
                eventsListener.onGameOver();
                return;
            }
            showGameOverDialogAndHandleChoice();
        }
    }

    // Centralized dialog used when there's no external event listener to handle game-over
    private void showGameOverDialogAndHandleChoice() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Game Over! You reached Level " + levelManager.getCurrentLevel() +
            "\\n\\nWould you like to play again?",
            "Game Over",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            restartGame();
        } else {
            System.exit(0);
        }
    }

    private void showLevelComplete() {


        int choice = JOptionPane.showConfirmDialog(
            this,
            "Level " + levelManager.getCurrentLevel() + " Complete!\\n\\n" +
            "Continue to Level " + (levelManager.getCurrentLevel() + 1) + "?",
            "Level Complete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

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
        submitRankingOnce();

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Congratulations! You completed all " + levelManager.getMaxLevels() +
            " levels!\\n\\nWould you like to play again?",
            "Game Complete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            restartGame();
        } else {
            System.exit(0);
        }
    }

    private void restartGame() {
        levelManager.reset();
        initializeLevel();
        if (gameLoop != null) gameLoop.start();
        // reset run stats for a new session (used in local replay flow)
        elapsedMsAccum = 0;
        levelsCompleted = 0;
        totalBlocksDestroyed = 0;
        rankingSubmitted = false;
    }

    private int countDestroyedDestructable() {
        int c = 0;
        for (Block b : blocks) {
            if (b.getHitsRemaining() != GameConfig.UNDESTRUCTABLE_BLOCK && b.isDestroyed()) c++;
        }
        return c;
    }

    private void submitRankingOnce() {
        if (rankingSubmitted) return;
        rankingSubmitted = true;
        try {
            RankingManager.addEntry(playerName != null ? playerName : "Player", levelsCompleted, totalBlocksDestroyed, elapsedMsAccum);
        } catch (Exception ignored) {}
    }

    private void showLevelMap() {
        // Delegate to LevelBuilder which now provides a reusable preview dialog
        String input = JOptionPane.showInputDialog(this, "Nhập số level (1-" + GameConfig.MAX_LEVELS + "):", "Xem Map Level", JOptionPane.QUESTION_MESSAGE);
        if (input == null) return;
            try {
            int levelNum = Integer.parseInt(input.trim());
            if (levelNum < 1 || levelNum > GameConfig.MAX_LEVELS) {
                JOptionPane.showMessageDialog(this, "Level phải từ 1 đến " + GameConfig.MAX_LEVELS, "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            levels.LevelPreview.show(this, levelNum);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // forward to InputHandler for future refactor (non-invasive)
        try { inputHandler.keyPressed(e); } catch (Throwable ignore) {}

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = true;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = true;
            case KeyEvent.VK_P -> {
                // Bật/tắt tạm dừng
                Pause.getInstance().toggle();
            }
            // Bỏ phím tắt lưu 'S' để chuyển sang dùng nút Save trên màn hình
            case KeyEvent.VK_SPACE -> {
                if (gameLoop != null && !gameLoop.isRunning()) {
                    gameLoop.start();
                }
            }
            case KeyEvent.VK_ESCAPE -> {
                int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to quit?",
                    "Quit Game",
                    JOptionPane.YES_NO_OPTION
                );
                if (choice == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }

            case KeyEvent.VK_M -> {
                // Hiển thị map của level khi nhập số level
                showLevelMap();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // forward to InputHandler for future refactor (non-invasive)
        try { inputHandler.keyReleased(e); } catch (Throwable ignore) {}

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = false;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = false;
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

    // Khởi tạo hoặc khôi phục thông tin người chơi và thống kê phiên chơi (khi solo mới hoặc load save)
    public void setPlayerRunInfo(String playerName, long elapsedMs, int levelsCompleted, int totalBlocksDestroyed) {
        if (playerName != null && !playerName.isBlank()) this.playerName = playerName.trim();
        this.elapsedMsAccum = Math.max(0, elapsedMs);
        this.levelsCompleted = Math.max(0, levelsCompleted);
        this.totalBlocksDestroyed = Math.max(0, totalBlocksDestroyed);
    }



    public static class RunStats {
        public final String player; public final long elapsedMs; public final int levels; public final int blocks;
        public RunStats(String p, long e, int l, int b) { this.player=p; this.elapsedMs=e; this.levels=l; this.blocks=b; }
    }
    public RunStats getRunStats() { return new RunStats(playerName, elapsedMsAccum, levelsCompleted, totalBlocksDestroyed); }

    public interface GameEvents {
        void onGameOver();
    }

    private void resetAllPowerUps() {
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
