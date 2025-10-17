package entities;

import java.awt.Color;
import java.awt.Graphics;
import utils.GameConfig;
import powerup.PowerUp;
import java.awt.Rectangle;

public class Paddle {
    private double x;
    private int y;
    private javax.swing.Timer sizeTimer;
    public int normalWidth = GameConfig.PADDLE_WIDTH;

    public Paddle(double x, int y) {
        this.x = x;
        this.y = y;
    }

    public void update(boolean leftPressed, boolean rightPressed, int frameWidth, double dt) {
        double velocity = 0;

        if (leftPressed) {
            velocity -= GameConfig.PADDLE_SPEED;
        }
        if (rightPressed) {
            velocity += GameConfig.PADDLE_SPEED;
        }

        x += velocity * dt;

        // Keep paddle within screen bounds
        if (x < 0) {
            x = 0;
        }
        if (x + GameConfig.PADDLE_WIDTH > frameWidth) {
            x = frameWidth - GameConfig.PADDLE_WIDTH;
        }
    }

    public void draw(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillRect((int) Math.round(x), y, GameConfig.PADDLE_WIDTH, GameConfig.PADDLE_HEIGHT);
    }

    // Check va chạm của paddle với ball
    public boolean isHit(int ballX, int ballY, int ballSize) {
        return ballX + ballSize > x && ballX < x + GameConfig.PADDLE_WIDTH &&
               ballY + ballSize > y && ballY < y + GameConfig.PADDLE_HEIGHT;
    }

    public double getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return GameConfig.PADDLE_WIDTH;
    }

    public int getHeight() {
        return GameConfig.PADDLE_HEIGHT;
    }

    public void resetSize() {
        GameConfig.PADDLE_WIDTH = normalWidth; // quay về kích thước ban đầu
    }


    public void applyPowerUp(PowerUp.Type type) {
        if (sizeTimer != null && sizeTimer.isRunning()) {
            sizeTimer.stop();
            resetSize();
        }
        if (type == PowerUp.Type.PADDLE_EXPAND) GameConfig.PADDLE_WIDTH *= 1.4;
        else if (type == PowerUp.Type.PADDLE_SHRINK) GameConfig.PADDLE_WIDTH /= 1.2;

        sizeTimer = new javax.swing.Timer(10000, e -> { if (e != null) { /* use event to avoid unused warning */ } resetSize(); sizeTimer.stop(); });
        sizeTimer.setRepeats(false);
        sizeTimer.start();
    }
    public Rectangle getBounds() {
        return new Rectangle((int)x, y, GameConfig.PADDLE_WIDTH, GameConfig.PADDLE_HEIGHT);
    }
}
