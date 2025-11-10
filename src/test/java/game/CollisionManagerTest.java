package game;

import entities.Ball;
import entities.Block;
import entities.Paddle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.GameConfig;
import game.CollisionManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CollisionManagerTest {

    private CollisionManager cmTop;
    private CollisionManager cmBottom;

    @BeforeEach
    void setup() {
        cmTop = new CollisionManager();
        cmBottom = new CollisionManager();
        // reset a few critical configs if needed (assumes defaults in code)
        GameConfig.BALL_SIZE = GameConfig.DEFAULT_BALL_SIZE;
    }

    @Test
    void bottomPaddleBounceUp() {
        Ball ball = new Ball(100, 200);
        // moving down
        ball.setVelocity(new utils.Velocity(0, 5));
        Paddle paddle = new Paddle(90, 210); // paddle below ball
        List<Block> blocks = new ArrayList<>();

        // place ball overlapping paddle area (y+size > paddle.y)
        ball.setPosition(100, paddle.getY() - GameConfig.BALL_SIZE + 2);
        cmBottom.handleCollisions(ball, paddle, blocks, false);

        assertTrue(ball.getVelocity().getDy() < 0, "Ball should bounce upward on bottom paddle");
        assertEquals(paddle.getY() - GameConfig.BALL_SIZE - 1, ball.getY(),
                "Ball should be positioned just above paddle after collision");
    }

    @Test
    void topPaddleBounceDown() {
        Ball ball = new Ball(100, 60);
        ball.setVelocity(new utils.Velocity(0, -5)); // moving up
        Paddle top = new Paddle(90, 30, false); // y small
        List<Block> blocks = new ArrayList<>();

        // overlap with top paddle area (ball intersects paddle rect)
        ball.setPosition(100, top.getY() + top.getHeight() - 2);
        cmTop.handleCollisions(ball, top, blocks, true);

        assertTrue(ball.getVelocity().getDy() > 0, "Ball should bounce downward on top paddle");
        assertEquals(top.getY() + top.getHeight() + 1, ball.getY(),
                "Ball should be positioned just below top paddle after collision");
    }

    @Test
    void blockCollisionDamagesAndBounces() {
        Ball ball = new Ball(50, 50);
        ball.setVelocity(new utils.Velocity(5, 0)); // moving right
        Block block = new Block(70, 50, 1);
        List<Block> blocks = new ArrayList<>();
        blocks.add(block);

        // simulate previous position by making one move step to set prevX/prevY
    ball.setPosition(40, 50);
        ball.move(); // now at 65,50 if speed was applied; ensure overlap
        ball.setPosition(69, 50); // force near collision

        double beforeDx = ball.getVelocity().getDx();
        double beforeDy = ball.getVelocity().getDy();
        cmBottom.handleCollisions(ball, new Paddle(0, 9999), blocks, false);

        assertTrue(block.isDestroyed(), "Block should be destroyed after hit (hits=1)");
        // velocity should change on at least one axis
        boolean velocityChanged = (Math.abs(ball.getVelocity().getDx() - beforeDx) > 1e-9)
                || (Math.abs(ball.getVelocity().getDy() - beforeDy) > 1e-9);
        assertTrue(velocityChanged, "Ball velocity should change after hitting block");
    }

    @Test
    void cooldownPreventsSecondBlockHitInSameFrame() {
        Ball ball = new Ball(100, 50);
        ball.setVelocity(new utils.Velocity(5, 0));
        Block b1 = new Block(110, 50, 1);
        Block b2 = new Block(120, 50, 1);
        List<Block> blocks = new ArrayList<>();
        blocks.add(b1);
        blocks.add(b2);

        // place overlapping first block
        ball.setPosition(109, 50);
        cmBottom.handleCollisions(ball, new Paddle(0, 9999), blocks, false);
        // try again same frame without ticking cooldown
        cmBottom.handleCollisions(ball, new Paddle(0, 9999), blocks, false);

        int destroyedCount = (b1.isDestroyed() ? 1 : 0) + (b2.isDestroyed() ? 1 : 0);
        assertEquals(1, destroyedCount, "Only one block should be processed per frame due to cooldown");

        // after ticking cooldown, allow another collision
        cmBottom.tickCooldown();
        cmBottom.tickCooldown();
        // reposition to overlap second block
        ball.setPosition(119, 50);
        cmBottom.handleCollisions(ball, new Paddle(0, 9999), blocks, false);
        destroyedCount = (b1.isDestroyed() ? 1 : 0) + (b2.isDestroyed() ? 1 : 0);
        assertEquals(2, destroyedCount, "Second block can be hit after cooldown");
    }

    @Test
    void noCollisionKeepsVelocity() {
        Ball ball = new Ball(10, 10);
        ball.setVelocity(new utils.Velocity(3, 4));
        Paddle paddle = new Paddle(300, 300);
        List<Block> blocks = new ArrayList<>();

        cmBottom.handleCollisions(ball, paddle, blocks, false);
        assertEquals(3.0, ball.getVelocity().getDx(), 1e-6);
        assertEquals(4.0, ball.getVelocity().getDy(), 1e-6);
    }
}
