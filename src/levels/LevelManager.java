package levels;

import utils.GameConfig;

public class LevelManager {
    private int currentLevel = 1;

    public int getCurrentLevel() {
        return currentLevel;
    }

    public boolean advanceLevel() {
        if (currentLevel < GameConfig.MAX_LEVELS) {
            currentLevel++;
            return true;
        }
        return false;
    }

    public boolean isFinalLevel() {
        return currentLevel == GameConfig.MAX_LEVELS;
    }

    public void reset() {
        currentLevel = 1;
    }
    
    public int getMaxLevels() {
        return GameConfig.MAX_LEVELS;
    }

    // Cho phép đặt level trực tiếp khi LOAD save.
    // Dùng bởi GamePanel.applyGameState(...) để khớp màn hiện tại với dữ liệu đã lưu.
    public void setLevel(int level) {
        if (level < 1) level = 1;
        if (level > GameConfig.MAX_LEVELS) level = GameConfig.MAX_LEVELS;
        this.currentLevel = level;
    }
}