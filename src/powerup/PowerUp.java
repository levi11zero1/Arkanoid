package powerup;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.File;

public class PowerUp {
    public enum Type {
    PADDLE_EXPAND,   // Tăng kích thước paddle
    PADDLE_SHRINK,   // Giảm kích thước paddle
    BALL_EXPAND,     // Tăng kích thước bóng
    BALL_SHRINK,     // Giảm kích thước bóng
    BALL_SLOW,       // Giảm tốc độ bóng
    PADDLE_SPEED_UP, // Tăng tốc độ thanh paddle
    BALL_MULTIPLY_THREE // Nhân bóng lên 3 quả
    }

    private Type type;
    private Color color;
    private int x, y;
    private int width = 46, height = 18;// kích thước của power up
    private double fallSpeed = 3.6; // tốc độ power up rơi xuống
    private BufferedImage image;

    public PowerUp(Type type, int startX, int startY) {
        this.type = type;
        this.x = startX;
        this.y = startY;

        loadImage();
    }

    public void updatePosition() { y += fallSpeed; }

    public boolean isOutOfBounds(int panelHeight) { return y > panelHeight; }

    public Rectangle getBounds() { return new Rectangle(x, y, width, height); }

    public Type getType() { return type; }
    public Color getColor() { return color; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void draw(Graphics g) {
        if (image != null) {
            g.drawImage(image, x, y, width, height, null);
        } else {
            // debug nếu không
            g.setColor(Color.WHITE);
            g.fillRect(x, y, width, height);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, width, height);
        }
    }

    private void loadImage() {
        try {
            String path = switch (type) {
                case PADDLE_EXPAND -> "images/paddle_expand.png";
                case PADDLE_SHRINK -> "images/paddle_shrink.png";
                case BALL_EXPAND -> "images/ball_expand.png";
                case BALL_SHRINK -> "images/ball_shrink.png";
                case BALL_SLOW -> "images/ball__slow.png";
                case PADDLE_SPEED_UP -> "images/paddle_speedup.png";
                case BALL_MULTIPLY_THREE -> "images/ball_multiply_three.png";
            };

            File file = new File(path);
            image = ImageIO.read(file);

        } catch (IOException e) {
            e.printStackTrace();
            image = null;
        }
    }

}
