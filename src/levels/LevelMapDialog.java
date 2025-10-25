package levels;

import entities.Block;
import utils.GameConfig;

import javax.swing.*;
import java.awt.*;
 
/**
 * Dialog hiển thị bản đồ (map) của một level.
 * Được tách ra khỏi GamePanel để tái sử dụng và giữ code gọn.
 */
public class LevelMapDialog {

    /**
     * Hiển thị dialog modal để người dùng nhập số level và xem preview.
     */
    public static void showLevelMap(Component parent) {
        String input = JOptionPane.showInputDialog(
            parent,
            "Nhập số level (1-" + GameConfig.MAX_LEVELS + "):",
            "Xem Map Level",
            JOptionPane.QUESTION_MESSAGE
        );

        if (input == null) return;

        try {
            int levelNum = Integer.parseInt(input.trim());
            if (levelNum < 1 || levelNum > GameConfig.MAX_LEVELS) {
                JOptionPane.showMessageDialog(parent, "Level phải từ 1 đến " + GameConfig.MAX_LEVELS, "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            java.util.List<Block> previewBlocks = LevelBuilder.createLevel(levelNum);

            JDialog mapDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(parent), "Map Level " + levelNum, true);

            JPanel mapPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    for (Block block : previewBlocks) {
                        block.draw(g);
                    }
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 16));
                    g.drawString("Level " + levelNum + " Preview", 10, 25);
                    g.drawString("Total Blocks: " + previewBlocks.size(), 10, 45);
                }
            };

            mapPanel.setPreferredSize(new Dimension(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT));
            mapPanel.setBackground(Color.BLACK);

            mapDialog.add(mapPanel);
            mapDialog.pack();
            mapDialog.setLocationRelativeTo(parent);
            mapDialog.setVisible(true);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(parent, "Vui lòng nhập số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
