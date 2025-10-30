package game;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import entities.Ball;
import entities.Paddle;
import entities.Block;
import powerup.PowerUp;
import levels.LevelManager;
import utils.GameConfig;

/**
 * Renderer implementation responsible for drawing the game world.
 */
public class Renderer implements IRenderer {
    @Override
    public void render(Graphics2D g, Ball ball, Paddle paddle, List<Block> blocks, List<PowerUp> powerUps, LevelManager levelManager) {
        // Draw HUD info
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Level: " + levelManager.getCurrentLevel(), 10, 25);

        long remainingBlocks = blocks.stream().filter(block -> !block.isDestroyed()).count();
        g.drawString("Blocks: " + remainingBlocks, GameConfig.SCREEN_WIDTH - 100, 25);

        // Draw entities
        if (ball != null) ball.draw(g);
        if (paddle != null) paddle.draw(g);
        if (blocks != null) {
            for (Block block : blocks) {
                block.draw(g);
            }
        }

        // Draw powerups
        if (powerUps != null) {
            for (PowerUp p : powerUps) {
                g.setColor(p.getColor());
                g.fillRect(p.getX(), p.getY(), p.getWidth(), p.getHeight());
            }
        }
    }
}
