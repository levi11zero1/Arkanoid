package entities;

import java.awt.Color;
import java.awt.Graphics;
import utils.GameConfig;

public class Block {
    private int x, y;
    private boolean destroyed = false;
    private int hitsRemaining;


    public Block(int x, int y, int hitsRemaining) {
        this.x = x; 
        this.y = y;
        this.hitsRemaining = Math.max(1, hitsRemaining);
    }

    // Set màu các block khác nhàu 
    public void draw(Graphics g) {
        if (!destroyed) {
            Color color = switch (hitsRemaining) {
                case 3 -> Color.MAGENTA; 
                case 2 -> Color.ORANGE; 
                default -> Color.RED;
            };
            
            g.setColor(color);
            g.fillRect(x, y, GameConfig.BLOCK_WIDTH, GameConfig.BLOCK_HEIGHT);
            
            g.setColor(Color.BLACK);
            g.drawRect(x, y, GameConfig.BLOCK_WIDTH, GameConfig.BLOCK_HEIGHT);
        }
    }

    public boolean isHit(int ballX, int ballY, int ballSize) {
        if (!destroyed && 
            ballX + ballSize > x && ballX < x + GameConfig.BLOCK_WIDTH && 
            ballY + ballSize > y && ballY < y + GameConfig.BLOCK_HEIGHT) {
            
            hitsRemaining--;
            if (hitsRemaining <= 0) {
                destroyed = true;
            }
            return true;
        }
        return false;
    }
    
    // Check va chạm như paddle
    public String getCollisionSide(double ballX, double ballY, double ballSize, double ballVx, double ballVy) {
        if (destroyed) return null;
        
        double ballCenterX = ballX + ballSize / 2;
        double ballCenterY = ballY + ballSize / 2;
        
        double leftDistance = Math.abs(ballCenterX - x);
        double rightDistance = Math.abs(ballCenterX - (x + GameConfig.BLOCK_WIDTH));
        double topDistance = Math.abs(ballCenterY - y);
        double bottomDistance = Math.abs(ballCenterY - (y + GameConfig.BLOCK_HEIGHT));
        
        double minHorizontal = Math.min(leftDistance, rightDistance);
        double minVertical = Math.min(topDistance, bottomDistance);
        
        if (minHorizontal < minVertical) {
            return (ballVx > 0) ? "left" : "right";
        } else {
            return (ballVy > 0) ? "top" : "bottom";
        }
    }

    public boolean isDestroyed() {
        return destroyed;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getHitsRemaining() {
        return hitsRemaining;
    }
}