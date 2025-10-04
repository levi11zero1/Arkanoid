import java.awt.Color;
import java.awt.Graphics;

public class Ball {
    public int x, y, size = 20;
    public int dx = 2, dy = -2;

    public Ball(int x, int y) {
        this.x = x; this.y = y;
    }

    public void move() {
        x += dx;
        y += dy;
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillOval(x, y, size, size);
    }

    public void bounceX() { dx = -dx; }
    public void bounceY() { dy = -dy; }
}

/**
 * Quản lý vị trí bóng trên màn hình (x, y).
 *
 * Di chuyển bóng theo hướng (dx, dy) bằng hàm move().
 *
 * Vẽ bóng lên giao diện bằng hàm draw(Graphics g).
 *
 * Xử lý va chạm bằng cách đảo chiều chuyển động:
 *
 * bounceX() → đổi hướng ngang khi chạm tường trái/phải.
 *
 * bounceY() → đổi hướng dọc khi chạm tường trên, paddle, hoặc block.
 *
 */