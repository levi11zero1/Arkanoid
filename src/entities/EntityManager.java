package entities;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
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
    private final Random rng = new Random();

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

    public void multiplyBallsTo(int multiplier) {
        if (primaryBall == null) return;

        int currentCount = balls.size();
        int desiredTotal = Math.min(currentCount * multiplier, 10);

        if (desiredTotal <= currentCount) return;

        int needed = desiredTotal - currentCount;
        double baseX = primaryBall.getPreciseX();
        double baseY = primaryBall.getPreciseY();
        Velocity baseVelocity = primaryBall.getVelocity();

        double baseSpeed = (baseVelocity != null) ? baseVelocity.getMagnitude() : GameConfig.BALL_DEFAULT_SPEED;
        if (baseSpeed <= 0) baseSpeed = GameConfig.BALL_DEFAULT_SPEED;

        double baselineAngle = Math.toDegrees(Math.atan2(
                baseVelocity != null ? baseVelocity.getDy() : -GameConfig.BALL_DEFAULT_SPEED,
                baseVelocity != null ? baseVelocity.getDx() : 0));
        if (Double.isNaN(baselineAngle) || Double.isInfinite(baselineAngle)) baselineAngle = -90;

        for (int i = 0; i < needed; i++) {
            double spread = 120.0 / multiplier;
            double angle = baselineAngle - 60 + spread * i;
            if (angle > -30) angle = -30;
            Velocity vel = Velocity.fromAngle(angle, baseSpeed);

            Ball clone = new Ball((int) baseX, (int) baseY, vel);
            clone.detachFromPaddle();
            balls.add(clone);
        }
    }



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

