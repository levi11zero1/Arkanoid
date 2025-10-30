package function;

/**
 * Lightweight mutable container representing a single play session's run stats.
 * This is intentionally small and serializable-to-file is delegated to RankingManager.
 */
public class GameSession {
    private String playerName;
    private long elapsedMs;
    private int levelsCompleted;
    private int totalBlocksDestroyed;
    private boolean submitted;

    public GameSession(String playerName, long elapsedMs, int levelsCompleted, int totalBlocksDestroyed) {
        this.playerName = playerName == null ? "Player" : playerName;
        this.elapsedMs = Math.max(0, elapsedMs);
        this.levelsCompleted = Math.max(0, levelsCompleted);
        this.totalBlocksDestroyed = Math.max(0, totalBlocksDestroyed);
        this.submitted = false;
    }

    public String getPlayerName() { return playerName; }
    public long getElapsedMs() { return elapsedMs; }
    public int getLevelsCompleted() { return levelsCompleted; }
    public int getTotalBlocksDestroyed() { return totalBlocksDestroyed; }

    public void setPlayerName(String playerName) { if (playerName != null && !playerName.isBlank()) this.playerName = playerName; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = Math.max(0, elapsedMs); }
    public void setLevelsCompleted(int levelsCompleted) { this.levelsCompleted = Math.max(0, levelsCompleted); }
    public void setTotalBlocksDestroyed(int totalBlocksDestroyed) { this.totalBlocksDestroyed = Math.max(0, totalBlocksDestroyed); }

    public boolean isSubmitted() { return submitted; }
    public void markSubmitted() { this.submitted = true; }
    public void resetSubmitted() { this.submitted = false; }
}
