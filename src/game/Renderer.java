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
    public void render(Graphics2D g, Ball ball, Paddle paddle, List<Block> blocks, List<PowerUp> powerUps, LevelManager levelManager) {
        
    g.setColor(Color.WHITE);
    g.setFont(new Font("Arial", Font.BOLD, 16));
    g.drawString("Level: " + levelManager.getCurrentLevel(), 10, 25);
    long remainingBlocks = blocks.stream().filter(block -> !block.isDestroyed()).count();.
    int blocksTextX = GameConfig.SCREEN_WIDTH - 170;
    int blocksBoxY = 8;
    int blocksBoxW = 160;
    int blocksBoxH = 22;
    java.awt.Composite old = g.getComposite();
    g.setColor(new java.awt.Color(0, 0, 0, 160));
    g.fillRoundRect(blocksTextX - 6, blocksBoxY, blocksBoxW, blocksBoxH, 6, 6);
    g.setComposite(old);
    g.setColor(Color.WHITE);
    g.drawString("Blocks: " + remainingBlocks, blocksTextX, 25);
        if (ball != null) ball.draw(g);
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
