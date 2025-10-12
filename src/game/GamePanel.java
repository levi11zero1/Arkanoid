package game;

import entities.Ball;
import entities.Block;
import entities.Paddle;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import levels.LevelBuilder;
import levels.LevelManager;
import utils.GameConfig;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Ball ball;
    private Paddle paddle;
    private List<Block> blocks;
    private Timer gameTimer;
    private LevelManager levelManager;
    private GameEvents eventsListener;

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private long lastNanos;
    private int collisionCooldown = 0; // Tránh nhiều va chạm trong 1 frame


    public GamePanel() {
        levelManager = new LevelManager();
        initializeLevel();

        gameTimer = new Timer(GameConfig.TIMER_DELAY, this);
        gameTimer.start();
        lastNanos = System.nanoTime();

        setFocusable(true);
        addKeyListener(this);
        setBackground(Color.BLACK);
    }


    private void initializeLevel() {
        ball = new Ball(GameConfig.SCREEN_WIDTH / 2, GameConfig.SCREEN_HEIGHT / 2);
        paddle = new Paddle(
            GameConfig.SCREEN_WIDTH / 2 - GameConfig.PADDLE_WIDTH / 2, 
            GameConfig.SCREEN_HEIGHT - 100
        );
        
        // Tạo block
        blocks = LevelBuilder.createLevel(levelManager.getCurrentLevel());
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (g instanceof Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Level: " + levelManager.getCurrentLevel(), 10, 25);
        
        long remainingBlocks = blocks.stream().filter(block -> !block.isDestroyed()).count();
        g.drawString("Blocks: " + remainingBlocks, GameConfig.SCREEN_WIDTH - 100, 25);
        
        ball.draw(g);
        paddle.draw(g);
        for (Block block : blocks) {
            block.draw(g);
        }
    }

    // Game loop
    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double deltaTime = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;

        updateGame(deltaTime);
        handleCollisions();
        checkGameState();
        repaint();
    }
    
    private void updateGame(double deltaTime) {
        ball.move();
        ball.checkBounds(getWidth(), getHeight());
        paddle.update(leftPressed, rightPressed, getWidth(), deltaTime);
        
        if (collisionCooldown > 0) {
            collisionCooldown--;
        }
    }
    
    private void handleCollisions() {
        // Paddle collision
        if (paddle.isHit(ball.getX(), ball.getY(), GameConfig.BALL_SIZE)) {
            ball.bounceOffPaddle(paddle.getX(), paddle.getWidth());
            // Tránh nhiều va chạm
            ball.setPosition(ball.getPreciseX(), paddle.getY() - GameConfig.BALL_SIZE - 1);
            collisionCooldown = 2; // Set cooldown va chạm
        }

        if (collisionCooldown == 0) {
            for (Block block : blocks) {
                if (block.isHit(ball.getX(), ball.getY(), GameConfig.BALL_SIZE)) {
                    String collisionSide = block.getCollisionSide(
                        ball.getPreciseX(), ball.getPreciseY(), GameConfig.BALL_SIZE,
                        ball.getVelocity().getDx(), ball.getVelocity().getDy()
                    );
                    
                    double ballX = ball.getPreciseX();
                    double ballY = ball.getPreciseY();
                    
                    if ("left".equals(collisionSide)) {
                        ball.bounceX();
                        ball.setPosition(block.getX() - GameConfig.BALL_SIZE - 1, ballY);
                    } else if ("right".equals(collisionSide)) {
                        ball.bounceX();
                        ball.setPosition(block.getX() + GameConfig.BLOCK_WIDTH + 1, ballY);
                    } else if ("top".equals(collisionSide)) {
                        ball.bounceY();
                        ball.setPosition(ballX, block.getY() - GameConfig.BALL_SIZE - 1);
                    } else { // bottom
                        ball.bounceY();
                        ball.setPosition(ballX, block.getY() + GameConfig.BLOCK_HEIGHT + 1);
                    }
                    
                    collisionCooldown = 2; 
                    break; // Xử lý 1 va chạm/frame
                }
            }
        }
    }
    
    private void checkGameState() {
        boolean allBlocksDestroyed = blocks.stream().allMatch(Block::isDestroyed);
        if (allBlocksDestroyed) {
            handleLevelComplete();
        }

        if (ball.getY() > getHeight()) {
            handleGameOver();
        }
    }
    
    private void handleLevelComplete() {
        if (levelManager.isFinalLevel()) {
            gameTimer.stop();
            showGameComplete();
        } else {
            gameTimer.stop();
            showLevelComplete();
        }
    }
    
    private void handleGameOver() {
        gameTimer.stop();
        
        // If event listener is set (menu integration), notify it
        if (eventsListener != null) {
            eventsListener.onGameOver();
            return;
        }
        
        // Otherwise, show default dialog
        int choice = JOptionPane.showConfirmDialog(
            this, 
            "Game Over! You reached Level " + levelManager.getCurrentLevel() + 
            "\n\nWould you like to play again?", 
            "Game Over", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (choice == JOptionPane.YES_OPTION) {
            restartGame();
        } else {
            System.exit(0);
        }
    }
    
    private void showLevelComplete() {
        int choice = JOptionPane.showConfirmDialog(
            this, 
            "Level " + levelManager.getCurrentLevel() + " Complete!\n\n" +
            "Continue to Level " + (levelManager.getCurrentLevel() + 1) + "?", 
            "Level Complete", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (choice == JOptionPane.YES_OPTION) {
            levelManager.advanceLevel();
            initializeLevel();
            gameTimer.start();
        } else {
            System.exit(0);
        }
    }
    
    private void showGameComplete() {
        int choice = JOptionPane.showConfirmDialog(
            this, 
            "Congratulations! You completed all " + levelManager.getMaxLevels() + 
            " levels!\n\nWould you like to play again?", 
            "Game Complete", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (choice == JOptionPane.YES_OPTION) {
            restartGame();
        } else {
            System.exit(0);
        }
    }
    
    private void restartGame() {
        levelManager.reset();
        initializeLevel();
        gameTimer.start();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = true;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = true;
            case KeyEvent.VK_SPACE -> {
                if (!gameTimer.isRunning()) {
                    gameTimer.start();
                }
            }
            case KeyEvent.VK_ESCAPE -> {
                int choice = JOptionPane.showConfirmDialog(
                    this, 
                    "Are you sure you want to quit?", 
                    "Quit Game", 
                    JOptionPane.YES_NO_OPTION
                );
                if (choice == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = false;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = false;
        }
    }

    @Override 
    public void keyTyped(KeyEvent e) {
    }
    
    // Cho phép ArkanoidGame đăng ký lắng nghe sự kiện trong game
    public void setEventsListener(GameEvents listener) {
        this.eventsListener = listener;
    }

    public interface GameEvents {
        void onGameOver();
    }
}