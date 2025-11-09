package entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import powerup.PowerUp;
import utils.AudioManager;
import utils.GameConfig;
import utils.Velocity;

public class Ball implements GameObject {
    private double x, y;
    private Velocity velocity;
    private javax.swing.Timer sizeTimer;
    private double prevX, prevY;
    private int prevSize;
    private boolean attachedToPaddle;

    private boolean slowed = false;
    private static final double SLOW_MULTIPLIER = 0.7;
    private double baseSpeed = GameConfig.BALL_DEFAULT_SPEED;
    private double speedMultiplier = 1.0;


    public Ball(int x, int y) {
        this.x = x;
        this.y = y;
        double angle = -60 + Math.random() * 120;
        this.velocity = new Velocity(GameConfig.BALL_DEFAULT_SPEED, -GameConfig.BALL_DEFAULT_SPEED);
        this.attachedToPaddle = false;
    }

    public Ball(int x, int y, Velocity velocity) {
        this.x = x;
        this.y = y;
        this.velocity = velocity;
    }

    public boolean isSlowed() {
        return slowed;
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
        double currentSpeed = velocity.getMagnitude();
        double targetSpeed = baseSpeed * speedMultiplier;

        if (Math.abs(currentSpeed - targetSpeed) / targetSpeed > 0.05) {
            velocity = velocity.scale(targetSpeed / currentSpeed);
        }
    }

    public Velocity getVelocity() {
        return velocity;
    }

    public void resetSize() {
        // Giữ nguyên tâm bóng khi đổi kích thước để tránh cảm giác "nhảy" vị trí
        double cx = x + GameConfig.BALL_SIZE / 2.0;
        double cy = y + GameConfig.BALL_SIZE / 2.0;
        GameConfig.BALL_SIZE = GameConfig.DEFAULT_BALL_SIZE;
        this.x = cx - GameConfig.BALL_SIZE / 2.0;
        this.y = cy - GameConfig.BALL_SIZE / 2.0;
        if (sizeTimer != null) {
            sizeTimer.stop();
        }
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

        // Nếu bóng chưa có vận tốc, đặt hướng chéo lên
        if (velocity.getMagnitude() == 0) {
            velocity = Velocity.fromAngle(-75 + Math.random() * -30, GameConfig.BALL_DEFAULT_SPEED);
            return;
        }

        // Dù có vận tốc, nếu gần như ngang thì ép bay lên
        if (Math.abs(velocity.getDy()) < 1) {
            double direction = (Math.random() < 0.5) ? -1 : 1; // ngẫu nhiên trái/phải
            velocity = Velocity.fromAngle(-70 * direction, GameConfig.BALL_DEFAULT_SPEED);
        }

        // Đảm bảo bóng luôn bay lên trên
        if (velocity.getDy() > 0) {
            velocity.setDy(-Math.abs(velocity.getDy()));
        }
    }

    private void updateVelocityMagnitude() {
        double angle = Math.atan2(velocity.getDy(), velocity.getDx());
        velocity.setDx(Math.cos(angle) * baseSpeed * speedMultiplier);
        velocity.setDy(Math.sin(angle) * baseSpeed * speedMultiplier);
    }

    public void centerOnPaddle(Paddle paddle) {
        if (paddle == null) {
            return;
        }
        double paddleCenter = paddle.getX() + paddle.getWidth() / 2.0;
        this.x = paddleCenter - GameConfig.BALL_SIZE / 2.0;
        this.y = paddle.getY() - GameConfig.BALL_SIZE - 1;
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
            // Giữ nguyên tâm bóng khi nở để tránh "nhảy" vị trí
            double cx = x + GameConfig.BALL_SIZE / 2.0;
            double cy = y + GameConfig.BALL_SIZE / 2.0;
            GameConfig.BALL_SIZE = (int) Math.round(GameConfig.BALL_SIZE * 1.5);
            this.x = cx - GameConfig.BALL_SIZE / 2.0;
            this.y = cy - GameConfig.BALL_SIZE / 2.0;
        } else if (type == PowerUp.Type.BALL_SHRINK) {
            double cx = x + GameConfig.BALL_SIZE / 2.0;
            double cy = y + GameConfig.BALL_SIZE / 2.0;
            GameConfig.BALL_SIZE = (int) Math.max(4, Math.round(GameConfig.BALL_SIZE / 1.5));
            this.x = cx - GameConfig.BALL_SIZE / 2.0;
            this.y = cy - GameConfig.BALL_SIZE / 2.0;
            setSpeedMultiplier(1.2);
        } else if (type == PowerUp.Type.BALL_SLOW) {
            slowDown(); // ✅ Gọi hàm mới để giảm tốc độ bóng
            return;
        }
    // thời gian hiệu lực của power up
    sizeTimer = new javax.swing.Timer(8000, e -> { if (e != null) { resetSize(); resetSpeed(); sizeTimer.stop(); } });
        sizeTimer.setRepeats(false);
        sizeTimer.start();
    }

    private void slowDown() {
        if (slowed) return;
        slowed = true;
        setSpeedMultiplier(SLOW_MULTIPLIER);

        new Thread(() -> {
            try {
                Thread.sleep(7000);
            } catch (InterruptedException ignored) {}
            setSpeedMultiplier(1.0);
            slowed = false;
        }).start();
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

    public void resetSpeed() {
        if (slowed) return; // Nếu đang chậm do power-up thì không reset
        double angle = Math.atan2(velocity.getDy(), velocity.getDx());
        velocity.setDx(Math.cos(angle) * GameConfig.BALL_DEFAULT_SPEED);
        velocity.setDy(Math.sin(angle) * GameConfig.BALL_DEFAULT_SPEED);
    }

    public void applySlowEffect() {
        if (slowed) return; // Nếu đã bị chậm thì bỏ qua
        slowed = true;

        // Giảm tốc độ xuống còn 60%
        velocity.setDx(velocity.getDx() * 0.7);
        velocity.setDy(velocity.getDy() * 0.7);

        // Sau 10s thì trả lại tốc độ ban đầu
        javax.swing.Timer slowTimer = new javax.swing.Timer(10000, e -> {
            if (e != null) { /* tránh cảnh báo biến chưa dùng */ }
            velocity.setDx(velocity.getDx() / 0.7);
            velocity.setDy(velocity.getDy() / 0.7);
            slowed = false;
        });
        slowTimer.setRepeats(false);
        slowTimer.start();
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(getX(), getY(), GameConfig.BALL_SIZE, GameConfig.BALL_SIZE);
    }

    private void setSpeedMultiplier(double newMultiplier) {
        if (newMultiplier <= 0) return;
        speedMultiplier = newMultiplier;
        updateVelocityMagnitude();
    }
}
