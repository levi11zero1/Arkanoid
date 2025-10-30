package function;

import game.GamePanel;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import java.util.List;
import java.nio.file.Files;

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
        String name = JOptionPane.showInputDialog(parent, "Nhập tên bản lưu:", "Lưu game", JOptionPane.PLAIN_MESSAGE);
        if (name == null) return false; // cancelled
        name = name.trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Tên bản lưu không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            SaveManager.Metadata meta = new SaveManager.Metadata(panel.getRunStats().player, panel.getRunStats().elapsedMs, panel.getRunStats().levels, panel.getRunStats().blocks);
            SaveManager.save(panel.toGameState(), name, meta);
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Lưu game thất bại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
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
            panel.applyGameState(state);
            if (meta != null) {
                panel.setPlayerRunInfo(meta.player, meta.elapsedMs, meta.levelsCompleted, meta.blocksDestroyed);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Không thể tải bản lưu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
