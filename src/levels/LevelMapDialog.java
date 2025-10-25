package levels;

import entities.Block;
import utils.GameConfig;

import javax.swing.*;
import java.awt.*;
 
/**
 * Dialog/helper to preview a level map. Extracted from GamePanel.showLevelMap.
 */
public class LevelMapDialog {

    /**
     * Show a dialog that prompts for a level number and displays a preview of that level's blocks.
     * The method is modal and returns after the dialog is closed.
     */
    public static void showLevelMap(Component parent) {
        // Prompt for level
        String input = JOptionPane.showInputDialog(
            parent,
            "Nhập số level (1-" + GameConfig.MAX_LEVELS + "):",
            "Xem Map Level",
            JOptionPane.QUESTION_MESSAGE
        );

        if (input == null) return; // User cancelled

        try {
            int levelNum = Integer.parseInt(input.trim());
            if (levelNum < 1 || levelNum > GameConfig.MAX_LEVELS) {
                JOptionPane.showMessageDialog(
                    parent,
                    "Level phải từ 1 đến " + GameConfig.MAX_LEVELS,
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // Create blocks for the requested level
            java.util.List<Block> previewBlocks = LevelBuilder.createLevel(levelNum);

            // Build and show dialog
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
            JOptionPane.showMessageDialog(
                parent,
                "Vui lòng nhập số hợp lệ!",
                "Lỗi",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
