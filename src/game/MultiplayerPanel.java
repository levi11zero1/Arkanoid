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
        GameConfig.SCREEN_WIDTH / 2 - GameConfig.DEFAULT_PADDLE_WIDTH / 2,
        GameConfig.SCREEN_HEIGHT - 60,
        false
    );
    paddleTop = new Paddle(
        GameConfig.SCREEN_WIDTH / 2 - GameConfig.DEFAULT_PADDLE_WIDTH / 2,
        30,
        false
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
    double midX = (getWidth() > 0 ? getWidth() : GameConfig.SCREEN_WIDTH) / 2.0 - GameConfig.DEFAULT_PADDLE_WIDTH / 2.0;
    paddleTop = new Paddle(midX, paddleTop.getY(), false);
    paddleBottom = new Paddle(midX, paddleBottom.getY(), false);
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
            try {
                AudioManager.playOnce("music/wall.wav", null);
            } catch (Throwable ignored) {}
        }

        // collisions with top blocks first (overlap -> applyHit; fallback to CCD)
        boolean topHit = false;
        for (Block b : topBlocks) {
            if (!b.isDestroyed() &&
                ball.getX() + GameConfig.BALL_SIZE > b.getX() && ball.getX() < b.getX() + GameConfig.BLOCK_WIDTH &&
                ball.getY() + GameConfig.BALL_SIZE > b.getY() && ball.getY() < b.getY() + GameConfig.BLOCK_HEIGHT) {
                b.applyHit();
                if (ball.getVelocity().getDy() < 0) {
                    ball.bounceY();
                    // push out just below the block to avoid sticking
                    ball.setPosition(ball.getPreciseX(), b.getY() + GameConfig.BLOCK_HEIGHT + 0.01);
                }
                topHit = true;
                // play brick sound for overlap hit
                try { AudioManager.playOnce("music/brick.wav", null); } catch (Throwable ignored) {}
                break;
            }
        }
        if (!topHit && ball.getVelocity().getDy() < 0) {
            // swept AABB for top blocks
            Block hitBlock = null;
            double bestT = Double.POSITIVE_INFINITY;
            boolean hitVertical = false;
            double radius = GameConfig.BALL_SIZE / 2.0;
            double prevCenterX = ball.getPrevX() + GameConfig.BALL_SIZE / 2.0;
            double prevCenterY = ball.getPrevY() + GameConfig.BALL_SIZE / 2.0;
            double curCenterX = ball.getPreciseX() + GameConfig.BALL_SIZE / 2.0;
            double curCenterY = ball.getPreciseY() + GameConfig.BALL_SIZE / 2.0;
            double dx = curCenterX - prevCenterX;
            double dy = curCenterY - prevCenterY;
            if (dx != 0 || dy != 0) {
                for (Block b : topBlocks) {
                    if (b.isDestroyed()) continue;
                    double rx1 = b.getX() - radius;
                    double ry1 = b.getY() - radius;
                    double rx2 = b.getX() + GameConfig.BLOCK_WIDTH + radius;
                    double ry2 = b.getY() + GameConfig.BLOCK_HEIGHT + radius;
                    SweepResult r = sweptSegmentAABB(prevCenterX, prevCenterY, dx, dy, rx1, ry1, rx2, ry2);
                    if (r.hit && r.tEnter >= 0 && r.tEnter <= 1.0) {
                        if (r.tEnter < bestT) {
                            bestT = r.tEnter;
                            hitBlock = b;
                            hitVertical = r.hitVertical;
                        }
                    }
                }
            }
                if (hitBlock != null) {
                double t = Math.max(0.0, Math.min(1.0, bestT));
                double hitCenterX = prevCenterX + dx * t;
                double hitCenterY = prevCenterY + dy * t;
                double newX = hitCenterX - radius;
                double newY = hitCenterY - radius;
                double eps = 0.01;
                if (hitVertical) {
                    ball.setPosition(newX, newY + (dy > 0 ? -eps : eps));
                    ball.bounceY();
                } else {
                    ball.setPosition(newX + (dx > 0 ? -eps : eps), newY);
                    ball.bounceX();
                }
                hitBlock.applyHit();
                try { AudioManager.playOnce("music/brick.wav", null); } catch (Throwable ignored) {}
            }
        }

        // top paddle collision
        if (paddleTop.isHit(ball.getX(), ball.getY(), GameConfig.BALL_SIZE)) {
            if (ball.getVelocity().getDy() < 0) {
                ball.bounceY();
                // place just below the top paddle to avoid sticky collisions
                ball.setPosition(ball.getPreciseX(), paddleTop.getY() + GameConfig.PADDLE_HEIGHT + 1);
                try { AudioManager.playOnce("music/padle.wav", null); } catch (Throwable ignored) {}
            }
        }

        // bottom blocks (overlap -> applyHit; fallback to CCD)
        boolean bottomHit = false;
        for (Block b : bottomBlocks) {
            if (!b.isDestroyed() &&
                ball.getX() + GameConfig.BALL_SIZE > b.getX() && ball.getX() < b.getX() + GameConfig.BLOCK_WIDTH &&
                ball.getY() + GameConfig.BALL_SIZE > b.getY() && ball.getY() < b.getY() + GameConfig.BLOCK_HEIGHT) {
                b.applyHit();
                if (ball.getVelocity().getDy() > 0) {
                    ball.bounceY();
                    // push out just above the block
                    ball.setPosition(ball.getPreciseX(), b.getY() - GameConfig.BALL_SIZE - 0.01);
                }
                bottomHit = true;
                    // play brick sound for overlap hit
                    try { AudioManager.playOnce("music/brick.wav", null); } catch (Throwable ignored) {}
                break;
            }
        }
        if (!bottomHit && ball.getVelocity().getDy() > 0) {
            Block hitBlock = null;
            double bestT = Double.POSITIVE_INFINITY;
            boolean hitVertical = false;
            double radius = GameConfig.BALL_SIZE / 2.0;
            double prevCenterX = ball.getPrevX() + GameConfig.BALL_SIZE / 2.0;
            double prevCenterY = ball.getPrevY() + GameConfig.BALL_SIZE / 2.0;
            double curCenterX = ball.getPreciseX() + GameConfig.BALL_SIZE / 2.0;
            double curCenterY = ball.getPreciseY() + GameConfig.BALL_SIZE / 2.0;
            double dx = curCenterX - prevCenterX;
            double dy = curCenterY - prevCenterY;
            if (dx != 0 || dy != 0) {
                for (Block b : bottomBlocks) {
                    if (b.isDestroyed()) continue;
                    double rx1 = b.getX() - radius;
                    double ry1 = b.getY() - radius;
                    double rx2 = b.getX() + GameConfig.BLOCK_WIDTH + radius;
                    double ry2 = b.getY() + GameConfig.BLOCK_HEIGHT + radius;
                    SweepResult r = sweptSegmentAABB(prevCenterX, prevCenterY, dx, dy, rx1, ry1, rx2, ry2);
                    if (r.hit && r.tEnter >= 0 && r.tEnter <= 1.0) {
                        if (r.tEnter < bestT) {
                            bestT = r.tEnter;
                            hitBlock = b;
                            hitVertical = r.hitVertical;
                        }
                    }
                }
            }
            if (hitBlock != null) {
                double t = Math.max(0.0, Math.min(1.0, bestT));
                double hitCenterX = prevCenterX + dx * t;
                double hitCenterY = prevCenterY + dy * t;
                double newX = hitCenterX - radius;
                double newY = hitCenterY - radius;
                double eps = 0.01;
                if (hitVertical) {
                    ball.setPosition(newX, newY + (dy > 0 ? -eps : eps));
                    ball.bounceY();
                } else {
                    ball.setPosition(newX + (dx > 0 ? -eps : eps), newY);
                    ball.bounceX();
                }
                hitBlock.applyHit();
                try { AudioManager.playOnce("music/brick.wav", null); } catch (Throwable ignored) {}
            }
        }

        // bottom paddle collision
        if (paddleBottom.isHit(ball.getX(), ball.getY(), GameConfig.BALL_SIZE)) {
            if (ball.getVelocity().getDy() > 0) {
                ball.bounceY();
                ball.setPosition(ball.getPreciseX(), paddleBottom.getY() - GameConfig.BALL_SIZE - 1);
                try { AudioManager.playOnce("music/padle.wav", null); } catch (Throwable ignored) {}
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

    // Sweep structures for CCD
    private static class SweepResult {
        boolean hit;
        double tEnter;
        boolean hitVertical;
    }

    private static SweepResult sweptSegmentAABB(double px, double py, double dx, double dy,
                                                double rx1, double ry1, double rx2, double ry2) {
        SweepResult res = new SweepResult();
        double t0 = 0.0, t1 = 1.0;
        boolean xEnter = false, yEnter = false;

        if (dx == 0) {
            if (px < rx1 || px > rx2) return res;
        } else {
            double tx1 = (rx1 - px) / dx;
            double tx2 = (rx2 - px) / dx;
            double txEnter = Math.min(tx1, tx2);
            double txExit = Math.max(tx1, tx2);
            if (txEnter > t0) { t0 = txEnter; xEnter = true; }
            if (txExit < t1) { t1 = txExit; }
            if (t0 > t1) return res;
        }

        if (dy == 0) {
            if (py < ry1 || py > ry2) return res;
        } else {
            double ty1 = (ry1 - py) / dy;
            double ty2 = (ry2 - py) / dy;
            double tyEnter = Math.min(ty1, ty2);
            double tyExit = Math.max(ty1, ty2);
            if (tyEnter > t0) { t0 = tyEnter; xEnter = false; yEnter = true; }
            if (tyExit < t1) { t1 = tyExit; }
            if (t0 > t1) return res;
        }

        res.hit = (t0 >= 0 && t0 <= 1.0);
        res.tEnter = t0;
        res.hitVertical = yEnter && !xEnter ? true : (!yEnter && xEnter ? false : Math.abs(dy) > Math.abs(dx));
        return res;
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
