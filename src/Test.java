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
    System.out.println("Enter level number to view map (0-" + GameConfig.MAX_LEVELS + "):");
        System.out.print("Level: ");
        
        try {
            int levelNum = scanner.nextInt();
            
            if (levelNum < 0 || levelNum > GameConfig.MAX_LEVELS) {
                System.err.println("Error: Level must be from 0 to " + GameConfig.MAX_LEVELS);
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
                
                // Try to show images for bricks (fallback to colored rects if missing)
                java.awt.Image img1 = loadLegendImage("images/Brick1_4.png");
                java.awt.Image img2 = loadLegendImage("images/Brick2_4.png");
                java.awt.Image img3 = loadLegendImage("images/Brick3_4.png");
                java.awt.Image img9 = loadLegendImage("images/Brick9_4.png");

                int bw = GameConfig.BLOCK_WIDTH > 0 ? Math.min(GameConfig.BLOCK_WIDTH, 60) : 30;
                int bh = GameConfig.BLOCK_HEIGHT > 0 ? Math.min(GameConfig.BLOCK_HEIGHT, 30) : 15;

                // 1-hit
                if (img1 != null) g.drawImage(img1, legendX, legendY + 10, bw, bh, null);
                else {
                    g.setColor(Color.RED);
                    g.fillRect(legendX, legendY + 10, bw, bh);
                }
                g.setColor(Color.WHITE);
                g.drawString("= 1 hit", legendX + bw + 10, legendY + 10 + bh - 2);

                // 2-hit
                if (img2 != null) g.drawImage(img2, legendX, legendY + 10 + bh + 10, bw, bh, null);
                else {
                    g.setColor(Color.ORANGE);
                    g.fillRect(legendX, legendY + 10 + bh + 10, bw, bh);
                }
                g.setColor(Color.WHITE);
                g.drawString("= 2 hits", legendX + bw + 10, legendY + 10 + bh + 10 + bh - 2);

                // 3-hit
                if (img3 != null) g.drawImage(img3, legendX, legendY + 10 + (bh + 10) * 2, bw, bh, null);
                else {
                    g.setColor(Color.MAGENTA);
                    g.fillRect(legendX, legendY + 10 + (bh + 10) * 2, bw, bh);
                }
                g.setColor(Color.WHITE);
                g.drawString("= 3 hits", legendX + bw + 10, legendY + 10 + (bh + 10) * 2 + bh - 2);

                // Undestructable
                if (img9 != null) g.drawImage(img9, legendX, legendY + 10 + (bh + 10) * 3, bw, bh, null);
                else {
                    g.setColor(Color.WHITE);
                    g.fillRect(legendX, legendY + 10 + (bh + 10) * 3, bw, bh);
                    g.setColor(Color.BLACK);
                    g.drawRect(legendX, legendY + 10 + (bh + 10) * 3, bw, bh);
                }
                g.setColor(Color.WHITE);
                g.drawString("= Undestructable", legendX + bw + 10, legendY + 10 + (bh + 10) * 3 + bh - 2);
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

    // Load image helper used by the legend. Tries classpath resources first, then file system.
    private static java.awt.Image loadLegendImage(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            java.net.URL res = Test.class.getClassLoader().getResource(path);
            if (res != null) return new javax.swing.ImageIcon(res).getImage();
            java.io.File f = new java.io.File(path);
            if (f.exists()) return javax.imageio.ImageIO.read(f);
        } catch (Exception ignored) {
        }
        return null;
    }
}
