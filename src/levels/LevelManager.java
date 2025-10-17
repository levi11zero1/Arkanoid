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

    // Allow direct jump to a specific level (1..MAX_LEVELS)
    public void setCurrentLevel(int level) {
        if (level < 1) level = 1;
        if (level > GameConfig.MAX_LEVELS) level = GameConfig.MAX_LEVELS;
        currentLevel = level;
    }
}