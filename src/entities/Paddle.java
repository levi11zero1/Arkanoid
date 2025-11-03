package entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import javax.swing.Timer;
import powerup.PowerUp;
import utils.GameConfig;

public class Paddle implements GameObject {
    private final boolean useSkin;
    private double x;
    private int y;
    private Timer sizeTimer;
    private Timer speedTimer;
    private int width;
    private int height;
    private int normalWidth;
    private double normalSpeed;

    public Paddle(double x, int y) {
        this(x, y, true);
    }

    public Paddle(double x, int y, boolean useSkin) {
        this.useSkin = useSkin;
        this.y = y;
        this.sizeTimer = null;
        this.speedTimer = null;
        this.width = GameConfig.DEFAULT_PADDLE_WIDTH;
        this.height = GameConfig.PADDLE_HEIGHT;
        this.normalWidth = GameConfig.DEFAULT_PADDLE_WIDTH;
        this.normalSpeed = GameConfig.DEFAULT_PADDLE_SPEED;
        double centerX = x + GameConfig.DEFAULT_PADDLE_WIDTH / 2.0;
        applySkin(centerX);
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

        if (x < 0) {
            x = 0;
        }
        if (x + width > frameWidth) {
            x = frameWidth - width;
        }
    }

    @Override
    public void draw(Graphics g) {
        int drawX = (int) Math.round(x);
        if (useSkin) {
            PaddleSkin.Skin skin = PaddleSkin.getSkin();
            if (skin != null) {
                Image image = skin.image();
                int skinWidth = skin.width();
                int skinHeight = skin.height();
                if (skinWidth > 0 && skinHeight > 0) {
                    g.drawImage(image, drawX, y, width, height, null);
                    return;
                }
            }
        }

        g.setColor(Color.GREEN);
        g.fillRect(drawX, y, width, height);
    }

    public boolean isHit(int ballX, int ballY, int ballSize) {
        return ballX + ballSize > x && ballX < x + width &&
               ballY + ballSize > y && ballY < y + height;
    }

    public double getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void resetSize() {
        double centerX = x + width / 2.0;
        width = normalWidth;
        if (sizeTimer != null) {
            sizeTimer.stop();
        }
        applySkin(centerX);
    }

    public void resetSpeed() {
        GameConfig.PADDLE_SPEED = normalSpeed;
        if (speedTimer != null) {
            speedTimer.stop();
        }
    }

    public void applyPowerUp(PowerUp.Type type) {
        if (type == PowerUp.Type.PADDLE_EXPAND || type == PowerUp.Type.PADDLE_SHRINK) {
            if (sizeTimer != null && sizeTimer.isRunning()) {
                sizeTimer.stop();
                resetSize();
            }

            double centerX = x + width / 2.0;
            if (type == PowerUp.Type.PADDLE_EXPAND) {
                width = (int) Math.max(10, Math.round(width * 1.35));
            } else {
                width = (int) Math.max(10, Math.round(width / 1.3));
            }
            x = centerX - width / 2.0;

            sizeTimer = new Timer(10000, e -> {
                if (e != null) {
                    e.getSource();
                }
                resetSize();
                sizeTimer.stop();
            });
            sizeTimer.setRepeats(false);
            sizeTimer.start();
            return;
        }

        if (type == PowerUp.Type.PADDLE_SPEED_UP) {
            if (speedTimer != null && speedTimer.isRunning()) {
                speedTimer.stop();
                resetSpeed();
            }

            GameConfig.PADDLE_SPEED *= 1.5;
            speedTimer = new Timer(10000, e -> {
                if (e != null) {
                    e.getSource();
                }
                resetSpeed();
                speedTimer.stop();
            });
            speedTimer.setRepeats(false);
            speedTimer.start();
        }
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle((int) Math.round(x), y, width, height);
    }

    private void applySkin(double centerX) {
        if (useSkin) {
            PaddleSkin.Skin skin = PaddleSkin.getSkin();
            if (skin != null && skin.width() > 0 && skin.height() > 0) {
                width = skin.width();
                height = Math.max(GameConfig.PADDLE_HEIGHT, skin.height());
                normalWidth = width;
            } else {
                width = GameConfig.DEFAULT_PADDLE_WIDTH;
                height = GameConfig.PADDLE_HEIGHT;
                normalWidth = width;
            }
        } else {
            width = GameConfig.DEFAULT_PADDLE_WIDTH;
            height = GameConfig.PADDLE_HEIGHT;
            normalWidth = width;
        }

        int overflow = y + height - GameConfig.SCREEN_HEIGHT;
        if (overflow > 0) {
            y = Math.max(0, y - overflow);
        }

        x = centerX - width / 2.0;
    }
}
