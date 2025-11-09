package input;

import java.awt.event.KeyEvent;
import game.GamePanel;

/**
 * InputHandler: xử lý sự kiện bàn phím cho GamePanel.
 */
public class InputHandler {
    public void keyPressed(KeyEvent e, GamePanel panel) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> panel.setLeftPressed(true);
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> panel.setRightPressed(true);
            case KeyEvent.VK_P -> {
                // toggle pause via panel helper
                panel.togglePauseAction();
            }
            case KeyEvent.VK_SPACE -> panel.startIfNotRunning();
            case KeyEvent.VK_R -> panel.playSkillMusic();
            case KeyEvent.VK_ESCAPE -> {
                int choice = panel.confirm("Quit Game", "Are you sure you want to quit?", javax.swing.JOptionPane.YES_NO_OPTION);
                if (choice == javax.swing.JOptionPane.YES_OPTION) System.exit(0);
            }
            // 'M' key (level map preview) removed - no-op to avoid dev-only UI in runtime
        }
    }

    public void keyReleased(KeyEvent e, GamePanel panel) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> panel.setLeftPressed(false);
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> panel.setRightPressed(false);
        }
    }
}
