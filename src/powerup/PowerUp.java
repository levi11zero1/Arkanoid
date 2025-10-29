package powerup;

import java.awt.*;

public class PowerUp {
    public enum Type {
        PADDLE_EXPAND,   // Tăng kích thước paddle
        PADDLE_SHRINK,   // Giảm kích thước paddle
        BALL_EXPAND,     // Tăng kích thước bóng
        BALL_SHRINK,     // Giảm kích thước bóng
        BALL_SLOW// Giảm tốc độ bóng
    }

    private Type type;
    private Color color;
    private int x, y;
    private int width = 20, height = 20;
    private double fallSpeed = 2.2; // tốc độ rơi xuống

    public PowerUp(Type type, int startX, int startY) {
        this.type = type;
        this.x = startX;
        this.y = startY;

        switch (type) {
            case PADDLE_EXPAND: color = Color.PINK; break;
            case PADDLE_SHRINK: color = Color.MAGENTA; break;
            case BALL_EXPAND: color = Color.CYAN; break;
            case BALL_SHRINK: color = Color.YELLOW; break;
            case BALL_SLOW:  color = new Color(200, 255, 200); break;
        }
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
}
