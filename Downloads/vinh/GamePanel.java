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
    private Level level;

    private boolean leftPressed = false;
    private boolean rightPressed = false;

    private long lastNanos;

    public GamePanel() {
        level = new Level();
        initializeLevel();

        timer = new Timer(10, this);
        timer.start();
        lastNanos = System.nanoTime();

        setFocusable(true);
        addKeyListener(this);
    }

    //tạo level
    private void initializeLevel() {
        ball = new Ball(200, 300);
        paddle = new Paddle(150, 550);
        blocks = new ArrayList<>();

        createBlocks(level.getCurrentLevel());
    }

    private void createBlocks(int levelNumber) {
        blocks.clear();
        
        switch (levelNumber) {
            case 1 -> createLevel1();
            case 2 -> createLevel2();
            case 3 -> createLevel3();
        }
    }

    private void createLevel1() {
        // Level 1: 5x8
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
    }

    private void createLevel2() {
        // Level 2: Kiểu tháp tháp
        for (int row = 0; row < 6; row++) {
            int blocksInRow = 8 - row;
            int startX = 50 + (row * 22);
            
            for (int col = 0; col < blocksInRow; col++) {
                int x = startX + col * 45;
                int y = 50 + row * 25;
                
                if (row < 2) {
                    blocks.add(new Block(x, y, 40, 20, 3));
                } else if (row < 4) {
                    blocks.add(new Block(x, y, 40, 20, 2));
                } else {
                    blocks.add(new Block(x, y, 40, 20, 1));
                }
            }
        }
    }

    private void createLevel3() {
        // Level 3:
        int centerX = 200;
        
        for (int row = 0; row < 7; row++) {
            int blocksInRow = row < 4 ? row + 1 : 7 - row;
            int startX = centerX - (blocksInRow * 22);
            
            for (int col = 0; col < blocksInRow; col++) {
                int x = startX + col * 45;
                int y = 50 + row * 25;
                
                // Randomize block strength for added challenge
                int hits = ((row + col) % 3) + 1;
                blocks.add(new Block(x, y, 40, 20, hits));
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        //vẽ level
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Level: " + level.getCurrentLevel(), 10, 25);
        
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

        // Check mép
        ball.checkBounds(getWidth(), getHeight());

        // Va chạm với paddle
        if (paddle.isHit(ball.getX(), ball.getY(), ball.size)) {
            ball.bounceOffPaddle(paddle.x, paddle.width);
        }

        // Va chạm với block
        for (Block block : blocks) {
            if (block.isHit(ball.getX(), ball.getY(), ball.size)) {
                String collisionSide = block.getCollisionSide(
                    ball.getPreciseX(), ball.getPreciseY(), ball.size,
                    ball.getVelocity().getDx(), ball.getVelocity().getDy()
                );
                
                if ("left".equals(collisionSide) || "right".equals(collisionSide)) {
                    ball.bounceX();
                } else {
                    ball.bounceY();
                }
                break;
            }
        }

        // Kiểm tra tất cả blocks đã bị phá chưa
        boolean allBlocksDestroyed = blocks.stream().allMatch(Block::isDestroyed);
        if (allBlocksDestroyed) {
            if (level.isFinalLevel()) {
                timer.stop();
                JOptionPane.showMessageDialog(this, "Bạn đã thắng!!!");
            } else {
                level.advanceLevel();
                timer.stop();
                int choice = JOptionPane.showConfirmDialog(this, 
                    "Level " + (level.getCurrentLevel() - 1) + " hoàn thành, tiếp tục tới level " + level.getCurrentLevel() + "?", 
                    "Thắng level", 
                    JOptionPane.YES_NO_OPTION);
                
                if (choice == JOptionPane.YES_OPTION) {
                    createBlocks(level.getCurrentLevel());
                    ball = new Ball(200, 300);
                    paddle = new Paddle(150, 550);
                    timer.start();
                } else {
                    System.exit(0);
                }
            }
        }

        // Game Over nếu bóng rơi xuống dưới
        if (ball.getY() > getHeight()) {
            timer.stop();
            JOptionPane.showMessageDialog(this, "Game Over! Reached Level: " + level.getCurrentLevel());
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