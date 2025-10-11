import java.awt.Color;
import java.awt.Graphics;

public class Block {
    private int x, y, width, height;
    private boolean destroyed = false;

    private int hitsRemaining;

    public Block(int x, int y, int width, int height, int hitsRemaining) {
        this.x = x; this.y = y;
        this.width = width; this.height = height;
        this.hitsRemaining = Math.max(1, hitsRemaining);
    }

    public void draw(Graphics g) {
        if (!destroyed) {
            Color c;
            c = switch (hitsRemaining) {
                case 3 -> Color.MAGENTA;
                case 2 -> Color.ORANGE;
                default -> Color.RED;
            };
            g.setColor(c);
            g.fillRect(x, y, width, height);
        }
    }

    public boolean isHit(int ballX, int ballY, int ballSize) {
        if (!destroyed && ballX + ballSize > x && ballX < x + width && ballY + ballSize > y && ballY < y + height) {
            hitsRemaining--;
            if (hitsRemaining <= 0) {
                destroyed = true;
            }
            return true;
        }
        return false;
    }
    
    //Xem xem bóng đập vào bên nào của paddle
    public String getCollisionSide(double ballX, double ballY, double ballSize, double ballVx, double ballVy) {
        if (destroyed) return null;
        
        double ballCenterX = ballX + ballSize / 2;
        double ballCenterY = ballY + ballSize / 2;
        
        // TÍnh khoảng cách tới mép
        double leftDist = Math.abs(ballCenterX - x);
        double rightDist = Math.abs(ballCenterX - (x + width));
        double topDist = Math.abs(ballCenterY - y);
        double bottomDist = Math.abs(ballCenterY - (y + height));
        
        double minHorizontal = Math.min(leftDist, rightDist);
        double minVertical = Math.min(topDist, bottomDist);
        
        if (minHorizontal < minVertical) {
            return (ballVx > 0) ? "left" : "right";
        } else {
            return (ballVy > 0) ? "top" : "bottom";
        }
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}

/**
 * Đại diện cho một viên gạch trong game.
 *
 * Lưu vị trí và kích thước của viên gạch (x, y, width, height).
 * 
 * Kiểm tra số va chạm còn lại qua biến hitsRemaining
 *
 * Kiểm tra va chạm với bóng bằng hàm isHit(...).
 *
 * Ẩn viên gạch khi bị phá bằng cách đánh dấu destroyed = true.
 *
 * Vẽ viên gạch lên màn hình bằng hàm draw(Graphics g) nếu chưa bị phá.
 *
 */