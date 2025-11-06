package function;

import game.GamePanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import ui.StyledButton;

/**
 * UI and orchestration helpers for saving and loading games.
 * Keeps GamePanel thin by moving dialogs and calls to SaveManager here.
 */
public final class SaveController {
    private SaveController() {}

    /**
     * Prompt the user for a save name and save the given panel's state.
     * Returns true if a save was performed, false if cancelled.
     */
    public static boolean promptAndSave(Component parent, GamePanel panel) {
        if (panel == null) return false;
        final String[] result = { null };

        // Build a custom modal dialog with nicer styled buttons
        java.awt.Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog;
        if (owner instanceof java.awt.Frame) {
            dialog = new JDialog((java.awt.Frame) owner, "Lưu game", true);
        } else if (owner instanceof java.awt.Dialog) {
            dialog = new JDialog((java.awt.Dialog) owner, "Lưu game", true);
        } else {
            dialog = new JDialog((java.awt.Frame) null, "Lưu game", true);
        }
        JPanel content = new JPanel(new BorderLayout(8,8));
        content.setBorder(javax.swing.BorderFactory.createEmptyBorder(12,12,12,12));

        JLabel lbl = new JLabel("Nhập tên bản lưu:");
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));

        JTextField tf = new JTextField();
        tf.setColumns(24);

        JPanel center = new JPanel(new BorderLayout(6,6));
        center.add(lbl, BorderLayout.NORTH);
        center.add(tf, BorderLayout.CENTER);

        StyledButton saveExit = new StyledButton("Lưu và thoát");
        StyledButton cancel = new StyledButton("Hủy");
        saveExit.setPreferredSize(new Dimension(160, 40));
        cancel.setPreferredSize(new Dimension(120, 40));
        saveExit.setFont(saveExit.getFont().deriveFont(Font.BOLD, 14f));
        cancel.setFont(cancel.getFont().deriveFont(Font.PLAIN, 14f));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.add(cancel);
        bottom.add(saveExit);

        content.add(center, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);

        // Actions
        saveExit.addActionListener(ev -> {
            String name = tf.getText();
            if (name == null) name = "";
            name = name.trim();
            if (name.isEmpty()) {
                // simple inline feedback: focus field
                tf.requestFocusInWindow();
                return;
            }
            try {
                SaveManager.Metadata meta = new SaveManager.Metadata(panel.getRunStats().player, panel.getRunStats().elapsedMs, panel.getRunStats().levels, panel.getRunStats().blocks, panel.getLives());
                SaveManager.save(panel.toGameState(), name, meta);
                result[0] = name;
                dialog.dispose();
            } catch (IOException ex) {
                javax.swing.JOptionPane.showMessageDialog(parent, "Lưu game thất bại: " + ex.getMessage(), "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });

        cancel.addActionListener(ev -> {
            result[0] = null;
            dialog.dispose();
        });

        tf.addActionListener(ev -> saveExit.doClick());

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parent);
        tf.requestFocusInWindow();
        dialog.setVisible(true);

        return result[0] != null;
    }

    /**
     * List available save files. Delegates to SaveManager.
     */
    public static List<Path> listSaves() throws IOException {
        return SaveManager.listSaves();
    }

    /**
     * Delete the given save file. Returns true if deleted or false otherwise.
     */
    public static boolean deleteSave(Path file) throws IOException {
        if (file == null) return false;
        return Files.deleteIfExists(file);
    }

    /**
     * Load the specified save file and apply it to the given panel. Shows dialogs on error.
     */
    public static void loadAndApply(Component parent, GamePanel panel, Path file) {
        if (panel == null || file == null) return;
        try {
            GameState state = SaveManager.load(file);
            SaveManager.Metadata meta = SaveManager.readMetadata(file);
            if (meta != null && meta.lives >= 0) {
                LifeManager.setLives(meta.lives);
            }
            panel.applyGameState(state);
            if (meta != null) {
                panel.setPlayerRunInfo(meta.player, meta.elapsedMs, meta.levelsCompleted, meta.blocksDestroyed);
            }
            // The selected save is meant as a one-time resume; delete it after successful load
            try {
                Files.deleteIfExists(file);
            } catch (Exception ignored) {
                // ignore deletion failures (won't block resume)
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Không thể tải bản lưu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
