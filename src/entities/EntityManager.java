package entities;

import java.util.List;
import java.util.ArrayList;


/**
 * Kiểm soát các thực thể trong trò chơi: bóng, thanh trượt, và các khối.
 * 
 */
public class EntityManager {
    private Ball ball;
    private Paddle paddle;
    private List<Block> blocks = new ArrayList<>();

    public EntityManager() { }

    public void setEntities(Ball ball, Paddle paddle, List<Block> blocks) {
        this.ball = ball;
        this.paddle = paddle;
        this.blocks = blocks;
    }

    /** Cập nhật tất cả các thực thể sau mỗi tick. */
    public void updateAll(double deltaSeconds, int panelWidth, int panelHeight, boolean leftPressed, boolean rightPressed) {
        if (ball != null) {
            ball.move();
            ball.checkBounds(panelWidth, panelHeight);
        }
        if (paddle != null) {
            paddle.update(leftPressed, rightPressed, panelWidth, deltaSeconds);
        }
    }

    public Ball getBall() { return ball; }
    public Paddle getPaddle() { return paddle; }
    public List<Block> getBlocks() { return blocks; }

    public void addBlock(Block b) { if (blocks != null) blocks.add(b); }
    public void removeBlock(Block b) { if (blocks != null) blocks.remove(b); }
}

