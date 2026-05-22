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
        GameConfig.BALL_SIZE = GameConfig.DEFAULT_BALL_SIZE;
    }

    @Test
    void bottomPaddleBounceUp() {
        Ball ball = new Ball(100, 200);

        ball.setVelocity(new utils.Velocity(0, 5));
        Paddle paddle = new Paddle(90, 210); 
        List<Block> blocks = new ArrayList<>();

        ball.setPosition(100, paddle.getY() - GameConfig.BALL_SIZE + 2);
        cmBottom.handleCollisions(ball, paddle, blocks, false);

        assertTrue(ball.getVelocity().getDy() < 0, "Bóng nên bật lên trên khi va chạm với paddle dưới");
        assertEquals(paddle.getY() - GameConfig.BALL_SIZE - 1, ball.getY(),
                "Bóng nên được đặt ngay trên paddle sau va chạm");
    }

    @Test
    void topPaddleBounceDown() {
        Ball ball = new Ball(100, 60);
        ball.setVelocity(new utils.Velocity(0, -5)); 
        Paddle top = new Paddle(90, 30, false); 
        List<Block> blocks = new ArrayList<>();

        ball.setPosition(100, top.getY() + top.getHeight() - 2);
        cmTop.handleCollisions(ball, top, blocks, true);

        assertTrue(ball.getVelocity().getDy() > 0, "Bóng nên bật xuống dưới khi va chạm với paddle trên");
        assertEquals(top.getY() + top.getHeight() + 1, ball.getY(),
                "Bóng nên được đặt ngay dưới paddle sau va chạm");
    }

    @Test
    void blockCollisionDamagesAndBounces() {
        Ball ball = new Ball(50, 50);
        ball.setVelocity(new utils.Velocity(5, 0)); 
        Block block = new Block(70, 50, 1);
        List<Block> blocks = new ArrayList<>();
        blocks.add(block);


    ball.setPosition(40, 50);
        ball.move(); 
        ball.setPosition(69, 50); 

        double beforeDx = ball.getVelocity().getDx();
        double beforeDy = ball.getVelocity().getDy();
        cmBottom.handleCollisions(ball, new Paddle(0, 9999), blocks, false);

        assertTrue(block.isDestroyed(), "Gạch nên bị phá hủy sau khi trúng (hits=1)");
        boolean velocityChanged = (Math.abs(ball.getVelocity().getDx() - beforeDx) > 1e-9)
                || (Math.abs(ball.getVelocity().getDy() - beforeDy) > 1e-9);
        assertTrue(velocityChanged, "Vận tốc bóng nên thay đổi sau khi trúng gạch");
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


        ball.setPosition(109, 50);
        cmBottom.handleCollisions(ball, new Paddle(0, 9999), blocks, false);

        cmBottom.handleCollisions(ball, new Paddle(0, 9999), blocks, false);

        int destroyedCount = (b1.isDestroyed() ? 1 : 0) + (b2.isDestroyed() ? 1 : 0);
        assertEquals(1, destroyedCount, "Chỉ một viên gạch nên được xử lý mỗi khung hình do cooldown");

        cmBottom.tickCooldown();
        cmBottom.tickCooldown();

        ball.setPosition(119, 50);
        cmBottom.handleCollisions(ball, new Paddle(0, 9999), blocks, false);
        destroyedCount = (b1.isDestroyed() ? 1 : 0) + (b2.isDestroyed() ? 1 : 0);
        assertEquals(2, destroyedCount, "Viên gạch thứ hai có thể bị trúng sau khi cooldown");
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
