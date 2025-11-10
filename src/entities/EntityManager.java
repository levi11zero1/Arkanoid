package entities;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import game.CollisionManager;
import utils.GameConfig;
import utils.Velocity;

/**
 * Kiểm soát các thực thể trong trò chơi: bóng, thanh trượt, và các khối.
 *
 */
public class EntityManager {
    private final List<Ball> balls = new ArrayList<>();
    private Ball primaryBall;
    private Paddle paddle;
    private List<Block> blocks = new ArrayList<>();
    private final CollisionManager collisionManager = new CollisionManager();
    private boolean launchQueued;

    public EntityManager() { }

    public void setEntities(Ball ball, Paddle paddle, List<Block> blocks) {
        this.primaryBall = ball;
        this.paddle = paddle;
        this.blocks = (blocks != null) ? blocks : new ArrayList<>();
        balls.clear();
        if (ball != null) {
            balls.add(ball);
            if (paddle != null && ball.isAttachedToPaddle()) {
                ball.centerOnPaddle(paddle);
            }
        }
    }

    /** Cập nhật tất cả các thực thể sau mỗi tick. */
    public void updateAll(double deltaSeconds, int panelWidth, int panelHeight, boolean leftPressed, boolean rightPressed) {
        if (paddle != null) {
            paddle.update(leftPressed, rightPressed, panelWidth, deltaSeconds);
        }

        List<Ball> snapshot = new ArrayList<>(balls);
        for (Ball ball : snapshot) {
            if (ball == null) {
                continue;
            }
            if (ball.isAttachedToPaddle()) {
                ball.centerOnPaddle(paddle);
                if (ball == primaryBall && launchQueued) {
                    ball.detachFromPaddle();
                    launchQueued = false;
                }
                continue;
            }
            ball.move();
            ball.checkBounds(panelWidth, panelHeight);
        }

        if (paddle != null) {
            // nothing extra
        }
        // Xử lý va chạm
        collisionManager.tickCooldown();
        for (Ball ball : new ArrayList<>(balls)) {
            collisionManager.handleCollisions(ball, paddle, blocks);
        }
    }

    public Ball getBall() { return primaryBall; }
    public Ball getPrimaryBall() { return primaryBall; }
    public List<Ball> getBalls() { return Collections.unmodifiableList(balls); }
    public int getActiveBallCount() { return balls.size(); }
    public Paddle getPaddle() { return paddle; }
    public List<Block> getBlocks() { return blocks; }

    public void addBlock(Block b) { if (blocks != null) blocks.add(b); }
    public void removeBlock(Block b) { if (blocks != null) blocks.remove(b); }

    public void queueLaunch() {
        if (primaryBall != null && primaryBall.isAttachedToPaddle()) {
            launchQueued = true;
        }
    }

    public void cancelQueuedLaunch() {
        launchQueued = false;
    }

    public boolean removeOutOfBoundsBalls(int panelHeight) {
        boolean primaryLost = false;
        Iterator<Ball> it = balls.iterator();
        while (it.hasNext()) {
            Ball b = it.next();
            if (b == null) {
                it.remove();
                continue;
            }
            if (b.getY() > panelHeight) {
                if (b == primaryBall) {
                    primaryLost = true;
                }
                it.remove();
            }
        }
        if (primaryLost) {
            primaryBall = balls.isEmpty() ? null : balls.get(0);
        }
        return primaryLost;
    }

    public void resetToSingleBall(Ball newPrimary) {
        if (newPrimary == null) {
            primaryBall = null;
            balls.clear();
            launchQueued = false;
            collisionManager.resetCooldown();
            return;
        }
        primaryBall = newPrimary;
        balls.clear();
        balls.add(newPrimary);
        launchQueued = false;
        collisionManager.resetCooldown();
    }

    /**
     * hàm nhân bóng , xử lí power up x3.
     * @param multiplier
     */
    public void multiplyBallsTo(int multiplier) {
        if (primaryBall == null || multiplier <= 1) return;

        int currentCount = balls.size();
        int targetCount = currentCount * multiplier;

        // ✅ Giới hạn không vượt quá 10 bóng
        if (targetCount > 10) {
            targetCount = 10;
        }

        // Nếu đã đạt giới hạn rồi thì bỏ qua
        if (currentCount >= targetCount) return;

        int needed = targetCount - currentCount;
        double baseX = primaryBall.getPreciseX();
        double baseY = primaryBall.getPreciseY();
        Velocity baseVelocity = primaryBall.getVelocity();
        double baseSpeed = (baseVelocity != null) ? baseVelocity.getMagnitude() : GameConfig.BALL_DEFAULT_SPEED;

        // Góc ban đầu của bóng chính
        double baseAngle = Math.toDegrees(Math.atan2(
                baseVelocity != null ? baseVelocity.getDy() : -GameConfig.BALL_DEFAULT_SPEED,
                baseVelocity != null ? baseVelocity.getDx() : 0
        ));
        if (Double.isNaN(baseAngle)) baseAngle = -90;

        for (int i = 0; i < needed; i++) {
            double angle = baseAngle + (i - needed / 2.0) * 15;
            Velocity vel = Velocity.fromAngle(angle, baseSpeed);
            Ball clone = new Ball((int) Math.round(baseX), (int) Math.round(baseY), vel);
            clone.detachFromPaddle();
            balls.add(clone);
        }
    }

    /** xử lí khi hết thời gian power up slow.
     */
    public void resetAllBallSpeeds() {
        for (Ball b : balls) {
            b.resetSpeed();
        }
    }

    public boolean isBallSlowed() {
        for (Ball b : balls) {
            if (b.isSlowed()) return true;
        }
        return false;
    }
}

