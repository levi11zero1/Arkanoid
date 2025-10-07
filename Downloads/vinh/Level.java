public class Level {
    private int currentLevel = 1;
    private final int maxLevel = 2;

    public int getCurrentLevel() {
        return currentLevel;
    }

    public boolean advanceLevel() {
        if (currentLevel < maxLevel) {
            currentLevel++;
            return true;
        }
        return false;
    }

    public boolean isFinalLevel() {
        return currentLevel == maxLevel;
    }
}