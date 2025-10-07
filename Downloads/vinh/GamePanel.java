import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class GamePanel extends JPanel implements ActionListener, KeyListener {

    private Ball ball;
    private Paddle paddle;
    private List<Block> blocks;
    private Timer timer;

    // New key state flags
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    // For delta time (optional but nice)
    private long lastNanos;

    public GamePanel() {
        ball = new Ball(200, 300);
        paddle = new Paddle(150, 550);
        blocks = new ArrayList<>();

        // Tạo lưới block: 5 hàng × 8 cột
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 8; col++) {
                int x = 50 + col * 45;
                int y = 50 + row * 25;

                switch (row) {
                    case 0 -> blocks.add(new Block(x, y, 40, 20, 3));
                    case 1 -> blocks.add(new Block(x, y, 40, 20, 2));
                    default -> blocks.add(new Block(x, y, 40, 20, 1));
                }
            }
        }

        timer = new Timer(10, this);
        timer.start();
        lastNanos = System.nanoTime();

        setFocusable(true);
        addKeyListener(this);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        ball.draw(g);
        paddle.draw(g);
        for (Block block : blocks) {
            block.draw(g);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;

        ball.move();

        // Va chạm với mép màn hình
        if (ball.x <= 0 || ball.x + ball.size >= getWidth()) ball.bounceX();
        if (ball.y <= 0) ball.bounceY();

        // Va chạm với paddle
        if (paddle.isHit(ball.x, ball.y, ball.size)) ball.bounceY();

        // Va chạm với block
        for (Block block : blocks) {
            if (block.isHit(ball.x, ball.y, ball.size)) {
                ball.bounceY();
                break;
            }
        }

        // Game Over nếu bóng rơi xuống dưới
        if (ball.y > getHeight()) {
            timer.stop();
            JOptionPane.showMessageDialog(this, "Game Over!");
        }

        paddle.update(leftPressed, rightPressed, getWidth(), dt);

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT)  leftPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT)  leftPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
    }

    @Override public void keyTyped(KeyEvent e) {}
}

/**
 * Quản lý toàn bộ logic game: bóng, paddle, blocks.
 *
 * Xử lý va chạm, di chuyển, vẽ các thành phần.
 *
 * Kiểm tra điều kiện thắng/thua (bóng rơi xuống → thua).
 *
 * Lắng nghe phím điều khiển trái/phải từ người chơi.
 *
 * Vẽ toàn bộ giao diện game trong paintComponent(...).
 */