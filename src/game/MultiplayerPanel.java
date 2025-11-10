package game;

import entities.Ball;
import entities.Block;
import entities.Paddle;
import function.Pause;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import utils.GameConfig;
import utils.AudioManager;

/**
 * MultiplayerPanel cho chế độ chơi đôi người chơi.
 */
public class MultiplayerPanel extends JPanel implements ActionListener, KeyListener {
    private static final long serialVersionUID = 1L;
    private Ball ball;
    private Paddle paddleTop;
    private Paddle paddleBottom;
    private IGameLoop gameLoop;
    private List<Block> topBlocks;
    private List<Block> bottomBlocks;
    private int scoreTop = 0;
    private int scoreBottom = 0;
    private final int TARGET_SCORE = 5;
    private final CollisionManager topCollisionManager = new CollisionManager();
    private final CollisionManager bottomCollisionManager = new CollisionManager();

    private boolean topLeftPressed = false;
    private boolean topRightPressed = false;
    private boolean bottomLeftPressed = false;
    private boolean bottomRightPressed = false;

    public MultiplayerPanel() {
        setPreferredSize(new Dimension(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT));
        ball = new Ball(GameConfig.SCREEN_WIDTH / 2, GameConfig.SCREEN_HEIGHT / 2);
        paddleBottom = new Paddle(
                GameConfig.SCREEN_WIDTH / 2 - GameConfig.DEFAULT_PADDLE_WIDTH / 2,
                GameConfig.SCREEN_HEIGHT - 80,
                false);
        paddleTop = new Paddle(
                GameConfig.SCREEN_WIDTH / 2 - GameConfig.DEFAULT_PADDLE_WIDTH / 2,
                30,
                false);

        topBlocks = new ArrayList<>();
        bottomBlocks = new ArrayList<>();
        resetBlocks();

        gameLoop = new GameLoop();
        gameLoop.setTargetFps(60);
        gameLoop.setTickListener(dt -> {
            try {
                updateLogic(dt);
            } catch (Throwable ignored) {}
        });
        gameLoop.start();

        Pause.getInstance().setListener(new Pause.PauseListener() {
            @Override
            public void onPause() {
                if (gameLoop != null) gameLoop.pause();
            }

            @Override
            public void onResume() {
                if (gameLoop != null) gameLoop.resume();
            }
        });

        setFocusable(true);
        addKeyListener(this);
        setBackground(Color.BLACK);
    }

    private int getPreferredBottomBlockY() {
        return getPreferredSize().height - 3 * GameConfig.BLOCK_HEIGHT ;
    }

    private void resetBlocks() {
        topBlocks.clear();
        bottomBlocks.clear();
        int cols = Math.max(1, GameConfig.SCREEN_WIDTH/ GameConfig.BLOCK_SPACING);
        for (int col = 0; col < cols; col++) {
            int x = col * GameConfig.BLOCK_SPACING;
            int topY = 5;
            int bottomY = getPreferredBottomBlockY();
            topBlocks.add(new Block(x, topY, 1));
            bottomBlocks.add(new Block(x, bottomY, 1));
        }
    }

    private void resetRoundAfterScore(boolean topScores) {
        // place ball at center
        int cx = getWidth() > 0 ? getWidth() / 2 : GameConfig.SCREEN_WIDTH / 2;
        int cy = getHeight() > 0 ? getHeight() / 2 : GameConfig.SCREEN_HEIGHT / 2;
        ball.setPosition(cx - GameConfig.BALL_SIZE / 2.0, cy - GameConfig.BALL_SIZE / 2.0);

        // serve toward the player who conceded
        double speed = ball.getVelocity().getMagnitude();
        if (topScores) {
            // send downwards
            ball.setVelocity(utils.Velocity.fromAngle(90, Math.max(speed, GameConfig.BALL_DEFAULT_SPEED)));
        } else {
            // send upwards
            ball.setVelocity(utils.Velocity.fromAngle(-90, Math.max(speed, GameConfig.BALL_DEFAULT_SPEED)));
        }

        // center paddles
        double midX = (getWidth() > 0 ? getWidth() : GameConfig.SCREEN_WIDTH) / 2.0
                - GameConfig.DEFAULT_PADDLE_WIDTH / 2.0;
        paddleTop = new Paddle(midX, paddleTop.getY(), false);
        paddleBottom = new Paddle(midX, paddleBottom.getY(), false);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        ball.draw(g);
        for (Block b : topBlocks)
            b.draw(g);
        for (Block b : bottomBlocks)
            b.draw(g);
        paddleBottom.draw(g);
        paddleTop.draw(g);

        g.setColor(Color.WHITE);
        g.drawString("Top: A/D", 8, 12);
        g.drawString("Bottom: ←/→", getWidth() - 130, getHeight() - 12);
        String scoreTopStr = "Top: " + scoreTop;
        String scoreBottomStr = "Bottom: " + scoreBottom;
        g.drawString(scoreTopStr, getWidth() / 2 - 40, 20);
        g.drawString(scoreBottomStr, getWidth() / 2 - 40, getHeight() - 6);
    }

    /**
     * Cập nhật toàn bộ logic khung hình.
     * @param dt thời gian trôi qua giữa 2 lần tick (giây). 
     */
    private void updateLogic(double dt) {
        // 1) Cập nhật paddle theo phím nhấn
        paddleTop.update(topLeftPressed, topRightPressed, getWidth(), dt);
        paddleBottom.update(bottomLeftPressed, bottomRightPressed, getWidth(), dt);

        // 2) Di chuyển bóng
        ball.move();

        // 3) Va chạm tường trái/phải
        if (ball.getX() <= 0 || ball.getX() + GameConfig.BALL_SIZE >= getWidth()) {
            ball.bounceX();
            try { AudioManager.playOnce("music/wall.wav", null); } catch (Throwable ignored) {}
        }

        // 4) Va chạm paddle và block (đã tập trung trong CollisionManager)
        topCollisionManager.tickCooldown();
        bottomCollisionManager.tickCooldown();
        topCollisionManager.handleCollisions(ball, paddleTop, topBlocks, true);
        bottomCollisionManager.handleCollisions(ball, paddleBottom, bottomBlocks, false);

        // 5) Ghi điểm khi bóng lọt qua biên
        if (ball.getY() + GameConfig.BALL_SIZE < 0) {
            scoreBottom++;
            if (scoreBottom >= TARGET_SCORE) {
                if (gameLoop != null) gameLoop.stop();
                JOptionPane.showMessageDialog(this, "Bottom player wins " + scoreBottom + " - " + scoreTop + "!");
            } else {
                resetBlocks();
                resetRoundAfterScore(false);
            }
        }

        if (ball.getY() > getHeight()) {
            scoreTop++;
            if (scoreTop >= TARGET_SCORE) {
                if (gameLoop != null) gameLoop.stop();
                JOptionPane.showMessageDialog(this, "Top player wins " + scoreTop + " - " + scoreBottom + "!");
            } else {
                resetBlocks();
                resetRoundAfterScore(true);
            }
        }

        // 6) Yêu cầu vẽ lại trên EDT
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }


    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_P) {
            Pause.getInstance().toggle();
            return;
        }
        if (Pause.getInstance().isPaused())
            return;


        if (code == KeyEvent.VK_A)
            topLeftPressed = true;
        if (code == KeyEvent.VK_D)
            topRightPressed = true;

        if (code == KeyEvent.VK_LEFT)
            bottomLeftPressed = true;
        if (code == KeyEvent.VK_RIGHT)
            bottomRightPressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_A)
            topLeftPressed = false;
        if (code == KeyEvent.VK_D)
            topRightPressed = false;
        if (code == KeyEvent.VK_LEFT)
            bottomLeftPressed = false;
        if (code == KeyEvent.VK_RIGHT)
            bottomRightPressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}
