package powerup;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

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
        Graphics2D g2d = (Graphics2D) g.create();

        // Bật khử răng cưa + blend alpha
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setComposite(java.awt.AlphaComposite.SrcOver);

        if (image != null) {
            g.drawImage(image, x, y, width, height, null);
        } else {
            // debug nếu không
            g2d.setColor(Color.WHITE);
            g2d.fillRect(x, y, width, height);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, y, width, height);
        }
    }

    private void loadImage() {
        try {
            String path = switch (type) {
                case PADDLE_EXPAND -> "images/PowerUp/paddle_expand.png";
                case PADDLE_SHRINK -> "images/PowerUp/paddle_shrink.png";
                case BALL_EXPAND -> "images/PowerUp/ball_expand.png";
                case BALL_SHRINK -> "images/PowerUp/ball_shrink.png";
                case BALL_SLOW -> "images/PowerUp/ball__slow.png";
                case PADDLE_SPEED_UP -> "images/PowerUp/paddle_speedup.png";
                case BALL_MULTIPLY_THREE -> "images/PowerUp/ball_multiply_three.png";
            };

            File file = new File(path);
            image = ImageIO.read(file);

        } catch (IOException e) {
            e.printStackTrace();
            image = null;
        }
    }

}
