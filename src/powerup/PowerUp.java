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
        try {
            // Bật khử răng cưa + blend alpha
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Tránh viền trắng khi scale PNG trong suốt
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2d.setComposite(java.awt.AlphaComposite.SrcOver);

            if (image != null) {
                // Dùng g2d (với hints ở trên) thay vì g để áp dụng đúng interpolation/alpha
                g2d.drawImage(image, x, y, width, height, null);
            } else {
                // debug nếu không có ảnh
                g2d.setColor(Color.WHITE);
                g2d.fillRect(x, y, width, height);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, width, height);
            }
        } finally {
            g2d.dispose();
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
            BufferedImage raw = ImageIO.read(file);
            image = postProcessImage(raw);

        } catch (IOException e) {
            System.err.println("[PowerUp] Lỗi đọc ảnh: " + e.getMessage());
            image = null;
        }
    }

    // Đảm bảo ảnh có kênh alpha, và nếu ảnh nền trắng (không có alpha), chuyển nền trắng thành trong suốt
    private static BufferedImage postProcessImage(BufferedImage src) {
        if (src == null) return null;

        // Nếu ảnh đã có alpha, chỉ cần đảm bảo định dạng ARGB để vẽ ổn định
        if (src.getColorModel().hasAlpha()) {
            return toARGB(src);
        }

        // Không có alpha: coi nền gần trắng là trong suốt để tránh ô trắng bao quanh
        BufferedImage argb = toARGB(src);
        int w = argb.getWidth();
        int h = argb.getHeight();
        int threshold = 250; // ngưỡng gần trắng
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgba = argb.getRGB(x, y);
                int r = (rgba >> 16) & 0xFF;
                int g = (rgba >> 8) & 0xFF;
                int b = (rgba) & 0xFF;
                if (r >= threshold && g >= threshold && b >= threshold) {
                    // đặt alpha = 0 giữ nguyên màu tránh viền do anti-alias nền trắng
                    argb.setRGB(x, y, 0x00FFFFFF & rgba);
                }
            }
        }
        return argb;
    }

    private static BufferedImage toARGB(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) {
            return src;
        }
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        try {
            g2.setComposite(AlphaComposite.Src);
            g2.drawImage(src, 0, 0, null);
        } finally {
            g2.dispose();
        }
        return out;
    }

}
