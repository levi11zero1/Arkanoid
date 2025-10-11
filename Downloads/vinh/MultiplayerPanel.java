import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class MultiplayerPanel extends JPanel implements ActionListener, KeyListener {
    private Ball ball;
    private Paddle paddleTop;
    private Paddle paddleBottom;
    private Timer timer;
    private List<Block> topBlocks;
    private List<Block> bottomBlocks;
    private int scoreTop = 0;
    private int scoreBottom = 0;
    private final int TARGET_SCORE = 5;

    public MultiplayerPanel() {
        setPreferredSize(new Dimension(400, 600));

        ball = new Ball(200, 300);
        paddleBottom = new Paddle(150, 540); // người chơi dưới
        paddleTop = new Paddle(150, 30); // người chơi trên

        // tạo các hàng block bảo vệ phía sau mỗi người chơi
        topBlocks = new ArrayList<>();
        bottomBlocks = new ArrayList<>();
        resetBlocks();

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

    private void resetBlocks() {
        topBlocks.clear();
        bottomBlocks.clear();
        for (int col = 0; col <= 8; col++) {
            int x = col * 45;
            int topY = 5;
            int bottomY = 560;
            topBlocks.add(new Block(x, topY, 40, 20));
            bottomBlocks.add(new Block(x, bottomY, 40, 20));
        }
    }

    private void resetRoundAfterScore(boolean topScores) {
    // đặt bóng vào giữa
        ball.x = getWidth()/2 - ball.size/2;
        ball.y = getHeight()/2 - ball.size/2;
        // gửi bóng về phía người vừa bị thủng lưới (người ghi điểm sẽ giao)
        if (topScores) {
            ball.dy = Math.abs(ball.dy); // send downwards
        } else {
            ball.dy = -Math.abs(ball.dy); // send upwards
        }
    // đặt lại vị trí paddle về giữa
        paddleTop.x = getWidth()/2 - paddleTop.width/2;
        paddleBottom.x = getWidth()/2 - paddleBottom.width/2;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // nền
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        // vẽ bóng, thanh đỡ và các block bảo vệ
        ball.draw(g);
        for (Block b : topBlocks) b.draw(g);
        for (Block b : bottomBlocks) b.draw(g);
        paddleBottom.draw(g);
        paddleTop.draw(g);

        // vẽ HUD (thông tin giao diện)
        g.setColor(Color.WHITE);
        g.drawString("Top: A/D", 8, 12);
        g.drawString("Bottom: ←/→", 300, 588);
        // vẽ điểm số
        String scoreTopStr = "Top: " + scoreTop;
        String scoreBottomStr = "Bottom: " + scoreBottom;
        g.drawString(scoreTopStr, getWidth()/2 - 40, 20);
        g.drawString(scoreBottomStr, getWidth()/2 - 40, getHeight() - 6);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
        ball.move();

        // va chạm với tường ngang (đổi hướng X)
        if (ball.x <= 0 || ball.x + ball.size >= getWidth()) ball.bounceX();

        // kiểm tra va chạm với các block bảo vệ phía trên trước
        for (Block b : topBlocks) {
            if (b.isHit(ball.x, ball.y, ball.size)) {
                if (ball.dy < 0) ball.bounceY();
                break;
            }
        }

        // kiểm tra va chạm với thanh đỡ trên
        if (paddleTop.isHit(ball.x, ball.y, ball.size)) {
            if (ball.dy < 0) ball.bounceY();
        }

        // kiểm tra va chạm với các block bảo vệ phía dưới trước
        for (Block b : bottomBlocks) {
            if (b.isHit(ball.x, ball.y, ball.size)) {
                if (ball.dy > 0) ball.bounceY();
                break;
            }
        }

        // kiểm tra va chạm với thanh đỡ dưới
        if (paddleBottom.isHit(ball.x, ball.y, ball.size)) {
            if (ball.dy > 0) ball.bounceY();
        }

        // kiểm tra bóng đi ra ngoài qua phía trên (bị thủng lưới ở trên)
        if (ball.y + ball.size < 0) {
            // người chơi dưới ghi điểm
            scoreBottom++;
            if (scoreBottom >= TARGET_SCORE) {
                timer.stop();
                JOptionPane.showMessageDialog(this, "Bottom player wins " + scoreBottom + " - " + scoreTop + "!");
            } else {
                // đặt lại block và bóng cho ván tiếp theo
                resetBlocks();
                resetRoundAfterScore(false);
            }
        }

        // kiểm tra bóng đi ra ngoài qua phía dưới (bị thủng lưới ở dưới)
        if (ball.y > getHeight()) {
            // người chơi trên ghi điểm
            scoreTop++;
            if (scoreTop >= TARGET_SCORE) {
                timer.stop();
                JOptionPane.showMessageDialog(this, "Top player wins " + scoreTop + " - " + scoreBottom + "!");
            } else {
                resetBlocks();
                resetRoundAfterScore(true);
            }
        }

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        // chuyển trạng thái tạm dừng
        if (code == KeyEvent.VK_P) {
            Pause.getInstance().toggle();
        }

        // khi đang tạm dừng, bỏ qua phím di chuyển
        if (Pause.getInstance().isPaused()) return;

        // người chơi trên: A (trái), D (phải)
        if (code == KeyEvent.VK_A) paddleTop.moveLeft();
        if (code == KeyEvent.VK_D) paddleTop.moveRight(getWidth());

        // người chơi dưới: phím mũi tên
        if (code == KeyEvent.VK_LEFT) paddleBottom.moveLeft();
        if (code == KeyEvent.VK_RIGHT) paddleBottom.moveRight(getWidth());

        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}
