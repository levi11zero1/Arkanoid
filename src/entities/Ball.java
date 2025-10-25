package entities;

import java.awt.Color;
import java.awt.Graphics;
import powerup.PowerUp;
import utils.GameConfig;
import utils.MusicPlayer;
import utils.Velocity;
import powerup.PowerUp;

public class Ball {
    private double x, y;
    private Velocity velocity;
    private javax.swing.Timer sizeTimer;
    private double prevX, prevY;
    private int prevSize;

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
        // Delegate movement to Movement utility (records previous pos/size and clamps speed)
        Movement.BallMoveResult r = Movement.moveBall(x, y, velocity, GameConfig.BALL_SIZE);
        this.prevX = r.prevX;
        this.prevY = r.prevY;
        this.prevSize = r.prevSize;
        this.x = r.x;
        this.y = r.y;
        this.velocity = r.velocity;
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

    public double getPrevX() { return prevX; }
    public double getPrevY() { return prevY; }
    public int getPrevSize() { return prevSize; }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillOval(getX(), getY(), GameConfig.BALL_SIZE, GameConfig.BALL_SIZE);
    }

    public void bounceX() {
        velocity = Physics.reflectHorizontal(velocity);
        velocity = Physics.clampSpeed(velocity);
    }

    public void bounceY() {
        velocity = Physics.reflectVertical(velocity);
        velocity = Physics.clampSpeed(velocity);
    }

    // Nảy bóng dựa trên điểm chạm
    public void bounceOffPaddle(double paddleX, double paddleWidth) {
        velocity = Physics.bounceFromPaddle(x, GameConfig.BALL_SIZE, paddleX, (int)paddleWidth, velocity);
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

    sizeTimer = new javax.swing.Timer(10000, e -> { if (e != null) { resetSize(); sizeTimer.stop(); } });
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
            clampSpeed();
        }
    }
}
