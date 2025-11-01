package levels;

import utils.GameConfig;

public class LevelManager {
    private int currentLevel = 0;

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
    currentLevel = 0;
    }
    
    public int getMaxLevels() {
        return GameConfig.MAX_LEVELS;
    }
    // Cho phép đặt hoặc thiết lập level trực tiếp khi LOAD save hoặc phím tắt.
    // Dùng bởi GamePanel.applyGameState(...) để khớp màn hiện tại với dữ liệu đã lưu,
    // và bởi các phím tắt để nhảy trực tiếp.
    public void setLevel(int level) {
    if (level < 0) level = 0;
        if (level > GameConfig.MAX_LEVELS) level = GameConfig.MAX_LEVELS;
        this.currentLevel = level;
    }

    // Backwards-compatible alias used by other code (setCurrentLevel)
    public void setCurrentLevel(int level) {
        setLevel(level);
    }
}