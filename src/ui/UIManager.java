package ui;

import function.Pause;
import function.SaveController;
import game.GamePanel;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * UIManager skeleton. Draw HUD and overlays here (score, lives, pause menu).
 * Also provides small helpers for creating overlay buttons and dialogs so views
 * such as GamePanel can remain thin.
 */
public class UIManager {
    public void renderOverlay(Graphics2D g) { }

    /**
     * Create and attach a Save button to the given GamePanel. The onSaved consumer
     * is invoked with true when a save occurred, false when cancelled or failed.
     */
    public StyledButton createSaveButton(GamePanel panel, Consumer<Boolean> onSaved) {
        final StyledButton saveButton = new StyledButton("Save");
        saveButton.setFont(saveButton.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        saveButton.setToolTipText("Lưu game");

        final int btnW = 90, btnH = 34;
        final int rightMargin = 100;
        final int topMargin = 10;
        int baseW = panel.getWidth() > 0 ? panel.getWidth() : panel.getWidth();
        int x = Math.max(0, baseW - btnW - rightMargin);
        saveButton.setBounds(x, topMargin, btnW, btnH);

        panel.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int newX = panel.getWidth() - btnW - rightMargin;
                saveButton.setLocation(Math.max(0, newX), topMargin);
            }
        });

        SwingUtilities.invokeLater(() -> {
            int newX = panel.getWidth() - btnW - rightMargin;
            saveButton.setLocation(Math.max(0, newX), topMargin);
        });

        saveButton.addActionListener(ev -> {
            boolean saved = false;
            try {
                // Pause the game while the save dialog is open. If the user cancels
                // we'll resume; if they choose "Lưu và thoát" we leave it paused so
                // the caller can perform stop/exit behavior.
                try { Pause.getInstance().pause(); } catch (Throwable ignored) {}

                saved = SaveController.promptAndSave(panel, panel);
            } catch (Throwable t) {
                // swallow: SaveController will show its own dialogs
            } finally {
                if (!saved) {
                    // user cancelled or save failed -> resume gameplay
                    try { Pause.getInstance().resume(); } catch (Throwable ignored) {}
                }
            }
            if (onSaved != null) onSaved.accept(saved);
        });

        panel.add(saveButton);
        return saveButton;
    }

    public int showConfirm(Component parent, String title, String message, int optionType) {
        return JOptionPane.showConfirmDialog(parent, message, title, optionType, JOptionPane.QUESTION_MESSAGE);
    }

    public String promptInput(Component parent, String title, String message) {
        return JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE);
    }

    public void showMessage(Component parent, String title, String message, int messageType) {
        JOptionPane.showMessageDialog(parent, message, title, messageType);
    }
}
