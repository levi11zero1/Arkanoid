package game;

import entities.Ball;
import entities.Block;
import entities.Paddle;
import function.GameState;
import function.Pause;
import function.SaveManager;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;           // Ảnh chụp trạng thái game để lưu/khôi phục
import java.util.Random;         // Quản lý đọc/ghi file save
import javax.swing.*;               // Điều khiển tạm dừng/tiếp tục
import levels.LevelBuilder;
import levels.LevelManager;
import powerup.PowerUp;
import utils.GameConfig;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Ball ball;
    private Paddle paddle;
    private List<Block> blocks;
    private Timer gameTimer;
    private LevelManager levelManager;
    private GameEvents eventsListener;

    private java.util.List<PowerUp> activePowerUps = new ArrayList<>();
    private javax.swing.Timer spawnTimer;
    private Random random = new Random();  // ✅ chỉ tạo 1 lần

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private long lastNanos;
    // Quản lý va chạm tách riêng
    private final CollisionManager collisionManager = new CollisionManager();


    public GamePanel() {
        levelManager = new LevelManager();
        initializeLevel();

        gameTimer = new Timer(GameConfig.TIMER_DELAY, this);
        gameTimer.start();
        lastNanos = System.nanoTime();

        // Trong constructor GamePanel()
        spawnTimer = new javax.swing.Timer(14000, e -> spawnRandomPowerUp()); // mỗi 30s
        spawnTimer.setRepeats(true);
        spawnTimer.start();

        // Đăng ký Pause: dừng timer khi pause, chạy lại khi resume
        Pause.getInstance().setListener(new Pause.PauseListener() {
            @Override public void onPause() {
                if (gameTimer != null) gameTimer.stop();
                if (spawnTimer != null) spawnTimer.stop();
            }
            @Override public void onResume() {
                if (gameTimer != null) gameTimer.start();
                if (spawnTimer != null) spawnTimer.start();
            }
        });

        setFocusable(true);
        addKeyListener(this);
        setBackground(Color.BLACK);
    }


    private void initializeLevel() {
        ball = new Ball(GameConfig.SCREEN_WIDTH / 2, GameConfig.SCREEN_HEIGHT / 2);
        paddle = new Paddle(
            GameConfig.SCREEN_WIDTH / 2 - GameConfig.PADDLE_WIDTH / 2,
            GameConfig.SCREEN_HEIGHT - 100
        );

        // Tạo block
        blocks = LevelBuilder.createLevel(levelManager.getCurrentLevel());
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

        // 4) Vẽ lại
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (g instanceof Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Level: " + levelManager.getCurrentLevel(), 10, 25);

        long remainingBlocks = blocks.stream().filter(block -> !block.isDestroyed()).count();
        g.drawString("Blocks: " + remainingBlocks, GameConfig.SCREEN_WIDTH - 100, 25);

        ball.draw(g);
        paddle.draw(g);
        for (Block block : blocks) {
            block.draw(g);
        }
        for (PowerUp p : activePowerUps) {
            g.setColor(p.getColor());
            g.fillRect(p.getX(), p.getY(), p.getWidth(), p.getHeight());
        }
    }

    // Game loop
    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double deltaTime = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;

        updateGame(deltaTime);
        handleCollisions();
        checkGameState();

        for (Iterator<PowerUp> it = activePowerUps.iterator(); it.hasNext();) {
            PowerUp p = it.next();
            p.updatePosition();

            if (p.getBounds().intersects(paddle.getBounds())) {
                applyPowerUpEffect(p);
                it.remove();
                continue;
            }
            if (p.isOutOfBounds(getHeight())) {
                it.remove();
            }
        }

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
        if (levelManager.isFinalLevel()) {
            gameTimer.stop();
            showGameComplete();
        } else {
            gameTimer.stop();
            showLevelComplete();
        }
    }

    private void handleGameOver() {
        gameTimer.stop();

        // If event listener is set (menu integration), notify it
        if (eventsListener != null) {
            eventsListener.onGameOver();
            return;
        }

        // Otherwise, show default dialog
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Game Over! You reached Level " + levelManager.getCurrentLevel() +
            "\n\nWould you like to play again?",
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
            "Level " + levelManager.getCurrentLevel() + " Complete!\n\n" +
            "Continue to Level " + (levelManager.getCurrentLevel() + 1) + "?",
            "Level Complete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            levelManager.advanceLevel();
            initializeLevel();
            gameTimer.start();
        } else {
            System.exit(0);
        }
    }

    private void showGameComplete() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Congratulations! You completed all " + levelManager.getMaxLevels() +
            " levels!\n\nWould you like to play again?",
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
        gameTimer.start();
    }

    private void showLevelMap() {
        // Yêu cầu người dùng nhập số level
        String input = JOptionPane.showInputDialog(
            this,
            "Nhập số level (1-" + GameConfig.MAX_LEVELS + "):",
            "Xem Map Level",
            JOptionPane.QUESTION_MESSAGE
        );

        if (input == null) return; // User cancelled

        try {
            int levelNum = Integer.parseInt(input.trim());
            if (levelNum < 1 || levelNum > GameConfig.MAX_LEVELS) {
                JOptionPane.showMessageDialog(
                    this,
                    "Level phải từ 1 đến " + GameConfig.MAX_LEVELS,
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // Tạo blocks cho level được chọn
            List<Block> previewBlocks = LevelBuilder.createLevel(levelNum);

            // Tạo dialog để hiển thị map
            JDialog mapDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Map Level " + levelNum, true);
            
            JPanel mapPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    
                    // Vẽ các block
                    for (Block block : previewBlocks) {
                        block.draw(g);
                    }
                    
                    // Vẽ thông tin level
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 16));
                    g.drawString("Level " + levelNum + " Preview", 10, 25);
                    g.drawString("Total Blocks: " + previewBlocks.size(), 10, 45);
                }
            };
            
            mapPanel.setPreferredSize(new Dimension(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT));
            mapPanel.setBackground(Color.BLACK);
            
            mapDialog.add(mapPanel);
            mapDialog.pack();
            mapDialog.setLocationRelativeTo(this);
            mapDialog.setVisible(true);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Vui lòng nhập số hợp lệ!",
                "Lỗi",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = true;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = true;
            case KeyEvent.VK_P -> {
                // Bật/tắt tạm dừng
                Pause.getInstance().toggle();
            }
            case KeyEvent.VK_S -> {
                // Lưu nhanh trạng thái hiện tại (phục vụ chức năng "Tiếp tục" trong menu)
                try {
                    SaveManager.save(toGameState());
                    Toolkit.getDefaultToolkit().beep();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lưu game thất bại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
            case KeyEvent.VK_SPACE -> {
                if (!gameTimer.isRunning()) {
                    gameTimer.start();
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
            case KeyEvent.VK_R -> {
                // Skip to next level, hoặc về level đầu nếu đang ở cuối
                if (!levelManager.isFinalLevel()) {
                    levelManager.advanceLevel();
                    initializeLevel();
                    gameTimer.start();
                } else {
                    // Nếu đang ở level cuối, chuyển về level đầu tiên
                    levelManager.reset();
                    initializeLevel();
                    gameTimer.start();
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
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = false;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = false;
        }
    }
    private void spawnRandomPowerUp() {
        PowerUp.Type[] types = PowerUp.Type.values();
        PowerUp.Type randomType = types[random.nextInt(types.length)];
        int spawnX = random.nextInt(getWidth() - 20);
        PowerUp p = new PowerUp(randomType, spawnX, 0);

        activePowerUps.add(p);
    }
    private void applyPowerUpEffect(PowerUp p) {
        PowerUp.Type type = p.getType();
        if (type == PowerUp.Type.PADDLE_EXPAND || type == PowerUp.Type.PADDLE_SHRINK) {
            paddle.applyPowerUp(type);
        } else if (type == PowerUp.Type.BALL_EXPAND || type == PowerUp.Type.BALL_SHRINK) {
            ball.applyPowerUp(type);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // Cho phép ArkanoidGame đăng ký lắng nghe sự kiện trong game
    public void setEventsListener(GameEvents listener) {
        this.eventsListener = listener;
    }

    public interface GameEvents {
        void onGameOver();
    }
}