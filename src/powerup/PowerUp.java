package powerup;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class PowerUp {
    private static final Logger LOGGER = Logger.getLogger(PowerUp.class.getName());
    public enum Type {
    PADDLE_EXPAND,   // Tăng kích thước paddle
    PADDLE_SHRINK,   // Giảm kích thước paddle
    BALL_EXPAND,     // Tăng kích thước bóng
    BALL_SHRINK,     // Giảm kích thước bóng
    BALL_SLOW,       // Giảm tốc độ bóng
    PADDLE_SPEED_UP, // Tăng tốc độ thanh paddle
    BALL_MULTIPLY_THREE // Nhân bóng lên 3 quả
    }

    private final Type type;
    private Color color;
    private final int x;
    private int y;
    private final int width = 46;
    private final int height = 18; // kích thước của power up
    private final double fallSpeed = 3.6; // tốc độ power up rơi xuống
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
            Graphics2D g2 = (Graphics2D) g.create();
            // Improve scaling quality and respect alpha
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.SrcOver);
            g2.drawImage(image, x, y, width, height, null);
            g2.dispose();
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
            // Chuyển nền trắng (hoặc gần trắng) thành trong suốt để loại bỏ viền trắng
            image = makeNearWhiteTransparent(image, 250);

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load power-up image", e);
            image = null;
        }
    }

    // Loại bỏ nền trắng: bất kỳ pixel nào có R,G,B >= threshold sẽ được đặt alpha = 0
    private BufferedImage makeNearWhiteTransparent(BufferedImage src, int threshold) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                int rgba = src.getRGB(xx, yy);
                int a = (rgba >>> 24) & 0xFF;
                int r = (rgba >>> 16) & 0xFF;
                int g = (rgba >>> 8) & 0xFF;
                int b = (rgba) & 0xFF;

                // Nếu pixel gần như trắng và không hoàn toàn trong suốt, đặt alpha = 0
                if (a > 0 && r >= threshold && g >= threshold && b >= threshold) {
                    out.setRGB(xx, yy, (rgba & 0x00FFFFFF)); // alpha = 0
                } else {
                    out.setRGB(xx, yy, rgba);
                }
            }
        }
        return out;
    }

}
