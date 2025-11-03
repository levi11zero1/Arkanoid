package entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import powerup.PowerUp;
import utils.GameConfig;
import utils.AudioManager;
import utils.Velocity;

public class Ball implements GameObject {
    private double x, y;
    private Velocity velocity;
    private javax.swing.Timer sizeTimer;
    private double prevX, prevY;
    private int prevSize;
    private boolean attachedToPaddle;

    public Ball(int x, int y) {
        this.x = x;
        this.y = y;
        this.velocity = new Velocity(GameConfig.BALL_DEFAULT_SPEED, -GameConfig.BALL_DEFAULT_SPEED);
        this.attachedToPaddle = false;
    }

    public Ball(int x, int y, Velocity velocity) {
        this.x = x;
        this.y = y;
        this.velocity = velocity;
    }

    public void move() {
        if (attachedToPaddle) {
            return;
        }

        // Lưu vị trí trước khi di chuyển
        prevX = x;
        prevY = y;
        prevSize = GameConfig.BALL_SIZE;
        x += velocity.getDx();
        y += velocity.getDy();

        // Speed cap ổn định
        clampSpeed();
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

    @Override
    public void draw(Graphics g) {
        BallSkin.Skin skin = BallSkin.getSkin();
        if (skin != null) {
            if (g instanceof Graphics2D g2d) {
                // Ghép ảnh vào clip tròn để dễ thay skin mà vẫn giữ hình tròn
                Shape oldClip = g2d.getClip();
                Ellipse2D circle = new Ellipse2D.Double(getX(), getY(), GameConfig.BALL_SIZE, GameConfig.BALL_SIZE);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setClip(circle);
                g2d.drawImage(skin.image(), getX(), getY(), GameConfig.BALL_SIZE, GameConfig.BALL_SIZE, null);
                g2d.setClip(oldClip);
            } else {
                g.drawImage(skin.image(), getX(), getY(), GameConfig.BALL_SIZE, GameConfig.BALL_SIZE, null);
            }
            return;
        }
        g.setColor(Color.BLUE);
        g.fillOval(getX(), getY(), GameConfig.BALL_SIZE, GameConfig.BALL_SIZE);
    }

    public void bounceX() {
        velocity.setDx(-velocity.getDx());
        clampSpeed();
    }

    public void bounceY() {
        velocity.setDy(-velocity.getDy());
        clampSpeed();
    }

    // Nảy bóng dựa trên điểm chạm
    public void bounceOffPaddle(double paddleX, double paddleWidth) {
        if (attachedToPaddle) {
            return;
        }
        double paddleCenter = paddleX + paddleWidth / 2;
        double ballCenter = x + GameConfig.BALL_SIZE / 2;
        double hitOffset = (ballCenter - paddleCenter) / (paddleWidth / 2); // -1 -> 1

        hitOffset = Math.max(-1.0, Math.min(1.0, hitOffset));

        double angle = hitOffset * GameConfig.MAX_PADDLE_ANGLE;
        double speed = velocity.getMagnitude();

        // Bắn thẳng
        velocity = Velocity.fromAngle(angle - 90, speed);
        clampSpeed();
    }

    private void clampSpeed() {
        double speed = velocity.getMagnitude();
        if (speed > 0 && speed < GameConfig.BALL_MIN_SPEED) {
            velocity = velocity.scale(GameConfig.BALL_MIN_SPEED / speed);
        } else if (speed > GameConfig.BALL_MAX_SPEED) {
            velocity = velocity.scale(GameConfig.BALL_MAX_SPEED / speed);
        }
    }

    public Velocity getVelocity() {
        return velocity;
    }

    public void resetSize() {
        GameConfig.BALL_SIZE = GameConfig.DEFAULT_BALL_SIZE;
        if (sizeTimer != null) {
            sizeTimer.stop();
        }
    }

    public void resetSpeed() {
        double magnitude = velocity.getMagnitude();
        if (magnitude == 0) {
            velocity = Velocity.fromAngle(-90, GameConfig.BALL_DEFAULT_SPEED);
            return;
        }
        double angle = Math.atan2(velocity.getDy(), velocity.getDx());
        velocity.setDx(Math.cos(angle) * GameConfig.BALL_DEFAULT_SPEED);
        velocity.setDy(Math.sin(angle) * GameConfig.BALL_DEFAULT_SPEED);
    }

    public boolean isAttachedToPaddle() {
        return attachedToPaddle;
    }

    public void attachToPaddle(Paddle paddle) {
        if (paddle == null) {
            return;
        }
        attachedToPaddle = true;
        velocity = new Velocity(0, 0);
        centerOnPaddle(paddle);
    }

    public void detachFromPaddle() {
        attachedToPaddle = false;
        if (velocity.getMagnitude() == 0) {
            velocity = Velocity.fromAngle(-90, GameConfig.BALL_DEFAULT_SPEED);
        }
    }

    public void centerOnPaddle(Paddle paddle) {
        if (paddle == null) {
            return;
        }
        double paddleCenter = paddle.getX() + paddle.getWidth() / 2.0;
        this.x = paddleCenter - GameConfig.BALL_SIZE / 2.0;
        this.y = paddle.getY() - GameConfig.BALL_SIZE;
        this.prevX = x;
        this.prevY = y;
    }


    public void setVelocity(Velocity velocity) {
        this.velocity = velocity;
    }
    public void applyPowerUp(PowerUp.Type type) {
        if (sizeTimer != null && sizeTimer.isRunning()) {
            sizeTimer.stop();
            resetSize();
        }
        if (type == PowerUp.Type.BALL_EXPAND) {
            GameConfig.BALL_SIZE *= 1.5;
        } else if (type == PowerUp.Type.BALL_SHRINK) {
            GameConfig.BALL_SIZE /= 1.5;
            velocity.setDx(velocity.getDx() * 1.5);
            velocity.setDy(velocity.getDy() * 1.5);
        } else if (type == PowerUp.Type.BALL_SLOW) {
            slowDown(); // ✅ Gọi hàm mới để giảm tốc độ bóng
            return;
        }

    sizeTimer = new javax.swing.Timer(9000, e -> { if (e != null) { resetSize(); resetSpeed(); sizeTimer.stop(); } });
        sizeTimer.setRepeats(false);
        sizeTimer.start();
    }

    private void slowDown() {
        velocity.setDx(velocity.getDx() * 0.6);
        velocity.setDy(velocity.getDy() * 0.6);

        javax.swing.Timer slowTimer = new javax.swing.Timer(15000, e -> {
            // Sau 20s, trả lại tốc độ bình thường
            velocity.setDx(velocity.getDx() / 0.6);
            velocity.setDy(velocity.getDy() / 0.6);
        });
        slowTimer.setRepeats(false);
        slowTimer.start();
    }

    // Check mép để ko lỗi
    public void checkBounds(int screenWidth, int screenHeight) {
        if (attachedToPaddle) {
            return;
        }
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
            // Play wall hit sound once (WAV)
            try {
                AudioManager.playOnce("music/wall.wav", null);
            } catch (Throwable ignored) {
                // ignore
            }
        }
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(getX(), getY(), GameConfig.BALL_SIZE, GameConfig.BALL_SIZE);
    }
}
