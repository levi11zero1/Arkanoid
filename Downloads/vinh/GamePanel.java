import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import NTT.Pause;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Ball ball;
    private Paddle paddle;
    private List<Block> blocks;
    private Timer timer;

    public GamePanel() {
        ball = new Ball(200, 300);
        paddle = new Paddle(150, 550);
        blocks = new ArrayList<>();

        // Tạo lưới block: 5 hàng × 8 cột
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 8; col++) {
                int x = 50 + col * 45;
                int y = 50 + row * 25;
                blocks.add(new Block(x, y, 40, 20));
            }
        }

        timer = new Timer(10, this);
        timer.start();

    // Kết nối pause với Swing timer: dừng timer khi tạm dừng, khởi động lại khi tiếp tục
        Pause.getInstance().setListener(new Pause.PauseListener() {
            @Override
            public void onPause() {
                timer.stop();
            }

            @Override
            public void onResume() {
                timer.start();
            }
        });
        

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
        ball.move();

    // Va chạm với mép màn hình
        if (ball.x <= 0 || ball.x + ball.size >= getWidth()) ball.bounceX();
        if (ball.y <= 0) ball.bounceY();

    // Va chạm với paddle
        if (paddle.isHit(ball.x, ball.y, ball.size)) ball.bounceY();

    // Va chạm với viên gạch (block)
        for (Block block : blocks) {
            if (block.isHit(ball.x, ball.y, ball.size)) {
                ball.bounceY();
                break; // tránh va chạm nhiều viên gạch cùng lúc
            }
        }

    // Game Over nếu bóng rơi xuống dưới
        if (ball.y > getHeight()) {
            timer.stop();
            JOptionPane.showMessageDialog(this, "Game Over!");
        }

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
    //chuyển trạng thái tạm dừng
        if (code == KeyEvent.VK_P) {
            Pause.getInstance().toggle();
        }

    // khi đang tạm dừng, bỏ qua phím di chuyển
        if (Pause.getInstance().isPaused()) return;

        if (code == KeyEvent.VK_LEFT) paddle.moveLeft();
        if (code == KeyEvent.VK_RIGHT) paddle.moveRight(getWidth());

    }

    @Override public void keyReleased(KeyEvent e) {}
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