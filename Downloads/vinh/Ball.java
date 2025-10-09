import java.awt.Color;
import java.awt.Graphics;

public class Ball {
    public int x, y, size = 20;
    private Velocity velocity;

    public Ball(int x, int y) {
        this.x = x; 
        this.y = y;
        this.velocity = new Velocity(3, -3);
    }
    
    public Ball(int x, int y, Velocity velocity) {
        this.x = x;
        this.y = y;
        this.velocity = velocity;
    }

    public void move() {
        x += (int) velocity.getDx();
        y += (int) velocity.getDy();
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillOval(x, y, size, size);
    }

    public void bounceX() { 
        velocity.setDx(-velocity.getDx()); 
    }
    
    public void bounceY() { 
        velocity.setDy(-velocity.getDy()); 
    }
    
    public Velocity getVelocity() {
        return velocity;
    }
    
    public void setVelocity(Velocity velocity) {
        this.velocity = velocity;
    }
    
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