package levels;

import entities.Block;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import utils.GameConfig;


public final class LevelPreview {
    private LevelPreview() {
    }

  
    public static void showMapInteractive() {
        String s = JOptionPane.showInputDialog(null, "Enter level number (0.." + GameConfig.MAX_LEVELS + "):", "Show Level", JOptionPane.QUESTION_MESSAGE);
        if (s == null) return; // cancelled
        try {
            int lvl = Integer.parseInt(s.trim());
            showMap(lvl);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Invalid level number: " + s, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

   
    public static void showMap(int level) {
        List<Block> blocks = LevelBuilder.createLevel(level);
        Image bg = LevelBackgrounds.getForLevel(level);

        JFrame f = new JFrame("Level Preview - " + level);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // draw background stretched to panel
                if (bg != null) {
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
                }
                // draw blocks
                for (Block b : blocks) {
                    b.draw(g);
                }
            }
        };
        panel.setPreferredSize(new Dimension(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT));

        f.setContentPane(panel);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
