package game;

import javax.swing.*;
import utils.GameConfig;

public class GameFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    
    public GameFrame() {
        initializeFrame();
        setupGamePanel();
    }
    
    private void initializeFrame() {
        setTitle("Arkanoid");
        setSize(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        
    }
    
    private void setupGamePanel() {
        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        
        gamePanel.setFocusable(true);
        gamePanel.requestFocusInWindow();
    }
}