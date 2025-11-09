package function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import function.RankingManager;

public class RankingManagerTest {

    private Path savesDir;
    private Path rankingFile;

    @BeforeEach
    void setup() throws IOException {
        savesDir = Path.of("saves");
        rankingFile = savesDir.resolve("ranking.csv");
        if (Files.exists(rankingFile)) {
            Files.delete(rankingFile);
        }
        if (!Files.exists(savesDir)) {
            Files.createDirectories(savesDir);
        }
    }

    @Test
    void addEntryCreatesFile() throws IOException {
        RankingManager.addEntry("PlayerA", 2, 10, 5000);
        assertTrue(Files.exists(rankingFile));
        List<RankingManager.Entry> list = RankingManager.loadAll();
        assertEquals(1, list.size());
        assertEquals("PlayerA", list.get(0).player);
    }

    @Test
    void escapeCommaInName() throws IOException {
        RankingManager.addEntry("Alice,Bob", 3, 11, 4000);
        List<RankingManager.Entry> list = RankingManager.loadAll();
        assertEquals(1, list.size());
        assertEquals("Alice,Bob", list.get(0).player);
    }

    @Test
    void escapeQuoteInName() throws IOException {
        RankingManager.addEntry("He says \"Hi\"", 1, 5, 2000);
        List<RankingManager.Entry> list = RankingManager.loadAll();
        assertEquals(1, list.size());
        assertEquals("He says \"Hi\"", list.get(0).player);
    }

    @Test
    void sortingOrderLevelsBlocksElapsed() throws IOException, InterruptedException {
        RankingManager.addEntry("P1", 2, 10, 5000);
        Thread.sleep(10);
        RankingManager.addEntry("P2", 3, 5, 6000); // higher level should rank above
        Thread.sleep(10);
        RankingManager.addEntry("P3", 3, 7, 8000); // same level, higher blocks than P2
        List<RankingManager.Entry> sorted = RankingManager.getSorted(10);
        assertEquals("P3", sorted.get(0).player);
        assertEquals("P2", sorted.get(1).player);
        assertEquals("P1", sorted.get(2).player);
    }

    @Test
    void pruneKeepsTopN() throws IOException {
        for (int i=0;i<7;i++) {
            RankingManager.addEntry("P"+i, i, i*2, 1000 + i);
        }
        RankingManager.pruneToTop(3);
        List<RankingManager.Entry> list = RankingManager.loadAll();
        assertEquals(3, list.size());
        // highest levels should remain
        List<RankingManager.Entry> sorted = RankingManager.getSorted(3);
        assertEquals("P6", sorted.get(0).player);
        assertEquals("P5", sorted.get(1).player);
        assertEquals("P4", sorted.get(2).player);
    }
}
