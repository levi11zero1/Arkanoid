package game;

import entities.Ball;
import entities.Block;
import entities.Paddle;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;
import levels.LevelManager;
import powerup.PowerUp;
import utils.GameConfig;

public class Renderer implements IRenderer {
    @Override
    public void render(Graphics2D g, List<Ball> balls, Paddle paddle, List<Block> blocks, List<PowerUp> powerUps, LevelManager levelManager) {

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Level: " + levelManager.getCurrentLevel(), 10, 25);
    long remainingBlocks = blocks.stream()
        .filter(block -> block.getHitsRemaining() != utils.GameConfig.UNDESTRUCTABLE_BLOCK)
        .filter(block -> !block.isDestroyed())
        .count();
        g.drawString("Blocks: " + remainingBlocks, GameConfig.SCREEN_WIDTH - 100, 25);
        if (balls != null) {
            for (Ball b : balls) {
                if (b != null) {
                    b.draw(g);
                }
            }
        }
        if (paddle != null) paddle.draw(g);
        if (blocks != null) {
            for (Block block : blocks) {
                block.draw(g);
            }
        }

        if (powerUps != null) {
            for (PowerUp p : powerUps) {
                g.setColor(p.getColor());
                g.fillRect(p.getX(), p.getY(), p.getWidth(), p.getHeight());
            }
        }
    }
}
