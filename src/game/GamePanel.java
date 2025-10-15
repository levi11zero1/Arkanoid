package game;

import entities.Ball;
import entities.Block;
import entities.Paddle;
import function.GameState;
import function.SaveManager;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import levels.LevelBuilder;
import levels.LevelManager;
import utils.GameConfig;
import powerup.PowerUp;

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
    private int collisionCooldown = 0; // Tránh nhiều va chạm trong 1 frame


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

    // --- Save/Load helpers ---
    // Chuyển trạng thái hiện tại của game thành GameState (ảnh chụp) để SaveManager ghi ra tệp.
    // Bao gồm: level hiện tại, vị trí + vận tốc bóng, vị trí paddle, danh sách block (vị trí/số đòn/destroyed)
    private GameState toGameState() {
        java.util.ArrayList<GameState.BlockState> bs = new java.util.ArrayList<>();
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

    // Áp ảnh chụp trạng thái (đọc từ SaveManager.load()) để khôi phục game đúng vị trí đã lưu.
    // Trình tự khôi phục:
    // 1) Cập nhật level về đúng màn đã lưu
    // 2) Dựng lại danh sách block theo thông tin trong save (x, y, hitsRemaining, destroyed)
    // 3) Đặt lại vị trí & vận tốc bóng
    // 4) Khởi tạo paddle với X theo save (Y dùng theo cấu hình)
    public void applyGameState(GameState state) {
        // Set level
        levelManager.setLevel(state.level);
        // Rebuild blocks from save
        java.util.ArrayList<Block> newBlocks = new java.util.ArrayList<>();
        for (GameState.BlockState b : state.blocks) {
            newBlocks.add(new Block(b.x, b.y, b.hitsRemaining, b.destroyed));
        }
        this.blocks = newBlocks;
        // Ball
        ball.setPosition(state.ballX, state.ballY);
        ball.setVelocity(new utils.Velocity(state.ballDx, state.ballDy));
        // Paddle: reconstruct to set X in current API
        this.paddle = new Paddle((int)Math.round(state.paddleX), state.paddleY);
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

        if (collisionCooldown > 0) {
            collisionCooldown--;
        }
    }

    private void handleCollisions() {
        // Paddle collision
        if (paddle.isHit(ball.getX(), ball.getY(), GameConfig.BALL_SIZE)) {
            ball.bounceOffPaddle(paddle.getX(), paddle.getWidth());
            // Tránh nhiều va chạm
            ball.setPosition(ball.getPreciseX(), paddle.getY() - GameConfig.BALL_SIZE - 1);
            collisionCooldown = 2; // Set cooldown va chạm
        }

        if (collisionCooldown == 0) {
            for (Block block : blocks) {
                if (block.isHit(ball.getX(), ball.getY(), GameConfig.BALL_SIZE)) {
                    String collisionSide = block.getCollisionSide(
                        ball.getPreciseX(), ball.getPreciseY(), GameConfig.BALL_SIZE,
                        ball.getVelocity().getDx(), ball.getVelocity().getDy()
                    );

                    double ballX = ball.getPreciseX();
                    double ballY = ball.getPreciseY();

                    if ("left".equals(collisionSide)) {
                        ball.bounceX();
                        ball.setPosition(block.getX() - GameConfig.BALL_SIZE - 1, ballY);
                    } else if ("right".equals(collisionSide)) {
                        ball.bounceX();
                        ball.setPosition(block.getX() + GameConfig.BLOCK_WIDTH + 1, ballY);
                    } else if ("top".equals(collisionSide)) {
                        ball.bounceY();
                        ball.setPosition(ballX, block.getY() - GameConfig.BALL_SIZE - 1);
                    } else { // bottom
                        ball.bounceY();
                        ball.setPosition(ballX, block.getY() + GameConfig.BLOCK_HEIGHT + 1);
                    }

                    collisionCooldown = 2;
                    break; // Xử lý 1 va chạm/frame
                }
            }
        }
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

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = true;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = true;
            case KeyEvent.VK_S -> {
                // Save current state
                // Gọi SaveManager.save(...) với ảnh chụp hiện tại do toGameState() tạo ra.
                // Nếu thành công: beep() báo hiệu; nếu thất bại: hiện hộp thoại lỗi và giữ nguyên game.
                try {
                    SaveManager.save(toGameState());
                    Toolkit.getDefaultToolkit().beep();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            case KeyEvent.VK_L -> {
                // Load state
                // Đọc trạng thái từ SaveManager.load() rồi áp vào game bằng applyGameState(...).
                // Lưu ý: nếu tệp không đúng định dạng hoặc không tồn tại -> bắt lỗi và báo cho người chơi.
                try {
                    GameState state = SaveManager.load();
                    applyGameState(state);
                    Toolkit.getDefaultToolkit().beep();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

        int spawnY = 0;

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
