package function;

import java.io.IOException;

/**
 * Update điểm số.
 */
public class ScoreManager {

    /**
     * cập nhật phiên chơi đến bảng xếp hạng. 
     */
    public void submitIfNotSubmitted(GameSession session) {
        if (session == null) return;
        if (session.isSubmitted()) return;

        session.markSubmitted();
        try {
            RankingManager.addEntry(session.getPlayerName(), session.getLevelsCompleted(), session.getTotalBlocksDestroyed(), session.getElapsedMs());
        } catch (IOException ignored) {

        }
    }
}
