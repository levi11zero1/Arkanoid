package game;

import java.awt.Graphics2D;
import entities.Ball;
import entities.Paddle;
import entities.Block;
import java.util.List;
import powerup.PowerUp;
import levels.LevelManager;

/**
 * Renderer interface for drawing the game scene.
 */
public interface IRenderer {
    void render(Graphics2D g, List<Ball> balls, Paddle paddle, List<Block> blocks, List<PowerUp> powerUps, LevelManager levelManager);
}
