package game;

import entities.Ball;
import entities.Block;
import entities.Paddle;
import java.util.List;
import utils.GameConfig;

/**
 * Handles collision detection and response between the ball, paddle and blocks.
 * Keeps an internal small cooldown to avoid multiple bounces in a single frame.
 */
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

                    // If the block just got destroyed (side may be null), infer side from previous position
                    if (collisionSide == null) {
                        double prevX = ball.getPrevX();
                        double prevY = ball.getPrevY();
                        double size = ball.getPrevSize();
                        int bx = block.getX();
                        int by = block.getY();
                        int bw = GameConfig.BLOCK_WIDTH;

                        if (prevX + size <= bx) {
                            collisionSide = "left";    // came from left
                        } else if (prevX >= bx + bw) {
                            collisionSide = "right";   // came from right
                        } else if (prevY + size <= by) {
                            collisionSide = "top";     // came from top
                        } else {
                            collisionSide = "bottom";  // came from bottom
                        }
                    }

                    switch (collisionSide) {
                        case "left" -> {
                            ball.bounceX();
                            ball.setPosition(block.getX() - GameConfig.BALL_SIZE - 0.01, ballY);
                        }
                        case "right" -> {
                            ball.bounceX();
                            ball.setPosition(block.getX() + GameConfig.BLOCK_WIDTH + 0.01, ballY);
                        }
                        case "top" -> {
                            ball.bounceY();
                            ball.setPosition(ballX, block.getY() - GameConfig.BALL_SIZE - 0.01);
                        }
                        default -> { // bottom
                            ball.bounceY();
                            ball.setPosition(ballX, block.getY() + GameConfig.BLOCK_HEIGHT + 0.01);
                        }
                    }

                    collisionCooldown = 2; // handle 1 collision per frame
                    break;
                }
            }
        }
    }
}
