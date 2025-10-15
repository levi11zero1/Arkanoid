package entities;

import java.awt.Color;
import java.awt.Graphics;
import utils.GameConfig;
import utils.Velocity;
import powerup.PowerUp;

public class Ball {
    private double x, y;
    private Velocity velocity;
    private javax.swing.Timer sizeTimer;

    public Ball(int x, int y) {
        this.x = x;
        this.y = y;
        this.velocity = new Velocity(GameConfig.BALL_DEFAULT_SPEED, -GameConfig.BALL_DEFAULT_SPEED);
    }

    public Ball(int x, int y, Velocity velocity) {
        this.x = x;
        this.y = y;
        this.velocity = velocity;
    }

    public void move() {
        x += velocity.getDx();
        y += velocity.getDy();

        // Speed cap
        double speed = velocity.getMagnitude();
        if (speed < GameConfig.BALL_MIN_SPEED) {
            velocity = velocity.scale(GameConfig.BALL_MIN_SPEED / speed);
        } else if (speed > GameConfig.BALL_MAX_SPEED) {
            velocity = velocity.scale(GameConfig.BALL_MAX_SPEED / speed);
        }
    }


    public int getX() {
        return (int) Math.round(x);
    }

    public int getY() {
        return (int) Math.round(y);
    }

    public double getPreciseX() {
        return x;
    }

    public double getPreciseY() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillOval(getX(), getY(), GameConfig.BALL_SIZE, GameConfig.BALL_SIZE);
    }

    public void bounceX() {
        velocity.setDx(-velocity.getDx());
        addRandomVariation();
    }

    public void bounceY() {
        velocity.setDy(-velocity.getDy());
        addRandomVariation();
    }

    // Nảy bóng dựa trên điểm chạm
    public void bounceOffPaddle(double paddleX, double paddleWidth) {
        double paddleCenter = paddleX + paddleWidth / 2;
        double ballCenter = x + GameConfig.BALL_SIZE / 2;
        double hitOffset = (ballCenter - paddleCenter) / (paddleWidth / 2); // -1 -> 1

        hitOffset = Math.max(-1.0, Math.min(1.0, hitOffset));

        double angle = hitOffset * GameConfig.MAX_PADDLE_ANGLE;
        double speed = velocity.getMagnitude();

        // Bắn thẳng
        velocity = Velocity.fromAngle(angle - 90, speed);
        addRandomVariation();
    }

    //Randomize cho nó chất
    private void addRandomVariation() {
        double randomFactor = 1 + (Math.random() - 0.5) * GameConfig.VELOCITY_VARIATION;
        velocity = velocity.scale(randomFactor);
    }

    public Velocity getVelocity() {
        return velocity;
    }

    public void resetSize() {
        GameConfig.BALL_SIZE = 20;
    }

    public void setVelocity(Velocity velocity) {
        this.velocity = velocity;
    }
    public void applyPowerUp(PowerUp.Type type) {
        if (sizeTimer != null && sizeTimer.isRunning()) {
            sizeTimer.stop();
            resetSize();
        }
        if (type == PowerUp.Type.BALL_EXPAND) GameConfig.BALL_SIZE *= 1.5;
        else if (type == PowerUp.Type.BALL_SHRINK) GameConfig.BALL_SIZE /= 1.5;

        sizeTimer = new javax.swing.Timer(10000, e -> { resetSize(); sizeTimer.stop(); });
        sizeTimer.setRepeats(false);
        sizeTimer.start();
    }

    // Check mép để ko lỗi
    public void checkBounds(int screenWidth, int screenHeight) {
        boolean bounced = false;

        if (x < 0) {
            x = 0;
            velocity.setDx(Math.abs(velocity.getDx()));
            bounced = true;
        }

        if (x + GameConfig.BALL_SIZE > screenWidth) {
            x = screenWidth - GameConfig.BALL_SIZE;
            velocity.setDx(-Math.abs(velocity.getDx()));
            bounced = true;
        }


        if (y < 0) {
            y = 0;
            velocity.setDy(Math.abs(velocity.getDy()));
            bounced = true;
        }

        if (bounced) {
            addRandomVariation();
        }
    }
}
