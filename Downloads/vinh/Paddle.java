import java.awt.Color;
import java.awt.Graphics;

public class Paddle {
    public int x, y, width = 100, height = 15;

    public Paddle(int x, int y) {
        this.x = x; this.y = y;
    }

    public void moveLeft() {
        if (x > 0) x -= 20;
    }

    public void moveRight(int frameWidth) {
        if (x + width < frameWidth) x += 20;
    }

    public void draw(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillRect(x, y, width, height);
    }

    public boolean isHit(int ballX, int ballY, int ballSize) {
        return ballX + ballSize > x && ballX < x + width &&
                ballY + ballSize > y && ballY < y + height;
    }
}

/**
 * Đại diện cho thanh paddle do người chơi điều khiển.
 *
 * Lưu vị trí và kích thước (x, y, width, height).
 *
 * Di chuyển trái/phải bằng moveLeft() và moveRight(...).
 *
 * Kiểm tra va chạm với bóng bằng isHit(...).
 *
 * Vẽ paddle lên màn hình bằng draw(Graphics g).
 */