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
}