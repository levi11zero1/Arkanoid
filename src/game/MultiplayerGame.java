package game;

import javax.swing.*;

// nếu muốn chơi nhiều người thì chạy file này
public class MultiplayerGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Arkanoid - Multiplayer");
            MultiplayerPanel panel = new MultiplayerPanel();
            frame.add(panel);
            frame.setSize(utils.GameConfig.SCREEN_WIDTH, utils.GameConfig.SCREEN_HEIGHT);
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.requestFocusInWindow();
        });
    }
}
