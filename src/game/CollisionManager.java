package game;

import entities.Ball;
import entities.Block;
import entities.Paddle;
import java.util.List;
import utils.AudioManager;
import utils.GameConfig;

public class CollisionManager {
    private int collisionCooldown = 0; // frames of cooldown

    public void tickCooldown() {
        if (collisionCooldown > 0) collisionCooldown--;
    }

    public void resetCooldown() {
        collisionCooldown = 0;
    }

    public void handleCollisions(Ball ball, Paddle paddle, List<Block> blocks) {
        if (ball == null || paddle == null || blocks == null) return;

        // Paddle collision
        if (paddle.isHit(ball.getX(), ball.getY(), GameConfig.BALL_SIZE)) {
            ball.bounceOffPaddle(paddle.getX(), paddle.getWidth());
            // Prevent multiple hits in a row by nudging the ball above the paddle
            ball.setPosition(ball.getPreciseX(), paddle.getY() - GameConfig.BALL_SIZE - 1);
            collisionCooldown = 2;
            // Play paddle hit sound once (WAV)
            try {
                AudioManager.playOnce("music/padle.wav", null);
            } catch (Throwable ignored) {
                // ignore sound errors
            }
        }

        if (collisionCooldown == 0) {
            for (Block block : blocks) {
                if (block.isHit(ball.getX(), ball.getY(), GameConfig.BALL_SIZE)) {
                    // Determine collision side consistently using previous position
                    double prevX = ball.getPrevX();
                    double prevY = ball.getPrevY();
                    double size = ball.getPrevSize();
                    int bx = block.getX();
                    int by = block.getY();
                    int bw = GameConfig.BLOCK_WIDTH;

                    String collisionSide;
                    if (prevX + size <= bx) {
                        collisionSide = "left";    // came from left
                    } else if (prevX >= bx + bw) {
                        collisionSide = "right";   // came from right
                    } else if (prevY + size <= by) {
                        collisionSide = "top";     // came from top
                    } else {
                        collisionSide = "bottom";  // came from bottom
                    }

                    double ballX = ball.getPreciseX();
                    double ballY = ball.getPreciseY();

                    switch (collisionSide) {
                        case "left" -> {
                            ball.bounceX();
                            int nx = block.getX() - GameConfig.BALL_SIZE - 1; // integer-safe gap
                            int ny = (int) Math.round(ballY);
                            ball.setPosition(nx, ny);
                        }
                        case "right" -> {
                            ball.bounceX();
                            int nx = block.getX() + GameConfig.BLOCK_WIDTH + 1;
                            int ny = (int) Math.round(ballY);
                            ball.setPosition(nx, ny);
                        }
                        case "top" -> {
                            ball.bounceY();
                            int nx = (int) Math.round(ballX);
                            int ny = block.getY() - GameConfig.BALL_SIZE - 1;
                            ball.setPosition(nx, ny);
                        }
                        default -> { // bottom
                            ball.bounceY();
                            int nx = (int) Math.round(ballX);
                            int ny = block.getY() + GameConfig.BLOCK_HEIGHT + 1;
                            ball.setPosition(nx, ny);
                        }
                    }

                    collisionCooldown = 2; // handle 1 collision per frame
                    // Play brick hit sound once
                    try {
                        AudioManager.playOnce("music/brick.wav", null);
                    } catch (Throwable ignored) {
                        // ignore
                    }
                    break;
                }
            }
        }
    }
}
