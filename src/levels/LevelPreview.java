package levels;

import entities.Block;
import utils.GameConfig;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Small utility class to show a modal preview of a level.
 * Kept as a separate class to keep LevelBuilder focused on building data.
 */
public class LevelPreview {

    /**
     * Show a modal preview dialog for the given level.
     * Safe to call from Swing EDT.
     */
    public static void show(Component parent, int levelNum) {
    if (levelNum < 0 || levelNum > GameConfig.MAX_LEVELS) return;
        List<Block> previewBlocks = LevelBuilder.createLevel(levelNum);

        Window owner = SwingUtilities.getWindowAncestor(parent);
        Frame ownerFrame = owner instanceof Frame ? (Frame) owner : null;
        JDialog mapDialog = new JDialog(ownerFrame, "Map Level " + levelNum, true);

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
    }
}
