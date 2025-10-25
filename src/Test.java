import entities.Block;
import java.awt.*;
import java.util.List;
import java.util.Scanner;
import javax.swing.*;
import levels.LevelBuilder;
import utils.GameConfig;

/**
 * Test class để xem map của level
 * Nhập số level từ console và hiển thị map trong cửa sổ
 */
public class Test {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== ARKANOID LEVEL MAP VIEWER ===");
        System.out.println("Enter level number to view map (1-" + GameConfig.MAX_LEVELS + "):");
        System.out.print("Level: ");
        
        try {
            int levelNum = scanner.nextInt();
            
            if (levelNum < 1 || levelNum > GameConfig.MAX_LEVELS) {
                System.err.println("Error: Level must be from 1 to " + GameConfig.MAX_LEVELS);
                return;
            }
            
            System.out.println("\nCreating map for Level " + levelNum + "...");
            
            // Tạo blocks cho level
            List<Block> blocks = LevelBuilder.createLevel(levelNum);
            
            System.out.println("Total blocks: " + blocks.size());
            
            // Đếm số gạch theo loại
            int count1Hit = 0, count2Hit = 0, count3Hit = 0, countUndestructable = 0;
            for (Block block : blocks) {
                int hits = block.getHitsRemaining();
                if (hits == 1) count1Hit++;
                else if (hits == 2) count2Hit++;
                else if (hits == 3) count3Hit++;
                else if (hits == GameConfig.UNDESTRUCTABLE_BLOCK) countUndestructable++;
            }
            
            System.out.println("  - 1 hit blocks (Red): " + count1Hit);
            System.out.println("  - 2 hit blocks (Orange): " + count2Hit);
            System.out.println("  - 3 hit blocks (Magenta): " + count3Hit);
            System.out.println("  - Undestructable blocks (White): " + countUndestructable);
            System.out.println("\nDisplaying map...\n");
            
            // Hiển thị map trong GUI
            showMapGUI(levelNum, blocks);
            
        } catch (Exception e) {
            System.err.println("Error: Please enter a valid number!");
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    private static void showMapGUI(int levelNum, List<Block> blocks) {
        // Tạo JFrame
        JFrame frame = new JFrame("Level " + levelNum + " Map Preview");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Tạo panel để vẽ map
        JPanel mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                
                // Nền đen
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
                
                // Vẽ các block
                for (Block block : blocks) {
                    block.draw(g);
                }
                
                // Vẽ thông tin
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                g.drawString("Level " + levelNum + " Map", 10, 25);
                
                g.setFont(new Font("Arial", Font.PLAIN, 16));
                g.drawString("Total Blocks: " + blocks.size(), 10, 50);
                
                // Legend
                g.setFont(new Font("Arial", Font.BOLD, 14));
                int legendX = GameConfig.SCREEN_WIDTH - 200;
                int legendY = 30;
                
                g.drawString("LEGEND:", legendX, legendY);
                
                // Gạch đỏ - 1 hit
                g.setColor(Color.RED);
                g.fillRect(legendX, legendY + 10, 30, 15);
                g.setColor(Color.WHITE);
                g.drawString("= 1 hit", legendX + 40, legendY + 23);
                
                // Gạch cam - 2 hits
                g.setColor(Color.ORANGE);
                g.fillRect(legendX, legendY + 35, 30, 15);
                g.setColor(Color.WHITE);
                g.drawString("= 2 hits", legendX + 40, legendY + 48);
                
                // Gạch tím - 3 hits
                g.setColor(Color.MAGENTA);
                g.fillRect(legendX, legendY + 60, 30, 15);
                g.setColor(Color.WHITE);
                g.drawString("= 3 hits", legendX + 40, legendY + 73);
                
                // Gạch trắng - Undestructable
                g.setColor(Color.WHITE);
                g.fillRect(legendX, legendY + 85, 30, 15);
                g.setColor(Color.BLACK);
                g.drawRect(legendX, legendY + 85, 30, 15);
                g.setColor(Color.WHITE);
                g.drawString("= Undestructable", legendX + 40, legendY + 98);
            }
        };
        
        mapPanel.setPreferredSize(new Dimension(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT));
        mapPanel.setBackground(Color.BLACK);
        
        frame.add(mapPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        System.out.println("Map window opened!");
        System.out.println("Close the window to exit.");
    }
}
