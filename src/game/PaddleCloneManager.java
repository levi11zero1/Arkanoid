package game;

import entities.Ball;
import entities.Paddle;
import java.awt.*;
import java.util.List;
import utils.GameConfig;

/**
 * Quản lý phân thân paddle: tạo 2 paddle phụ, cập nhật vị trí, vẽ, va chạm và thời lượng.
 */
public class PaddleCloneManager {
    private boolean active = false;
    private Paddle leftClone;
    private Paddle rightClone;
    private long expireAt = 0L; // epoch millis

    // cấu hình
    private final int gap = 40;         // khoảng cách từ mép paddle chính
    private final int durationMs = 15_000; // 15 giây tồn tại

    public boolean isActive() { return active; }

    public void reset() {
        active = false;
        leftClone = null;
        rightClone = null;
        expireAt = 0L;
    }

    public void toggle(Paddle main) {
        if (active) { reset(); return; }
        if (main == null) return;
        active = true;
        leftClone = new Paddle(Math.max(0, main.getX() - main.getWidth() - gap), main.getY(), true);
        rightClone = new Paddle(Math.min(GameConfig.SCREEN_WIDTH - main.getWidth(), main.getX() + main.getWidth() + gap), main.getY(), true);
        expireAt = System.currentTimeMillis() + durationMs;
    }

    public void update(Paddle main) {
        if (!active) return;
        if (System.currentTimeMillis() > expireAt || main == null) { reset(); return; }
        if (leftClone != null) {
            double nx = main.getX() - main.getWidth() - gap;
            if (nx < 0) nx = 0;
            leftClone.setXForClone(nx);
        }
        if (rightClone != null) {
            double nx = main.getX() + main.getWidth() + gap;
            if (nx + rightClone.getWidth() > GameConfig.SCREEN_WIDTH) nx = GameConfig.SCREEN_WIDTH - rightClone.getWidth();
            rightClone.setXForClone(nx);
        }
    }

    public void render(Graphics2D g2) {
        if (!active || g2 == null) return;
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        if (leftClone != null) leftClone.draw(g2);
        if (rightClone != null) rightClone.draw(g2);
        g2.setComposite(old);
    }

    public void handleCollisions(List<Ball> balls) {
        if (!active || balls == null) return;
        for (Ball b : balls) {
            if (b == null) continue;
            if (leftClone != null && leftClone.isHit(b.getX(), b.getY(), GameConfig.BALL_SIZE)) {
                b.bounceOffPaddle(leftClone.getX(), leftClone.getWidth());
                b.setPosition(b.getPreciseX(), leftClone.getY() - GameConfig.BALL_SIZE - 1);
            } else if (rightClone != null && rightClone.isHit(b.getX(), b.getY(), GameConfig.BALL_SIZE)) {
                b.bounceOffPaddle(rightClone.getX(), rightClone.getWidth());
                b.setPosition(b.getPreciseX(), rightClone.getY() - GameConfig.BALL_SIZE - 1);
            }
        }
    }
}
