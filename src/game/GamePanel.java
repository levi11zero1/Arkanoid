package game;

import entities.Ball;
import entities.Block;
import entities.Paddle;
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
    private List<Ball> balls = new ArrayList<>();
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
    // Hạn chế tần suất power-up nhân 10 bóng
    private long lastMultiSpawnMs = 0L;
    private static final long MIN_MULTI_COOLDOWN_MS = 60000; // 60s giữa 2 lần xuất hiện MULTI


    public GamePanel() {
        levelManager = new LevelManager();
        // Optional: allow starting at a specific level via -DstartLevel=NUM or env START_LEVEL
        try {
            String prop = System.getProperty("startLevel");
            if (prop == null || prop.isBlank()) {
                prop = System.getenv("START_LEVEL");
            }
            if (prop != null && !prop.isBlank()) {
                int lv = Integer.parseInt(prop.trim());
                levelManager.setCurrentLevel(lv);
            }
        } catch (Exception ignore) { }
        initializeLevel();

        gameTimer = new Timer(GameConfig.TIMER_DELAY, this);
        gameTimer.start();
        lastNanos = System.nanoTime();

        // Trong constructor GamePanel()
    spawnTimer = new javax.swing.Timer(14000, e -> { if (e != null) spawnRandomPowerUp(); }); // mỗi 14s
        spawnTimer.setRepeats(true);
        spawnTimer.start();

        setFocusable(true);
        addKeyListener(this);
        setBackground(Color.BLACK);

        // Robust key binding for 'R' to skip level (works WHEN_IN_FOCUSED_WINDOW)
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "skipNext");
        im.put(KeyStroke.getKeyStroke('r'), "skipNext");
        im.put(KeyStroke.getKeyStroke('R'), "skipNext");
        am.put("skipNext", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { skipToNextLevel(); }
        });
    }


    private void initializeLevel() {
        balls.clear();
        balls.add(new Ball(GameConfig.SCREEN_WIDTH / 2, GameConfig.SCREEN_HEIGHT / 2));
        paddle = new Paddle(
            GameConfig.SCREEN_WIDTH / 2 - GameConfig.PADDLE_WIDTH / 2,
            GameConfig.SCREEN_HEIGHT - 100
        );

        // Tạo block
        blocks = LevelBuilder.createLevel(levelManager.getCurrentLevel());
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

        for (Ball b : balls) {
            b.draw(g);
        }
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
        for (Ball b : balls) {
            b.move();
            b.checkBounds(getWidth(), getHeight());
        }
        paddle.update(leftPressed, rightPressed, getWidth(), deltaTime);
        if (collisionCooldown > 0) collisionCooldown--; // giữ cooldown nếu muốn dùng sau
    }

    private void handleCollisions() {
        // Xử lý va chạm cho từng bóng độc lập
        List<Ball> ballsSnapshot = new ArrayList<>(balls);
        for (Ball ball : ballsSnapshot) {
            // Paddle collision (ưu tiên kiểm tra cắt mép trên; fallback CCD chống xuyên)
            if (ball.getVelocity().getDy() > 0) {
                boolean collidedWithPaddle = false;
                // 1) Kiểm tra cắt mép trên paddle (discrete)
                double prevBottom = ball.getPrevY() + ball.getPrevSize();
                double curBottom = ball.getPreciseY() + GameConfig.BALL_SIZE;
                double paddleTop = paddle.getY();
                double paddleLeft = paddle.getX();
                double paddleRight = paddle.getX() + paddle.getWidth();
                double ballLeft = ball.getPreciseX();
                double ballRight = ballLeft + GameConfig.BALL_SIZE;

                boolean horizOverlap = ballRight > paddleLeft && ballLeft < paddleRight;
                // Dải đứng không quá chặt để tránh lỡ va chạm khi tốc độ cao
                boolean withinVerticalBand = (curBottom >= paddleTop);
                double eps = 0.01;
                boolean crossedTop = (prevBottom < paddleTop - eps) && (curBottom >= paddleTop + eps);

                if (horizOverlap && withinVerticalBand && crossedTop) {
                    ball.bounceOffPaddle(paddle.getX(), paddle.getWidth());
                    ball.setPosition(ball.getPreciseX(), paddleTop - GameConfig.BALL_SIZE - 0.01);
                    collidedWithPaddle = true;
                }

                // 2) Nếu chưa va chạm: dùng CCD (swept) giữa đoạn di chuyển tâm bóng và AABB paddle mở rộng bởi bán kính bóng
                if (!collidedWithPaddle) {
                    double prevCenterX = ball.getPrevX() + GameConfig.BALL_SIZE / 2.0;
                    double prevCenterY = ball.getPrevY() + ball.getPrevSize() / 2.0; // dùng prevSize cho chính xác
                    double curCenterX = ball.getPreciseX() + GameConfig.BALL_SIZE / 2.0;
                    double curCenterY = ball.getPreciseY() + GameConfig.BALL_SIZE / 2.0;
                    double dxMove = curCenterX - prevCenterX;
                    double dyMove = curCenterY - prevCenterY;

                    if (dxMove != 0 || dyMove != 0) {
                        double radius = GameConfig.BALL_SIZE / 2.0;
                        double rx1 = paddle.getX() - radius;
                        double ry1 = paddle.getY() - radius;
                        double rx2 = paddle.getX() + paddle.getWidth() + radius;
                        double ry2 = paddle.getY() + GameConfig.PADDLE_HEIGHT + radius;

                        SweepResult res = sweptSegmentAABB(prevCenterX, prevCenterY, dxMove, dyMove, rx1, ry1, rx2, ry2);
                        if (res.hit && res.tEnter >= 0 && res.tEnter <= 1.0) {
                            // Tính điểm chạm
                            double t = Math.max(0.0, Math.min(1.0, res.tEnter));
                            double hitCenterX = prevCenterX + dxMove * t;
                            double hitCenterY = prevCenterY + dyMove * t;
                            double newX = hitCenterX - GameConfig.BALL_SIZE / 2.0;

                            // Chỉ xử lý như đụng mặt trên của paddle để tránh "bóng bật từ không khí"
                            double topY = paddle.getY();
                            double sideL = paddle.getX();
                            double sideR = paddle.getX() + paddle.getWidth();
                            double epsTop = 0.5;
                            // Chấp nhận tiếp xúc mặt trên nếu tâm chạm nằm không vượt quá topY + bán kính + epsilon
                            boolean isTopContact = res.hitVertical && dyMove > 0 && hitCenterY <= topY + radius + epsTop;
                            boolean withinX = hitCenterX >= sideL - radius && hitCenterX <= sideR + radius;

                            if (isTopContact && withinX) {
                                // Phản xạ theo paddle để có góc theo điểm chạm
                                ball.setPosition(newX, topY - GameConfig.BALL_SIZE - 0.01);
                                ball.bounceOffPaddle(paddle.getX(), paddle.getWidth());
                                continue;
                            }
                        }
                    }
                }
            }

            // Block collision: chọn 1 block tốt nhất cho bóng này (ưu tiên overlap hiện tại)
            Block bestBlock = null;
            double bestDist2 = Double.POSITIVE_INFINITY;
            double curX = ball.getPreciseX();
            double curY = ball.getPreciseY();
            double prevX = ball.getPrevX();
            double prevY = ball.getPrevY();

            for (Block block : blocks) {
                if (!block.isDestroyed() &&
                    curX + GameConfig.BALL_SIZE > block.getX() && curX < block.getX() + GameConfig.BLOCK_WIDTH &&
                    curY + GameConfig.BALL_SIZE > block.getY() && curY < block.getY() + GameConfig.BLOCK_HEIGHT) {
                    double centerPrevX = prevX + GameConfig.BALL_SIZE / 2.0;
                    double centerPrevY = prevY + GameConfig.BALL_SIZE / 2.0;
                    double bx = Math.max(block.getX(), Math.min(centerPrevX, block.getX() + GameConfig.BLOCK_WIDTH));
                    double by = Math.max(block.getY(), Math.min(centerPrevY, block.getY() + GameConfig.BLOCK_HEIGHT));
                    double dx = centerPrevX - bx;
                    double dy = centerPrevY - by;
                    double dist2 = dx * dx + dy * dy;
                    if (dist2 < bestDist2) {
                        bestDist2 = dist2;
                        bestBlock = block;
                    }
                }
            }

            if (bestBlock != null) {
                // Gây sát thương 1 block (đã có overlap => áp dụng trực tiếp)
                bestBlock.applyHit();

                boolean wasLeft = prevX + GameConfig.BALL_SIZE <= bestBlock.getX();
                boolean wasRight = prevX >= bestBlock.getX() + GameConfig.BLOCK_WIDTH;
                boolean wasAbove = prevY + GameConfig.BALL_SIZE <= bestBlock.getY();
                boolean wasBelow = prevY >= bestBlock.getY() + GameConfig.BLOCK_HEIGHT;

                if (wasLeft && !wasRight) {
                    ball.bounceX();
                    ball.setPosition(bestBlock.getX() - GameConfig.BALL_SIZE - 0.01, curY);
                } else if (wasRight && !wasLeft) {
                    ball.bounceX();
                    ball.setPosition(bestBlock.getX() + GameConfig.BLOCK_WIDTH + 0.01, curY);
                } else if (wasAbove && !wasBelow) {
                    ball.bounceY();
                    ball.setPosition(curX, bestBlock.getY() - GameConfig.BALL_SIZE - 0.01);
                } else if (wasBelow && !wasAbove) {
                    ball.bounceY();
                    ball.setPosition(curX, bestBlock.getY() + GameConfig.BLOCK_HEIGHT + 0.01);
                } else {
                    String side = bestBlock.getCollisionSide(
                        curX, curY, GameConfig.BALL_SIZE,
                        ball.getVelocity().getDx(), ball.getVelocity().getDy()
                    );
                    if ("left".equals(side)) {
                        ball.bounceX();
                        ball.setPosition(bestBlock.getX() - GameConfig.BALL_SIZE - 0.01, curY);
                    } else if ("right".equals(side)) {
                        ball.bounceX();
                        ball.setPosition(bestBlock.getX() + GameConfig.BLOCK_WIDTH + 0.01, curY);
                    } else if ("top".equals(side)) {
                        ball.bounceY();
                        ball.setPosition(curX, bestBlock.getY() - GameConfig.BALL_SIZE - 0.01);
                    } else {
                        ball.bounceY();
                        ball.setPosition(curX, bestBlock.getY() + GameConfig.BLOCK_HEIGHT + 0.01);
                    }
                }
            }
            else {
                // Không overlap: dùng swept AABB để bắt va chạm bị bỏ lỡ do tốc độ cao
                Block sweepHitBlock = null;
                double sweepTHit = Double.POSITIVE_INFINITY;
                boolean hitVertical = false; // true => đụng theo trục Y (phản xạ Y), false => theo trục X

                double prevCenterX = prevX + GameConfig.BALL_SIZE / 2.0;
                double prevCenterY = prevY + GameConfig.BALL_SIZE / 2.0;
                double curCenterX = curX + GameConfig.BALL_SIZE / 2.0;
                double curCenterY = curY + GameConfig.BALL_SIZE / 2.0;
                double dxMove = curCenterX - prevCenterX;
                double dyMove = curCenterY - prevCenterY;
                if (dxMove != 0 || dyMove != 0) {
                    double radius = GameConfig.BALL_SIZE / 2.0;
                    for (Block block : blocks) {
                        if (block.isDestroyed()) continue;
                        double rx1 = block.getX() - radius;
                        double ry1 = block.getY() - radius;
                        double rx2 = block.getX() + GameConfig.BLOCK_WIDTH + radius;
                        double ry2 = block.getY() + GameConfig.BLOCK_HEIGHT + radius;

                        SweepResult res = sweptSegmentAABB(prevCenterX, prevCenterY, dxMove, dyMove, rx1, ry1, rx2, ry2);
                        if (res.hit && res.tEnter >= 0 && res.tEnter <= 1.0) {
                            if (res.tEnter < sweepTHit) {
                                sweepTHit = res.tEnter;
                                sweepHitBlock = block;
                                hitVertical = res.hitVertical;
                            }
                        }
                    }
                }

                if (sweepHitBlock != null) {
                    // Tính vị trí chạm và phản xạ theo trục thích hợp
                    double t = Math.max(0.0, Math.min(1.0, sweepTHit));
                    double hitCenterX = prevCenterX + dxMove * t;
                    double hitCenterY = prevCenterY + dyMove * t;
                    double newX = hitCenterX - GameConfig.BALL_SIZE / 2.0;
                    double newY = hitCenterY - GameConfig.BALL_SIZE / 2.0;

                    // Áp sát ra ngoài một chút để tránh dính
                    double eps = 0.01;
                    if (hitVertical) {
                        // va chạm mặt trên/dưới => đảo dy
                        ball.setPosition(newX, newY + (dyMove > 0 ? -eps : eps));
                        ball.bounceY();
                    } else {
                        // va chạm mặt trái/phải => đảo dx
                        ball.setPosition(newX + (dxMove > 0 ? -eps : eps), newY);
                        ball.bounceX();
                    }

                    // Gây sát thương block bị đụng (CCD: áp lực trực tiếp)
                    sweepHitBlock.applyHit();
                }
            }
        }
    }

    // Kết quả sweep: có hit và trục phản xạ
    private static class SweepResult {
        boolean hit;
        double tEnter;
        boolean hitVertical; // true nếu đụng biên trên/dưới (phản xạ Y), false nếu trái/phải (phản xạ X)
    }

    // Liang-Barsky cho đoạn thẳng (prev + t*delta) so với AABB mở rộng
    private static SweepResult sweptSegmentAABB(double px, double py, double dx, double dy,
                                               double rx1, double ry1, double rx2, double ry2) {
        SweepResult res = new SweepResult();
        double t0 = 0.0, t1 = 1.0;
        boolean xEnter = false, yEnter = false;

        // X slabs
        if (dx == 0) {
            if (px < rx1 || px > rx2) return res; // ngoài slab => không bao giờ vào
        } else {
            double tx1 = (rx1 - px) / dx;
            double tx2 = (rx2 - px) / dx;
            double txEnter = Math.min(tx1, tx2);
            double txExit = Math.max(tx1, tx2);
            if (txEnter > t0) { t0 = txEnter; xEnter = true; }
            if (txExit < t1) { t1 = txExit; }
            if (t0 > t1) return res;
        }

        // Y slabs
        if (dy == 0) {
            if (py < ry1 || py > ry2) return res;
        } else {
            double ty1 = (ry1 - py) / dy;
            double ty2 = (ry2 - py) / dy;
            double tyEnter = Math.min(ty1, ty2);
            double tyExit = Math.max(ty1, ty2);
            if (tyEnter > t0) { t0 = tyEnter; xEnter = false; yEnter = true; }
            if (tyExit < t1) { t1 = tyExit; }
            if (t0 > t1) return res;
        }

        res.hit = (t0 >= 0 && t0 <= 1.0);
        res.tEnter = t0;
        // Xác định trục va chạm: nếu t0 đến từ X slab thì phản xạ X, nếu từ Y thì phản xạ Y
        // Ưu tiên trục có tEnter gần hơn như logic trên
        // Nếu cả hai cập nhật t0, yEnter sẽ là true khi Y cập nhật cuối và tạo t0 lớn hơn
        res.hitVertical = yEnter && !xEnter ? true : (!yEnter && xEnter ? false : Math.abs(dy) > Math.abs(dx));
        return res;
    }

    private void checkGameState() {
        boolean allBlocksDestroyed = blocks.stream().allMatch(Block::isDestroyed);
        if (allBlocksDestroyed) {
            handleLevelComplete();
        }

        // Loại bỏ bóng rơi khỏi màn và xử lý game over khi hết bóng
        balls.removeIf(b -> b.getY() > getHeight());
        if (balls.isEmpty()) {
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
            // 'R' handled by key bindings above to avoid duplicate events
            case KeyEvent.VK_1, KeyEvent.VK_NUMPAD1 -> jumpToLevel(1);
            case KeyEvent.VK_2, KeyEvent.VK_NUMPAD2 -> jumpToLevel(2);
            case KeyEvent.VK_3, KeyEvent.VK_NUMPAD3 -> jumpToLevel(3);
            case KeyEvent.VK_4, KeyEvent.VK_NUMPAD4 -> jumpToLevel(4);
            case KeyEvent.VK_5, KeyEvent.VK_NUMPAD5 -> jumpToLevel(5);
            case KeyEvent.VK_6, KeyEvent.VK_NUMPAD6 -> jumpToLevel(6);
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
    @Override
    public void keyTyped(KeyEvent e) {
        // no-op: avoid handling 'R' here to prevent double-trigger with keyPressed
    }

    private void skipToNextLevel() {
        // Skip to next level; wrap to level 1 if at final
        if (levelManager.isFinalLevel()) {
            levelManager.reset();
        } else {
            levelManager.advanceLevel();
        }
        activePowerUps.clear();
        initializeLevel();
        if (!gameTimer.isRunning()) gameTimer.start();
        requestFocusInWindow();
        repaint();
    }

    // Jump directly to a specific level (1..MAX_LEVELS)
    private void jumpToLevel(int level) {
        levelManager.setCurrentLevel(level);
        activePowerUps.clear();
        initializeLevel();
        if (!gameTimer.isRunning()) gameTimer.start();
        requestFocusInWindow();
        // Optional quick HUD feedback
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Jumped to Level " + level));
        repaint();
    }
    private void spawnRandomPowerUp() {
        long now = System.currentTimeMillis();
        boolean canSpawnMulti = (now - lastMultiSpawnMs) >= MIN_MULTI_COOLDOWN_MS;

        // Xác suất MULTI nhỏ; nếu chưa hết cooldown thì không cho ra MULTI
        PowerUp.Type chosen;
        double roll = random.nextDouble();
        double multiProb = 0.06; // 6% nếu không cooldown
        if (canSpawnMulti && roll < multiProb) {
            chosen = PowerUp.Type.BALL_MULTI_X10;
            lastMultiSpawnMs = now;
        } else {
            // Chọn trong các loại còn lại
            PowerUp.Type[] all = PowerUp.Type.values();
            java.util.List<PowerUp.Type> pool = new java.util.ArrayList<>();
            for (PowerUp.Type t : all) if (t != PowerUp.Type.BALL_MULTI_X10) pool.add(t);
            chosen = pool.get(random.nextInt(pool.size()));
        }

        int panelWidth = getWidth() > 0 ? getWidth() : GameConfig.SCREEN_WIDTH;
        int spawnX = random.nextInt(Math.max(1, panelWidth - 20));
        PowerUp p = new PowerUp(chosen, spawnX, 0);
        activePowerUps.add(p);
    }
    private void applyPowerUpEffect(PowerUp p) {
        PowerUp.Type type = p.getType();
        if (type == PowerUp.Type.PADDLE_EXPAND || type == PowerUp.Type.PADDLE_SHRINK) {
            paddle.applyPowerUp(type);
        } else if (type == PowerUp.Type.BALL_EXPAND || type == PowerUp.Type.BALL_SHRINK) {
            // Áp dụng kích thước lên bóng (global) bằng API hiện có của Ball
            if (!balls.isEmpty()) {
                balls.get(0).applyPowerUp(type);
            }
        } else if (type == PowerUp.Type.BALL_MULTI_X10) {
            // Nhân số bóng hiện tại lên 10x
            List<Ball> toAdd = new ArrayList<>();
            for (Ball b : new ArrayList<>(balls)) {
                double baseAngle = Math.atan2(b.getVelocity().getDy(), b.getVelocity().getDx());
                double speed = b.getVelocity().getMagnitude();
                int clones = 9; // thêm 9 -> tổng 10
                double spreadDeg = 60.0; // trải trong ±30°
                for (int i = 0; i < clones; i++) {
                    double t = (clones == 1) ? 0.5 : (double)i / (clones - 1);
                    double offsetDeg = -spreadDeg/2 + t * spreadDeg;
                    double ang = baseAngle + Math.toRadians(offsetDeg);
                    double dx = speed * Math.cos(ang);
                    double dy = speed * Math.sin(ang);
                    toAdd.add(new Ball(b.getX(), b.getY(), new utils.Velocity(dx, dy)));
                }
            }
            balls.addAll(toAdd);
            // Giới hạn số lượng bóng để tránh quá tải (an toàn)
            int MAX_BALLS = 80;
            if (balls.size() > MAX_BALLS) {
                balls = new ArrayList<>(balls.subList(0, MAX_BALLS));
            }
        }
    }

    // Cho phép ArkanoidGame đăng ký lắng nghe sự kiện trong game
    public void setEventsListener(GameEvents listener) {
        this.eventsListener = listener;
    }

    public interface GameEvents {
        void onGameOver();
    }
}
