package game;

import entities.Ball;
import entities.Block;
import entities.Paddle;
import function.Pause;
import utils.GameConfig;

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

    // input state
    private boolean topLeftPressed = false;
    private boolean topRightPressed = false;
    private boolean bottomLeftPressed = false;
    private boolean bottomRightPressed = false;

    private long lastNanos;

    public MultiplayerPanel() {
        setPreferredSize(new Dimension(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT));

        // Center ball
        ball = new Ball(GameConfig.SCREEN_WIDTH / 2, GameConfig.SCREEN_HEIGHT / 2);
        // paddles
        paddleBottom = new Paddle(
                GameConfig.SCREEN_WIDTH / 2 - GameConfig.PADDLE_WIDTH / 2,
                GameConfig.SCREEN_HEIGHT - 60
        );
        paddleTop = new Paddle(
                GameConfig.SCREEN_WIDTH / 2 - GameConfig.PADDLE_WIDTH / 2,
                30
        );

        // create protective rows
        topBlocks = new ArrayList<>();
        bottomBlocks = new ArrayList<>();
        resetBlocks();

        timer = new Timer(GameConfig.TIMER_DELAY, this);
        timer.start();
        lastNanos = System.nanoTime();

        // Pause integration: stop timer when paused, resume when unpaused
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
        setBackground(Color.BLACK);
    }

    private int getPreferredBottomBlockY() {
        return getPreferredSize().height - GameConfig.BLOCK_HEIGHT - 20;
    }

    private void resetBlocks() {
        topBlocks.clear();
        bottomBlocks.clear();
        // distribute across the width using spacing
        int cols = Math.max(1, (GameConfig.SCREEN_WIDTH - 10) / GameConfig.BLOCK_SPACING);
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
        double midX = (getWidth() > 0 ? getWidth() : GameConfig.SCREEN_WIDTH) / 2.0 - GameConfig.PADDLE_WIDTH / 2.0;
        // Using reflection of Paddle API: set by constructing new paddles at same Y
        paddleTop = new Paddle((int) Math.round(midX), paddleTop.getY());
        paddleBottom = new Paddle((int) Math.round(midX), paddleBottom.getY());
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // draw ball, paddles, blocks
        ball.draw(g);
        for (Block b : topBlocks) b.draw(g);
        for (Block b : bottomBlocks) b.draw(g);
        paddleBottom.draw(g);
        paddleTop.draw(g);

        // HUD
        g.setColor(Color.WHITE);
        g.drawString("Top: A/D", 8, 12);
        g.drawString("Bottom: ←/→", getWidth() - 130, getHeight() - 12);
        String scoreTopStr = "Top: " + scoreTop;
        String scoreBottomStr = "Bottom: " + scoreBottom;
        g.drawString(scoreTopStr, getWidth() / 2 - 40, 20);
        g.drawString(scoreBottomStr, getWidth() / 2 - 40, getHeight() - 6);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;

        // update paddles
        paddleTop.update(topLeftPressed, topRightPressed, getWidth(), dt);
        paddleBottom.update(bottomLeftPressed, bottomRightPressed, getWidth(), dt);

        // move ball
        ball.move();

        // bounce off left/right walls
        if (ball.getX() <= 0 || ball.getX() + GameConfig.BALL_SIZE >= getWidth()) {
            ball.bounceX();
        }

        // collisions with top blocks first
        for (Block b : topBlocks) {
            if (b.isHit(ball.getX(), ball.getY(), GameConfig.BALL_SIZE)) {
                if (ball.getVelocity().getDy() < 0) ball.bounceY();
                break;
            }
        }

        // top paddle collision
        if (paddleTop.isHit(ball.getX(), ball.getY(), GameConfig.BALL_SIZE)) {
            if (ball.getVelocity().getDy() < 0) {
                ball.bounceY();
                // place just below the top paddle to avoid sticky collisions
                ball.setPosition(ball.getPreciseX(), paddleTop.getY() + GameConfig.PADDLE_HEIGHT + 1);
            }
        }

        // bottom blocks
        for (Block b : bottomBlocks) {
            if (b.isHit(ball.getX(), ball.getY(), GameConfig.BALL_SIZE)) {
                if (ball.getVelocity().getDy() > 0) ball.bounceY();
                break;
            }
        }

        // bottom paddle collision
        if (paddleBottom.isHit(ball.getX(), ball.getY(), GameConfig.BALL_SIZE)) {
            if (ball.getVelocity().getDy() > 0) {
                ball.bounceY();
                ball.setPosition(ball.getPreciseX(), paddleBottom.getY() - GameConfig.BALL_SIZE - 1);
            }
        }

        // scoring: ball out of top
        if (ball.getY() + GameConfig.BALL_SIZE < 0) {
            scoreBottom++;
            if (scoreBottom >= TARGET_SCORE) {
                timer.stop();
                JOptionPane.showMessageDialog(this, "Bottom player wins " + scoreBottom + " - " + scoreTop + "!");
            } else {
                resetBlocks();
                resetRoundAfterScore(false);
            }
        }

        // scoring: ball out of bottom
        if (ball.getY() > getHeight()) {
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
        if (code == KeyEvent.VK_P) {
            Pause.getInstance().toggle();
            return;
        }
        if (Pause.getInstance().isPaused()) return;

        // top: A/D
        if (code == KeyEvent.VK_A) topLeftPressed = true;
        if (code == KeyEvent.VK_D) topRightPressed = true;

        // bottom: arrows
        if (code == KeyEvent.VK_LEFT) bottomLeftPressed = true;
        if (code == KeyEvent.VK_RIGHT) bottomRightPressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_A) topLeftPressed = false;
        if (code == KeyEvent.VK_D) topRightPressed = false;
        if (code == KeyEvent.VK_LEFT) bottomLeftPressed = false;
        if (code == KeyEvent.VK_RIGHT) bottomRightPressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
